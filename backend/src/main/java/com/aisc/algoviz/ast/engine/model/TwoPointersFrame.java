package com.aisc.algoviz.ast.engine.model;

import java.util.List;

/** Runtime evidence for one pre-location two-pointer snapshot. */
public record TwoPointersFrame(
    Long invocationId,
    Long inputObjectId,
    String movement,
    Long firstIndex,
    Long secondIndex,
    Integer length,
    Object firstValue,
    Object secondValue,
    Long firstDelta,
    Long secondDelta,
    String effectStatus,
    List<String> sourceEvidence,
    String snapshotPhase) {
  public TwoPointersFrame { sourceEvidence = List.copyOf(sourceEvidence); }
}
