package reports.verdu_erp.util;

import java.io.File;
import java.io.FileOutputStream;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;

/**
 * Utilitário para compilar relatórios JasperReports
 * Compila arquivos .jrxml para .jasper para eliminar compilação em tempo de execução
 */
public class JasperCompiler {
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: java JasperCompiler <arquivo.jrxml> <saida.jasper>");
            System.exit(1);
        }
        
        String inputFile = args[0];
        String outputFile = args[1];
        
        try {
            System.out.println("📄 Compilando: " + inputFile + " -> " + outputFile);
            
            // Configurar propriedades do sistema para compilação segura
            System.setProperty("net.sf.jasperreports.compiler.class", "net.sf.jasperreports.engine.design.JRJdtCompiler");
            System.setProperty("net.sf.jasperreports.compiler.java", "false");
            System.setProperty("net.sf.jasperreports.compiler.expression.class", "false");
            System.setProperty("net.sf.jasperreports.compiler.expression.groovy", "false");
            System.setProperty("net.sf.jasperreports.compiler.expression.javascript", "false");
            System.setProperty("net.sf.jasperreports.compiler.cache.temp.files", "false");
            
            // Compilar o relatório
            JasperReport compiledReport = JasperCompileManager.compileReport(inputFile);
            
            // Salvar o relatório compilado
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                net.sf.jasperreports.engine.util.JRSaver.saveObject(compiledReport, fos);
            }
            
            System.out.println("✅ Relatório compilado com sucesso: " + outputFile);
            System.out.println("📊 Tamanho do arquivo: " + new File(outputFile).length() + " bytes");
            
        } catch (Exception e) {
            System.err.println("❌ Erro ao compilar relatório: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}