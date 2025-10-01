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
import reports.verdu_erp.dto.ReportUploadResponseDTO;
import reports.verdu_erp.dto.ReportParametrosRequestDTO;
import reports.verdu_erp.dto.ReportWithParametersDTO;
import reports.verdu_erp.service.ServiceReports;

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
            @Parameter(description = "Parâmetros do relatório em formato JSON", required = false, example = "{\"titulo\": \"string\", \"dataInicio\": \"date\", \"dataFim\": \"date\"}")
            @RequestParam(value = "parametros", required = false) String parametros) {
        
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
            
            // Cria DTO de parâmetros com os dados fornecidos
            ReportParametrosRequestDTO reportParametros = new ReportParametrosRequestDTO();
            reportParametros.setNome(descricao != null ? descricao : reportName);
            reportParametros.setCodigo(codigo != null ? codigo : reportName);
            reportParametros.setParametros(parametros != null ? parametros : "");
            
            // Faz upload do relatório
            serviceReports.uploadReport(reportName, file, reportParametros);
            
            return ResponseEntity.ok(ReportUploadResponseDTO.success(reportName));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ReportUploadResponseDTO.error("Erro ao importar relatório: " + e.getMessage()));
        }
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
}