package com.aisc.algoviz.ast.engine.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Static evidence for a two-pointer traversal, without a problem-correctness claim. */
public record TwoPointersPattern(
    int schemaVersion,
    Movement movement,
    String representation,
    Map<String, String> declarationIds,
    Map<String, String> operationAstNodeIds,
    List<String> evidence) {
  public enum Movement { OPPOSING, SAME_DIRECTION }

  public TwoPointersPattern {
    if (schemaVersion != 1) throw new IllegalArgumentException("Unsupported two-pointer schema");
    Objects.requireNonNull(movement, "movement");
    if (!"INT_ARRAY".equals(representation) && !"INTEGER_LIST".equals(representation))
      throw new IllegalArgumentException("Unsupported two-pointer data");
    declarationIds = Map.copyOf(declarationIds);
    operationAstNodeIds = Map.copyOf(operationAstNodeIds);
    evidence = List.copyOf(evidence);
    if (!declarationIds.containsKey("data") || operationAstNodeIds.isEmpty())
      throw new IllegalArgumentException("Missing two-pointer evidence");
  }
}
