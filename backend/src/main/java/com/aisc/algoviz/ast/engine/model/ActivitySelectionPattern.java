package com.aisc.algoviz.ast.engine.model;

import java.util.Map;
import java.util.Objects;

/** Static evidence for Activity Selection; ordering evidence is separate from selection proof. */
public record ActivitySelectionPattern(
    int schemaVersion,
    String representation,
    OverlapPolicy overlapPolicy,
    OrderingEvidence orderingEvidence,
    Map<String, String> declarationIds,
    Map<String, String> roleValues,
    Map<String, String> operationAstNodeIds,
    boolean firstCandidateGuard) {
  public enum OverlapPolicy { HALF_OPEN, CLOSED }

  public enum OrderingEvidence { SOURCE_PROVEN, INPUT_PRECONDITION, CONTRADICTED }

  public ActivitySelectionPattern {
    if (schemaVersion != 1) throw new IllegalArgumentException("Unsupported activity selection schema");
    if (!"INT_MATRIX".equals(representation) && !"INTERVAL_LIST".equals(representation))
      throw new IllegalArgumentException("Unsupported interval representation");
    Objects.requireNonNull(overlapPolicy, "overlapPolicy");
    Objects.requireNonNull(orderingEvidence, "orderingEvidence");
    declarationIds = Map.copyOf(declarationIds);
    roleValues = Map.copyOf(roleValues);
    operationAstNodeIds = Map.copyOf(operationAstNodeIds);
    if (declarationIds.isEmpty() || operationAstNodeIds.isEmpty())
      throw new IllegalArgumentException("Activity selection evidence requires source IDs");
  }
}
