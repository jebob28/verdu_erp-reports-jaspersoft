package reports.verdu_erp.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ReportWithParametersDTO {
    private Long id;
    private String name;
    private String codigo;
    private String description;
    private Long fileSize;
    private String contentType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ReportParameterDTO> parameters;

    // Constructors
    public ReportWithParametersDTO() {}

    public ReportWithParametersDTO(Long id, String name, String codigo, String description, 
                                   Long fileSize, String contentType, LocalDateTime createdAt, 
                                   LocalDateTime updatedAt, List<ReportParameterDTO> parameters) {
        this.id = id;
        this.name = name;
        this.codigo = codigo;
        this.description = description;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.parameters = parameters;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<ReportParameterDTO> getParameters() {
        return parameters;
    }

    public void setParameters(List<ReportParameterDTO> parameters) {
        this.parameters = parameters;
    }
}