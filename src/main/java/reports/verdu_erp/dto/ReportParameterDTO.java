package reports.verdu_erp.dto;

import java.time.LocalDateTime;

public class ReportParameterDTO {
    private Long id;
    private String parameterName;
    private String parameterType;
    private String defaultValue;
    private Boolean isRequired;
    private String description;
    private String metadata;
    private LocalDateTime createdAt;

    // Constructors
    public ReportParameterDTO() {}

    public ReportParameterDTO(Long id, String parameterName, String parameterType, 
                              String defaultValue, Boolean isRequired, String description, 
                              String metadata, LocalDateTime createdAt) {
        this.id = id;
        this.parameterName = parameterName;
        this.parameterType = parameterType;
        this.defaultValue = defaultValue;
        this.isRequired = isRequired;
        this.description = description;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }

    // Backward-compatible constructor (without metadata)
    public ReportParameterDTO(Long id, String parameterName, String parameterType,
                              String defaultValue, Boolean isRequired, String description,
                              LocalDateTime createdAt) {
        this.id = id;
        this.parameterName = parameterName;
        this.parameterType = parameterType;
        this.defaultValue = defaultValue;
        this.isRequired = isRequired;
        this.description = description;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    public String getParameterType() {
        return parameterType;
    }

    public void setParameterType(String parameterType) {
        this.parameterType = parameterType;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Boolean getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
