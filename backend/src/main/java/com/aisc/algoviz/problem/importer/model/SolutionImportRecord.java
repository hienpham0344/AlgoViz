package com.aisc.algoviz.problem.importer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Cấu trúc knowledge-base solution được đọc từ dataset JSON bên ngoài.
 *
 * <p>{@code problemId} là {@code leetcode_id} của problem ở source; trước khi ghi vào
 * {@code kb_solutions}, giá trị này được đổi sang database problem ID do hệ thống tự sinh.</p>
 */
public record SolutionImportRecord(
        @JsonProperty("id") Integer sourceId,
        @JsonProperty("problem_id") Integer problemId,
        @JsonProperty("pattern_id") Integer patternId,
        @JsonProperty("code_snippet") String codeSnippet,
        List<Double> embedding,
        String explanation
) {
}
