package reports.verdu_erp.service;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Serviço de compilação prévia de relatórios JasperReports
 * Compila todos os relatórios .jrxml na inicialização para evitar erros de compilação em tempo de execução
 */
@Component
public class ReportPrecompilerService {
    
    private final Map<String, JasperReport> compiledReports = new HashMap<>();
    
    @PostConstruct
    public void precompileAllReports() {
        System.out.println("[PRECOMPILER] Iniciando compilação prévia de relatórios...");
        
        try {
            // Lista de relatórios a compilar
            String[] reportsToCompile = {
                "relatorios/logistica/separacaodecarga.jrxml"
                // Adicione mais relatórios aqui
            };
            
            for (String reportPath : reportsToCompile) {
                try {
                    JasperReport compiledReport = compileReport(reportPath);
                    if (compiledReport != null) {
                        String reportName = extractReportName(reportPath);
                        compiledReports.put(reportName, compiledReport);
                        System.out.println("[PRECOMPILER] ✅ Relatório compilado: " + reportName);
                    }
                } catch (Exception e) {
                    System.err.println("[PRECOMPILER] ❌ Erro ao compilar relatório: " + reportPath + " - " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            System.out.println("[PRECOMPILER] ✨ Compilação prévia concluída! Total: " + compiledReports.size() + " relatórios");
            
        } catch (Exception e) {
            System.err.println("[PRECOMPILER] ❌ Erro geral na compilação prévia: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Compila um relatório .jrxml individual
     */
    private JasperReport compileReport(String reportPath) throws Exception {
        try (InputStream reportStream = getClass().getClassLoader().getResourceAsStream(reportPath)) {
            if (reportStream == null) {
                System.err.println("[PRECOMPILER] ❌ Relatório não encontrado: " + reportPath);
                return null;
            }
            
            // Configurar propriedades do sistema para compilação segura
            System.setProperty("net.sf.jasperreports.compiler.class", "net.sf.jasperreports.engine.design.JRJdtCompiler");
            System.setProperty("net.sf.jasperreports.compiler.java", "false");
            System.setProperty("net.sf.jasperreports.compiler.expression.class", "false");
            System.setProperty("net.sf.jasperreports.compiler.expression.groovy", "false");
            System.setProperty("net.sf.jasperreports.compiler.expression.javascript", "false");
            System.setProperty("net.sf.jasperreports.compiler.cache.temp.files", "false");
            
            // Carregar o design do relatório
            JasperDesign jasperDesign = JRXmlLoader.load(reportStream);
            
            // Compilar o relatório
            JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
            
            System.out.println("[PRECOMPILER] 📋 Relatório compilado com sucesso: " + reportPath);
            return jasperReport;
            
        } catch (Exception e) {
            System.err.println("[PRECOMPILER] ❌ Erro na compilação de: " + reportPath);
            throw e;
        }
    }
    
    /**
     * Extrai o nome do relatório do caminho
     */
    private String extractReportName(String reportPath) {
        String fileName = reportPath.substring(reportPath.lastIndexOf('/') + 1);
        return fileName.replace(".jrxml", "");
    }
    
    /**
     * Obtém um relatório já compilado
     */
    public JasperReport getCompiledReport(String reportName) {
        return compiledReports.get(reportName);
    }
    
    /**
     * Verifica se um relatório está pré-compilado
     */
    public boolean isReportPrecompiled(String reportName) {
        return compiledReports.containsKey(reportName);
    }
    
    /**
     * Lista todos os relatórios compilados
     */
    public void listCompiledReports() {
        System.out.println("[PRECOMPILER] 📋 Relatórios pré-compilados disponíveis:");
        for (String reportName : compiledReports.keySet()) {
            System.out.println("[PRECOMPILER]    - " + reportName);
        }
    }
}