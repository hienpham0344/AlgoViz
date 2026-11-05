package com.aisc.algoviz.ast.engine.model;

import java.util.Map;
import java.util.Objects;

/** Source proof of one complete in-place Heap Sort family. */
public record HeapPattern(
    int schemaVersion,
    SortPattern.Direction direction,
    String buildKind,
    Map<String, String> operationAstNodeIds,
    boolean siftInclusiveBoundary,
    boolean buildSiftInclusiveBoundary) {
  public HeapPattern {
    if (schemaVersion != 1) throw new IllegalArgumentException("Unsupported heap schema version");
    Objects.requireNonNull(direction, "direction");
    if (!"BOTTOM_UP".equals(buildKind) && !"TOP_DOWN".equals(buildKind))
      throw new IllegalArgumentException("Unsupported heap build kind");
    operationAstNodeIds = Map.copyOf(operationAstNodeIds);
    if (operationAstNodeIds.isEmpty()
        || operationAstNodeIds.entrySet().stream()
            .anyMatch(entry -> entry.getKey().isBlank() || entry.getValue().isBlank()))
      throw new IllegalArgumentException("Heap operations require source IDs");
  }
}
