package com.aisc.algoviz.problem.importer.model;

import com.aisc.algoviz.problem.enums.Difficulty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Cấu trúc problem được đọc từ dataset JSON bên ngoài.
 *
 * <p>{@code sourceId} chỉ được giữ lại như metadata của nguồn. Primary key của database do
 * PostgreSQL tự sinh; {@code leetcodeId} là business/source key dùng để upsert problem và
 * resolve các reference từ solution.</p>
 */
public record ProblemImportRecord(
        @JsonProperty("id") Integer sourceId,
        @JsonProperty("leetcode_id") Integer leetcodeId,
        String title,
        String slug,
        Difficulty difficulty,
        String description,
        @JsonProperty("pattern_tags") List<String> patternTags,
        @JsonProperty("time_limit") Integer timeLimit,
        @JsonProperty("memory_limit") Integer memoryLimit
) {
}
