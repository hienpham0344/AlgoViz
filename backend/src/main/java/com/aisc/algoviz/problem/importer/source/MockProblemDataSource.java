package com.aisc.algoviz.problem.importer.source;

import com.aisc.algoviz.problem.importer.config.ProblemImportProperties;
import com.aisc.algoviz.problem.importer.model.ImportDataSet;

import java.nio.file.Path;

/**
 * Mock adapter dùng các fixture file có thể cấu hình.
 *
 * <p>Fixture nằm ngoài production code, nhưng test vẫn chạy qua cùng reader, validator,
 * service và repository flow như file source thật.</p>
 */
public class MockProblemDataSource implements ProblemDataSource {

    private final ProblemImportProperties properties;
    private final JsonProblemDataReader reader;

    public MockProblemDataSource(ProblemImportProperties properties, JsonProblemDataReader reader) {
        this.properties = properties;
        this.reader = reader;
    }

    @Override
    public ImportDataSet load() {
        return reader.read(
                Path.of(required(properties.getMockDataDirectory(), "mock-data-directory")),
                required(properties.getMockProblemsFile(), "mock-problems-file"),
                required(properties.getMockSolutionsFile(), "mock-solutions-file")
        );
    }

    private String required(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing import property: algoviz.import." + propertyName);
        }
        return value;
    }
}
