package com.aisc.algoviz.ast.engine.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Static AST metadata, not evidence that any comparison or mutation has executed. */
public record SortPattern(
    int schemaVersion,
    Direction direction,
    String innerLoopAstNodeId,
    String comparisonAstNodeId,
    String conditionStatementAstNodeId,
    List<String> swapStatementAstNodeIds,
    Operand first,
    Operand second,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) Map<String, String> operationAstNodeIds) {
  public enum Direction {
    ASCENDING,
    DESCENDING
  }

  /** Index = runtime value of the bound local plus offset. */
  public record Operand(String indexDeclarationId, int offset) {
    public Operand {
      requireId(indexDeclarationId);
    }
  }

  public SortPattern(
      int schemaVersion,
      Direction direction,
      String innerLoopAstNodeId,
      String comparisonAstNodeId,
      String conditionStatementAstNodeId,
      List<String> swapStatementAstNodeIds,
      Operand first,
      Operand second) {
    this(
        schemaVersion,
        direction,
        innerLoopAstNodeId,
        comparisonAstNodeId,
        conditionStatementAstNodeId,
        swapStatementAstNodeIds,
        first,
        second,
        Map.of());
  }

  public SortPattern {
    if (schemaVersion != 1) throw new IllegalArgumentException("Unsupported sort schema version");
    Objects.requireNonNull(direction, "direction");
    requireId(innerLoopAstNodeId);
    requireId(comparisonAstNodeId);
    requireId(conditionStatementAstNodeId);
    swapStatementAstNodeIds = List.copyOf(swapStatementAstNodeIds);
    if (!swapStatementAstNodeIds.isEmpty()
        && (swapStatementAstNodeIds.size() != 3
            || swapStatementAstNodeIds.stream().distinct().count() != 3)) {
      throw new IllegalArgumentException("A temporary swap requires three distinct statements");
    }
    swapStatementAstNodeIds.forEach(SortPattern::requireId);
    Objects.requireNonNull(first, "first");
    Objects.requireNonNull(second, "second");
    operationAstNodeIds = Map.copyOf(operationAstNodeIds);
    operationAstNodeIds.forEach(
        (operation, id) -> {
          if (operation.isBlank()) throw new IllegalArgumentException("Operation name required");
          requireId(id);
        });
  }

  private static void requireId(String value) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException("AST ID required");
  }
}
