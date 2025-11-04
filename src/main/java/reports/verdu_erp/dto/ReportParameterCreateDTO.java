package reports.verdu_erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "DTO para criação/atualização de parâmetros de relatório")
public class ReportParameterCreateDTO {
    @Schema(description = "Código do relatório (prioritário)", example = "relatorio_vendas")
    private String reportCode;

    @Schema(description = "Nome do relatório (arquivo no bucket)", example = "relatorio_vendas.jrxml")
    private String reportName;

    @Schema(description = "Nome do parâmetro", example = "vendedorId", required = true)
    private String parameterName;

    @Schema(description = "Tipo do parâmetro", example = "Integer", allowableValues = {"String","Integer","Long","Double","BigDecimal","Boolean","Date","Timestamp"}, required = true)
    private String parameterType;

    @Schema(description = "Valor padrão do parâmetro", example = "0")
    private String defaultValue;

    @Schema(description = "Se o parâmetro é obrigatório", example = "false")
    private Boolean isRequired;

    @Schema(description = "Descrição do parâmetro", example = "Identificador do vendedor")
    private String description;
}