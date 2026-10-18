package com.aisc.algoviz.problem.importer;

import com.aisc.algoviz.problem.importer.source.ProblemDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Read-only verification for the deliberate import run. */
@SpringBootTest(properties = "algoviz.import.enabled=false")
@EnabledIfEnvironmentVariable(named = "ALGOVIZ_RUN_IMPORT_TEST", matches = "true")
class ProblemImportDatabaseVerificationTest {

    private final JdbcTemplate jdbcTemplate;
    private final ProblemDataSource dataSource;

    @Autowired
    ProblemImportDatabaseVerificationTest(JdbcTemplate jdbcTemplate, ProblemDataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Test
    void verifiesImportedRowsAndSourceReferences() {
        Long problemCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM problems WHERE leetcode_id IS NOT NULL",
                Long.class
        );
        Long solutionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM kb_solutions",
                Long.class
        );
        Long unresolvedSolutionCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM kb_solutions solution
                LEFT JOIN problems problem ON problem.id = solution.problem_id
                WHERE problem.id IS NULL
                """,
                Long.class
        );

        var expectedDataSet = dataSource.load();
        assertEquals(expectedDataSet.problems().size(), problemCount);
        assertEquals(expectedDataSet.solutions().size(), solutionCount);
        assertEquals(0L, unresolvedSolutionCount);
    }
}
