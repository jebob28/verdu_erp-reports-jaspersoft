package reports.verdu_erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Map;

/**
 * DTO para requisições de geração de relatórios
 */
@Data
@Schema(description = "DTO para requisições de geração de relatórios")
public class ReportRequestDTO {
    
    @Schema(description = "Nome do relatório no bucket", example = "relatorio_vendas.jrxml", required = true)
    private String reportName;
    
    @Schema(description = "Formato de saída do relatório", example = "pdf", allowableValues = {"pdf", "html", "csv", "xml", "xlsx"})
    private String format; // pdf, html, csv, xml, xlsx
    
    @Schema(description = "Parâmetros dinâmicos do relatório", example = "{\"dataInicio\": \"2024-01-01\", \"dataFim\": \"2024-12-31\", \"vendedorId\": 123}")
    private Map<String, Object> parameters;
    
    public ReportRequestDTO() {}
    
    public ReportRequestDTO(String reportName, String format, Map<String, Object> parameters) {
        this.reportName = reportName;
        this.format = format;
        this.parameters = parameters;
    }
}