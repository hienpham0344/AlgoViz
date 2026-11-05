package com.aisc.algoviz.ast.engine.model;

/** Runtime observations of a proven Heap Sort source pattern. */
public record HeapFrame(
    String phase,
    Long invocationId,
    Integer heapEnd,
    Integer node,
    Integer left,
    Integer right,
    Integer chosen,
    SortFrame.Comparison comparison,
    SortFrame.Mutation pendingMutation,
    SortFrame.Mutation completedMutation,
    String heapStatus,
    String suffixStatus,
    String fullArrayStatus,
    String snapshotPhase,
    Integer verifiedSuffixStart) {}
