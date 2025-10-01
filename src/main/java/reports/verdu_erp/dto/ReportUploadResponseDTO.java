package reports.verdu_erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * DTO para resposta de upload de relatórios
 */
@Data
@Schema(description = "DTO para resposta de upload de relatórios")
public class ReportUploadResponseDTO {
    
    @Schema(description = "Mensagem de resposta", example = "Relatório enviado com sucesso")
    private String message;
    
    @Schema(description = "Nome do relatório", example = "relatorio_vendas.jrxml")
    private String reportName;
    
    @Schema(description = "Indica se a operação foi bem-sucedida", example = "true")
    private boolean success;
    
    public ReportUploadResponseDTO() {}
    
    public ReportUploadResponseDTO(String message, String reportName, boolean success) {
        this.message = message;
        this.reportName = reportName;
        this.success = success;
    }
    
    public static ReportUploadResponseDTO success(String reportName) {
        return new ReportUploadResponseDTO("Relatório enviado com sucesso", reportName, true);
    }
    
    public static ReportUploadResponseDTO error(String message) {
        return new ReportUploadResponseDTO(message, null, false);
    }
}