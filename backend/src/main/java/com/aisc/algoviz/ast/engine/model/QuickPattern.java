package com.aisc.algoviz.ast.engine.model;

import java.util.Map;
import java.util.Objects;

/** Static source operations of the supported inclusive-range Lomuto family. */
public record QuickPattern(
    int schemaVersion,
    SortPattern.Direction direction,
    Map<String, String> operationAstNodeIds) {
  public QuickPattern {
    if (schemaVersion != 1) throw new IllegalArgumentException("Unsupported quick schema version");
    Objects.requireNonNull(direction, "direction");
    operationAstNodeIds = Map.copyOf(operationAstNodeIds);
    if (operationAstNodeIds.isEmpty()
        || operationAstNodeIds.entrySet().stream()
            .anyMatch(entry -> entry.getKey().isBlank() || entry.getValue().isBlank()))
      throw new IllegalArgumentException("Quick operations require source IDs");
  }
}
