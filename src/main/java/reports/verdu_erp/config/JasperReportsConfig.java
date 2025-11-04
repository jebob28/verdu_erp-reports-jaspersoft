package reports.verdu_erp.config;

import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

/**
 * Configuração do JasperReports para melhor suporte a fontes
 */
@Configuration
public class JasperReportsConfig {

    @PostConstruct
    public void configureJasperReports() {
        // Configurações globais para melhor suporte a fontes no PDF
        System.setProperty("net.sf.jasperreports.awt.ignore.missing.font", "true");
        System.setProperty("net.sf.jasperreports.default.font.name", "DejaVu Sans");
        System.setProperty("net.sf.jasperreports.default.pdf.font.name", "DejaVu Sans");
        System.setProperty("net.sf.jasperreports.default.pdf.encoding", "UTF-8");
        System.setProperty("net.sf.jasperreports.default.pdf.embedded", "true");
        
        // Configurações adicionais para melhor renderização
        System.setProperty("net.sf.jasperreports.export.pdf.force.svg.shapes", "true");
        System.setProperty("net.sf.jasperreports.export.pdf.create.batch.mode.bookmarks", "false");
        
        // Configuração para usar fontes DejaVu por padrão
        System.setProperty("net.sf.jasperreports.extension.registry.factory.fonts", 
                          "net.sf.jasperreports.engine.fonts.SimpleFontExtensionRegistryFactory");
        System.setProperty("net.sf.jasperreports.extension.fonts.dejavu", 
                          "net.sf.jasperreports.fonts.dejavu.DejaVuFontExtension");
        
        System.out.println("[INFO] JasperReports configurado com suporte aprimorado a fontes");
    }
}