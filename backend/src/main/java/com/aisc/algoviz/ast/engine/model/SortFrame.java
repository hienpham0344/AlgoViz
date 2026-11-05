package com.aisc.algoviz.ast.engine.model;

/** Runtime-derived sorting details at a single BEFORE_LOCATION snapshot. */
public record SortFrame(
    String phase,
    Comparison comparison,
    Mutation pendingMutation,
    Mutation completedMutation,
    SortedRegion sortedRegion,
    String snapshotPhase) {

  public record Comparison(
      Integer firstIndex,
      Integer secondIndex,
      Object firstValue,
      Object secondValue,
      @com.fasterxml.jackson.annotation.JsonInclude(
              com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
          Boolean swapRequired,
      String status,
      @com.fasterxml.jackson.annotation.JsonInclude(
              com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
          String action,
      @com.fasterxml.jackson.annotation.JsonInclude(
              com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
          Boolean actionRequired) {
    public Comparison(
        Integer firstIndex,
        Integer secondIndex,
        Object firstValue,
        Object secondValue,
        Boolean swapRequired,
        String status) {
      this(
          firstIndex,
          secondIndex,
          firstValue,
          secondValue,
          swapRequired,
          status,
          "SWAP",
          swapRequired);
    }
  }

  /** The operation is pending until a later snapshot confirms the write occurred. */
  public record Mutation(
      String kind, Integer fromIndex, Integer toIndex, Object value, String status) {}

  public record SortedRegion(
      int start,
      int endExclusive,
      String status,
      String basis,
      @com.fasterxml.jackson.annotation.JsonInclude(
              com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
          Integer violationIndex,
      @com.fasterxml.jackson.annotation.JsonInclude(
              com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
          Object previousValue,
      @com.fasterxml.jackson.annotation.JsonInclude(
              com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
          Object currentValue) {
    public SortedRegion(int start, int endExclusive, String status, String basis) {
      this(start, endExclusive, status, basis, null, null, null);
    }
  }
}
