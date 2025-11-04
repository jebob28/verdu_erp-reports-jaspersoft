package reports.verdu_erp.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enum para tipos de parâmetros suportados no sistema de relatórios
 */
@Schema(description = "Tipos de parâmetros suportados")
public enum ParameterType {
    
    @Schema(description = "Texto livre")
    STRING("String", "Texto", "texto"),
    
    @Schema(description = "Número inteiro")
    INTEGER("Integer", "Número Inteiro", "123"),
    
    @Schema(description = "Número longo")
    LONG("Long", "Número Longo", "123456789"),
    
    @Schema(description = "Número decimal")
    DOUBLE("Double", "Número Decimal", "123.45"),
    
    @Schema(description = "Número decimal de alta precisão")
    DECIMAL("BigDecimal", "Decimal Preciso", "1234.56"),
    
    @Schema(description = "Verdadeiro ou Falso")
    BOOLEAN("Boolean", "Sim/Não", "true"),
    
    @Schema(description = "Data (dd/MM/yyyy)")
    DATE("Date", "Data", "31/12/2024"),
    
    @Schema(description = "Data e hora (dd/MM/yyyy HH:mm)")
    DATETIME("Timestamp", "Data e Hora", "31/12/2024 23:59");
    
    private final String javaType;
    private final String displayName;
    private final String example;
    
    ParameterType(String javaType, String displayName, String example) {
        this.javaType = javaType;
        this.displayName = displayName;
        this.example = example;
    }
    
    public String getJavaType() {
        return javaType;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getExample() {
        return example;
    }
    
    /**
     * Retorna o formato padrão para o tipo
     */
    public String getDefaultFormat() {
        switch (this) {
            case DATE:
                return "dd/MM/yyyy";
            case DATETIME:
                return "dd/MM/yyyy HH:mm";
            case DECIMAL:
                return "#,##0.00";
            case DOUBLE:
                return "#,##0.00";
            default:
                return null;
        }
    }
    
    /**
     * Valida se um valor é compatível com o tipo
     */
    public boolean isValidValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Valores vazios são válidos (serão tratados como null)
        }
        
        try {
            switch (this) {
                case STRING:
                    return true;
                case INTEGER:
                    Integer.parseInt(value);
                    return true;
                case LONG:
                    Long.parseLong(value);
                    return true;
                case DOUBLE:
                    Double.parseDouble(value);
                    return true;
                case DECIMAL:
                    new java.math.BigDecimal(value);
                    return true;
                case BOOLEAN:
                    return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value) ||
                           "sim".equalsIgnoreCase(value) || "não".equalsIgnoreCase(value) ||
                           "nao".equalsIgnoreCase(value) || "1".equals(value) || "0".equals(value);
                case DATE:
                case DATETIME:
                    // Validação básica - pode ser melhorada com SimpleDateFormat
                    return value.matches("\\d{2}/\\d{2}/\\d{4}.*");
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
}