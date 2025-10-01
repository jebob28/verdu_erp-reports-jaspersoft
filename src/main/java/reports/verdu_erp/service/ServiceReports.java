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
import net.sf.jasperreports.engine.JasperReport;
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
import reports.verdu_erp.dto.ReportParametrosRequestDTO;
import reports.verdu_erp.dto.ReportWithParametersDTO;
import reports.verdu_erp.dto.ReportParameterDTO;
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
    public void uploadReport(String reportName, MultipartFile file, ReportParametrosRequestDTO reportParametros) throws Exception {
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
         report.setDescription(reportParametros.getNome());
         report.setCodigo(reportParametros.getCodigo());
         report.setContentType(file.getContentType());
         report.setFileSize(file.getSize());
         
         // Salva o relatório
         Report savedReport = reportRepository.save(report);
         
         // Salva os parâmetros se existirem
         if (reportParametros.getParametros() != null && !reportParametros.getParametros().trim().isEmpty()) {
             ReportParameter param = new ReportParameter();
             param.setParameterName("parametros");
             param.setDefaultValue(reportParametros.getParametros());
             param.setParameterType("String");
             param.setReport(savedReport);
             
             reportParameterRepository.save(param);
         }
    }
    
    /**
     * Baixa um arquivo de relatório do MinIO
     */
    public InputStream downloadReport(String reportName) throws Exception {
        return minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(BUCKET_NAME)
                .object(reportName)
                .build()
        );
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
        // Baixa o arquivo do relatório do MinIO
        InputStream reportStream = downloadReport(reportName);
        
        // Compila o relatório se for .jrxml
        JasperReport jasperReport;
        if (reportName.endsWith(".jrxml")) {
            jasperReport = JasperCompileManager.compileReport(reportStream);
        } else {
            jasperReport = (JasperReport) JRLoader.loadObject(reportStream);
        }
        
        // Preenche o relatório com dados
        Connection connection = dataSource.getConnection();
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, connection);
        connection.close();
        
        // Exporta no formato solicitado
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        switch (format.toLowerCase()) {
            case "pdf":
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
            Optional<Report> reportOpt = reportRepository.findByCodigo(code);
            
            if (!reportOpt.isPresent()) {
                System.out.println("[DEBUG] Relatório não encontrado com código: " + code);
                return Optional.empty();
            }
            
            System.out.println("[DEBUG] Relatório encontrado: " + reportOpt.get().getName());
            
            // Se encontrou o relatório, busca novamente com parâmetros carregados
            reportOpt = reportRepository.findByNameWithParameters(reportOpt.get().getName());
            
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
}
