package com.aisc.algoviz.ast.engine.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Self-contained, variable-derived rendering data at the enclosing step's snapshot phase. */
public record VisualState(
    String id,
    int hintIndex,
    String algorithm,
    String visualPlan,
    String status,
    ArrayValue array,
    Map<String, Pointer> pointers,
    Map<String, Scalar> scalars,
    IndexRange range,
    List<String> notes,
    @com.fasterxml.jackson.annotation.JsonInclude(
            com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        SortFrame sortFrame,
    @com.fasterxml.jackson.annotation.JsonInclude(
            com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        QuickFrame quickFrame,
    @com.fasterxml.jackson.annotation.JsonInclude(
            com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        MergeFrame mergeFrame,
    @com.fasterxml.jackson.annotation.JsonInclude(
            com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        HeapFrame heapFrame,
    @com.fasterxml.jackson.annotation.JsonInclude(
            com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        ActivitySelectionFrame activitySelectionFrame,
    @com.fasterxml.jackson.annotation.JsonInclude(
            com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        TwoPointersFrame twoPointersFrame) {
  public VisualState(
      String id,
      int hintIndex,
      String algorithm,
      String visualPlan,
      String status,
      ArrayValue array,
      Map<String, Pointer> pointers,
      Map<String, Scalar> scalars,
      IndexRange range,
      List<String> notes,
      SortFrame sortFrame,
      QuickFrame quickFrame,
      MergeFrame mergeFrame,
      HeapFrame heapFrame,
      ActivitySelectionFrame activitySelectionFrame) {
    this(id, hintIndex, algorithm, visualPlan, status, array, pointers, scalars, range,
        notes, sortFrame, quickFrame, mergeFrame, heapFrame, activitySelectionFrame, null);
  }

  public VisualState(
      String id,
      int hintIndex,
      String algorithm,
      String visualPlan,
      String status,
      ArrayValue array,
      Map<String, Pointer> pointers,
      Map<String, Scalar> scalars,
      IndexRange range,
      List<String> notes,
      SortFrame sortFrame,
      QuickFrame quickFrame,
      MergeFrame mergeFrame,
      HeapFrame heapFrame) {
    this(id, hintIndex, algorithm, visualPlan, status, array, pointers, scalars, range, notes,
        sortFrame, quickFrame, mergeFrame, heapFrame, null);
  }
  public VisualState(
      String id,
      int hintIndex,
      String algorithm,
      String visualPlan,
      String status,
      ArrayValue array,
      Map<String, Pointer> pointers,
      Map<String, Scalar> scalars,
      IndexRange range,
      List<String> notes,
      SortFrame sortFrame,
      QuickFrame quickFrame,
      MergeFrame mergeFrame) {
    this(
        id,
        hintIndex,
        algorithm,
        visualPlan,
        status,
        array,
        pointers,
        scalars,
        range,
        notes,
        sortFrame,
        quickFrame,
        mergeFrame,
        null);
  }

  public VisualState(
      String id,
      int hintIndex,
      String algorithm,
      String visualPlan,
      String status,
      ArrayValue array,
      Map<String, Pointer> pointers,
      Map<String, Scalar> scalars,
      IndexRange range,
      List<String> notes,
      SortFrame sortFrame,
      QuickFrame quickFrame) {
    this(
        id,
        hintIndex,
        algorithm,
        visualPlan,
        status,
        array,
        pointers,
        scalars,
        range,
        notes,
        sortFrame,
        quickFrame,
        null);
  }

  public VisualState(
      String id,
      int hintIndex,
      String algorithm,
      String visualPlan,
      String status,
      ArrayValue array,
      Map<String, Pointer> pointers,
      Map<String, Scalar> scalars,
      IndexRange range,
      List<String> notes,
      SortFrame sortFrame) {
    this(
        id,
        hintIndex,
        algorithm,
        visualPlan,
        status,
        array,
        pointers,
        scalars,
        range,
        notes,
        sortFrame,
        null,
        null);
  }

  public VisualState(
      String id,
      int hintIndex,
      String algorithm,
      String visualPlan,
      String status,
      ArrayValue array,
      Map<String, Pointer> pointers,
      Map<String, Scalar> scalars,
      IndexRange range,
      List<String> notes) {
    this(
        id,
        hintIndex,
        algorithm,
        visualPlan,
        status,
        array,
        pointers,
        scalars,
        range,
        notes,
        null,
        null,
        null);
  }

  public VisualState {
    pointers = Collections.unmodifiableMap(new LinkedHashMap<>(pointers));
    scalars = Collections.unmodifiableMap(new LinkedHashMap<>(scalars));
    notes = List.copyOf(notes);
  }

  public record ArrayValue(
      String variableName,
      String astNodeId,
      String status,
      List<Object> values,
      Integer length,
      boolean truncated) {
    public ArrayValue {
      // A Java reference array may legitimately contain null elements.
      values = Collections.unmodifiableList(new ArrayList<>(values));
    }
  }

  public record Pointer(String variableName, String astNodeId, Long index, String status) {}

  public record Scalar(String variableName, String astNodeId, Object value, String status) {}

  /** [start, endExclusive); a pointer-derived span, never proof of accumulator membership. */
  public record IndexRange(String kind, Long start, Long endExclusive, String status) {}
}
