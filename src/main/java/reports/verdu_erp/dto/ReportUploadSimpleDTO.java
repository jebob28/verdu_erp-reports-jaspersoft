package reports.verdu_erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

/**
 * DTO simplificado para upload de relatórios com parâmetros dinâmicos
 */
@Data
@Schema(description = "DTO para upload simplificado de relatórios (ainda suportado, mas 'data' agora pode ser String JSON)")
public class ReportUploadSimpleDTO {
    
    @Schema(description = "Código único do relatório", example = "REL_VENDAS_001", required = true)
    private String codigo;
    
    @Schema(description = "Descrição do relatório", example = "Relatório de Vendas Mensal")
    private String descricao;
    
    @Schema(description = "Lista de parâmetros do relatório (opcional). Cada item pode conter somente 'nome' e 'tipo'.")
    private List<ReportParameterSimpleDTO> parametros;
    
    /**
     * DTO para parâmetros individuais do relatório
     */
    @Data
    @Schema(description = "Parâmetro individual do relatório")
    public static class ReportParameterSimpleDTO {
        
        @Schema(description = "Nome do parâmetro", example = "dataInicio", required = true)
        private String nome;
        
        @Schema(description = "Tipo do parâmetro", 
                example = "DATE", 
                allowableValues = {"STRING", "INTEGER", "LONG", "DOUBLE", "DECIMAL", "BOOLEAN", "DATE", "DATETIME"},
                required = true)
        private String tipo;
        
        @Schema(description = "Valor padrão do parâmetro", example = "2024-01-01")
        private String valorPadrao;
        
        @Schema(description = "Se o parâmetro é obrigatório", example = "true")
        private Boolean obrigatorio = false;
        
        @Schema(description = "Descrição do parâmetro", example = "Data de início do período")
        private String descricao;
        
        @Schema(description = "Máscara/formato para o parâmetro (ex: dd/MM/yyyy para datas)", example = "dd/MM/yyyy")
        private String formato;
        
        @Schema(description = "Valor mínimo (para números e datas)", example = "0")
        private String valorMinimo;
        
        @Schema(description = "Valor máximo (para números e datas)", example = "999999")
        private String valorMaximo;
        
        @Schema(description = "Lista de opções válidas (para campos de seleção)", example = "[\"Ativo\", \"Inativo\"]")
        private List<String> opcoes;
    }
}