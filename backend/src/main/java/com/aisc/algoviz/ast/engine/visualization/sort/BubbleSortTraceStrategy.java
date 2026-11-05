package com.aisc.algoviz.ast.engine.visualization.sort;

import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.aisc.algoviz.ast.engine.model.ExecutionObservation;
import com.aisc.algoviz.ast.engine.model.ObservationResult;
import com.aisc.algoviz.ast.engine.model.SortFrame;
import com.aisc.algoviz.ast.engine.model.SortFrame.Comparison;
import com.aisc.algoviz.ast.engine.model.SortFrame.Mutation;
import com.aisc.algoviz.ast.engine.model.SortFrame.SortedRegion;
import com.aisc.algoviz.ast.engine.model.SortPattern.Direction;
import com.aisc.algoviz.ast.engine.model.VisualState.ArrayValue;
import java.util.List;
import java.util.function.Function;

final class BubbleSortTraceStrategy extends AbstractSortTraceStrategy {
  @Override
  public String algorithm() {
    return "bubble-sort";
  }

  @Override
  public SortFrame build(
      AlgorithmHint hint,
      ExecutionObservation previous,
      ExecutionObservation step,
      ArrayValue previousArray,
      ArrayValue array,
      Function<String, ObservationResult> currentValue,
      Function<String, ObservationResult> previousValue,
      boolean terminalPoint) {
    var pattern = hint.sort();
    String pointId = step.statementAstNodeId();
    Integer scan = index(currentValue.apply("scan"));
    Integer previousScan = previous == null ? null : index(previousValue.apply("scan"));
    Mutation completed =
        completedSwapAt(
            hint,
            previous,
            step,
            previousArray,
            array,
            previousValue,
            previousScan,
            previousScan == null ? null : previousScan + 1);
    String phase = basePhase(terminalPoint, completed);
    Comparison comparison = null;
    Mutation pending = null;

    if (pointId.equals(pattern.conditionStatementAstNodeId())) {
      phase = completed == null ? "COMPARISON" : "SWAP_COMPLETED_AND_COMPARISON";
      comparison =
          comparison(
              array,
              scan,
              scan == null ? null : scan + 1,
              pattern.direction() == Direction.ASCENDING,
              "SWAP");
    }

    List<String> swap = pattern.swapStatementAstNodeIds();
    if (swap.size() == 3 && scan != null) {
      pending = pendingSwap(pointId, swap, array, scan, scan + 1, currentValue);
      if (pending != null) phase = pendingPhase(swap, pointId);
    }

    SortedRegion region =
        terminalPoint
            ? terminalRegion(array, pattern.direction())
            : bubbleSortedRegion(array, currentValue.apply("pass"), pattern.direction());
    return new SortFrame(phase, comparison, pending, completed, region, "BEFORE_LOCATION");
  }

  private SortedRegion bubbleSortedRegion(
      ArrayValue array, ObservationResult pass, Direction direction) {
    Long count = pass == null ? null : integer(pass.visualValue());
    if (count == null || array.length() == null || count < 0 || count > array.length())
      return new SortedRegion(0, 0, "UNKNOWN", "");
    int start = Math.max(0, array.length() - count.intValue());
    return checkedRegion(array, start, array.length(), direction, "BUBBLE_SUFFIX_RUNTIME_CHECK");
  }
}
