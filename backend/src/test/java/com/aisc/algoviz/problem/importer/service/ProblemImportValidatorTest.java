package com.aisc.algoviz.problem.importer.service;

import com.aisc.algoviz.problem.importer.model.ImportDataSet;
import com.aisc.algoviz.problem.importer.model.ProblemImportRecord;
import com.aisc.algoviz.problem.importer.model.SolutionImportRecord;
import com.aisc.algoviz.problem.enums.Difficulty;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ProblemImportValidatorTest {

    private final ProblemImportValidator validator = new ProblemImportValidator();

    @Test
    void rejectsSolutionWithUnknownSourceProblemId() {
        var problems = List.of(problem(1));
        var solutions = List.of(new SolutionImportRecord(10, 999, null, "code", null, "explanation"));

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(new ImportDataSet(problems, solutions))
        );
    }

    private ProblemImportRecord problem(Integer leetcodeId) {
        return new ProblemImportRecord(
                leetcodeId,
                leetcodeId,
                "Problem " + leetcodeId,
                "problem-" + leetcodeId,
                Difficulty.EASY,
                "description",
                List.of(),
                null,
                null
        );
    }
}
