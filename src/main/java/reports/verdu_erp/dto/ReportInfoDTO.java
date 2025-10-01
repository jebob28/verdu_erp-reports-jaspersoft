package reports.verdu_erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.ZonedDateTime;

/**
 * DTO para informações de relatórios disponíveis
 */
@Schema(description = "DTO para informações de relatórios disponíveis")
public class ReportInfoDTO {
    
    @Schema(description = "Nome do relatório", example = "relatorio_vendas.jrxml")
    private String name;
    
    @Schema(description = "Tamanho do arquivo em bytes", example = "15420")
    private long size;
    
    @Schema(description = "Data da última modificação", example = "2024-01-15T10:30:00Z")
    private ZonedDateTime lastModified;
    
    @Schema(description = "ETag do arquivo", example = "d41d8cd98f00b204e9800998ecf8427e")
    private String etag;
    
    public ReportInfoDTO() {}
    
    public ReportInfoDTO(String name, long size, ZonedDateTime lastModified, String etag) {
        this.name = name;
        this.size = size;
        this.lastModified = lastModified;
        this.etag = etag;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public long getSize() {
        return size;
    }
    
    public void setSize(long size) {
        this.size = size;
    }
    
    public ZonedDateTime getLastModified() {
        return lastModified;
    }
    
    public void setLastModified(ZonedDateTime lastModified) {
        this.lastModified = lastModified;
    }
    
    public String getEtag() {
        return etag;
    }
    
    public void setEtag(String etag) {
        this.etag = etag;
    }
}