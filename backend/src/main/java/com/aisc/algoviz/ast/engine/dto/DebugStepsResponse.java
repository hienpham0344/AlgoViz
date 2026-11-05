package com.aisc.algoviz.ast.engine.dto;

import com.aisc.algoviz.ast.engine.model.ExecutionObservation;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import java.util.List;

/** Additive grouped execution response for the visualizer; the legacy debug response is unchanged. */
public record DebugStepsResponse(
    List<ExecutionObservation> steps, List<String> warnings, List<AlgorithmHint> algorithmHints) {

  public DebugStepsResponse(List<ExecutionObservation> steps, List<String> warnings) {
    this(steps, warnings, List.of());
  }

  public DebugStepsResponse {
    steps = List.copyOf(steps);
    warnings = List.copyOf(warnings);
    algorithmHints = List.copyOf(algorithmHints);
  }
}
