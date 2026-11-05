package com.aisc.algoviz.ast.engine.visualization.sort;

import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.aisc.algoviz.ast.engine.model.ExecutionObservation;
import com.aisc.algoviz.ast.engine.model.ObservationResult;
import com.aisc.algoviz.ast.engine.model.SortFrame;
import com.aisc.algoviz.ast.engine.model.VisualState.ArrayValue;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Routes a hint to its algorithm-specific runtime trace strategy. */
public final class SortTraceBuilder {
  private final Map<String, SortTraceStrategy> strategies =
      List.<SortTraceStrategy>of(
              new BubbleSortTraceStrategy(),
              new SelectionSortTraceStrategy(),
              new InsertionSortTraceStrategy())
          .stream()
          .collect(Collectors.toUnmodifiableMap(SortTraceStrategy::algorithm, Function.identity()));

  public SortFrame build(
      AlgorithmHint hint,
      ExecutionObservation previous,
      ExecutionObservation step,
      ArrayValue previousArray,
      ArrayValue array,
      Function<String, ObservationResult> currentValue,
      Function<String, ObservationResult> previousValue,
      boolean terminalPoint) {
    SortTraceStrategy strategy = strategies.get(hint.type());
    if (hint.sort() == null || strategy == null) return null;
    return strategy.build(
        hint, previous, step, previousArray, array, currentValue, previousValue, terminalPoint);
  }
}
