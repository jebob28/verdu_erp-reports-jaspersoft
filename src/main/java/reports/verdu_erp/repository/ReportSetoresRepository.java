package reports.verdu_erp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import reports.verdu_erp.entity.ReportSetores;

@Repository
public interface ReportSetoresRepository extends JpaRepository<ReportSetores, Long> {
}

