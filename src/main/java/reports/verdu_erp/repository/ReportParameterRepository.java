package reports.verdu_erp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reports.verdu_erp.entity.ReportParameter;

import java.util.List;

@Repository
public interface ReportParameterRepository extends JpaRepository<ReportParameter, Long> {
    
    List<ReportParameter> findByReportId(Long reportId);
    
    List<ReportParameter> findByReportName(String reportName);
    
    @Query("SELECT rp FROM ReportParameter rp WHERE rp.report.name = :reportName ORDER BY rp.parameterName ASC")
    List<ReportParameter> findByReportNameOrderByParameterName(@Param("reportName") String reportName);
    
    void deleteByReportId(Long reportId);
    
    boolean existsByReportIdAndParameterName(Long reportId, String parameterName);
}