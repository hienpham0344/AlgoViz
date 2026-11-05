package com.aisc.algoviz.ast.engine.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** One suspended JDI breakpoint hit and the visible locals captured at that moment. */
public record ExecutionObservation(
    long sequence,
    String statementAstNodeId,
    int lineNumber,
    int startLine,
    int endLine,
    int startColumn,
    int endColumn,
    String statementKind,
    String code,
    List<ObservationResult> variables,
    ExecutionContext context,
    List<VisualState> visualStates,
    List<VisualEvent> visualEvents) {

  public ExecutionObservation(
      long sequence,
      String statementAstNodeId,
      int lineNumber,
      int startLine,
      int endLine,
      int startColumn,
      int endColumn,
      String statementKind,
      String code,
      List<ObservationResult> variables,
      ExecutionContext context) {
    this(
        sequence,
        statementAstNodeId,
        lineNumber,
        startLine,
        endLine,
        startColumn,
        endColumn,
        statementKind,
        code,
        variables,
        context,
        List.of(),
        List.of());
  }

  public ExecutionObservation withVisualization(
      List<VisualState> states, List<VisualEvent> events) {
    return new ExecutionObservation(
        sequence,
        statementAstNodeId,
        lineNumber,
        startLine,
        endLine,
        startColumn,
        endColumn,
        statementKind,
        code,
        variables,
        context,
        states,
        events);
  }

  /** A breakpoint precedes its bytecode location, not necessarily an entire AST statement. */
  @JsonProperty("snapshotPhase")
  public String snapshotPhase() {
    return "BEFORE_LOCATION";
  }

  @JsonProperty("granularity")
  public String granularity() {
    return "LINE_BREAKPOINT";
  }

  public ExecutionObservation {
    if (sequence < 1) {
      throw new IllegalArgumentException("Execution sequence must be positive.");
    }
    variables = List.copyOf(variables);
    visualStates = List.copyOf(visualStates);
    visualEvents = List.copyOf(visualEvents);
  }
}
