package com.aisc.algoviz.ast.engine.model;

import java.util.Map;
import java.util.Objects;

/** Static source operations of the supported top-down auxiliary-array Merge family. */
public record MergePattern(
    int schemaVersion, SortPattern.Direction direction, Map<String, String> operationAstNodeIds) {
  public MergePattern {
    if (schemaVersion != 1) throw new IllegalArgumentException("Unsupported merge schema version");
    Objects.requireNonNull(direction, "direction");
    operationAstNodeIds = Map.copyOf(operationAstNodeIds);
    if (operationAstNodeIds.isEmpty()
        || operationAstNodeIds.entrySet().stream()
            .anyMatch(entry -> entry.getKey().isBlank() || entry.getValue().isBlank()))
      throw new IllegalArgumentException("Merge operations require source IDs");
  }
}
