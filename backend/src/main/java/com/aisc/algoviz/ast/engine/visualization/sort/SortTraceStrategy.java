package com.aisc.algoviz.ast.engine.visualization.sort;

import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.aisc.algoviz.ast.engine.model.ExecutionObservation;
import com.aisc.algoviz.ast.engine.model.ObservationResult;
import com.aisc.algoviz.ast.engine.model.SortFrame;
import com.aisc.algoviz.ast.engine.model.VisualState.ArrayValue;
import java.util.function.Function;

interface SortTraceStrategy {
  String algorithm();

  SortFrame build(
      AlgorithmHint hint,
      ExecutionObservation previous,
      ExecutionObservation step,
      ArrayValue previousArray,
      ArrayValue array,
      Function<String, ObservationResult> currentValue,
      Function<String, ObservationResult> previousValue,
      boolean terminalPoint);
}
