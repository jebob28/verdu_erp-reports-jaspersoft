package reports.verdu_erp.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import io.minio.Result;
import io.minio.messages.Item;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import reports.verdu_erp.dto.ReportInfoDTO;
import reports.verdu_erp.dto.ReportRequestDTO;
import reports.verdu_erp.dto.ReportParameterCreateDTO;
import reports.verdu_erp.dto.ReportParameterDTO;
import reports.verdu_erp.dto.ReportUploadResponseDTO;
import reports.verdu_erp.dto.ReportUploadSimpleDTO;
import reports.verdu_erp.dto.ReportWithParametersDTO;
import reports.verdu_erp.enums.ParameterType;
import reports.verdu_erp.service.ServiceReports;

import java.util.List;

/**
 * Controller para gerenciamento de relatórios
 */
@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
@Tag(name = "Relatórios", description = "APIs para gerenciamento e geração de relatórios JasperReports")
public class ReportsController {
    
    @Autowired
    private ServiceReports serviceReports;
    
    /**
     * Endpoint de teste para verificar se a API está funcionando
     */
    @Operation(
        summary = "Teste de conectividade",
        description = "Endpoint simples para testar se a API está respondendo corretamente"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "API funcionando corretamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"status\": \"OK\", \"message\": \"API de relatórios funcionando\", \"timestamp\": \"2024-01-15T10:30:00Z\"}"
                )
            )
        )
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = Map.of(
            "status", "OK",
            "message", "API de relatórios funcionando",
            "timestamp", java.time.Instant.now().toString(),
            "note", "Para usar as funcionalidades completas, configure o MinIO em localhost:9000"
        );
        return ResponseEntity.ok(response);
    }
    
    /**
     * Rota para importar/upload de relatórios para o MinIO
     */
    @Operation(
        summary = "Importar relatório",
        description = "Faz upload de um arquivo de relatório (.jasper ou .jrxml) para o bucket MinIO"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Relatório importado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ReportUploadResponseDTO.class),
                examples = @ExampleObject(
                    value = "{\"message\": \"Relatório enviado com sucesso\", \"reportName\": \"relatorio_vendas.jrxml\", \"success\": true}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Erro na requisição - arquivo inválido ou parâmetros incorretos",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ReportUploadResponseDTO.class),
                examples = @ExampleObject(
                    value = "{\"message\": \"Arquivo deve ser .jasper ou .jrxml\", \"reportName\": null, \"success\": false}"
                )
            )
        )
    })
    @PostMapping( value= "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReportUploadResponseDTO> importReport(
            @Parameter(description = "Arquivo do relatório (.jasper ou .jrxml)", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Nome do relatório no bucket", required = true, example = "relatorio_vendas.jrxml")
            @RequestParam("reportName") String reportName,
            @Parameter(description = "Código único do relatório", required = false, example = "REL001")
            @RequestParam(value = "codigo", required = false) String codigo,
            @Parameter(description = "Descrição do relatório", required = false, example = "Relatório de vendas mensais")
            @RequestParam(value = "descricao", required = false) String descricao,
            @Parameter(
                description = "Parâmetros do relatório (lista/objeto/mapa)",
                required = false,
                content = @Content(
                    mediaType = "application/json",
                    examples = {
                        @ExampleObject(
                            name = "Lista minimalista",
                            value = "[{\n  \"nome\": \"dataInicio\", \"tipo\": \"DATE\"\n}, {\n  \"nome\": \"dataFim\", \"tipo\": \"DATE\"\n}]"
                        ),
                        @ExampleObject(
                            name = "Objeto único minimalista",
                            value = "{\n  \"nome\": \"ROTA_ID\",\n  \"tipo\": \"INTEGER\"\n}"
                        ),
                        @ExampleObject(
                            name = "Mapa simples (nome → tipo)",
                            value = "{\n  \"dataInicio\": \"DATE\",\n  \"dataFim\": \"DATE\"\n}"
                        )
                    }
                )
            )
            @RequestPart(value = "parametros", required = false) String parametrosRaw) {
        
        try {
            // Validações básicas
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ReportUploadResponseDTO.error("Arquivo não pode estar vazio"));
            }
            
            if (!isValidReportFile(file.getOriginalFilename())) {
                return ResponseEntity.badRequest()
                    .body(ReportUploadResponseDTO.error("Arquivo deve ser .jasper ou .jrxml"));
            }
            
            // Preparar parâmetros flexíveis (lista, objeto único ou mapa)
            List<ReportParameterCreateDTO> parametros = parseFlexibleParameters(parametrosRaw, codigo, reportName);

            // Faz upload do relatório
            serviceReports.uploadReport(reportName, file, codigo, descricao, (parametros == null || parametros.isEmpty()) ? null : parametros);
            
            return ResponseEntity.ok(ReportUploadResponseDTO.success(reportName));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ReportUploadResponseDTO.error("Erro ao importar relatório: " + e.getMessage()));
        }
    }

    private List<ReportParameterCreateDTO> parseFlexibleParameters(String parametrosRaw, String codigo, String reportName) {
        try {
            if (parametrosRaw == null || parametrosRaw.trim().isEmpty()) return null;

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(parametrosRaw);

            List<ReportParameterCreateDTO> result = new java.util.ArrayList<>();

            if (root.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode node : root) {
                    ReportParameterCreateDTO dto = convertNodeToDTO(node, codigo, reportName);
                    if (dto != null) result.add(dto);
                }
                return result;
            }

            if (root.isObject()) {
                // Caso seja um objeto no formato DTO ou um mapa simples nome->valor
                boolean looksLikeDTO = root.has("parameterName") || root.has("nome");
                if (looksLikeDTO) {
                    ReportParameterCreateDTO dto = convertNodeToDTO(root, codigo, reportName);
                    if (dto != null) result.add(dto);
                } else {
                    // Tratar como mapa: cada field vira um parâmetro
                    java.util.Iterator<java.util.Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> fields = root.fields();
                    while (fields.hasNext()) {
                        java.util.Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> entry = fields.next();
                        String name = entry.getKey();
                        com.fasterxml.jackson.databind.JsonNode valueNode = entry.getValue();

                        ReportParameterCreateDTO dto = new ReportParameterCreateDTO();
                        dto.setReportCode(codigo);
                        dto.setReportName(reportName);
                        dto.setParameterName(name);
                        String inferredType = inferType(valueNode);
                        dto.setParameterType(inferredType);
                        dto.setDefaultValue(valueNode.isNull() ? null : valueNode.asText());
                        dto.setIsRequired(false);
                        dto.setDescription(null);
                        result.add(dto);
                    }
                }
                return result;
            }

            // Qualquer outro caso: tentar parse como lista tipada
            try {
                java.util.List<ReportParameterCreateDTO> list = mapper.readValue(parametrosRaw,
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.List<ReportParameterCreateDTO>>() {});
                return list;
            } catch (Exception ignore) {}

            return null;
        } catch (Exception e) {
            // Em caso de erro de parsing, retornar null para o fluxo padrão tratar
            return null;
        }
    }

    private ReportParameterCreateDTO convertNodeToDTO(com.fasterxml.jackson.databind.JsonNode node, String codigo, String reportName) {
        try {
            ReportParameterCreateDTO dto = new ReportParameterCreateDTO();
            dto.setReportCode(codigo);
            dto.setReportName(reportName);

            // Suporte a campos em inglês (DTO) e português (simple DTO)
            String name = getString(node, "parameterName");
            if (name == null) name = getString(node, "nome");
            dto.setParameterName(name);

            String type = getString(node, "parameterType");
            if (type == null) type = getString(node, "tipo");
            if (type == null) {
                // inferir a partir do valor padrão
                com.fasterxml.jackson.databind.JsonNode valNode = node.get("defaultValue");
                if (valNode == null) valNode = node.get("valorPadrao");
                type = inferType(valNode);
            }
            dto.setParameterType(type);

            String defaultValue = getString(node, "defaultValue");
            if (defaultValue == null) defaultValue = getString(node, "valorPadrao");
            dto.setDefaultValue(defaultValue);

            Boolean required = getBoolean(node, "isRequired");
            if (required == null) required = getBoolean(node, "obrigatorio");
            dto.setIsRequired(required != null ? required : false);

            String desc = getString(node, "description");
            if (desc == null) desc = getString(node, "descricao");
            dto.setDescription(desc);

            // Validação básica
            if (dto.getParameterName() == null || dto.getParameterName().trim().isEmpty()) return null;
            if (dto.getParameterType() == null || dto.getParameterType().trim().isEmpty()) dto.setParameterType("STRING");

            return dto;
        } catch (Exception e) {
            return null;
        }
    }

    private String inferType(com.fasterxml.jackson.databind.JsonNode valueNode) {
        if (valueNode == null || valueNode.isNull()) return "STRING";
        if (valueNode.isBoolean()) return "BOOLEAN";
        if (valueNode.isInt()) return "INTEGER";
        if (valueNode.isLong()) return "LONG";
        if (valueNode.isFloat() || valueNode.isDouble() || valueNode.isBigDecimal()) return "DECIMAL";

        String text = valueNode.asText();
        if (text == null) return "STRING";

        // Date and datetime heuristics
        if (text.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")) return "DATETIME";
        if (text.matches("\\d{4}-\\d{2}-\\d{2}")) return "DATE";
        if (text.matches("-?\\d+")) return "INTEGER";
        if (text.matches("-?\\d+\\.\\d+")) return "DECIMAL";
        return "STRING";
    }

    private String getString(com.fasterxml.jackson.databind.JsonNode node, String field) {
        com.fasterxml.jackson.databind.JsonNode f = node.get(field);
        return (f == null || f.isNull()) ? null : f.asText();
    }

    private Boolean getBoolean(com.fasterxml.jackson.databind.JsonNode node, String field) {
        com.fasterxml.jackson.databind.JsonNode f = node.get(field);
        return (f == null || f.isNull()) ? null : f.asBoolean();
    }
    
    /**
     * Rota genérica para visualização de relatórios em diferentes formatos
     */
    @Operation(
        summary = "Gerar relatório",
        description = "Gera um relatório em formato específico com parâmetros dinâmicos"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Relatório gerado com sucesso",
            content = @Content(
                mediaType = "application/octet-stream",
                schema = @Schema(type = "string", format = "binary")
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Relatório não encontrado"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Erro interno do servidor"
        )
    })
    @PostMapping("/generate/{reportName}")
    public ResponseEntity<byte[]> generateReport(
            @Parameter(description = "Nome do relatório no bucket", required = true, example = "relatorio_vendas.jrxml")
            @PathVariable String reportName,
            @Parameter(description = "Formato de saída do relatório", example = "pdf", schema = @Schema(allowableValues = {"pdf", "html", "csv", "xml", "xlsx"}))
            @RequestParam(defaultValue = "pdf") String format,
            @Parameter(description = "Parâmetros dinâmicos do relatório", 
                      content = @Content(examples = @ExampleObject(
                          value = "{\"dataInicio\": \"2024-01-01\", \"dataFim\": \"2024-12-31\", \"vendedorId\": 123}"
                      )))
            @RequestBody(required = false) Map<String, Object> parameters) {
        
        try {
            // Se parameters for null, cria um Map vazio
            if (parameters == null) {
                parameters = Map.of();
            }
            
            // Gera o relatório
            byte[] reportData = serviceReports.generateReport(reportName, parameters, format);
            
            // Define o content type baseado no formato
            MediaType contentType = getContentType(format);
            String filename = getFilename(reportName, format);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(contentType);
            headers.setContentDispositionFormData("attachment", filename);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(reportData);
                
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(("Erro ao gerar relatório: " + e.getMessage()).getBytes());
        }
    }
    
    /**
     * Rota alternativa usando POST com DTO
     */
    @Operation(
        summary = "Gerar relatório com DTO",
        description = "Gera um relatório usando um DTO estruturado com todos os parâmetros"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Relatório gerado com sucesso",
            content = @Content(
                mediaType = "application/octet-stream",
                schema = @Schema(type = "string", format = "binary")
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Requisição inválida - parâmetros obrigatórios ausentes"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Erro interno do servidor"
        )
    })
    @PostMapping("/generate")
    public ResponseEntity<byte[]> generateReportWithDTO(
            @Parameter(description = "Dados da requisição para geração do relatório", required = true)
            @RequestBody ReportRequestDTO request) {
        
        try {
            // Validações
            if (request.getReportName() == null || request.getReportName().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body("Nome do relatório é obrigatório".getBytes());
            }
            
            String format = request.getFormat() != null ? request.getFormat() : "pdf";
            Map<String, Object> parameters = request.getParameters() != null ? request.getParameters() : Map.of();
            
            // Gera o relatório
            byte[] reportData = serviceReports.generateReport(request.getReportName(), parameters, format);
            
            // Define o content type baseado no formato
            MediaType contentType = getContentType(format);
            String filename = getFilename(request.getReportName(), format);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(contentType);
            headers.setContentDispositionFormData("attachment", filename);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(reportData);
                
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(("Erro ao gerar relatório: " + e.getMessage()).getBytes());
        }
    }

    // ---------------------- CRUD de Parâmetros de Relatório ----------------------

    @Operation(summary = "Listar parâmetros de um relatório por código")
    @GetMapping("/{code}/parameters")
    public ResponseEntity<List<ReportParameterDTO>> listParametersByReportCode(@PathVariable String code) {
        try {
            Optional<ReportWithParametersDTO> opt = serviceReports.findReportWithParametersByCode(code);
            if (opt.isEmpty()) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(opt.get().getParameters());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Adicionar/Atualizar parâmetro de relatório")
    @PostMapping("/parameters")
    public ResponseEntity<ReportParameterDTO> upsertReportParameter(@RequestBody ReportParameterCreateDTO dto) {
        try {
            ReportParameterDTO saved = serviceReports.upsertReportParameter(dto);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Remover parâmetro de relatório por código ou nome e nome do parâmetro")
    @DeleteMapping("/parameters/{reportCodeOrName}/{name}")
    public ResponseEntity<Void> deleteReportParameter(@PathVariable String reportCodeOrName, @PathVariable String name) {
        try {
            boolean removed = serviceReports.deleteReportParameter(reportCodeOrName, name);
            if (removed) return ResponseEntity.noContent().build();
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Lista todos os relatórios disponíveis
     */
    @Operation(
        summary = "Listar relatórios",
        description = "Retorna a lista de todos os relatórios disponíveis no bucket MinIO"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de relatórios retornada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ReportInfoDTO.class),
                examples = @ExampleObject(
                    value = "[{\"name\": \"relatorio_vendas.jrxml\", \"size\": 15420, \"lastModified\": \"2024-01-15T10:30:00Z\", \"etag\": \"d41d8cd98f00b204e9800998ecf8427e\"}]"
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Erro interno do servidor"
        )
    })
    @GetMapping("/list")
    public ResponseEntity<List<ReportInfoDTO>> listReports() {
        try {
            List<ReportInfoDTO> reports = serviceReports.listReportsFromDatabase();
            return ResponseEntity.ok(reports);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ArrayList<>());
        }
    }

    /**
     * Buscar relatório por código
     */
    @Operation(
        summary = "Buscar relatório por código",
        description = "Busca um relatório específico pelo código no banco de dados"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Relatório encontrado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ReportInfoDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Relatório não encontrado"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Erro interno do servidor"
        )
    })
    @GetMapping("/search/{code}")
    public ResponseEntity<ReportInfoDTO> findReportByCode(
            @Parameter(description = "Código do relatório", required = true, example = "relatorio_vendas")
            @PathVariable String code) {
        try {
            Optional<reports.verdu_erp.entity.Report> reportOpt = serviceReports.findReportByCode(code);
            
            if (reportOpt.isPresent()) {
                reports.verdu_erp.entity.Report report = reportOpt.get();
                ReportInfoDTO reportInfo = new ReportInfoDTO(
                    report.getCodigo(),
                    report.getName(),
                    report.getFileSize(),
                    report.getCreatedAt().atZone(java.time.ZoneId.systemDefault()),
                    report.getId().toString()
                );
                return ResponseEntity.ok(reportInfo);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Buscar relatório com parâmetros por código
     */
    @Operation(
        summary = "Buscar relatório com parâmetros por código",
        description = "Busca um relatório específico pelo código no banco de dados incluindo seus parâmetros"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Relatório encontrado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ReportWithParametersDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Relatório não encontrado"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Erro interno do servidor"
        )
    })
    @GetMapping("/search/{code}/with-parameters")
    public ResponseEntity<ReportWithParametersDTO> findReportWithParametersByCode(
            @Parameter(description = "Código do relatório", required = true, example = "relatorio_vendas")
            @PathVariable String code) {
        try {
            Optional<ReportWithParametersDTO> reportOpt = serviceReports.findReportWithParametersByCode(code);
            
            if (reportOpt.isPresent()) {
                return ResponseEntity.ok(reportOpt.get());
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Remove um relatório
     */
    @Operation(
        summary = "Remover relatório",
        description = "Remove um relatório específico do bucket MinIO"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Relatório removido com sucesso",
            content = @Content(
                mediaType = "text/plain",
                examples = @ExampleObject(value = "Relatório removido com sucesso")
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Relatório não encontrado"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Erro interno do servidor"
        )
    })
    @DeleteMapping("/{reportName}")
    public ResponseEntity<String> deleteReport(
            @Parameter(description = "Nome do relatório a ser removido", required = true, example = "relatorio_vendas.jrxml")
            @PathVariable String reportName) {
        try {
            serviceReports.deleteReport(reportName);
            return ResponseEntity.ok("Relatório removido com sucesso");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erro ao remover relatório: " + e.getMessage());
        }
    }
    
    // Métodos auxiliares
    
    private boolean isValidReportFile(String filename) {
        if (filename == null) return false;
        String lowerCase = filename.toLowerCase();
        return lowerCase.endsWith(".jasper") || lowerCase.endsWith(".jrxml");
    }
    
    private MediaType getContentType(String format) {
        switch (format.toLowerCase()) {
            case "pdf":
                return MediaType.APPLICATION_PDF;
            case "html":
                return MediaType.TEXT_HTML;
            case "csv":
                return MediaType.parseMediaType("text/csv");
            case "xml":
                return MediaType.APPLICATION_XML;
            case "xlsx":
                return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            default:
                return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
    
    private String getFilename(String reportName, String format) {
        String baseName = reportName.replaceAll("\\.(jasper|jrxml)$", "");
        return baseName + "." + format.toLowerCase();
    }

    @Operation(
        summary = "Importar relatório (Simplificado)",
        description = "Endpoint simplificado para upload de relatórios com configuração dinâmica de parâmetros"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Relatório importado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ReportUploadResponseDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Erro na requisição - dados inválidos",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ReportUploadResponseDTO.class)
            )
        )
    })
    @PostMapping(value = "/import/simple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReportUploadResponseDTO> importReportSimple(
            @Parameter(description = "Arquivo do relatório (.jasper ou .jrxml)", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(
                description = "JSON do relatório (use 'data' OU 'body'). Aceita lista, objeto único ou mapa em 'parametros' com apenas nome e tipo.",
                required = false,
                content = @Content(
                    mediaType = "application/json",
                    examples = {
                        @ExampleObject(
                            name = "Lista minimalista",
                            value = "{\n  \"codigo\": \"REL_VENDAS\",\n  \"descricao\": \"Vendas por período\",\n  \"parametros\": [\n    { \"nome\": \"dataInicio\", \"tipo\": \"DATE\" },\n    { \"nome\": \"dataFim\", \"tipo\": \"DATE\" }\n  ]\n}"
                        ),
                        @ExampleObject(
                            name = "Objeto único minimalista",
                            value = "{\n  \"codigo\": \"REL_VENDAS\",\n  \"descricao\": \"Vendas por período\",\n  \"parametros\": { \"nome\": \"ROTA_ID\", \"tipo\": \"INTEGER\" }\n}"
                        ),
                        @ExampleObject(
                            name = "Mapa simples (nome → tipo)",
                            value = "{\n  \"codigo\": \"REL_VENDAS\",\n  \"descricao\": \"Vendas por período\",\n  \"parametros\": { \"dataInicio\": \"DATE\", \"dataFim\": \"DATE\" }\n}"
                        )
                    }
                )
            )
            @RequestPart(value = "data", required = false) String dataRaw,
            @Parameter(
                description = "Alias para o JSON do relatório (idêntico ao 'data'). Você pode enviar em 'body' ao invés de 'data'",
                required = false,
                content = @Content(
                    mediaType = "application/json",
                    examples = {
                        @ExampleObject(
                            name = "body: Lista minimalista",
                            value = "{\n  \"codigo\": \"REL_VENDAS\",\n  \"descricao\": \"Vendas por período\",\n  \"parametros\": [\n    { \"nome\": \"dataInicio\", \"tipo\": \"DATE\" },\n    { \"nome\": \"dataFim\", \"tipo\": \"DATE\" }\n  ]\n}"
                        ),
                        @ExampleObject(
                            name = "body: Objeto único minimalista",
                            value = "{\n  \"codigo\": \"REL_VENDAS\",\n  \"descricao\": \"Vendas por período\",\n  \"parametros\": { \"nome\": \"ROTA_ID\", \"tipo\": \"INTEGER\" }\n}"
                        ),
                        @ExampleObject(
                            name = "body: Mapa simples (nome → tipo)",
                            value = "{\n  \"codigo\": \"REL_VENDAS\",\n  \"descricao\": \"Vendas por período\",\n  \"parametros\": { \"dataInicio\": \"DATE\", \"dataFim\": \"DATE\" }\n}"
                        )
                    }
                )
            )
            @RequestPart(value = "body", required = false) String bodyRaw) {
        
        try {
            // Validar arquivo
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ReportUploadResponseDTO("Arquivo não pode estar vazio", null, false));
            }

            String filename = file.getOriginalFilename();
            if (filename == null || !isValidReportFile(filename)) {
                return ResponseEntity.badRequest()
                    .body(new ReportUploadResponseDTO("Arquivo deve ser .jasper ou .jrxml", filename, false));
            }

            // Parse do JSON flexível para aceitar apenas nome e tipo (em 'data' ou 'body')
            String raw = (dataRaw != null && !dataRaw.isBlank()) ? dataRaw : bodyRaw;
            if (raw == null || raw.isBlank()) {
                return ResponseEntity.badRequest()
                    .body(new ReportUploadResponseDTO("Informe o JSON no campo 'data' ou 'body'", filename, false));
            }

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root;
            try {
                root = mapper.readTree(raw);
            } catch (Exception e) {
                return ResponseEntity.badRequest()
                    .body(new ReportUploadResponseDTO("JSON inválido em 'data/body': " + e.getMessage(), filename, false));
            }

            String codigo = root.hasNonNull("codigo") ? root.get("codigo").asText() : null;
            String descricao = root.hasNonNull("descricao") ? root.get("descricao").asText() : null;

            List<ReportParameterCreateDTO> parametros = new ArrayList<>();
            com.fasterxml.jackson.databind.JsonNode paramsNode = root.get("parametros");
            if (paramsNode != null && !paramsNode.isNull()) {
                if (paramsNode.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode node : paramsNode) {
                        ReportParameterCreateDTO dto = convertNodeToDTO(node, codigo, filename);
                        if (dto.getParameterName() == null || dto.getParameterType() == null) {
                            return ResponseEntity.badRequest()
                                .body(new ReportUploadResponseDTO("Cada parâmetro deve conter ao menos 'nome' e 'tipo'", filename, false));
                        }
                        parametros.add(dto);
                    }
                } else if (paramsNode.isObject()) {
                    boolean looksLikeMap = true;
                    java.util.Iterator<java.util.Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> fields = paramsNode.fields();
                    java.util.List<ReportParameterCreateDTO> tmp = new java.util.ArrayList<>();
                    while (fields.hasNext()) {
                        java.util.Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> entry = fields.next();
                        String key = entry.getKey();
                        com.fasterxml.jackson.databind.JsonNode val = entry.getValue();
                        if (!val.isTextual() && !val.isObject()) {
                            looksLikeMap = false;
                            break;
                        }
                        if (val.isTextual()) {
                            // mapa simples: { "PARAM": "DATE" }
                            com.fasterxml.jackson.databind.node.ObjectNode obj = mapper.createObjectNode();
                            obj.put("nome", key);
                            obj.put("tipo", val.asText());
                            tmp.add(convertNodeToDTO(obj, codigo, filename));
                        } else {
                            // objeto completo por chave: { "PARAM": { nome, tipo, ... } }
                            com.fasterxml.jackson.databind.node.ObjectNode obj = mapper.createObjectNode();
                            obj.setAll((com.fasterxml.jackson.databind.node.ObjectNode) val);
                            if (!obj.has("nome") && !obj.has("parameterName")) obj.put("nome", key);
                            tmp.add(convertNodeToDTO(obj, codigo, filename));
                        }
                    }
                    if (looksLikeMap && !tmp.isEmpty()) {
                        parametros.addAll(tmp);
                    } else {
                        // objeto único representando um parâmetro
                        ReportParameterCreateDTO dto = convertNodeToDTO(paramsNode, codigo, filename);
                        if (dto.getParameterName() == null || dto.getParameterType() == null) {
                            return ResponseEntity.badRequest()
                                .body(new ReportUploadResponseDTO("Parâmetro deve conter ao menos 'nome' e 'tipo'", filename, false));
                        }
                        parametros.add(dto);
                    }
                } else {
                    return ResponseEntity.badRequest()
                        .body(new ReportUploadResponseDTO("Campo 'parametros' deve ser lista ou objeto", filename, false));
                }
            }

            // Defaults específicos: se nome indicar data de início/fim e tipo=DATE, apenas aceite (sem exigir extras)
            for (ReportParameterCreateDTO p : parametros) {
                if (p.getParameterType() != null && "DATE".equalsIgnoreCase(p.getParameterType())) {
                    String n = p.getParameterName() != null ? p.getParameterName().toLowerCase() : "";
                    if (n.replace(" ", "").contains("datainicio") || n.contains("inicial")) {
                        // Mantemos apenas nome e tipo; se required não definido, não forçamos
                    }
                    if (n.replace(" ", "").contains("datafim") || n.contains("final")) {
                        // Idem
                    }
                }
            }

            // Usar o serviço existente
            serviceReports.uploadReport(filename, file, codigo, descricao, parametros);
             
             ReportUploadResponseDTO response = new ReportUploadResponseDTO(
                 "Relatório enviado com sucesso", filename, true);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ReportUploadResponseDTO("Erro interno: " + e.getMessage(), null, false));
        }
    }

    @Operation(
        summary = "Listar tipos de parâmetros disponíveis",
        description = "Retorna todos os tipos de parâmetros suportados pelo sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de tipos retornada com sucesso",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "[{\"type\": \"STRING\", \"displayName\": \"Texto\", \"javaType\": \"String\", \"example\": \"Exemplo de texto\", \"defaultFormat\": null}, {\"type\": \"INTEGER\", \"displayName\": \"Número Inteiro\", \"javaType\": \"Integer\", \"example\": \"123\", \"defaultFormat\": null}]"
                )
            )
        )
    })
    @GetMapping("/parameter-types")
    public ResponseEntity<List<Map<String, Object>>> getParameterTypes() {
        List<Map<String, Object>> types = new ArrayList<>();
        
        for (ParameterType type : ParameterType.values()) {
            Map<String, Object> typeInfo = Map.of(
                "type", type.name(),
                "displayName", type.getDisplayName(),
                "javaType", type.getJavaType(),
                "example", type.getExample(),
                "defaultFormat", type.getDefaultFormat()
            );
            types.add(typeInfo);
        }
        
        return ResponseEntity.ok(types);
    }

    /**
     * Lista relatórios organizados por setor
     */
    @Operation(
        summary = "Listar relatórios por setor",
        description = "Retorna todos os relatórios organizados por setor (logística, financeiro, compras, etc.)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de relatórios por setor retornada com sucesso",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"logistica\": [\"separacao_carga.jrxml\"], \"financeiro\": [\"fluxo_caixa.jrxml\"], \"compras\": [\"pedidos_compra.jrxml\"]}"
                )
            )
        )
    })
    @GetMapping("/sectors")
    public ResponseEntity<Map<String, List<String>>> listReportsBySector() {
        try {
            Map<String, List<String>> reportsBySector = serviceReports.listReportsBySector();
            return ResponseEntity.ok(reportsBySector);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lista relatórios de um setor específico
     */
    @Operation(
        summary = "Listar relatórios de um setor específico",
        description = "Retorna todos os relatórios de um setor específico"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de relatórios do setor retornada com sucesso",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "[\"separacao_carga.jrxml\", \"controle_entregas.jrxml\"]"
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Setor não encontrado"
        )
    })
    @GetMapping("/sectors/{sector}")
    public ResponseEntity<List<String>> listReportsBySector(
            @Parameter(description = "Nome do setor", required = true, example = "logistica")
            @PathVariable String sector) {
        try {
            List<String> reports = serviceReports.listReportsBySector(sector);
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}