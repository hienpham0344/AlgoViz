package com.aisc.algoviz.problem.importer.model;

import java.util.List;
import java.util.Objects;

/** Một batch bất biến gồm các problem và knowledge-base solution tương ứng. */
public record ImportDataSet(
        List<ProblemImportRecord> problems,
        List<SolutionImportRecord> solutions
) {
    public ImportDataSet {
        // Copy danh sách ngay tại boundary để dữ liệu import không bị thay đổi trong lúc xử lý.
        problems = List.copyOf(Objects.requireNonNull(problems, "problems must not be null"));
        solutions = List.copyOf(Objects.requireNonNull(solutions, "solutions must not be null"));
    }
}
