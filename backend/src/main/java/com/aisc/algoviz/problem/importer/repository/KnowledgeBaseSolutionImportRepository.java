package com.aisc.algoviz.problem.importer.repository;

import com.aisc.algoviz.problem.importer.model.SolutionImportRecord;

import java.util.Collection;
import java.util.Map;

public interface KnowledgeBaseSolutionImportRepository {

    /** Thay thế snapshot solution đã import cho các database problem ID được cung cấp. */
    void replaceForProblems(Collection<SolutionImportRecord> solutions, Map<Integer, Long> problemIds);
}
