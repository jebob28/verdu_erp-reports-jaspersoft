package reports.verdu_erp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rp_report_parameters")
public class ReportParameter {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "parameter_name", nullable = false)
    private String parameterName;
    
    @Column(name = "parameter_type")
    private String parameterType;
    
    @Column(name = "default_value")
    private String defaultValue;
    
    @Column(name = "is_required")
    private Boolean isRequired = false;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;
    
    // Constructors
    public ReportParameter() {
        this.createdAt = LocalDateTime.now();
    }
    
    public ReportParameter(String parameterName, String parameterType, Report report) {
        this();
        this.parameterName = parameterName;
        this.parameterType = parameterType;
        this.report = report;
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Report getReport() {
        return report;
    }
    
    public void setReport(Report report) {
        this.report = report;
    }
}