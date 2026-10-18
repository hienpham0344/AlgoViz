package com.aisc.algoviz.problem.importer.source;

import com.aisc.algoviz.problem.importer.model.ImportDataSet;
import com.aisc.algoviz.problem.importer.model.ProblemImportRecord;
import com.aisc.algoviz.problem.importer.model.SolutionImportRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RequiredArgsConstructor
/** Đọc hai file JSON và tách parsing khỏi validation và persistence. */
public class JsonProblemDataReader {

    private final ObjectMapper objectMapper;

    public ImportDataSet read(Path directory, String problemsFile, String solutionsFile) {
        // Resolve cả hai file từ cùng một thư mục cấu hình để problem và solution
        // được import như một cặp dataset tương ứng.
        requireDirectory(directory);
        return new ImportDataSet(
                readList(directory.resolve(problemsFile), new TypeReference<List<ProblemImportRecord>>() {
                }),
                readList(directory.resolve(solutionsFile), new TypeReference<List<SolutionImportRecord>>() {
                })
        );
    }

    private <T> List<T> readList(Path file, TypeReference<List<T>> typeReference) {
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("Import file does not exist: " + file);
        }
        try {
            return objectMapper.readValue(file.toFile(), typeReference);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to parse import file: " + file, exception);
        }
    }

    private void requireDirectory(Path directory) {
        if (directory == null || !Files.isDirectory(directory)) {
            throw new IllegalStateException("Import data directory does not exist: " + directory);
        }
    }
}
