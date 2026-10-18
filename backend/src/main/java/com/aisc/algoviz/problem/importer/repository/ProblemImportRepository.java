package com.aisc.algoviz.problem.importer.repository;

import com.aisc.algoviz.problem.importer.model.ProblemImportRecord;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface ProblemImportRepository {

    /** Upsert problem và trả về database ID tự sinh, được map theo source leetcode ID. */
    Map<Integer, Long> upsertProblems(Collection<ProblemImportRecord> problems);

    /** Tìm các pattern reference không resolve được trong database hiện tại. */
    Set<Integer> findMissingPatternIds(Set<Integer> patternIds);
}
