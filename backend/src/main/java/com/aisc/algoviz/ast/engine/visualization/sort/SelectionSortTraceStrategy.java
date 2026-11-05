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
import java.util.Objects;
import java.util.function.Function;

final class SelectionSortTraceStrategy extends AbstractSortTraceStrategy {
  @Override
  public String algorithm() {
    return "selection-sort";
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
    Integer selected = index(currentValue.apply("selected"));
    Integer pass = index(currentValue.apply("pass"));
    Mutation completed =
        completedSwapAt(
            hint,
            previous,
            step,
            previousArray,
            array,
            previousValue,
            previous == null ? null : index(previousValue.apply("pass")),
            previous == null ? null : index(previousValue.apply("selected")));
    String phase = basePhase(terminalPoint, completed);
    Comparison comparison = null;
    Mutation pending = null;

    if (pointId.equals(pattern.conditionStatementAstNodeId())) {
      phase = completed == null ? "SELECTION_COMPARE" : "SWAP_COMPLETED_AND_COMPARISON";
      comparison =
          comparison(
              array,
              scan,
              selected,
              pattern.direction() == Direction.DESCENDING,
              "UPDATE_SELECTED_INDEX");
    }

    String updateId = pattern.operationAstNodeIds().get("selectedIndexUpdate");
    if (updateId != null
        && pointId.equals(updateId)
        && selectedIndexUpdateIsPending(
            previous, step, previousArray, previousValue, pattern.conditionStatementAstNodeId())) {
      pending =
          new Mutation("UPDATE_SELECTED_INDEX", selected, scan, scan, "PENDING_BEFORE_LOCATION");
      phase = "SELECTION_UPDATE_PENDING";
    }

    List<String> swap = pattern.swapStatementAstNodeIds();
    if (swap.size() == 3 && pass != null && selected != null) {
      Mutation swapPending = pendingSwap(pointId, swap, array, pass, selected, currentValue);
      if (swapPending != null) {
        pending = swapPending;
        phase = pendingPhase(swap, pointId);
      }
    }

    SortedRegion region =
        terminalPoint
            ? terminalRegion(array, pattern.direction())
            : selectionSortedRegion(array, currentValue.apply("pass"), pattern.direction());
    return new SortFrame(phase, comparison, pending, completed, region, "BEFORE_LOCATION");
  }

  private boolean selectedIndexUpdateIsPending(
      ExecutionObservation previous,
      ExecutionObservation step,
      ArrayValue previousArray,
      Function<String, ObservationResult> previousValue,
      String conditionId) {
    if (previous == null
        || previous.sequence() + 1 != step.sequence()
        || !previous.statementAstNodeId().equals(conditionId)
        || !sameInvocation(previous.context(), step.context())
        || previousArray == null
        || !previousArray.status().equals("AVAILABLE")
        || previousArray.truncated()) return false;
    Integer scan = index(previousValue.apply("scan"));
    Integer selected = index(previousValue.apply("selected"));
    if (!inside(previousArray, scan) || !inside(previousArray, selected)) return false;
    return previous.visualStates().stream()
        .filter(state -> state.algorithm().equals("selection-sort"))
        .map(state -> state.sortFrame())
        .filter(Objects::nonNull)
        .map(SortFrame::comparison)
        .filter(Objects::nonNull)
        .anyMatch(c -> Boolean.TRUE.equals(c.actionRequired()));
  }

  private SortedRegion selectionSortedRegion(
      ArrayValue array, ObservationResult pass, Direction direction) {
    Long count = pass == null ? null : integer(pass.visualValue());
    if (count == null || array.length() == null || count < 0 || count > array.length())
      return new SortedRegion(0, 0, "UNKNOWN", "");
    return checkedRegion(array, 0, count.intValue(), direction, "SELECTION_PREFIX_RUNTIME_CHECK");
  }
}
