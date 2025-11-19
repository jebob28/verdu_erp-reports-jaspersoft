package reports.verdu_erp.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import reports.verdu_erp.entity.ReportSetores;
import reports.verdu_erp.repository.ReportSetoresRepository;

@RestController
@RequestMapping("/api/reports/setores")
@RequiredArgsConstructor
public class ReportSetoresController {
    private final ReportSetoresRepository repo;

    @GetMapping
    public List<String> list() {
        return repo.findAll().stream().map(ReportSetores::getSetor).toList();
    }
}
