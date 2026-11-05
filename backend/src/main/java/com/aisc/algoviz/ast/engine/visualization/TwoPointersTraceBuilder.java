package com.aisc.algoviz.ast.engine.visualization;

import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.aisc.algoviz.ast.engine.model.ExecutionObservation;
import com.aisc.algoviz.ast.engine.model.ObservationResult;
import com.aisc.algoviz.ast.engine.model.TwoPointersFrame;
import com.aisc.algoviz.ast.engine.model.TwoPointersPattern;
import com.aisc.algoviz.ast.engine.model.VisualState;
import com.aisc.algoviz.ast.engine.model.VisualState.ArrayValue;
import com.aisc.algoviz.ast.engine.model.VisualState.IndexRange;
import com.aisc.algoviz.ast.engine.model.VisualState.Pointer;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Builds conservative movement frames from snapshots of one invocation and input. */
final class TwoPointersTraceBuilder {
  private record Binding(int index, AlgorithmHint hint, Node loop, CallableDeclaration<?> method) {}
  private record Key(int hintIndex, long threadId, long invocationId, long inputObjectId) {}
  private record Snapshot(long sequence, Long first, Long second, List<Object> values) {}
  private record Pending(String kind, long sequence, Snapshot before) {}
  private record Data(List<Object> values, Integer length, String status, boolean truncated) {}

  private static final class Progress {
    Snapshot last;
    Pending pending;
  }

  private final List<Binding> bindings;
  private final Map<String, Node> nodes;
  private final Map<Key, Progress> progressByKey = new HashMap<>();

  TwoPointersTraceBuilder(List<AlgorithmHint> hints, Map<String, Node> nodes) {
    this.nodes = nodes;
    List<Binding> found = new ArrayList<>();
    for (int index = 0; index < hints.size(); index++) {
      AlgorithmHint hint = hints.get(index);
      if (!"two-pointers".equals(hint.type()) || hint.twoPointers() == null) continue;
      Node loop = nodes.get(hint.patternAstNodeId());
      Node method = nodes.get(hint.methodAstNodeId());
      if (loop != null && method instanceof CallableDeclaration<?> callable)
        found.add(new Binding(index, hint, loop, callable));
    }
    bindings = List.copyOf(found);
  }

  List<VisualState> states(ExecutionObservation step, Node point, String mainClass) {
    if (step.context() == null || point == null
        || !mainClass.equals(step.context().className())) return List.of();
    List<VisualState> result = new ArrayList<>();
    for (Binding binding : bindings) {
      if (!within(point, binding.loop())
          || !binding.method().getNameAsString().equals(step.context().methodName())) continue;
      result.add(state(binding, step, point));
    }
    return List.copyOf(result);
  }

  private VisualState state(Binding binding, ExecutionObservation step, Node point) {
    AlgorithmHint hint = binding.hint();
    TwoPointersPattern pattern = hint.twoPointers();
    ObservationResult input = value(hint, step, "data");
    boolean opposing = pattern.movement() == TwoPointersPattern.Movement.OPPOSING;
    String firstRole = opposing ? "left" : "scan";
    String secondRole = opposing ? "right" : "slow";
    Long first = number(value(hint, step, firstRole));
    Long second = number(value(hint, step, secondRole));
    Data data = data(input);
    Long inputId = input == null ? null : input.objectId();
    Long invocationId = step.context().invocationId();
    boolean identity = inputId != null && invocationId != null;
    Progress progress = identity ? progressByKey.computeIfAbsent(
        new Key(binding.index(), step.context().threadId(), invocationId, inputId),
        unused -> new Progress()) : new Progress();
    Snapshot current = new Snapshot(step.sequence(), first, second, data.values());
    Long firstDelta = delta(progress.last == null ? null : progress.last.first(), first);
    Long secondDelta = delta(progress.last == null ? null : progress.last.second(), second);
    boolean complete = identity && first != null && second != null && data.length() != null
        && data.status().equals("AVAILABLE")
        && first >= 0 && second >= 0 && first < data.length() && second < data.length();
    String effect = data.truncated() ? "TRUNCATED" : "UNKNOWN";
    List<String> notes = new ArrayList<>();
    if (!identity) notes.add("MISSING_CONTEXT_OR_INPUT_ID");
    if (data.truncated()) notes.add("INPUT_TRUNCATED");
    if (first != null && second != null && data.length() != null
        && (first < 0 || second < 0 || first >= data.length() || second >= data.length()))
      notes.add("POINTER_OUT_OF_BOUNDS");
    boolean skipped = progress.last != null && (step.sequence() != progress.last.sequence() + 1
        || firstDelta != null && Math.abs(firstDelta) > 1
        || secondDelta != null && Math.abs(secondDelta) > 1);
    if (!complete) progress.pending = null;
    if (identity && progress.last != null && (wrong(firstDelta, 1)
        || wrong(secondDelta, opposing ? -1 : 1))) {
      effect = "VIOLATED";
      progress.pending = null;
    } else if (skipped) {
      effect = "UNKNOWN";
      progress.pending = null;
      notes.add("MISSING_INTERMEDIATE_SNAPSHOT");
    } else if (complete && progress.pending != null
        && progress.pending.sequence() < step.sequence()) {
      String pendingEffect = confirm(progress.pending, current, opposing);
      if (pendingEffect.equals("OBSERVED")) {
        effect = "OBSERVED";
        progress.pending = null;
      } else if (pendingEffect.equals("UNKNOWN")) {
        progress.pending = null;
      }
    }
    String operation = complete ? operationAt(point, pattern, binding.loop()) : null;
    if (operation != null && !effect.equals("VIOLATED")) {
      if (progress.pending != null && progress.pending.sequence() < step.sequence())
        progress.pending = null;
      progress.pending = new Pending(operation, step.sequence(), current);
      if (!effect.equals("OBSERVED") && !skipped) effect = "PENDING";
    }
    if (identity) progress.last = current;
    ArrayValue array = new ArrayValue(hint.variables().get("data"),
        pattern.declarationIds().get("data"), data.status(), data.values(),
        data.length(), data.truncated());
    Map<String, Pointer> pointers = new LinkedHashMap<>();
    pointers.put(firstRole, pointer(hint, firstRole, first, data.length(), data.values().size()));
    pointers.put(secondRole, pointer(hint, secondRole, second, data.length(), data.values().size()));
    IndexRange range = range(opposing, first, second, data.length());
    TwoPointersFrame frame = new TwoPointersFrame(invocationId, inputId,
        pattern.movement().name(), first, second, data.length(),
        at(data, first), at(data, second), firstDelta, secondDelta,
        effect, pattern.evidence(), step.snapshotPhase());
    String status = complete ? "READY" : data.truncated() ? "TRUNCATED" : "PARTIAL";
    return new VisualState("hint-" + binding.index(), binding.index(), hint.type(),
        hint.visualPlan(), status, array, pointers, Map.of(), range, notes,
        null, null, null, null, null, frame);
  }

  private String operationAt(Node point, TwoPointersPattern pattern, Node loop) {
    String first = pattern.operationAstNodeIds().get("firstUpdate");
    String second = pattern.operationAstNodeIds().get("secondUpdate");
    Node firstNode = nodes.get(first);
    Node secondNode = nodes.get(second);
    if (firstNode != null && secondNode != null && firstNode.getBegin().isPresent()
        && secondNode.getBegin().isPresent() && point.getBegin().isPresent()
        && firstNode.getBegin().get().line == secondNode.getBegin().get().line
        && firstNode.getBegin().get().line == point.getBegin().get().line) return null;
    boolean atFirst = firstNode != null && within(firstNode, point);
    boolean atSecond = secondNode != null && within(secondNode, point);
    int writes = 0;
    for (AssignExpr assignment : point.findAll(AssignExpr.class))
      if (assignment.getTarget() instanceof ArrayAccessExpr) writes++;
    for (MethodCallExpr call : point.findAll(MethodCallExpr.class))
      if (call.getNameAsString().equals("set")) writes++;
    if ((atFirst ? 1 : 0) + (atSecond ? 1 : 0) + writes != 1) return null;
    if (atFirst) return "FIRST";
    if (atSecond) return "SECOND";
    if (writes == 1 && within(point, loop)) return "DATA";
    return null;
  }

  private static String confirm(Pending pending, Snapshot current, boolean opposing) {
    Snapshot before = pending.before();
    if (pending.kind().equals("FIRST")) {
      Long change = delta(before.first(), current.first());
      return change == null ? "UNKNOWN" : change == 1 ? "OBSERVED" : "PENDING";
    }
    if (pending.kind().equals("SECOND")) {
      Long change = delta(before.second(), current.second());
      int expected = opposing ? -1 : 1;
      return change == null ? "UNKNOWN" : change == expected ? "OBSERVED" : "PENDING";
    }
    if (before.values().isEmpty() || current.values().isEmpty()
        || before.values().size() != current.values().size()) return "UNKNOWN";
    int changes = 0;
    for (int i = 0; i < before.values().size(); i++)
      if (!Objects.equals(before.values().get(i), current.values().get(i))) changes++;
    return changes == 1 ? "OBSERVED" : changes == 0 ? "PENDING" : "UNKNOWN";
  }

  private static boolean wrong(Long delta, int expected) {
    return delta != null && delta != 0 && Long.signum(delta) != Integer.signum(expected);
  }

  private static Long delta(Long before, Long after) {
    return before == null || after == null ? null : after - before;
  }

  private static boolean within(Node child, Node parent) {
    for (Node n = child; n != null; n = n.getParentNode().orElse(null))
      if (n == parent) return true;
    return false;
  }

  private static ObservationResult value(AlgorithmHint hint, ExecutionObservation step,
                                         String role) {
    String id = hint.variableDeclarationIds().get(role);
    String name = hint.variables().get(role);
    if (id == null || name == null) return null;
    return step.variables().stream()
        .filter(v -> id.equals(v.astNodeId()) && name.equals(v.variableName()))
        .findFirst().orElse(null);
  }

  private static Long number(ObservationResult value) {
    return value != null && value.visualValue() instanceof Number number
        ? number.longValue() : null;
  }

  private static Data data(ObservationResult input) {
    if (input == null) return new Data(List.of(), null, "UNKNOWN", false);
    if (input.visualType().equals("integer-list") && input.visualValue() instanceof Map<?, ?> map) {
      List<Object> values = copyValues(map.get("values"));
      Integer length = map.get("length") instanceof Number number ? number.intValue() : null;
      boolean truncated = Boolean.TRUE.equals(map.get("truncated"));
      return new Data(values, length, Objects.toString(map.get("status"), "UNKNOWN"), truncated);
    }
    if (input.visualType().equals("array") && input.visualValue() instanceof List<?> raw) {
      List<Object> values = new ArrayList<>();
      Integer length = null;
      boolean truncated = false;
      for (Object item : raw) {
        if (item instanceof Map<?, ?> marker && Boolean.TRUE.equals(marker.get("truncated"))) {
          if (marker.get("length") instanceof Number number) length = number.intValue();
          truncated = true;
        } else values.add(item);
      }
      if (!truncated) length = values.size();
      return new Data(values, length, truncated ? "TRUNCATED" : "AVAILABLE", truncated);
    }
    return new Data(List.of(), null, "UNKNOWN", false);
  }

  private static List<Object> copyValues(Object raw) {
    if (!(raw instanceof List<?> list)) return List.of();
    return new ArrayList<>(list);
  }

  private static Object at(Data data, Long index) {
    return index != null && index >= 0 && index < data.values().size()
        ? data.values().get(index.intValue()) : null;
  }

  private static Pointer pointer(AlgorithmHint hint, String role, Long index, Integer length,
                                 int capturedLength) {
    String status = index == null ? "MISSING" : length == null ? "ARRAY_UNAVAILABLE"
        : index < 0 || index >= length ? "OUT_OF_BOUNDS"
        : index >= capturedLength ? "NOT_CAPTURED" : "VALID";
    return new Pointer(hint.variables().get(role), hint.variableDeclarationIds().get(role),
        index, status);
  }

  private static IndexRange range(boolean opposing, Long first, Long second, Integer length) {
    String kind = opposing ? "POINTER_SPAN" : "SCANNED_PREFIX";
    if (first == null || second == null || length == null)
      return new IndexRange(kind, null, null, "UNKNOWN");
    long start = opposing ? first : 0;
    long end = (opposing ? second : first) + 1;
    String status = start < 0 || end < start || end > length ? "INVALID"
        : start == end ? "EMPTY" : "VALID";
    return new IndexRange(kind, start, end, status);
  }
}
