package reports.verdu_erp.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleHtmlExporterOutput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import reports.verdu_erp.entity.Report;
import reports.verdu_erp.entity.ReportParameter;
import reports.verdu_erp.repository.ReportRepository;
import reports.verdu_erp.repository.ReportParameterRepository;

import reports.verdu_erp.dto.ReportWithParametersDTO;
import reports.verdu_erp.dto.ReportParameterDTO;
import reports.verdu_erp.dto.ReportParameterCreateDTO;
import reports.verdu_erp.enums.ParameterType;
import jakarta.transaction.Transactional;

@Service
public class ServiceReports {

    private static final String BUCKET_NAME = "relatorios";
    
    @Autowired
    private MinioClient minioClient;
    
    @Autowired
    private DataSource dataSource;

    @Autowired
    private ReportRepository reportRepository;
    
    @Autowired
    private ReportParameterRepository reportParameterRepository;
    
    /**
     * Cria o bucket de relatórios se não existir
     */
    public void createBucketIfNotExists() throws Exception {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(BUCKET_NAME).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET_NAME).build());
        }
    }
    
    /**
     * Faz upload de um arquivo de relatório (.jasper ou .jrxml) para o MinIO
     * e salva os metadados no banco de dados
     */
    @Transactional
    public void uploadReport(String reportName, MultipartFile file, String codigo, String descricao, List<ReportParameterCreateDTO> parametros, String setor) throws Exception {
        // Validar parâmetros antes de processar
        validateReportParameters(parametros);
        
        // Salva o arquivo no MinIO
        createBucketIfNotExists();
        
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(BUCKET_NAME)
                .object(reportName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build()
        );
        
        // Salva os metadados no banco
         Report report = new Report();
         report.setName(reportName);
         report.setDescription(descricao);
         report.setCodigo(codigo);
         report.setContentType(file.getContentType());
         report.setFileSize(file.getSize());
         if (setor != null && !setor.trim().isEmpty()) {
             report.setSector(setor);
         }
         
         // Salva o relatório
         Report savedReport = reportRepository.save(report);

         // Salva os parâmetros do relatório, se houver
         if (parametros != null && !parametros.isEmpty()) {
             for (ReportParameterCreateDTO paramDTO : parametros) {
                 // Garante que o reportCode no DTO do parâmetro corresponda ao relatório atual
                 paramDTO.setReportCode(report.getCodigo());
                 upsertReportParameter(paramDTO);
             }
         }
         

    }

    /**
     * Define/atualiza o setor de um relatório identificado por código ou nome
     */
    @Transactional
    public boolean setReportSector(String codeOrName, String setor) {
        if (codeOrName == null || codeOrName.trim().isEmpty()) return false;
        if (setor == null || setor.trim().isEmpty()) return false;

        Optional<Report> byCode = reportRepository.findByCodigo(codeOrName);
        Optional<Report> byName = Optional.empty();
        if (byCode.isEmpty()) {
            byName = reportRepository.findByNameIgnoreCase(codeOrName);
        }
        Optional<Report> target = byCode.isPresent() ? byCode : byName;
        if (target.isEmpty()) return false;

        Report r = target.get();
        r.setSector(setor);
        reportRepository.save(r);
        return true;
    }
    
    /**
     * Baixa um arquivo de relatório do MinIO
     * Tenta diferentes variações do nome do arquivo se não encontrar diretamente
     */
    public InputStream downloadReport(String reportName) throws Exception {
        // Primeiro, tenta o nome exato como fornecido
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(reportName)
                    .build()
            );
        } catch (Exception e) {
            // Se não encontrou, tenta adicionar extensões comuns
            String[] extensions = {".jasper", ".jrxml"};
            
            for (String ext : extensions) {
                if (!reportName.endsWith(ext)) {
                    try {
                        String nameWithExt = reportName + ext;
                        return minioClient.getObject(
                            GetObjectArgs.builder()
                                .bucket(BUCKET_NAME)
                                .object(nameWithExt)
                                .build()
                        );
                    } catch (Exception ignored) {
                        // Continua tentando outras extensões
                    }
                }
            }
            
            // Se ainda não encontrou, tenta buscar por código no banco de dados
            try {
                Optional<Report> reportOpt = reportRepository.findByCodigo(reportName);
                if (reportOpt.isPresent()) {
                    String actualName = reportOpt.get().getName();
                    return minioClient.getObject(
                        GetObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(actualName)
                            .build()
                    );
                }
            } catch (Exception ignored) {
                // Se falhar, relança a exceção original
            }
            
            // Se nada funcionou, relança a exceção original
            throw e;
        }
    }
    
    /**
     * Busca relatório por código
     */
    public Optional<Report> findReportByCode(String code) {
        return reportRepository.findByCodigo(code);
    }
    
    /**
     * Lista todos os relatórios do banco de dados
     */
    public List<Report> listAllReports() {
        return reportRepository.findAllOrderByCreatedAtDesc();
    }
    
    /**
     * Lista apenas os nomes dos relatórios
     */
    public List<String> listReportNames() {
        return reportRepository.findAllReportNames();
    }
    
    /**
     * Lista relatórios do banco de dados com informações completas
     */
    public List<reports.verdu_erp.dto.ReportInfoDTO> listReportsFromDatabase() {
        System.out.println("[DEBUG] Iniciando listReportsFromDatabase");
        
        try {
            List<Report> reports = reportRepository.findAll();
            System.out.println("[DEBUG] Encontrados " + reports.size() + " relatórios no banco");
            
            List<reports.verdu_erp.dto.ReportInfoDTO> reportInfos = new ArrayList<>();
            
            for (Report report : reports) {
                System.out.println("[DEBUG] Processando relatório: " + report.getName() + ", ID: " + report.getId());
                
                reports.verdu_erp.dto.ReportInfoDTO reportInfo = new reports.verdu_erp.dto.ReportInfoDTO(
                        report.getCodigo(),
                        report.getName(),
                    report.getFileSize() != null ? report.getFileSize() : 0L,
                    report.getCreatedAt() != null ? report.getCreatedAt().atZone(java.time.ZoneId.systemDefault()) : java.time.ZonedDateTime.now(),
                    report.getId().toString()
                );
                reportInfos.add(reportInfo);
            }
            
            System.out.println("[DEBUG] Retornando " + reportInfos.size() + " relatórios");
            return reportInfos;
        } catch (Exception e) {
            System.out.println("[ERROR] Erro ao listar relatórios: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erro ao listar relatórios", e);
        }
    }
    
    /**
     * Gera relatório em formato específico
     */
    public byte[] generateReport(String reportName, Map<String, Object> parameters, String format) throws Exception {
        JasperPrint jasperPrint = prepareJasperPrint(reportName, parameters);

        // Exporta no formato solicitado
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        switch (format.toLowerCase()) {
            case "pdf":
                // Configurações para melhor suporte a fontes no PDF
                System.setProperty("net.sf.jasperreports.awt.ignore.missing.font", "true");
                System.setProperty("net.sf.jasperreports.default.font.name", "DejaVu Sans");
                System.setProperty("net.sf.jasperreports.default.pdf.font.name", "DejaVu Sans");
                System.setProperty("net.sf.jasperreports.default.pdf.encoding", "UTF-8");
                System.setProperty("net.sf.jasperreports.default.pdf.embedded", "true");

                JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream);
                break;
            case "html":
                HtmlExporter htmlExporter = new HtmlExporter();
                htmlExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                htmlExporter.setExporterOutput(new SimpleHtmlExporterOutput(outputStream));
                htmlExporter.exportReport();
                break;
            case "csv":
                JRCsvExporter csvExporter = new JRCsvExporter();
                csvExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                csvExporter.setExporterOutput(new SimpleWriterExporterOutput(outputStream));
                csvExporter.exportReport();
                break;
            case "xml":
                JasperExportManager.exportReportToXmlStream(jasperPrint, outputStream);
                break;
            case "xlsx":
                JRXlsxExporter xlsxExporter = new JRXlsxExporter();
                xlsxExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                xlsxExporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
                xlsxExporter.exportReport();
                break;
            default:
                throw new IllegalArgumentException("Formato não suportado: " + format);
        }

        return outputStream.toByteArray();
    }

    /**
     * Prepara e preenche um JasperPrint reutilizável, aplicando normalização e alinhamento de tipos.
     */
    public JasperPrint prepareJasperPrint(String reportName, Map<String, Object> parameters) throws Exception {
        System.out.println("[DEBUG] Iniciando geração do relatório: " + reportName);
        System.out.println("[DEBUG] Parâmetros recebidos: " + parameters);

        // Baixa o arquivo do relatório do MinIO
        InputStream reportStream = downloadReport(reportName);

        // Compila o relatório se for .jrxml
        JasperReport jasperReport;
        if (reportName.endsWith(".jrxml")) {
            jasperReport = JasperCompileManager.compileReport(reportStream);
        } else {
            jasperReport = (JasperReport) JRLoader.loadObject(reportStream);
        }

        System.out.println("[DEBUG] Relatório compilado/carregado com sucesso");

        // Aplica conversões automáticas SEMPRE para parâmetros conhecidos
        if (parameters != null) {
            Map<String, Object> normalized = new java.util.HashMap<>(parameters);
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                // Conversão automática para parâmetros que terminam com _ID
                if (key.endsWith("_ID") && value instanceof Number) {
                    normalized.put(key, ((Number) value).longValue());
                    System.out.println("[DEBUG] Conversão automática aplicada: " + key + " -> Long(" + ((Number) value).longValue() + ")");
                }
            }
            parameters = normalized;
        }

        // Normaliza/Converte tipos de parâmetros conforme definição salva (se existir)
        try {
            Optional<Report> reportOpt = reportRepository.findByNameWithParameters(reportName);
            if (reportOpt.isPresent()) {
                Report reportDef = reportOpt.get();
                System.out.println("[DEBUG] Definição do relatório encontrada no banco");
                if (reportDef.getParameters() != null && !reportDef.getParameters().isEmpty() && parameters != null) {
                    System.out.println("[DEBUG] Iniciando normalização de parâmetros");
                    Map<String, Object> normalized = new java.util.HashMap<>(parameters);
                    for (ReportParameter def : reportDef.getParameters()) {
                        String key = def.getParameterName();
                        System.out.println("[DEBUG] Processando parâmetro: " + key + " (Tipo: " + def.getParameterType() + ")");
                        if (normalized.containsKey(key)) {
                            Object val = normalized.get(key);
                            // Pula conversão se já foi convertido automaticamente para _ID
                            if (!key.endsWith("_ID")) {
                                Object conv = convertParameterValue(def.getParameterType(), val);
                                normalized.put(key, conv);
                            }
                        } else if (def.getDefaultValue() != null && !def.getDefaultValue().isEmpty()) {
                            // usa default salvo convertendo
                            Object conv = convertParameterValue(def.getParameterType(), def.getDefaultValue());
                            normalized.put(key, conv);
                        } else if (Boolean.TRUE.equals(def.getIsRequired())) {
                            throw new IllegalArgumentException("Parâmetro obrigatório ausente: " + key);
                        }
                    }
                    parameters = normalized;
                    System.out.println("[DEBUG] Parâmetros normalizados: " + parameters);
                }
            } else {
                System.out.println("[DEBUG] Nenhuma definição de parâmetros encontrada no banco");
            }
        } catch (Exception e) {
            // Mantém geração mesmo se normalização falhar, mas registra
            System.out.println("[WARN] Falha ao normalizar parâmetros: " + e.getMessage());
            e.printStackTrace();
        }

        // Ajuste final: alinhar tipos com JRXML (JasperReport) quando possível
        try {
            Map<String, Class<?>> jrParamTypes = extractJasperParameterTypes(jasperReport);
            if (parameters != null && !parameters.isEmpty() && jrParamTypes != null && !jrParamTypes.isEmpty()) {
                Map<String, Object> aligned = new java.util.HashMap<>(parameters);
                for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                    String key = entry.getKey();
                    Object val = entry.getValue();
                    Class<?> target = jrParamTypes.get(key);
                    if (target != null) {
                        Object conv = convertToTargetClass(target, val);
                        aligned.put(key, conv);
                        System.out.println("[DEBUG] Alinhado pelo JRXML: " + key + " -> " + target.getName() + " (classe original: " + (val != null ? val.getClass().getName() : "null") + ")");
                    }
                }
                parameters = aligned;
                System.out.println("[DEBUG] Parâmetros alinhados ao JRXML: " + parameters);
            }
        } catch (Exception e2) {
            System.out.println("[WARN] Falha ao alinhar tipos com JRXML: " + e2.getMessage());
            e2.printStackTrace();
        }

        System.out.println("[DEBUG] Iniciando preenchimento do relatório com JasperFillManager");
        System.out.println("[DEBUG] Parâmetros finais para JasperFillManager: " + parameters);

        // Preenche o relatório com dados
        Connection connection = dataSource.getConnection();
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, connection);
        connection.close();

        System.out.println("[DEBUG] Relatório preenchido com sucesso");

        return jasperPrint;
    }

    /**
     * Imprime diretamente no serviço de impressão padrão do servidor sem diálogo.
     */
    public void printReport(String reportName, Map<String, Object> parameters) throws Exception {
        JasperPrint jasperPrint = prepareJasperPrint(reportName, parameters);
        // false -> sem diálogo
        JasperPrintManager.printReport(jasperPrint, false);
    }

    /**
     * Extrai o mapa nome->classe dos parâmetros definidos no JasperReport (JRXML).
     */
    private Map<String, Class<?>> extractJasperParameterTypes(JasperReport jasperReport) {
        Map<String, Class<?>> map = new java.util.HashMap<>();
        if (jasperReport == null) return map;
        for (JRParameter p : jasperReport.getParameters()) {
            try {
                if (p != null && !p.isSystemDefined() && p.getName() != null && p.getValueClass() != null) {
                    map.put(p.getName(), p.getValueClass());
                }
            } catch (Exception ignored) {
            }
        }
        return map;
    }

    /**
     * Converte um valor para a classe alvo informada, com suporte básico a coleções.
     */
    private Object convertToTargetClass(Class<?> target, Object value) {
        if (value == null || target == null) return value;
        try {
            // Se já é instância da classe alvo, mantém
            if (target.isInstance(value)) return value;

            // Se alvo é Collection, garante que valor seja uma Collection/Lista
            if (java.util.Collection.class.isAssignableFrom(target)) {
                if (value instanceof java.util.Collection<?>) return value;
                if (value.getClass().isArray()) {
                    int length = java.lang.reflect.Array.getLength(value);
                    java.util.List<Object> converted = new java.util.ArrayList<>(length);
                    for (int i = 0; i < length; i++) {
                        converted.add(java.lang.reflect.Array.get(value, i));
                    }
                    return converted;
                }
                // valor único vira lista com 1 elemento
                return java.util.List.of(value);
            }

            // Mapeia classe para nossa string de tipo e reutiliza convertSingleValue
            String type;
            if (target.equals(Integer.class) || "int".equals(target.getName())) {
                type = "Integer";
            } else if (target.equals(Long.class) || "long".equals(target.getName())) {
                type = "Long";
            } else if (target.equals(Double.class) || "double".equals(target.getName())) {
                type = "Double";
            } else if (target.equals(java.math.BigDecimal.class)) {
                type = "BigDecimal";
            } else if (target.equals(Boolean.class) || "boolean".equals(target.getName())) {
                type = "Boolean";
            } else if (target.equals(java.util.Date.class)) {
                type = "DATE";
            } else if (target.equals(java.sql.Date.class)) {
                type = "DATE";
            } else if (target.equals(java.sql.Timestamp.class)) {
                type = "DATETIME";
            } else if (target.equals(String.class)) {
                type = "String";
            } else {
                // Classe desconhecida: retorna como string (fail-soft)
                return value.toString();
            }

            return convertSingleValue(type, value);
        } catch (Exception e) {
            System.out.println("[WARN] Falha ao converter para classe alvo '" + target.getName() + "' valor '" + value + "': " + e.getMessage());
            return value;
        }
    }

    /**
     * Converte um valor para o tipo desejado baseado em string parameterType.
     */
    private Object convertParameterValue(String parameterType, Object value) {
        if (value == null) return null;
        String type = parameterType != null ? parameterType.trim() : "String";

        System.out.println("[DEBUG] Convertendo parâmetro - Tipo: " + type + ", Valor: " + value + " (Classe: " + value.getClass().getSimpleName() + ")");

        // Suporte para listas: se o valor for Collection ou array, converte cada item
        if (value instanceof java.util.Collection<?>) {
            java.util.List<Object> converted = new java.util.ArrayList<>();
            for (Object item : (java.util.Collection<?>) value) {
                converted.add(convertSingleValue(type, item));
            }
            return converted;
        }
        if (value.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(value);
            java.util.List<Object> converted = new java.util.ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                Object item = java.lang.reflect.Array.get(value, i);
                converted.add(convertSingleValue(type, item));
            }
            return converted;
        }

        return convertSingleValue(type, value);
    }

    private Object convertSingleValue(String type, Object value) {
        try {
            Object result;
            switch (type) {
                case "Integer":
                case "INTEGER":
                    if (value instanceof Number) {
                        result = ((Number) value).intValue();
                    } else {
                        result = Integer.valueOf(value.toString());
                    }
                    break;
                case "Long":
                case "LONG":
                case "java.lang.Long":
                    if (value instanceof Number) {
                        result = ((Number) value).longValue();
                    } else {
                        result = Long.valueOf(value.toString());
                    }
                    break;
                case "Double":
                case "DOUBLE":
                    if (value instanceof Number) {
                        result = ((Number) value).doubleValue();
                    } else {
                        result = Double.valueOf(value.toString());
                    }
                    break;
                case "BigDecimal":
                case "DECIMAL":
                    if (value instanceof java.math.BigDecimal) {
                        result = value;
                    } else {
                        result = new java.math.BigDecimal(value.toString());
                    }
                    break;
                case "Boolean":
                case "BOOLEAN":
                    if (value instanceof Boolean) {
                        result = value;
                    } else {
                        String s = value.toString().toLowerCase();
                        result = ("true".equals(s) || "1".equals(s) || "sim".equals(s));
                    }
                    break;
                case "Date":
                case "DATE":
                    if (value instanceof java.util.Date) {
                        result = value;
                    } else {
                        java.time.LocalDate ld = java.time.LocalDate.parse(value.toString());
                        result = java.sql.Date.valueOf(ld);
                    }
                    break;
                case "Timestamp":
                case "DATETIME":
                    if (value instanceof java.util.Date) {
                        result = new java.sql.Timestamp(((java.util.Date) value).getTime());
                    } else {
                        java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(value.toString());
                        result = java.sql.Timestamp.valueOf(ldt);
                    }
                    break;
                case "String":
                case "STRING":
                    result = value.toString();
                    break;
                default:
                    result = value.toString();
                    break;
            }
            System.out.println("[DEBUG] Conversão bem-sucedida - Resultado: " + result + " (Classe: " + result.getClass().getSimpleName() + ")");
            return result;
        } catch (Exception e) {
            System.out.println("[WARN] Conversão falhou para tipo " + type + " e valor " + value + ": " + e.getMessage());
            return value.toString();
        }
    }

    // ---------------------- CRUD de Parâmetros de Relatório ----------------------

    @Transactional
    public ReportParameterDTO upsertReportParameter(ReportParameterCreateDTO dto) {
        if (dto.getParameterName() == null || dto.getParameterName().trim().isEmpty()) {
            throw new IllegalArgumentException("parameterName é obrigatório");
        }
        if (dto.getParameterType() == null || dto.getParameterType().trim().isEmpty()) {
            throw new IllegalArgumentException("parameterType é obrigatório");
        }

        Optional<Report> reportOpt = Optional.empty();
        if (dto.getReportCode() != null && !dto.getReportCode().trim().isEmpty()) {
            reportOpt = reportRepository.findByCodigo(dto.getReportCode());
        } else if (dto.getReportName() != null && !dto.getReportName().trim().isEmpty()) {
            reportOpt = reportRepository.findByName(dto.getReportName());
        } else {
            throw new IllegalArgumentException("Informe reportCode ou reportName");
        }

        if (reportOpt.isEmpty()) {
            throw new IllegalArgumentException("Relatório não encontrado");
        }

        Report report = reportOpt.get();

        ReportParameter entity = reportParameterRepository.findByReportIdAndParameterName(report.getId(), dto.getParameterName());
        if (entity == null) {
            entity = new ReportParameter();
            entity.setReport(report);
            entity.setParameterName(dto.getParameterName());
        }

        entity.setParameterType(dto.getParameterType());
        entity.setDefaultValue(dto.getDefaultValue());
        entity.setIsRequired(Boolean.TRUE.equals(dto.getIsRequired()));
        entity.setDescription(dto.getDescription());
        // Propagar metadata JSON (string) se fornecido
        // Validar e normalizar metadata como JSON válido
        String metadata = dto.getMetadata();
        if (metadata != null && !metadata.trim().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode node;
                // Se metadata vier como objeto JSON já serializado, valida parse
                node = mapper.readTree(metadata.trim());
                // Normaliza para string compacta
                entity.setMetadata(mapper.writeValueAsString(node));
            } catch (Exception e) {
                throw new IllegalArgumentException("Metadata inválida: deve ser JSON válido. Erro: " + e.getMessage());
            }
        } else {
            entity.setMetadata(null);
        }

        ReportParameter saved = reportParameterRepository.save(entity);
        ReportParameterDTO resultDto = new ReportParameterDTO(
            saved.getId(),
            saved.getParameterName(),
            saved.getParameterType(),
            saved.getDefaultValue(),
            saved.getIsRequired(),
            saved.getDescription(),
            saved.getCreatedAt()
        );
        resultDto.setMetadata(saved.getMetadata());
        return resultDto;
    }

    @Transactional
    public boolean deleteReportParameter(String reportCodeOrName, String parameterName) {
        if (parameterName == null || parameterName.trim().isEmpty()) return false;
        Optional<Report> reportOpt = reportRepository.findByCodigo(reportCodeOrName);
        if (reportOpt.isEmpty()) {
            reportOpt = reportRepository.findByName(reportCodeOrName);
        }
        if (reportOpt.isEmpty()) return false;
        Report report = reportOpt.get();
        ReportParameter existing = reportParameterRepository.findByReportIdAndParameterName(report.getId(), parameterName);
        if (existing == null) return false;
        reportParameterRepository.deleteByReportIdAndParameterName(report.getId(), parameterName);
        return true;
    }
    
    /**
     * Lista todos os relatórios disponíveis no bucket (para compatibilidade)
     */
    public Iterable<Result<Item>> listReportsFromMinIO() throws Exception {
        try {
            createBucketIfNotExists();
            return minioClient.listObjects(
                ListObjectsArgs.builder()
                    .bucket(BUCKET_NAME)
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Erro ao conectar com MinIO. Verifique se o serviço está rodando em localhost:9000. Erro: " + e.getMessage(), e);
        }
    }
    
    /**
     * Remove um relatório do bucket e do banco
     */
    @Transactional
    public void deleteReport(String reportName) throws Exception {
        // Remove do MinIO
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(BUCKET_NAME)
                .object(reportName)
                .build()
        );
        
        // Remove do banco
        Optional<Report> report = reportRepository.findByName(reportName);
        if (report.isPresent()) {
            reportParameterRepository.deleteByReportId(report.get().getId());
            reportRepository.delete(report.get());
        }
    }
    
    /**
     * Busca relatório com parâmetros por código
     */
    public Optional<ReportWithParametersDTO> findReportWithParametersByCode(String code) {
        try {
            System.out.println("[DEBUG] Buscando relatório com código: " + code);
            String normalizedCode = normalizeCode(code);
            Optional<Report> reportOpt = reportRepository.findByCodigoIgnoreCase(normalizedCode);
            
            if (!reportOpt.isPresent()) {
                System.out.println("[DEBUG] Relatório não encontrado com código: " + code);
                return Optional.empty();
            }
            
            System.out.println("[DEBUG] Relatório encontrado: " + reportOpt.get().getName());
            
            // Se encontrou o relatório, busca novamente com parâmetros carregados
            // Busca novamente com parâmetros carregados, normalizando nome
            String name = reportOpt.get().getName();
            Optional<Report> byName = reportRepository.findByNameWithParameters(name);
            if (!byName.isPresent()) {
                byName = reportRepository.findByNameIgnoreCase(name);
                if (byName.isPresent()) {
                    // reforça fetch dos parâmetros
                    byName = reportRepository.findByNameWithParameters(byName.get().getName());
                }
            }
            reportOpt = byName;
            
            if (reportOpt.isPresent()) {
            Report report = reportOpt.get();
            
            // Converte parâmetros para DTO
            List<ReportParameterDTO> parameterDTOs = new ArrayList<>();
            if (report.getParameters() != null) {
                for (ReportParameter param : report.getParameters()) {
                    ReportParameterDTO paramDTO = new ReportParameterDTO(
                        param.getId(),
                        param.getParameterName(),
                        param.getParameterType(),
                        param.getDefaultValue(),
                        param.getIsRequired(),
                        param.getDescription(),
                        param.getCreatedAt()
                    );
                    paramDTO.setMetadata(param.getMetadata());
                    parameterDTOs.add(paramDTO);
                }
            }
            
            // Cria o DTO do relatório com parâmetros
            ReportWithParametersDTO reportDTO = new ReportWithParametersDTO(
                report.getId(),
                report.getName(),
                report.getCodigo(),
                report.getDescription(),
                report.getFileSize(),
                report.getContentType(),
                report.getCreatedAt(),
                report.getUpdatedAt(),
                parameterDTOs
            );
            
            return Optional.of(reportDTO);
             }
             
             return Optional.empty();
             
        } catch (Exception e) {
            System.out.println("[DEBUG] Erro ao buscar relatório com parâmetros: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
     }

    private String normalizeCode(String code) {
        if (code == null) return null;
        // remove extensão caso venha com .jasper/.jrxml, remove espaços e normaliza acentos
        String withoutExt = code.replaceAll("\\.(jasper|jrxml)$", "");
        String trimmed = withoutExt.trim();
        // normaliza acentos
        String normalized = java.text.Normalizer.normalize(trimmed, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized;
    }

    /**
     * Valida os parâmetros de um relatório antes de salvá-los
     */
    public void validateReportParameters(List<ReportParameterCreateDTO> parametros) throws IllegalArgumentException {
        if (parametros == null || parametros.isEmpty()) {
            return; // Sem parâmetros para validar
        }

        for (ReportParameterCreateDTO param : parametros) {
            // Validar nome do parâmetro
            if (param.getParameterName() == null || param.getParameterName().trim().isEmpty()) {
                throw new IllegalArgumentException("Nome do parâmetro não pode estar vazio");
            }

            // Validar tipo do parâmetro
            if (param.getParameterType() == null || param.getParameterType().trim().isEmpty()) {
                throw new IllegalArgumentException("Tipo do parâmetro '" + param.getParameterName() + "' não pode estar vazio");
            }

            // Verificar se o tipo é válido
            try {
                ParameterType.valueOf(param.getParameterType().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Tipo de parâmetro inválido: '" + param.getParameterType() + 
                    "' para o parâmetro '" + param.getParameterName() + "'. Tipos válidos: " + 
                    java.util.Arrays.toString(ParameterType.values()));
            }

            // Validar valor padrão se fornecido
            if (param.getDefaultValue() != null && !param.getDefaultValue().trim().isEmpty()) {
                try {
                    ParameterType paramType = ParameterType.valueOf(param.getParameterType().toUpperCase());
                    if (!paramType.isValidValue(param.getDefaultValue())) {
                        throw new IllegalArgumentException("Valor padrão inválido '" + param.getDefaultValue() + 
                            "' para o parâmetro '" + param.getParameterName() + "' do tipo " + paramType.getDisplayName());
                    }
                } catch (Exception e) {
                    throw new IllegalArgumentException("Erro ao validar valor padrão do parâmetro '" + 
                        param.getParameterName() + "': " + e.getMessage());
                }
            }
        }
    }

    /**
     * Lista relatórios organizados por setor
     */
    public Map<String, List<reports.verdu_erp.dto.ReportInfoDTO>> listReportsBySector() {
        Map<String, List<reports.verdu_erp.dto.ReportInfoDTO>> bySector = new java.util.HashMap<>();

        List<Report> reports = reportRepository.findAllOrderByCreatedAtDesc();
        for (Report r : reports) {
            String sector = (r.getSector() != null && !r.getSector().trim().isEmpty()) ? r.getSector() : inferSector(r);
            if (sector == null || sector.trim().isEmpty()) sector = "SEM_SETOR";
            List<reports.verdu_erp.dto.ReportInfoDTO> arr = bySector.computeIfAbsent(sector, k -> new ArrayList<>());
            arr.add(new reports.verdu_erp.dto.ReportInfoDTO(
                r.getCodigo(),
                r.getName(),
                r.getFileSize() != null ? r.getFileSize() : 0L,
                r.getCreatedAt() != null ? r.getCreatedAt().atZone(java.time.ZoneId.systemDefault()) : java.time.ZonedDateTime.now(),
                r.getId().toString(),
                sector
            ));
        }
        return bySector;
    }

    private String inferSector(Report r) {
        // Estratégia: tenta pelo código prefixado, senão por nome contendo pista, senão geral
        String codigo = r.getCodigo() != null ? r.getCodigo().toLowerCase() : "";
        String name = r.getName() != null ? r.getName().toLowerCase() : "";
        String desc = r.getDescription() != null ? r.getDescription().toLowerCase() : "";
        if (codigo.startsWith("log_")) return "logistica";
        if (codigo.startsWith("fin_")) return "financeiro";
        if (codigo.startsWith("cmp_")) return "compras";
        if (codigo.startsWith("com_")) return "comercial";
        if (codigo.startsWith("est_")) return "estoque";
        // pistas por nome/descrição
        if (name.contains("logistica") || desc.contains("logistica")) return "logistica";
        if (name.contains("financeiro") || desc.contains("financeiro")) return "financeiro";
        if (name.contains("compras") || desc.contains("compras")) return "compras";
        if (name.contains("comercial") || desc.contains("comercial")) return "comercial";
        if (name.contains("estoque") || desc.contains("estoque")) return "estoque";
        return "geral";
    }

    /**
     * Lista relatórios de um setor específico
     */
    public List<reports.verdu_erp.dto.ReportInfoDTO> listReportsBySector(String sector) {
        List<reports.verdu_erp.dto.ReportInfoDTO> result = new ArrayList<>();
        String normalizedSector = sector == null ? "SEM_SETOR" : sector;

        List<Report> reports = reportRepository.findAllOrderByCreatedAtDesc();
        for (Report r : reports) {
            String rsector = (r.getSector() != null && !r.getSector().trim().isEmpty()) ? r.getSector() : inferSector(r);
            if (normalizedSector.equals(rsector)) {
                result.add(new reports.verdu_erp.dto.ReportInfoDTO(
                    r.getCodigo(),
                    r.getName(),
                    r.getFileSize() != null ? r.getFileSize() : 0L,
                    r.getCreatedAt() != null ? r.getCreatedAt().atZone(java.time.ZoneId.systemDefault()) : java.time.ZonedDateTime.now(),
                    r.getId().toString(),
                    rsector
                ));
            }
        }
        return result;
    }
}
