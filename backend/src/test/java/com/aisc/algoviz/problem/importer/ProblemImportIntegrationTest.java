package com.aisc.algoviz.problem.importer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Deliberate database import check. It is skipped unless explicitly enabled
 * through ALGOVIZ_RUN_IMPORT_TEST=true.
 */
@SpringBootTest(properties = {
        "algoviz.import.enabled=true",
        "algoviz.import.source=FILE"
})
@EnabledIfEnvironmentVariable(named = "ALGOVIZ_RUN_IMPORT_TEST", matches = "true")
class ProblemImportIntegrationTest {

    @Test
    void importsConfiguredDataset() {
        // ProblemImportRunner executes the configured import before this test.
    }
}
