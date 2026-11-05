package com.aisc.algoviz.ast.engine.model;

/** Runtime Quick Sort evidence for one suspended invocation or its caller's verified result. */
public record QuickFrame(
    String phase,
    Long invocationId,
    Integer low,
    Integer high,
    Integer pivotIndex,
    Object pivotValue,
    Integer scan,
    Integer store,
    SortFrame.Comparison comparison,
    SortFrame.Mutation pendingMutation,
    SortFrame.Mutation completedMutation,
    String partitionStatus,
    String fullArrayStatus,
    String snapshotPhase) {}
