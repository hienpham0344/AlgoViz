package com.aisc.algoviz.ast.engine.visualization.sort;

import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.aisc.algoviz.ast.engine.model.ExecutionContext;
import com.aisc.algoviz.ast.engine.model.ExecutionObservation;
import com.aisc.algoviz.ast.engine.model.ObservationResult;
import com.aisc.algoviz.ast.engine.model.SortFrame.Comparison;
import com.aisc.algoviz.ast.engine.model.SortFrame.Mutation;
import com.aisc.algoviz.ast.engine.model.SortFrame.SortedRegion;
import com.aisc.algoviz.ast.engine.model.SortPattern.Direction;
import com.aisc.algoviz.ast.engine.model.VisualState.ArrayValue;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

abstract class AbstractSortTraceStrategy implements SortTraceStrategy {
  protected Comparison comparison(
      ArrayValue array,
      Integer firstIndex,
      Integer secondIndex,
      boolean predicateGreater,
      String action) {
    if (!inside(array, firstIndex) || !inside(array, secondIndex))
      return new Comparison(
          firstIndex,
          secondIndex,
          valueAt(array, firstIndex),
          valueAt(array, secondIndex),
          null,
          "OPERAND_UNAVAILABLE",
          action,
          null);
    Object first = array.values().get(firstIndex);
    Object second = array.values().get(secondIndex);
    if (!(first instanceof Number a)
        || !(second instanceof Number b)
        || !Double.isFinite(a.doubleValue())
        || !Double.isFinite(b.doubleValue()))
      return new Comparison(
          firstIndex, secondIndex, first, second, null, "NON_NUMERIC_VALUE", action, null);
    boolean required =
        predicateGreater ? a.doubleValue() > b.doubleValue() : a.doubleValue() < b.doubleValue();
    return new Comparison(
        firstIndex,
        secondIndex,
        first,
        second,
        action.equals("SWAP") ? required : null,
        "AVAILABLE_FROM_SNAPSHOT",
        action,
        required);
  }

  protected Mutation pendingSwap(
      String pointId,
      List<String> statements,
      ArrayValue array,
      int fromIndex,
      int toIndex,
      Function<String, ObservationResult> currentValue) {
    if (!inside(array, fromIndex) || !inside(array, toIndex)) return null;
    if (pointId.equals(statements.get(0)))
      return new Mutation(
          "SAVE_LEFT_VALUE",
          fromIndex,
          toIndex,
          array.values().get(fromIndex),
          "PENDING_BEFORE_LOCATION");
    if (pointId.equals(statements.get(1)))
      return new Mutation(
          "WRITE_RIGHT_VALUE_TO_LEFT",
          toIndex,
          fromIndex,
          array.values().get(toIndex),
          "PENDING_BEFORE_LOCATION");
    if (pointId.equals(statements.get(2))) {
      ObservationResult temp = currentValue.apply("temp");
      return new Mutation(
          "WRITE_SAVED_VALUE_TO_RIGHT",
          fromIndex,
          toIndex,
          temp == null ? null : temp.visualValue(),
          temp == null ? "VALUE_UNAVAILABLE" : "PENDING_BEFORE_LOCATION");
    }
    return null;
  }

  protected Mutation completedSwapAt(
      AlgorithmHint hint,
      ExecutionObservation previous,
      ExecutionObservation step,
      ArrayValue previousArray,
      ArrayValue currentArray,
      Function<String, ObservationResult> previousValue,
      Integer fromIndex,
      Integer toIndex) {
    var statements = hint.sort().swapStatementAstNodeIds();
    if (previous == null
        || previous.sequence() + 1 != step.sequence()
        || fromIndex == null
        || toIndex == null
        || statements.size() != 3
        || !previous.statementAstNodeId().equals(statements.get(2))
        || !(sameInvocation(previous.context(), step.context())
            || helperReturned(hint, previous.context(), step.context()))) return null;
    if (previousArray == null
        || !previousArray.status().equals("AVAILABLE")
        || !currentArray.status().equals("AVAILABLE")
        || previousArray.truncated()
        || currentArray.truncated()
        || !inside(previousArray, fromIndex)
        || !inside(previousArray, toIndex)
        || !inside(currentArray, fromIndex)
        || !inside(currentArray, toIndex)) return null;
    ObservationResult temp = previousValue.apply("temp");
    Object copiedValue = previousArray.values().get(toIndex);
    if (temp == null
        || !Objects.equals(previousArray.values().get(fromIndex), copiedValue)
        || !Objects.equals(currentArray.values().get(fromIndex), copiedValue)
        || !Objects.equals(currentArray.values().get(toIndex), temp.visualValue())) return null;
    return new Mutation(
        fromIndex.equals(toIndex) ? "SELF_SWAP_COMPLETED" : "SWAP_COMPLETED",
        fromIndex,
        toIndex,
        temp.visualValue(),
        "OBSERVED_AFTER_WRITE");
  }

  protected String pendingPhase(List<String> statements, String pointId) {
    if (pointId.equals(statements.get(0))) return "SWAP_SAVE_PENDING";
    if (pointId.equals(statements.get(1))) return "SWAP_LEFT_WRITE_PENDING";
    return "SWAP_RIGHT_WRITE_PENDING";
  }

  protected String basePhase(boolean terminalPoint, Mutation completed) {
    if (terminalPoint) return "SORT_FINISHED";
    if (completed == null) return "LOOP_PROGRESS";
    return completed.kind().equals("SELF_SWAP_COMPLETED")
        ? "SELF_SWAP_COMPLETED"
        : "SWAP_COMPLETED";
  }

  protected boolean sameInvocation(ExecutionContext first, ExecutionContext second) {
    return first != null
        && second != null
        && first.threadId() == second.threadId()
        && first.stackDepth() == second.stackDepth()
        && Objects.equals(first.className(), second.className())
        && Objects.equals(first.methodName(), second.methodName())
        && Objects.equals(first.methodSignature(), second.methodSignature());
  }

  private boolean helperReturned(
      AlgorithmHint hint, ExecutionContext helper, ExecutionContext caller) {
    if (helper == null || caller == null
        || !hint.sort().operationAstNodeIds().containsKey("swapHelper")) return false;
    return helper.threadId() == caller.threadId()
        && helper.stackDepth() == caller.stackDepth() + 1
        && Objects.equals(helper.className(), caller.className())
        && !Objects.equals(helper.methodName(), caller.methodName());
  }

  protected SortedRegion terminalRegion(ArrayValue array, Direction direction) {
    if (!array.status().equals("AVAILABLE") || array.length() == null)
      return new SortedRegion(0, 0, "UNKNOWN", "ARRAY_UNAVAILABLE");
    if (array.truncated())
      return new SortedRegion(0, array.length(), "TRUNCATED", "RUNTIME_ORDER_CHECK");
    return checkedRegion(array, 0, array.length(), direction, "RUNTIME_ORDER_CHECK");
  }

  protected SortedRegion checkedRegion(
      ArrayValue array, int start, int end, Direction direction, String basis) {
    if (array == null
        || !array.status().equals("AVAILABLE")
        || array.truncated()
        || end > array.values().size()
        || start < 0
        || end < start) return new SortedRegion(start, end, "UNKNOWN", basis);
    for (int index = start + 1; index < end; index++) {
      Object previous = array.values().get(index - 1);
      Object current = array.values().get(index);
      if (!(previous instanceof Number left) || !(current instanceof Number right))
        return new SortedRegion(start, end, "UNKNOWN", "NON_NUMERIC_VALUE");
      int order = Double.compare(left.doubleValue(), right.doubleValue());
      if (direction == Direction.ASCENDING ? order > 0 : order < 0) {
        return new SortedRegion(start, end, "NOT_SORTED", basis, index, previous, current);
      }
    }
    String status =
        start == end
            ? basis.equals("RUNTIME_ORDER_CHECK") ? "VERIFIED_SORTED" : "EMPTY"
            : "VERIFIED_SORTED";
    return new SortedRegion(start, end, status, basis);
  }

  protected Integer index(ObservationResult value) {
    Long number = value == null ? null : integer(value.visualValue());
    return number == null ? null : number.intValue();
  }

  protected Long integer(Object value) {
    if (value instanceof Byte
        || value instanceof Short
        || value instanceof Integer
        || value instanceof Long) {
      long number = ((Number) value).longValue();
      if (number >= Integer.MIN_VALUE && number <= Integer.MAX_VALUE) return number;
    }
    return null;
  }

  private Object valueAt(ArrayValue array, Integer index) {
    return inside(array, index) ? array.values().get(index) : null;
  }

  protected boolean inside(ArrayValue array, Integer index) {
    return array != null
        && array.status().equals("AVAILABLE")
        && index != null
        && index >= 0
        && index < array.values().size();
  }
}
