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
import java.util.Objects;
import java.util.function.Function;

final class InsertionSortTraceStrategy extends AbstractSortTraceStrategy {
  @Override
  public String algorithm() {
    return "insertion-sort";
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
    var operations = pattern.operationAstNodeIds();
    String pointId = step.statementAstNodeId();
    Integer pass = index(currentValue.apply("pass"));
    Integer scan = index(currentValue.apply("scan"));
    Object key = visual(currentValue.apply("key"));
    Mutation completed =
        completedMutation(
            previous,
            step,
            previousArray,
            array,
            previousValue,
            operations.get("shiftWrite"),
            operations.get("insertWrite"));
    String phase =
        terminalPoint
            ? "SORT_FINISHED"
            : completed == null
                ? "LOOP_PROGRESS"
                : completed.kind().equals("SHIFT_RIGHT_COMPLETED")
                    ? "SHIFT_RIGHT_COMPLETED"
                    : "INSERT_KEY_COMPLETED";
    Comparison comparison = null;
    Mutation pending = null;

    if (pointId.equals(pattern.conditionStatementAstNodeId())) {
      if (scan != null && scan < 0) {
        comparison =
            new Comparison(
                scan, null, null, key, null, "LEFT_BOUNDARY_REACHED", "STOP_SHIFTING", false);
        phase = "INSERTION_BOUNDARY_REACHED";
      } else if (inside(array, scan) && key instanceof Number keyValue) {
        Object scanned = array.values().get(scan);
        if (scanned instanceof Number scanValue) {
          boolean shiftRequired =
              pattern.direction() == Direction.ASCENDING
                  ? scanValue.doubleValue() > keyValue.doubleValue()
                  : scanValue.doubleValue() < keyValue.doubleValue();
          comparison =
              new Comparison(
                  scan,
                  null,
                  scanned,
                  key,
                  null,
                  "AVAILABLE_FROM_SNAPSHOT",
                  shiftRequired ? "SHIFT_RIGHT" : "STOP_SHIFTING",
                  shiftRequired);
          phase = "INSERTION_COMPARE";
        }
      }
      if (comparison == null) {
        comparison =
            new Comparison(scan, null, null, key, null, "OPERAND_UNAVAILABLE", "SHIFT_RIGHT", null);
        phase = "INSERTION_COMPARE";
      }
    }

    if (pointId.equals(operations.get("shiftWrite")) && inside(array, scan)) {
      pending =
          new Mutation(
              "SHIFT_RIGHT", scan, scan + 1, array.values().get(scan), "PENDING_BEFORE_LOCATION");
      phase = "SHIFT_RIGHT_PENDING";
    } else if (pointId.equals(operations.get("insertWrite"))
        && pass != null
        && scan != null
        && inside(array, pass)
        && scan < pass) {
      pending = new Mutation("INSERT_KEY", pass, scan + 1, key, "PENDING_BEFORE_LOCATION");
      phase = "INSERT_KEY_PENDING";
    }

    SortedRegion region =
        terminalPoint
            ? terminalRegion(array, pattern.direction())
            : insertionSortedPrefix(array, currentValue.apply("pass"), pattern.direction());
    if (completed != null && comparison != null)
      phase =
          completed.kind().equals("SHIFT_RIGHT_COMPLETED")
              ? "SHIFT_COMPLETED_AND_COMPARISON"
              : "INSERT_COMPLETED_AND_COMPARISON";
    return new SortFrame(phase, comparison, pending, completed, region, "BEFORE_LOCATION");
  }

  private Mutation completedMutation(
      ExecutionObservation previous,
      ExecutionObservation step,
      ArrayValue previousArray,
      ArrayValue array,
      Function<String, ObservationResult> previousValue,
      String shiftWriteId,
      String insertWriteId) {
    if (previous == null
        || previous.sequence() + 1 != step.sequence()
        || !sameInvocation(previous.context(), step.context())
        || previousArray == null
        || !previousArray.status().equals("AVAILABLE")
        || previousArray.truncated()
        || !array.status().equals("AVAILABLE")
        || array.truncated()) return null;

    String previousPoint = previous.statementAstNodeId();
    if (shiftWriteId.equals(previousPoint)) {
      Integer from = index(previousValue.apply("scan"));
      if (inside(previousArray, from)
          && inside(array, from + 1)
          && Objects.equals(array.values().get(from + 1), previousArray.values().get(from))) {
        return new Mutation(
            "SHIFT_RIGHT_COMPLETED",
            from,
            from + 1,
            previousArray.values().get(from),
            "OBSERVED_AFTER_WRITE");
      }
    }

    if (insertWriteId.equals(previousPoint)) {
      Integer destination = index(previousValue.apply("scan"));
      Object key = visual(previousValue.apply("key"));
      if (destination != null) destination++;
      if (key != null
          && inside(previousArray, destination)
          && inside(array, destination)
          && Objects.equals(array.values().get(destination), key)) {
        return new Mutation(
            "INSERT_KEY_COMPLETED",
            index(previousValue.apply("pass")),
            destination,
            key,
            "OBSERVED_AFTER_WRITE");
      }
    }
    return null;
  }

  private SortedRegion insertionSortedPrefix(
      ArrayValue array, ObservationResult pass, Direction direction) {
    Long count = pass == null ? null : integer(pass.visualValue());
    if (count == null || array.length() == null || count < 0 || count > array.length())
      return new SortedRegion(0, 0, "UNKNOWN", "");
    return checkedRegion(array, 0, count.intValue(), direction, "INSERTION_PREFIX_RUNTIME_CHECK");
  }

  private Object visual(ObservationResult observation) {
    return observation == null ? null : observation.visualValue();
  }
}
