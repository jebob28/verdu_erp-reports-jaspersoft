package reports.verdu_erp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reports.verdu_erp.entity.Report;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    
    Optional<Report> findByName(String name);
    
    Optional<Report> findByCodigo(String codigo);

    @Query("SELECT r FROM Report r WHERE LOWER(r.codigo) = LOWER(:codigo)")
    Optional<Report> findByCodigoIgnoreCase(@Param("codigo") String codigo);
    
    boolean existsByName(String name);
    
    boolean existsByCodigo(String codigo);
    
    @Query("SELECT r FROM Report r LEFT JOIN FETCH r.parameters WHERE r.name = :name")
    Optional<Report> findByNameWithParameters(@Param("name") String name);

    @Query("SELECT r FROM Report r WHERE LOWER(r.name) = LOWER(:name)")
    Optional<Report> findByNameIgnoreCase(@Param("name") String name);
    
    @Query("SELECT r FROM Report r ORDER BY r.createdAt DESC")
    List<Report> findAllOrderByCreatedAtDesc();
    
    @Query("SELECT r.name FROM Report r ORDER BY r.name ASC")
    List<String> findAllReportNames();
    
    @Query("SELECT COUNT(r) FROM Report r")
    long countReports();
}
