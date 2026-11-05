package com.aisc.algoviz.ast.engine.visualization;

import com.aisc.algoviz.ast.engine.algorithm.LocalVariableBindings;
import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.aisc.algoviz.ast.engine.model.ExecutionObservation;
import com.aisc.algoviz.ast.engine.model.MergeFrame;
import com.aisc.algoviz.ast.engine.model.ObservationResult;
import com.aisc.algoviz.ast.engine.model.SortFrame.Comparison;
import com.aisc.algoviz.ast.engine.model.SortFrame.Mutation;
import com.aisc.algoviz.ast.engine.model.SortPattern.Direction;
import com.aisc.algoviz.ast.engine.model.VisualState;
import com.aisc.algoviz.ast.engine.model.VisualState.ArrayValue;
import com.aisc.algoviz.ast.engine.model.VisualState.IndexRange;
import com.aisc.algoviz.ast.engine.model.VisualState.Pointer;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Projects the proven auxiliary-array Merge flow onto JDI snapshots. */
final class MergeTraceBuilder {
  private record Binding(
      int index,
      AlgorithmHint hint,
      MethodDeclaration method,
      String choiceStatementId,
      BinaryExpr comparison) {}

  private record FrameKey(long threadId, Long invocationId) {}

  private static final class CallCandidate {
    final NameExpr array;
    final long sequence;
    final int callerDepth;
    final long arrayObjectId;
    Long rootInvocationId;

    CallCandidate(NameExpr array, long sequence, int callerDepth, long arrayObjectId) {
      this.array = array;
      this.sequence = sequence;
      this.callerDepth = callerDepth;
      this.arrayObjectId = arrayObjectId;
    }
  }

  private final List<Binding> bindings;
  private final Map<FrameKey, ExecutionObservation> previousByFrame = new HashMap<>();
  private final Map<Integer, Map<FrameKey, CallCandidate>> callsByHint = new HashMap<>();

  MergeTraceBuilder(List<AlgorithmHint> hints, Map<String, Node> nodes) {
    List<Binding> found = new ArrayList<>();
    for (int index = 0; index < hints.size(); index++) {
      AlgorithmHint hint = hints.get(index);
      if (hint.merge() == null || !hint.type().equals("merge-sort")) continue;
      Node owner = nodes.get(hint.methodAstNodeId());
      Node comparison = nodes.get(hint.merge().operationAstNodeIds().get("comparison"));
      if (!(owner instanceof MethodDeclaration method)
          || !(comparison instanceof BinaryExpr binary)) continue;
      Node choice =
          comparison.findAncestor(com.github.javaparser.ast.stmt.IfStmt.class).orElse(null);
      if (choice != null)
        found.add(new Binding(index, hint, method, AstAnalyzer.nodeId(choice), binary));
    }
    bindings = List.copyOf(found);
  }

  List<VisualState> states(ExecutionObservation step, Node point, String mainClass) {
    if (step.context() == null || point == null) return List.of();
    List<VisualState> result = new ArrayList<>();
    for (Binding binding : bindings) {
      if (!step.context().className().equals(mainClass)) continue;
      FrameKey key = new FrameKey(step.context().threadId(), step.context().invocationId());
      Map<FrameKey, CallCandidate> calls =
          callsByHint.computeIfAbsent(binding.index(), unused -> new HashMap<>());
      if (point.findAncestor(MethodDeclaration.class).orElse(null) == binding.method()
          && step.context().methodName().equals(binding.method().getNameAsString())) {
        ExecutionObservation previous = previousByFrame.put(key, step);
        result.add(inMethod(binding, step, previous, calls));
      } else {
        CallCandidate earlier = calls.get(key);
        if (earlier != null && earlier.sequence < step.sequence()) {
          calls.remove(key);
          if (earlier.rootInvocationId != null)
            result.add(completedArray(binding, step, earlier.array, earlier.rootInvocationId));
        }
        NameExpr arrayArgument = calledArray(point, binding.method());
        if (arrayArgument != null) {
          ObservationResult observed = resolvedVariable(step, arrayArgument);
          if (observed != null && observed.arrayObjectId() != null)
            calls.put(
                key,
                new CallCandidate(
                    arrayArgument,
                    step.sequence(),
                    step.context().stackDepth(),
                    observed.arrayObjectId()));
        }
      }
    }
    return List.copyOf(result);
  }

  private VisualState inMethod(
      Binding binding,
      ExecutionObservation step,
      ExecutionObservation previous,
      Map<FrameKey, CallCandidate> calls) {
    AlgorithmHint hint = binding.hint();
    Map<String, String> operations = hint.merge().operationAstNodeIds();
    ObservationResult arrayVariable = value(hint, step, "array");
    ArrayValue array =
        array(
            arrayVariable,
            hint.variables().get("array"),
            hint.variableDeclarationIds().get("array"));
    ArrayValue auxiliary =
        array(
            value(hint, step, "aux"),
            hint.variables().get("aux"),
            hint.variableDeclarationIds().get("aux"));
    Integer low = integer(value(hint, step, "low"));
    Integer mid = integer(value(hint, step, "mid"));
    Integer high = integer(value(hint, step, "high"));
    Integer left = integer(value(hint, step, "left"));
    Integer right = integer(value(hint, step, "right"));
    Integer write = integer(value(hint, step, "write"));
    Integer index = integer(value(hint, step, "index"));
    if (low != null
        && high != null
        && array.length() != null
        && low == 0
        && high == array.length() - 1
        && arrayVariable != null
        && arrayVariable.arrayObjectId() != null
        && step.context().invocationId() != null)
      calls.forEach(
          (key, candidate) -> {
            if (key.threadId() == step.context().threadId()
                && candidate.callerDepth < step.context().stackDepth()
                && candidate.arrayObjectId == arrayVariable.arrayObjectId())
              candidate.rootInvocationId = step.context().invocationId();
          });

    Comparison comparison = null;
    if (step.statementAstNodeId().equals(binding.choiceStatementId())
        && inArray(array, left)
        && inArray(array, right)
        && array.values().get(left) instanceof Number first
        && array.values().get(right) instanceof Number second) {
      boolean reversed =
          SortRuntimeComparison.arrayIndex(
              binding.comparison().getLeft(), hint.variables().get("right"));
      boolean takeLeft =
          SortRuntimeComparison.evaluate(
              binding.comparison(), first.intValue(), second.intValue(), reversed);
      comparison =
          new Comparison(
              left,
              right,
              first,
              second,
              null,
              "AVAILABLE_FROM_SNAPSHOT",
              takeLeft ? "TAKE_LEFT" : "TAKE_RIGHT",
              takeLeft);
    }
    Mutation pending = pending(operations, step, array, auxiliary, low, left, right, write, index);
    Mutation completed = completed(operations, hint, step, previous, array, auxiliary);
    String mergedRangeStatus = "UNKNOWN";
    Integer verifiedLow = null;
    Integer verifiedHigh = null;
    if (previous != null && low != null && mid != null && high != null) {
      if (previous.statementAstNodeId().equals(operations.get("leftCall"))
          && step.statementAstNodeId().equals(operations.get("rightCall"))) {
        verifiedLow = low;
        verifiedHigh = mid;
      } else if (previous.statementAstNodeId().equals(operations.get("rightCall"))
          && step.statementAstNodeId().equals(operations.get("auxAllocation"))) {
        verifiedLow = mid + 1;
        verifiedHigh = high;
      }
      if (verifiedLow != null)
        mergedRangeStatus = rangeStatus(array, verifiedLow, verifiedHigh, hint.merge().direction());
    }
    if (step.statementAstNodeId().equals(operations.get("copyBackLoop"))
        && low != null
        && high != null
        && index != null
        && index > high
        && auxiliary.status().equals("AVAILABLE")
        && !auxiliary.truncated()
        && auxiliary.length() == high - low + 1) {
      verifiedLow = low;
      verifiedHigh = high;
      mergedRangeStatus = rangeStatus(array, low, high, hint.merge().direction());
    }
    String phase =
        comparison != null
            ? "MERGE_COMPARE"
            : pending != null
                ? pending.kind()
                : completed != null
                    ? completed.kind()
                    : mergedRangeStatus.equals("VERIFIED_SORTED")
                        ? "MERGED_RANGE_VERIFIED"
                        : "MERGE_PROGRESS";
    MergeFrame frame =
        new MergeFrame(
            phase,
            step.context().invocationId(),
            low,
            mid,
            high,
            left,
            right,
            write,
            index,
            auxiliary,
            comparison,
            pending,
            completed,
            mergedRangeStatus,
            "UNKNOWN",
            "BEFORE_LOCATION",
            verifiedLow,
            verifiedHigh);
    Map<String, Pointer> pointers = new LinkedHashMap<>();
    pointers.put("low", pointer(hint, "low", low, array));
    pointers.put("mid", pointer(hint, "mid", mid, array));
    pointers.put("high", pointer(hint, "high", high, array));
    pointers.put("left", pointer(hint, "left", left, array));
    pointers.put("right", pointer(hint, "right", right, array));
    pointers.put("write", pointer(hint, "write", write, auxiliary));
    pointers.put("index", pointer(hint, "index", index, array));
    IndexRange range =
        new IndexRange(
            "ACTIVE_MERGE",
            low == null ? null : low.longValue(),
            high == null ? null : high.longValue() + 1,
            low == null || high == null ? "UNKNOWN" : low > high ? "EMPTY" : "VALID");
    return new VisualState(
        "hint-" + binding.index(),
        binding.index(),
        "merge-sort",
        hint.visualPlan(),
        visualStatus(array),
        array,
        pointers,
        Map.of(),
        range,
        List.of(),
        null,
        null,
        frame);
  }

  private VisualState completedArray(
      Binding binding, ExecutionObservation step, NameExpr arrayArgument, Long rootInvocationId) {
    ObservationResult observed = resolvedVariable(step, arrayArgument);
    String declarationId = LocalVariableBindings.resolve(arrayArgument).orElse(null);
    ArrayValue array = array(observed, arrayArgument.getNameAsString(), declarationId);
    String fullStatus =
        rangeStatus(array, 0, array.values().size() - 1, binding.hint().merge().direction());
    MergeFrame frame =
        new MergeFrame(
            "FULL_ARRAY_OBSERVED",
            rootInvocationId,
            0,
            null,
            array.length() == null ? null : array.length() - 1,
            null,
            null,
            null,
            null,
            new ArrayValue(
                binding.hint().variables().get("aux"),
                binding.hint().variableDeclarationIds().get("aux"),
                "UNAVAILABLE",
                List.of(),
                null,
                false),
            null,
            null,
            null,
            fullStatus,
            fullStatus,
            "BEFORE_LOCATION",
            0,
            array.length() == null ? null : array.length() - 1);
    return new VisualState(
        "hint-" + binding.index(),
        binding.index(),
        "merge-sort",
        binding.hint().visualPlan(),
        visualStatus(array),
        array,
        Map.of(),
        Map.of(),
        new IndexRange(
            "FULL_ARRAY",
            0L,
            array.length() == null ? null : array.length().longValue(),
            array.length() == null ? "UNKNOWN" : "VALID"),
        List.of(),
        null,
        null,
        frame);
  }

  private static Mutation pending(
      Map<String, String> ids,
      ExecutionObservation step,
      ArrayValue source,
      ArrayValue aux,
      Integer low,
      Integer left,
      Integer right,
      Integer write,
      Integer index) {
    String point = step.statementAstNodeId();
    Integer from =
        point.equals(ids.get("leftWrite")) || point.equals(ids.get("leftRemainderWrite"))
            ? left
            : point.equals(ids.get("rightWrite")) || point.equals(ids.get("rightRemainderWrite"))
                ? right
                : null;
    if (from != null && inArray(source, from) && inArray(aux, write))
      return new Mutation(
          "AUX_WRITE_PENDING", from, write, source.values().get(from), "PENDING_BEFORE_LOCATION");
    if (point.equals(ids.get("copyBackWrite"))
        && low != null
        && index != null
        && inArray(source, index)
        && inArray(aux, index - low))
      return new Mutation(
          "COPY_BACK_PENDING",
          index - low,
          index,
          aux.values().get(index - low),
          "PENDING_BEFORE_LOCATION");
    return null;
  }

  private static Mutation completed(
      Map<String, String> ids,
      AlgorithmHint hint,
      ExecutionObservation step,
      ExecutionObservation previous,
      ArrayValue source,
      ArrayValue aux) {
    if (previous == null
        || previous.context() == null
        || !Objects.equals(previous.context().invocationId(), step.context().invocationId())
        || previous.context().threadId() != step.context().threadId()) return null;
    String point = previous.statementAstNodeId();
    boolean leftWrite =
        point.equals(ids.get("leftWrite")) || point.equals(ids.get("leftRemainderWrite"));
    boolean rightWrite =
        point.equals(ids.get("rightWrite")) || point.equals(ids.get("rightRemainderWrite"));
    boolean copyBack = point.equals(ids.get("copyBackWrite"));
    if (!leftWrite && !rightWrite && !copyBack) return null;
    ArrayValue beforeSource =
        array(
            value(hint, previous, "array"),
            hint.variables().get("array"),
            hint.variableDeclarationIds().get("array"));
    ArrayValue beforeAux =
        array(
            value(hint, previous, "aux"),
            hint.variables().get("aux"),
            hint.variableDeclarationIds().get("aux"));
    if (beforeSource.truncated() || beforeAux.truncated() || source.truncated() || aux.truncated())
      return null;
    Integer low = integer(value(hint, previous, "low"));
    Integer index = integer(value(hint, previous, "index"));
    Integer from = integer(value(hint, previous, leftWrite ? "left" : "right"));
    Integer to = integer(value(hint, previous, "write"));
    if (copyBack) {
      if (low == null
          || index == null
          || !inArray(beforeAux, index - low)
          || !inArray(source, index)) return null;
      Object expected = beforeAux.values().get(index - low);
      if (!Objects.equals(source.values().get(index), expected)) return null;
      return new Mutation(
          "COPY_BACK_COMPLETED", index - low, index, expected, "OBSERVED_AFTER_WRITE");
    }
    if (!inArray(beforeSource, from) || !inArray(aux, to)) return null;
    Object expected = beforeSource.values().get(from);
    if (!Objects.equals(aux.values().get(to), expected)) return null;
    return new Mutation("AUX_WRITE_COMPLETED", from, to, expected, "OBSERVED_AFTER_WRITE");
  }

  static String rangeStatus(ArrayValue array, int low, int high, Direction direction) {
    if (!array.status().equals("AVAILABLE")
        || array.truncated()
        || low < 0
        || high >= array.values().size()
        || low > high + 1
        || array.length() == null
        || array.length() != array.values().size()) return "UNKNOWN";
    for (int index = low; index <= high; index++)
      if (!(array.values().get(index) instanceof Number)) return "UNKNOWN";
    for (int index = low + 1; index <= high; index++) {
      Number first = (Number) array.values().get(index - 1);
      Number second = (Number) array.values().get(index);
      if (direction == Direction.ASCENDING
          ? first.intValue() > second.intValue()
          : first.intValue() < second.intValue()) return "NOT_SORTED";
    }
    return "VERIFIED_SORTED";
  }

  private static ObservationResult value(
      AlgorithmHint hint, ExecutionObservation step, String role) {
    String id = hint.variableDeclarationIds().get(role);
    String name = hint.variables().get(role);
    return step.variables().stream()
        .filter(
            variable ->
                Objects.equals(variable.astNodeId(), id) && variable.variableName().equals(name))
        .findFirst()
        .orElse(null);
  }

  private static ObservationResult resolvedVariable(ExecutionObservation step, NameExpr name) {
    String id = LocalVariableBindings.resolve(name).orElse(null);
    return step.variables().stream()
        .filter(
            variable ->
                Objects.equals(variable.astNodeId(), id)
                    && variable.variableName().equals(name.getNameAsString()))
        .findFirst()
        .orElse(null);
  }

  private static Integer integer(ObservationResult variable) {
    return variable != null && variable.visualValue() instanceof Number number
        ? number.intValue()
        : null;
  }

  private static ArrayValue array(ObservationResult variable, String name, String id) {
    if (variable == null
        || !variable.visualType().equals("array")
        || !(variable.visualValue() instanceof List<?> observed))
      return new ArrayValue(name, id, "UNAVAILABLE", List.of(), null, false);
    List<Object> values = new ArrayList<>();
    Integer length = null;
    boolean truncated = false;
    for (Object element : observed) {
      if (element instanceof Map<?, ?> marker
          && Boolean.TRUE.equals(marker.get("truncated"))
          && marker.get("length") instanceof Integer total) {
        length = total;
        truncated = true;
      } else if (element == null
          || element instanceof Number
          || element instanceof String
          || element instanceof Boolean) values.add(element);
      else return new ArrayValue(name, id, "UNSUPPORTED", List.of(), null, false);
    }
    if (!truncated) length = values.size();
    return new ArrayValue(name, id, "AVAILABLE", values, length, truncated);
  }

  private static boolean inArray(ArrayValue array, Integer index) {
    return array.status().equals("AVAILABLE")
        && index != null
        && index >= 0
        && index < array.values().size();
  }

  private static String visualStatus(ArrayValue array) {
    return !array.status().equals("AVAILABLE")
        ? "UNAVAILABLE"
        : array.truncated() ? "TRUNCATED" : "READY";
  }

  private static Pointer pointer(AlgorithmHint hint, String role, Integer index, ArrayValue array) {
    return new Pointer(
        hint.variables().get(role),
        hint.variableDeclarationIds().get(role),
        index == null ? null : index.longValue(),
        index == null ? "MISSING" : inArray(array, index) ? "VALID" : "OUT_OF_BOUNDS");
  }

  private static NameExpr calledArray(Node point, MethodDeclaration merge) {
    if (!(point instanceof ExpressionStmt statement)
        || !(statement.getExpression() instanceof MethodCallExpr call)
        || call.getArguments().isEmpty()
        || !(call.getArgument(0) instanceof NameExpr array)
        || LocalVariableBindings.resolve(array).isEmpty()) return null;
    ClassOrInterfaceDeclaration owner =
        merge.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null);
    if (owner == null) return null;
    if (directMergeCall(call, merge)) return array;
    for (MethodDeclaration wrapper : owner.getMethodsByName(call.getNameAsString())) {
      if (wrapper == merge
          || wrapper.getParameters().size() != call.getArguments().size()
          || wrapper.getParameters().isEmpty()
          || !wrapper.getParameter(0).getType().asString().equals("int[]")
          || wrapper.getBody().isEmpty()
          || !SortCallSupport.sameClassCall(call, wrapper)) continue;
      String parameter = wrapper.getParameter(0).getNameAsString();
      boolean passesArray =
          wrapper.getBody().orElseThrow().findAll(MethodCallExpr.class).stream()
              .anyMatch(
                  inner ->
                      directMergeCall(inner, merge)
                          && inner.getArgument(0) instanceof NameExpr argument
                          && argument.getNameAsString().equals(parameter)
                          && LocalVariableBindings.resolve(argument)
                              .orElse("")
                              .equals(AstAnalyzer.nodeId(wrapper.getParameter(0))));
      if (passesArray) return array;
    }
    return null;
  }

  private static boolean directMergeCall(MethodCallExpr call, MethodDeclaration merge) {
    ClassOrInterfaceDeclaration owner =
        merge.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null);
    return owner != null
        && SortCallSupport.sameClassCall(call, merge)
        && call.getNameAsString().equals(merge.getNameAsString())
        && call.getArguments().size() == 3;
  }
}
