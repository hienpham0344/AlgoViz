package com.aisc.algoviz.ast.engine.visualization;

import com.aisc.algoviz.ast.engine.algorithm.LocalVariableBindings;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.aisc.algoviz.ast.engine.model.ExecutionObservation;
import com.aisc.algoviz.ast.engine.model.HeapFrame;
import com.aisc.algoviz.ast.engine.model.ObservationResult;
import com.aisc.algoviz.ast.engine.model.SortFrame.Comparison;
import com.aisc.algoviz.ast.engine.model.SortFrame.Mutation;
import com.aisc.algoviz.ast.engine.model.SortPattern.Direction;
import com.aisc.algoviz.ast.engine.model.VisualState;
import com.aisc.algoviz.ast.engine.model.VisualState.ArrayValue;
import com.aisc.algoviz.ast.engine.model.VisualState.IndexRange;
import com.aisc.algoviz.ast.engine.model.VisualState.Pointer;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Joins a source-proven Heap Sort with same-invocation, same-array JDI snapshots. */
final class HeapTraceBuilder {
  private record Binding(
      int index,
      AlgorithmHint hint,
      MethodDeclaration sort,
      MethodDeclaration sift,
      Node inlineSiftLoop,
      BinaryExpr choice,
      BinaryExpr rightChoice,
      MethodDeclaration buildSift,
      Node inlineBuildSiftLoop,
      BinaryExpr buildChoice) {}

  private record FrameKey(long thread, Long invocation) {}

  private static final class Pending {
    final Long arrayId;
    final int from;
    final int to;
    final Object oldFrom;
    final Object oldTo;
    final long sequence;
    boolean firstWriteSeen;
    boolean secondWriteSeen;

    Pending(Long arrayId, int from, int to, Object oldFrom, Object oldTo, long sequence) {
      this.arrayId = arrayId;
      this.from = from;
      this.to = to;
      this.oldFrom = oldFrom;
      this.oldTo = oldTo;
      this.sequence = sequence;
    }
  }

  private static final class CallCandidate {
    final NameExpr array;
    final long sequence;
    final int depth;
    final long arrayId;
    Long rootInvocation;

    CallCandidate(NameExpr array, long sequence, int depth, long arrayId) {
      this.array = array;
      this.sequence = sequence;
      this.depth = depth;
      this.arrayId = arrayId;
    }
  }

  private record SiftCandidate(
      long thread, Long rootInvocation, long arrayId, int callerDepth, long sequence) {}

  private final List<Binding> bindings;
  private final Map<Integer, Map<FrameKey, Pending>> pendingRoot = new HashMap<>();
  private final Map<Integer, Map<FrameKey, Pending>> pendingSift = new HashMap<>();
  private final Map<Integer, Map<FrameKey, CallCandidate>> calls = new HashMap<>();
  private final Map<Integer, Map<FrameKey, SiftCandidate>> siftCalls = new HashMap<>();
  private final Map<Integer, Map<FrameKey, Integer>> verifiedSuffixStart = new HashMap<>();

  HeapTraceBuilder(List<AlgorithmHint> hints, Map<String, Node> nodes) {
    List<Binding> found = new ArrayList<>();
    for (int i = 0; i < hints.size(); i++) {
      AlgorithmHint hint = hints.get(i);
      if (!"heap-sort".equals(hint.type()) || hint.heap() == null) continue;
      Node sort = nodes.get(hint.methodAstNodeId());
      Node sift = nodes.get(hint.heap().operationAstNodeIds().get("siftMethod"));
      if (!(sort instanceof MethodDeclaration sortMethod)
          || !(sift instanceof MethodDeclaration siftMethod)) continue;
      Node choiceNode = nodes.get(hint.heap().operationAstNodeIds().get("siftChoice"));
      Node buildSift = nodes.get(hint.heap().operationAstNodeIds().get("buildSiftMethod"));
      if (!(buildSift instanceof MethodDeclaration buildSiftMethod)) continue;
      found.add(
          new Binding(
              i,
              hint,
              sortMethod,
              siftMethod,
              nodes.get(hint.heap().operationAstNodeIds().get("siftLoop")),
              choice(choiceNode),
              choice(nodes.get(hint.heap().operationAstNodeIds().get("siftChoiceRight"))),
              buildSiftMethod,
              nodes.get(hint.heap().operationAstNodeIds().get("buildSiftLoop")),
              choice(nodes.get(hint.heap().operationAstNodeIds().get("buildSiftChoice")))));
    }
    bindings = List.copyOf(found);
  }

  private static BinaryExpr choice(Node node) {
    if (!(node instanceof IfStmt branch) || !(branch.getCondition() instanceof BinaryExpr binary))
      return null;
    return binary.getOperator() == BinaryExpr.Operator.AND
            && binary.getRight() instanceof BinaryExpr right
        ? right
        : binary;
  }

  private static boolean insideLoop(Node point, Node loop) {
    return loop != null
        && point != loop
        && point
            .findAncestor(com.github.javaparser.ast.stmt.WhileStmt.class)
            .filter(ancestor -> ancestor == loop)
            .isPresent();
  }

  List<VisualState> states(ExecutionObservation step, Node point, String mainClass) {
    if (step.context() == null || point == null || !step.context().className().equals(mainClass))
      return List.of();
    List<VisualState> result = new ArrayList<>();
    for (Binding binding : bindings) {
      FrameKey key = new FrameKey(step.context().threadId(), step.context().invocationId());
      MethodDeclaration owner = point.findAncestor(MethodDeclaration.class).orElse(null);
      if (owner == binding.sort()
          && step.context().methodName().equals(binding.sort().getNameAsString())) {
        boolean inBuild = insideLoop(point, binding.inlineBuildSiftLoop());
        boolean inRestore = insideLoop(point, binding.inlineSiftLoop());
        if (inBuild || inRestore) result.add(inSift(binding, step, key, inBuild));
        else result.add(inSort(binding, step, key));
      } else if ((owner == binding.sift() || owner == binding.buildSift())
          && step.context().methodName().equals(owner.getNameAsString())) {
        boolean building = owner == binding.buildSift() && owner != binding.sift();
        ObservationResult siftArray =
            value(binding.hint(), step, building ? "buildSiftArray" : "siftArray");
        boolean linked =
            siftArray != null
                && siftArray.arrayObjectId() != null
                && siftCalls
                    .computeIfAbsent(binding.index(), unused -> new HashMap<>())
                    .values()
                    .stream()
                    .anyMatch(
                        candidate ->
                            candidate.thread() == key.thread()
                                && candidate.rootInvocation() != null
                                && candidate.callerDepth() < step.context().stackDepth()
                                && candidate.sequence() < step.sequence()
                                && candidate.arrayId() == siftArray.arrayObjectId());
        if (linked) result.add(inSift(binding, step, key, building));
      } else {
        Map<FrameKey, CallCandidate> candidates =
            calls.computeIfAbsent(binding.index(), unused -> new HashMap<>());
        CallCandidate prior = candidates.get(key);
        if (prior != null && prior.sequence < step.sequence()) {
          candidates.remove(key);
          if (prior.rootInvocation != null) result.add(completedArray(binding, step, prior));
        }
        NameExpr argument = calledArray(point, binding.sort());
        if (argument != null) {
          ObservationResult observed = resolved(step, argument);
          if (observed != null && observed.arrayObjectId() != null)
            candidates.put(
                key,
                new CallCandidate(
                    argument,
                    step.sequence(),
                    step.context().stackDepth(),
                    observed.arrayObjectId()));
        }
      }
    }
    return List.copyOf(result);
  }

  private VisualState inSort(Binding binding, ExecutionObservation step, FrameKey key) {
    AlgorithmHint hint = binding.hint();
    Map<String, String> ops = hint.heap().operationAstNodeIds();
    ObservationResult observed = value(hint, step, "array");
    ArrayValue array =
        array(observed, hint.variables().get("array"), hint.variableDeclarationIds().get("array"));
    Integer size = integer(value(hint, step, "size"));
    Integer end = integer(value(hint, step, "end"));
    Integer buildCursor = integer(value(hint, step, "buildCursor"));
    String point = step.statementAstNodeId();
    boolean beforeRootSwap =
        point.equals(ops.get("extractLoop")) || point.equals(ops.get("rootSwapSave"));
    Integer heapEnd = end == null ? size : Integer.valueOf(beforeRootSwap ? end + 1 : end);
    Map<FrameKey, Integer> suffixByFrame =
        verifiedSuffixStart.computeIfAbsent(binding.index(), unused -> new HashMap<>());
    if (size != null && size >= 0) suffixByFrame.putIfAbsent(key, size);
    Map<FrameKey, SiftCandidate> activeSifts =
        siftCalls.computeIfAbsent(binding.index(), unused -> new HashMap<>());
    if ((point.equals(ops.get("buildCall")) || point.equals(ops.get("restoreCall")))
        && observed != null
        && observed.arrayObjectId() != null) {
      activeSifts.put(
          key,
          new SiftCandidate(
              key.thread(),
              key.invocation(),
              observed.arrayObjectId(),
              step.context().stackDepth(),
              step.sequence()));
    } else activeSifts.remove(key);
    Map<FrameKey, CallCandidate> candidates =
        calls.computeIfAbsent(binding.index(), unused -> new HashMap<>());
    if (observed != null
        && observed.arrayObjectId() != null
        && step.context().invocationId() != null) {
      for (var entry : candidates.entrySet()) {
        CallCandidate candidate = entry.getValue();
        if (entry.getKey().thread() == key.thread()
            && candidate.depth < step.context().stackDepth()
            && candidate.arrayId == observed.arrayObjectId())
          candidate.rootInvocation = step.context().invocationId();
      }
    }
    Map<FrameKey, Pending> pending =
        pendingRoot.computeIfAbsent(binding.index(), unused -> new HashMap<>());
    Pending current = pending.get(key);
    Mutation pendingMutation = null;
    Mutation completedMutation = null;
    if (point.equals(ops.get("rootSwapRootWrite"))
        && complete(array)
        && observed != null
        && observed.arrayObjectId() != null
        && validIndex(array, end)) {
      current =
          new Pending(
              observed.arrayObjectId(),
              0,
              end,
              array.values().get(0),
              array.values().get(end),
              step.sequence());
      pending.put(key, current);
      pendingMutation = new Mutation("ROOT_SWAP_PENDING", end, 0, current.oldTo, "PENDING");
    } else if (current != null
        && current.sequence < step.sequence()
        && Objects.equals(current.arrayId, observed == null ? null : observed.arrayObjectId())
        && complete(array)
        && validIndex(array, current.to)) {
      if (point.equals(ops.get("rootSwapEndWrite"))
          && Objects.equals(array.values().get(0), current.oldTo)) current.firstWriteSeen = true;
      if (point.equals(ops.get("restoreCall"))
          && current.firstWriteSeen
          && Objects.equals(array.values().get(0), current.oldTo)
          && Objects.equals(array.values().get(current.to), current.oldFrom))
        current.secondWriteSeen = true;
      if (current.firstWriteSeen && current.secondWriteSeen) {
        completedMutation =
            new Mutation(
                "ROOT_SWAP_COMPLETED", current.from, current.to, current.oldFrom, "OBSERVED");
        suffixByFrame.put(key, current.to);
        pending.remove(key);
      }
    }
    String heapStatus =
        beforeRootSwap ? heapStatus(array, heapEnd, hint.heap().direction()) : "UNKNOWN";
    Integer verifiedStart = suffixByFrame.get(key);
    if (verifiedStart != null && size != null && verifiedStart >= size) verifiedStart = null;
    String suffixStatus =
        sortedStatus(array, verifiedStart, array.length(), hint.heap().direction());
    String phase =
        point.equals(ops.get("buildCall"))
            ? "HEAP_BUILD"
            : point.equals(ops.get("restoreCall"))
                ? "HEAP_RESTORE"
                : pendingMutation != null || completedMutation != null ? "ROOT_SWAP" : "HEAP_SORT";
    Map<String, Pointer> pointers = new LinkedHashMap<>();
    pointers.put("end", pointer(hint, "end", end, array));
    pointers.put("buildCursor", pointer(hint, "buildCursor", buildCursor, array));
    HeapFrame frame =
        new HeapFrame(
            phase,
            step.context().invocationId(),
            heapEnd,
            buildCursor,
            null,
            null,
            null,
            null,
            pendingMutation,
            completedMutation,
            heapStatus,
            suffixStatus,
            "UNKNOWN",
            "BEFORE_LOCATION",
            verifiedStart);
    return state(
        binding,
        array,
        pointers,
        new IndexRange(
            "HEAP",
            0L,
            heapEnd == null ? null : heapEnd.longValue(),
            heapEnd == null ? "UNKNOWN" : "VALID"),
        frame);
  }

  private VisualState inSift(
      Binding binding, ExecutionObservation step, FrameKey key, boolean building) {
    AlgorithmHint hint = binding.hint();
    String prefix = building ? "buildSift" : "sift";
    ObservationResult observed = value(hint, step, prefix + "Array");
    ArrayValue array =
        array(
            observed,
            hint.variables().get(prefix + "Array"),
            hint.variableDeclarationIds().get(prefix + "Array"));
    Integer boundary = integer(value(hint, step, prefix + "Boundary"));
    boolean inclusive =
        building ? hint.heap().buildSiftInclusiveBoundary() : hint.heap().siftInclusiveBoundary();
    Integer heapEnd = boundary == null ? null : boundary + (inclusive ? 1 : 0);
    Integer node = integer(value(hint, step, prefix + "Node"));
    Integer left = integer(value(hint, step, "siftLeft"));
    Integer right = integer(value(hint, step, "siftRight"));
    Integer chosen = integer(value(hint, step, "siftChosen"));
    Integer parent = integer(value(hint, step, "buildSiftParent"));
    if (building) {
      left = null;
      right = null;
      chosen = null;
    }
    Comparison comparison = null;
    boolean rightChoice =
        !building
            && step.statementAstNodeId()
                .equals(hint.heap().operationAstNodeIds().get("siftChoiceRight"));
    BinaryExpr choice =
        building ? binding.buildChoice() : rightChoice ? binding.rightChoice() : binding.choice();
    if (rightChoice
        || step.statementAstNodeId()
            .equals(hint.heap().operationAstNodeIds().get(prefix + "Choice")))
      comparison = comparison(choice, step, array, heapEnd, building);
    Map<FrameKey, Pending> pending =
        pendingSift.computeIfAbsent(binding.index(), unused -> new HashMap<>());
    Pending current = pending.get(key);
    Mutation pendingMutation = null;
    Mutation completedMutation = null;
    Map<String, String> ops = hint.heap().operationAstNodeIds();
    String point = step.statementAstNodeId();
    Integer from = building ? parent : node;
    Integer to = building ? node : chosen;
    if (point.equals(ops.get(prefix + "Swap1"))
        && complete(array)
        && observed != null
        && observed.arrayObjectId() != null
        && validIndex(array, from)
        && validIndex(array, to)) {
      current =
          new Pending(
              observed.arrayObjectId(),
              from,
              to,
              array.values().get(from),
              array.values().get(to),
              step.sequence());
      pending.put(key, current);
      pendingMutation = new Mutation("SIFT_SWAP_PENDING", to, from, current.oldTo, "PENDING");
    } else if (current != null
        && current.sequence < step.sequence()
        && Objects.equals(current.arrayId, observed == null ? null : observed.arrayObjectId())
        && complete(array)
        && validIndex(array, current.from)
        && validIndex(array, current.to)) {
      if (point.equals(ops.get(prefix + "Swap2"))
          && Objects.equals(array.values().get(current.from), current.oldTo))
        current.firstWriteSeen = true;
      if (current.firstWriteSeen && Objects.equals(array.values().get(current.to), current.oldFrom))
        current.secondWriteSeen = true;
      if (current.firstWriteSeen && current.secondWriteSeen) {
        completedMutation =
            new Mutation(
                "SIFT_SWAP_COMPLETED", current.from, current.to, current.oldFrom, "OBSERVED");
        pending.remove(key);
      }
    }
    Map<String, Pointer> pointers = new LinkedHashMap<>();
    pointers.put("node", pointer(hint, prefix + "Node", node, array));
    if (building) pointers.put("parent", pointer(hint, "buildSiftParent", parent, array));
    else {
      pointers.put("left", pointer(hint, "siftLeft", left, array));
      pointers.put("right", pointer(hint, "siftRight", right, array));
      pointers.put("chosen", pointer(hint, "siftChosen", chosen, array));
    }
    HeapFrame frame =
        new HeapFrame(
            building ? "SIFT_UP" : "SIFT_DOWN",
            step.context().invocationId(),
            heapEnd,
            node,
            left,
            right,
            chosen,
            comparison,
            pendingMutation,
            completedMutation,
            "UNKNOWN",
            "UNKNOWN",
            "UNKNOWN",
            "BEFORE_LOCATION",
            null);
    return state(
        binding,
        array,
        pointers,
        new IndexRange(
            "HEAP",
            0L,
            heapEnd == null ? null : heapEnd.longValue(),
            heapEnd == null ? "UNKNOWN" : "VALID"),
        frame);
  }

  private static Comparison comparison(
      BinaryExpr condition,
      ExecutionObservation step,
      ArrayValue array,
      Integer heapEnd,
      boolean building) {
    if (condition == null
        || !(condition.getLeft() instanceof ArrayAccessExpr firstAccess)
        || !(condition.getRight() instanceof ArrayAccessExpr secondAccess)
        || !(firstAccess.getIndex() instanceof NameExpr firstName)
        || !(secondAccess.getIndex() instanceof NameExpr secondName)) return null;
    Integer first = integer(resolved(step, firstName));
    Integer second = integer(resolved(step, secondName));
    if (!validIndex(array, first)
        || !validIndex(array, second)
        || heapEnd == null
        || first >= heapEnd
        || second >= heapEnd
        || !(array.values().get(first) instanceof Number firstValue)
        || !(array.values().get(second) instanceof Number secondValue)) return null;
    boolean required =
        SortRuntimeComparison.evaluate(
            condition, firstValue.intValue(), secondValue.intValue(), false);
    return new Comparison(
        first,
        second,
        firstValue,
        secondValue,
        null,
        "AVAILABLE_FROM_SNAPSHOT",
        building ? required ? "STOP" : "SWAP_UP" : required ? "UPDATE_CHOSEN" : "KEEP_CHOSEN",
        required);
  }

  private VisualState completedArray(
      Binding binding, ExecutionObservation step, CallCandidate candidate) {
    ObservationResult observed = resolved(step, candidate.array);
    ArrayValue array =
        array(
            observed,
            candidate.array.getNameAsString(),
            LocalVariableBindings.resolve(candidate.array).orElse(null));
    String full = sortedStatus(array, 0, array.length(), binding.hint().heap().direction());
    HeapFrame frame =
        new HeapFrame(
            "FULL_ARRAY_OBSERVED",
            candidate.rootInvocation,
            array.length(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "UNKNOWN",
            full,
            full,
            "BEFORE_LOCATION",
            0);
    return state(
        binding,
        array,
        Map.of(),
        new IndexRange(
            "FULL_ARRAY",
            0L,
            array.length() == null ? null : array.length().longValue(),
            array.length() == null ? "UNKNOWN" : "VALID"),
        frame);
  }

  private static VisualState state(
      Binding binding,
      ArrayValue array,
      Map<String, Pointer> pointers,
      IndexRange range,
      HeapFrame frame) {
    return new VisualState(
        "hint-" + binding.index(),
        binding.index(),
        "heap-sort",
        binding.hint().visualPlan(),
        visualStatus(array),
        array,
        pointers,
        Map.of(),
        range,
        List.of(),
        null,
        null,
        null,
        frame);
  }

  static String heapStatus(ArrayValue array, Integer end, Direction direction) {
    if (!complete(array) || end == null || end < 0 || end > array.length())
      return array.truncated() ? "TRUNCATED" : "UNKNOWN";
    for (int parent = 0; parent < end / 2; parent++) {
      for (int child : new int[] {parent * 2 + 1, parent * 2 + 2}) {
        if (child >= end) continue;
        if (!(array.values().get(parent) instanceof Number p)
            || !(array.values().get(child) instanceof Number c)) return "UNKNOWN";
        if (direction == Direction.ASCENDING
            ? p.intValue() < c.intValue()
            : p.intValue() > c.intValue()) return "NOT_HEAP";
      }
    }
    return "VERIFIED_HEAP";
  }

  static String sortedStatus(ArrayValue array, Integer start, Integer end, Direction direction) {
    if (!complete(array)
        || start == null
        || end == null
        || start < 0
        || end > array.length()
        || start > end) return array.truncated() ? "TRUNCATED" : "UNKNOWN";
    for (int i = start + 1; i < end; i++) {
      if (!(array.values().get(i - 1) instanceof Number first)
          || !(array.values().get(i) instanceof Number second)) return "UNKNOWN";
      if (direction == Direction.ASCENDING
          ? first.intValue() > second.intValue()
          : first.intValue() < second.intValue()) return "NOT_SORTED";
    }
    return "VERIFIED_SORTED";
  }

  private static boolean complete(ArrayValue array) {
    return "AVAILABLE".equals(array.status())
        && !array.truncated()
        && array.length() != null
        && array.values().size() == array.length();
  }

  private static boolean validIndex(ArrayValue array, Integer index) {
    return complete(array) && index != null && index >= 0 && index < array.length();
  }

  private static Pointer pointer(AlgorithmHint hint, String role, Integer index, ArrayValue array) {
    return new Pointer(
        hint.variables().get(role),
        hint.variableDeclarationIds().get(role),
        index == null ? null : index.longValue(),
        index == null ? "MISSING" : validIndex(array, index) ? "VALID" : "OUT_OF_BOUNDS");
  }

  private static ObservationResult value(
      AlgorithmHint hint, ExecutionObservation step, String role) {
    String id = hint.variableDeclarationIds().get(role);
    String name = hint.variables().get(role);
    if (id == null || name == null) return null;
    return step.variables().stream()
        .filter(
            variable ->
                Objects.equals(variable.astNodeId(), id) && variable.variableName().equals(name))
        .findFirst()
        .orElse(null);
  }

  private static ObservationResult resolved(ExecutionObservation step, NameExpr name) {
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
        || !"array".equals(variable.visualType())
        || !(variable.visualValue() instanceof List<?> observed))
      return new ArrayValue(name, id, "UNAVAILABLE", List.of(), null, false);
    List<Object> values = new ArrayList<>();
    Integer length = null;
    boolean truncated = false;
    for (Object element : observed) {
      if (element instanceof Map<?, ?> marker
          && Boolean.TRUE.equals(marker.get("truncated"))
          && marker.get("length") instanceof Number total) {
        length = total.intValue();
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

  private static String visualStatus(ArrayValue array) {
    return !"AVAILABLE".equals(array.status())
        ? "UNAVAILABLE"
        : array.truncated() ? "TRUNCATED" : "READY";
  }

  private static NameExpr calledArray(Node point, MethodDeclaration sort) {
    if (!(point instanceof ExpressionStmt statement)
        || !(statement.getExpression() instanceof MethodCallExpr call)
        || !call.getNameAsString().equals(sort.getNameAsString())
        || call.getArguments().size() != 1
        || !(call.getArgument(0) instanceof NameExpr array)
        || LocalVariableBindings.resolve(array).isEmpty()) return null;
    var owner =
        sort.findAncestor(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class)
            .orElse(null);
    if (owner == null
        || call.findAncestor(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class)
                .orElse(null)
            != owner
        || call.getScope().isPresent()) return null;
    return array;
  }
}
