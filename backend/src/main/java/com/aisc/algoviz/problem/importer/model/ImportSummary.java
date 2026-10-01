package com.aisc.algoviz.problem.importer.model;

/** Số lượng record được báo cáo sau một transaction import thành công. */
public record ImportSummary(int importedProblems, int importedSolutions) {
}
