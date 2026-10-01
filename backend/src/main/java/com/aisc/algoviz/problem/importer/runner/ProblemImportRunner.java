package com.aisc.algoviz.problem.importer.runner;

import com.aisc.algoviz.problem.importer.config.ProblemImportProperties;
import com.aisc.algoviz.problem.importer.model.ImportSummary;
import com.aisc.algoviz.problem.importer.service.ProblemImportService;
import com.aisc.algoviz.problem.importer.source.ProblemDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProblemImportRunner implements CommandLineRunner {

    private final ProblemDataSource dataSource;
    private final ProblemImportService importService;
    private final ProblemImportProperties properties;

    public ProblemImportRunner(
            ProblemImportProperties properties,
            ProblemDataSource dataSource,
            ProblemImportService importService
    ) {
        this.properties = properties;
        this.dataSource = dataSource;
        this.importService = importService;
    }

    @Override
    public void run(String... args) {
        // Runner này cố ý không làm gì khi startup thông thường. Phải bật import rõ ràng
        // vì thao tác này có thể ghi một dataset lớn từ bên ngoài vào database.
        if (!properties.isEnabled()) {
            log.info("Problem import is disabled; no data was loaded");
            return;
        }
        ImportSummary summary = importService.importData(dataSource.load());
        log.info(
                "Problem import completed: {} problems and {} knowledge-base solutions",
                summary.importedProblems(),
                summary.importedSolutions()
        );
    }
}
