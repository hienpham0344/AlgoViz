package com.aisc.algoviz.problem.importer.source;

import com.aisc.algoviz.problem.enums.Difficulty;
import com.aisc.algoviz.problem.importer.config.ProblemImportProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonProblemDataReaderTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void readsConfiguredProblemAndSolutionFiles() throws Exception {
        Files.writeString(
                temporaryDirectory.resolve("problems.json"),
                """
                        [{
                          "id": 1,
                          "leetcode_id": 1,
                          "title": "Two Sum",
                          "slug": "two-sum",
                          "difficulty": "Easy",
                          "description": "description",
                          "pattern_tags": ["Array"]
                        }]
                        """
        );
        Files.writeString(
                temporaryDirectory.resolve("kb_solutions.json"),
                """
                        [{
                          "id": 10,
                          "problem_id": 1,
                          "pattern_id": null,
                          "code_snippet": "return 1;",
                          "embedding": null,
                          "explanation": "explanation"
                        }]
                        """
        );

        var dataSet = new JsonProblemDataReader(JsonMapper.builder().build())
                .read(temporaryDirectory, "problems.json", "kb_solutions.json");

        assertEquals(1, dataSet.problems().size());
        assertEquals(Difficulty.EASY, dataSet.problems().get(0).difficulty());
        assertEquals(1, dataSet.solutions().size());
        assertEquals(1, dataSet.solutions().get(0).problemId());

        var properties = new ProblemImportProperties();
        properties.setMockDataDirectory(temporaryDirectory.toString());
        properties.setMockProblemsFile("problems.json");
        properties.setMockSolutionsFile("kb_solutions.json");
        var mockDataSet = new MockProblemDataSource(
                properties,
                new JsonProblemDataReader(JsonMapper.builder().build())
        ).load();

        assertEquals(1, mockDataSet.problems().size());
        assertEquals(1, mockDataSet.solutions().size());
    }
}
