package com.aisc.algoviz.problem.importer.repository;

import com.aisc.algoviz.problem.importer.model.ProblemImportRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
@RequiredArgsConstructor
public class JdbcProblemImportRepository implements ProblemImportRepository {

    private static final String UPSERT_PROBLEM_SQL = """
            INSERT INTO problems (
                leetcode_id, title, slug, difficulty, description,
                pattern_tags, time_limit, memory_limit
            ) VALUES (
                :leetcodeId, :title, :slug, :difficulty, :description,
                CAST(:patternTags AS jsonb), :timeLimit, :memoryLimit
            )
            ON CONFLICT (leetcode_id) DO UPDATE SET
                title = EXCLUDED.title,
                slug = EXCLUDED.slug,
                difficulty = EXCLUDED.difficulty,
                description = EXCLUDED.description,
                pattern_tags = EXCLUDED.pattern_tags,
                time_limit = EXCLUDED.time_limit,
                memory_limit = EXCLUDED.memory_limit
            """;

    private static final String FIND_IDS_SQL = """
            SELECT id, leetcode_id
            FROM problems
            WHERE leetcode_id IN (:leetcodeIds)
            """;

    private static final String FIND_PATTERN_IDS_SQL = """
            SELECT id
            FROM patterns
            WHERE id IN (:patternIds)
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Map<Integer, Long> upsertProblems(Collection<ProblemImportRecord> problems) {
        // leetcode_id là source/business key ổn định. Database ID vẫn do database tự sinh
        // và được lookup sau khi upsert để dùng khi insert vào kb_solutions.
        List<MapSqlParameterSource> parameters = problems.stream()
                .map(this::toParameters)
                .toList();

        jdbcTemplate.batchUpdate(UPSERT_PROBLEM_SQL, parameters.toArray(MapSqlParameterSource[]::new));

        Map<Integer, Long> idsByLeetCodeId = new HashMap<>();
        jdbcTemplate.query(
                FIND_IDS_SQL,
                new MapSqlParameterSource("leetcodeIds", problems.stream()
                        .map(ProblemImportRecord::leetcodeId)
                        .toList()),
                (resultSet, rowNumber) -> Map.entry(
                        resultSet.getInt("leetcode_id"),
                        resultSet.getLong("id")
                )
        ).forEach(entry -> idsByLeetCodeId.put(entry.getKey(), entry.getValue()));
        return idsByLeetCodeId;
    }

    @Override
    public Set<Integer> findMissingPatternIds(Set<Integer> patternIds) {
        // Kiểm tra các reference tương tự foreign key trước khi thay đổi hai bảng đích.
        if (patternIds.isEmpty()) {
            return Set.of();
        }

        Set<Integer> existingIds = new HashSet<>(jdbcTemplate.query(
                FIND_PATTERN_IDS_SQL,
                new MapSqlParameterSource("patternIds", patternIds),
                (resultSet, rowNumber) -> resultSet.getInt("id")
        ));
        Set<Integer> missingIds = new HashSet<>(patternIds);
        missingIds.removeAll(existingIds);
        return missingIds;
    }

    private MapSqlParameterSource toParameters(ProblemImportRecord problem) {
        return new MapSqlParameterSource()
                .addValue("leetcodeId", problem.leetcodeId())
                .addValue("title", problem.title())
                .addValue("slug", problem.slug())
                .addValue("difficulty", problem.difficulty() == null
                        ? null
                        : problem.difficulty().getValue())
                .addValue("description", problem.description())
                .addValue("patternTags", jsonb(problem.patternTags()), Types.VARCHAR)
                .addValue("timeLimit", problem.timeLimit())
                .addValue("memoryLimit", problem.memoryLimit());
    }

    private String jsonb(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize problem pattern tags", exception);
        }
    }
}
