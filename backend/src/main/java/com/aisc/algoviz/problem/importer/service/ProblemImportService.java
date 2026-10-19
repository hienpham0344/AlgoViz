package com.aisc.algoviz.problem.importer.service;

import com.aisc.algoviz.problem.importer.model.ImportDataSet;
import com.aisc.algoviz.problem.importer.model.ImportSummary;
import com.aisc.algoviz.problem.importer.repository.KnowledgeBaseSolutionImportRepository;
import com.aisc.algoviz.problem.importer.repository.ProblemImportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProblemImportService {

    private final ProblemImportValidator validator;
    private final ProblemImportRepository problemRepository;
    private final KnowledgeBaseSolutionImportRepository solutionRepository;

    @Transactional
    public ImportSummary importData(ImportDataSet dataSet) {
        // Kiểm tra mọi thứ có thể validate từ source trước khi bắt đầu ghi dữ liệu.
        validator.validate(dataSet);

        Set<Integer> patternIds = dataSet.solutions().stream()
                .map(solution -> solution.patternId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Integer> missingPatternIds = problemRepository.findMissingPatternIds(patternIds);
        if (!missingPatternIds.isEmpty()) {
            throw new IllegalArgumentException("Unknown pattern IDs: " + missingPatternIds);
        }

        // Repository trả về các problems.id tự sinh, được map theo leetcode_id. Map này là
        // cầu nối giữa reference trong JSON source và foreign key trong relational database.
        var databaseProblemIds = problemRepository.upsertProblems(dataSet.problems());
        if (databaseProblemIds.size() != dataSet.problems().size()) {
            throw new IllegalStateException("Not every imported problem received a database ID");
        }

        // Toàn bộ thao tác nằm trong một transaction: nếu ghi solution thất bại thì phần
        // upsert problem cũng rollback, tránh để hai bảng ở trạng thái import dở dang.
        solutionRepository.replaceForProblems(dataSet.solutions(), databaseProblemIds);
        return new ImportSummary(dataSet.problems().size(), dataSet.solutions().size());
    }
}
