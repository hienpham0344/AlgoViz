package com.aisc.algoviz.problem.importer.service;

import com.aisc.algoviz.problem.importer.model.ImportDataSet;
import com.aisc.algoviz.problem.importer.model.ProblemImportRecord;
import com.aisc.algoviz.problem.importer.model.SolutionImportRecord;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/** Validate các reference giữa record trước khi importer bắt đầu ghi vào database. */
@Component
public class ProblemImportValidator {

    public void validate(ImportDataSet dataSet) {
        // Source ID không dùng làm database key, nhưng leetcode_id vẫn phải tồn tại,
        // không trùng và có thể dùng để resolve mọi solution reference trong batch này.
        if (dataSet.problems().isEmpty()) {
            throw new IllegalArgumentException("Import dataset must contain at least one problem");
        }

        Set<Integer> leetcodeIds = new HashSet<>();
        for (ProblemImportRecord problem : dataSet.problems()) {
            if (problem.leetcodeId() == null) {
                throw new IllegalArgumentException("Every problem must have leetcode_id");
            }
            if (!leetcodeIds.add(problem.leetcodeId())) {
                throw new IllegalArgumentException(
                        "Duplicate problem leetcode_id: " + problem.leetcodeId()
                );
            }
            if (problem.title() == null || problem.title().isBlank()) {
                throw new IllegalArgumentException(
                        "Problem title is blank for leetcode_id: " + problem.leetcodeId()
                );
            }
        }

        for (SolutionImportRecord solution : dataSet.solutions()) {
            if (solution.problemId() == null || !leetcodeIds.contains(solution.problemId())) {
                throw new IllegalArgumentException(
                        "Solution " + solution.sourceId()
                                + " references unknown source problem_id: " + solution.problemId()
                );
            }
            if (solution.embedding() != null && solution.embedding().stream()
                    .anyMatch(value -> value == null || !Double.isFinite(value))) {
                throw new IllegalArgumentException(
                        "Solution " + solution.sourceId() + " contains an invalid embedding"
                );
            }
        }
    }
}
