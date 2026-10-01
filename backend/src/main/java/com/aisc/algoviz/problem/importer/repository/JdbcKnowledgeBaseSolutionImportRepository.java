package com.aisc.algoviz.problem.importer.repository;

import com.aisc.algoviz.problem.importer.model.SolutionImportRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class JdbcKnowledgeBaseSolutionImportRepository implements KnowledgeBaseSolutionImportRepository {

    private static final String DELETE_SQL = """
            DELETE FROM kb_solutions
            WHERE problem_id IN (:problemIds)
            """;

    private static final String INSERT_SQL = """
            INSERT INTO kb_solutions (
                problem_id, pattern_id, code_snippet, embedding, explanation
            ) VALUES (
                :problemId, :patternId, :codeSnippet, CAST(:embedding AS vector), :explanation
            )
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public void replaceForProblems(Collection<SolutionImportRecord> solutions, Map<Integer, Long> problemIds) {
        // KB solution được xem như snapshot của các problem bị ảnh hưởng:
        // xóa snapshot cũ trước rồi insert các record mới từ source.
        List<Integer> databaseProblemIds = problemIds.values().stream()
                .map(this::toDatabaseInt)
                .toList();

        if (databaseProblemIds.isEmpty()) {
            return;
        }

        jdbcTemplate.update(DELETE_SQL, new MapSqlParameterSource("problemIds", databaseProblemIds));

        List<MapSqlParameterSource> parameters = solutions.stream()
                .map(solution -> toParameters(solution, problemIds))
                .toList();
        if (!parameters.isEmpty()) {
            jdbcTemplate.batchUpdate(INSERT_SQL, parameters.toArray(MapSqlParameterSource[]::new));
        }
    }

    private MapSqlParameterSource toParameters(
            SolutionImportRecord solution,
            Map<Integer, Long> problemIds
    ) {
        // Không thể ghi trực tiếp source problem ID vì kb_solutions.problem_id
        // tham chiếu đến primary key problems.id do database tự sinh.
        Long problemId = problemIds.get(solution.problemId());
        if (problemId == null) {
            throw new IllegalStateException(
                    "No database problem ID for source problem ID: " + solution.problemId()
            );
        }

        return new MapSqlParameterSource()
                .addValue("problemId", toDatabaseInt(problemId))
                .addValue("patternId", solution.patternId())
                .addValue("codeSnippet", solution.codeSnippet())
                .addValue("embedding", vector(solution.embedding()))
                .addValue("explanation", solution.explanation());
    }

    private Integer toDatabaseInt(Long id) {
        try {
            return Math.toIntExact(id);
        } catch (ArithmeticException exception) {
            throw new IllegalStateException(
                    "Database problem ID exceeds kb_solutions.problem_id int4 range: " + id,
                    exception
            );
        }
    }

    private String vector(List<Double> values) {
        // pgvector nhận được dạng chuỗi do List#toString() tạo ra.
        if (values == null) {
            return null;
        }
        return values.toString();
    }
}
