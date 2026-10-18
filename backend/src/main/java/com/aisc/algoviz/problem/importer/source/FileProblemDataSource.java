package com.aisc.algoviz.problem.importer.source;

import com.aisc.algoviz.problem.importer.config.ProblemImportProperties;
import com.aisc.algoviz.problem.importer.model.ImportDataSet;

import java.nio.file.Path;

/** Đọc dataset thật từ thư mục được cấu hình bởi ALGOVIZ_DATA_DIRECTORY. */
public class FileProblemDataSource implements ProblemDataSource {

    private final ProblemImportProperties properties;
    private final JsonProblemDataReader reader;

    public FileProblemDataSource(ProblemImportProperties properties, JsonProblemDataReader reader) {
        this.properties = properties;
        this.reader = reader;
    }

    @Override
    public ImportDataSet load() {
        // Fail fast khi thiếu path bắt buộc, thay vì âm thầm import một dataset rỗng.
        return reader.read(
                Path.of(required(properties.getDataDirectory(), "data-directory")),
                required(properties.getProblemsFile(), "problems-file"),
                required(properties.getSolutionsFile(), "solutions-file")
        );
    }

    private String required(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing import property: algoviz.import." + propertyName);
        }
        return value;
    }
}
