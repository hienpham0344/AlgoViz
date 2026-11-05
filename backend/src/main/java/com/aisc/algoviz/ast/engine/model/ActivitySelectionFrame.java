package com.aisc.algoviz.ast.engine.model;

import java.util.List;

/** Runtime evidence for one Activity Selection invocation at a pre-location snapshot. */
public record ActivitySelectionFrame(
    Long invocationId,
    Long inputObjectId,
    Integer candidateIndex,
    Integer start,
    Integer end,
    Integer lastEnd,
    String overlapPolicy,
    String decision,
    String effectStatus,
    List<Integer> selectedIndices,
    String orderingEvidence,
    String runtimeOrderingStatus,
    String nonOverlapStatus,
    String outputStatus,
    String optimalityStatus,
    String snapshotPhase) {
  public ActivitySelectionFrame {
    selectedIndices = List.copyOf(selectedIndices);
  }
}
