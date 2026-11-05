package com.aisc.algoviz.ast.engine.visualization;

import com.aisc.algoviz.ast.engine.model.ActivitySelectionFrame;
import com.aisc.algoviz.ast.engine.model.ActivitySelectionPattern;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.aisc.algoviz.ast.engine.model.ExecutionObservation;
import com.aisc.algoviz.ast.engine.model.ObservationResult;
import com.aisc.algoviz.ast.engine.model.VisualState;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Joins selection decisions to later writes within one input and invocation. */
final class ActivitySelectionTraceBuilder {
  private record Binding(int index, AlgorithmHint hint, MethodDeclaration method, IfStmt guard) {}
  private record FrameKey(int hintIndex, long threadId, long invocationId, long inputObjectId) {}
  private record Candidate(Integer start, Integer end, Long elementId, String status) {}
  private record Pending(int index, Integer start, Integer end, Long elementId,
      String decision, Integer oldLastEnd, Integer oldOutputSize, long sequence) {}

  private static final class Progress {
    int nextOrdinal;
    int decisions;
    int acceptedDecisions;
    Integer previousCandidateEnd;
    Integer previousSelectedEnd;
    boolean invalidDuration;
    boolean invalidOrder;
    boolean invalidOverlap;
    boolean invalidOutput;
    boolean zeroLengthHalfOpen;
    boolean unknown;
    Pending pending;
    final List<Integer> selected = new ArrayList<>();
  }

  private final List<Binding> bindings;
  private final Map<FrameKey, Progress> progressByFrame = new HashMap<>();

  ActivitySelectionTraceBuilder(List<AlgorithmHint> hints, Map<String, Node> nodes) {
    List<Binding> found = new ArrayList<>();
    for (int index = 0; index < hints.size(); index++) {
      AlgorithmHint hint = hints.get(index);
      if (hint.activitySelection() == null || !"activity-selection".equals(hint.type())) continue;
      Node method = nodes.get(hint.methodAstNodeId());
      Node decision = nodes.get(hint.activitySelection().operationAstNodeIds().get("decision"));
      IfStmt guard = decision == null ? null : decision.findAncestor(IfStmt.class).orElse(null);
      if (method instanceof MethodDeclaration declaration && guard != null)
        found.add(new Binding(index, hint, declaration, guard));
    }
    bindings = List.copyOf(found);
  }

  List<VisualState> states(ExecutionObservation step, Node point, String mainClass) {
    if (step.context() == null || point == null) return List.of();
    List<VisualState> result = new ArrayList<>();
    for (Binding binding : bindings) {
      if (!mainClass.equals(step.context().className())
          || !binding.method().getNameAsString().equals(step.context().methodName())
          || point.findAncestor(MethodDeclaration.class).orElse(null) != binding.method()) continue;
      result.add(state(binding, step, point));
    }
    return List.copyOf(result);
  }

  private VisualState state(Binding binding, ExecutionObservation step, Node point) {
    AlgorithmHint hint = binding.hint();
    ActivitySelectionPattern pattern = hint.activitySelection();
    ObservationResult input = value(hint, step, "input");
    ObservationResult boundary = value(hint, step, "lastEnd");
    ObservationResult output = value(hint, step, "output");
    Long inputId = input == null ? null : input.objectId();
    Long invocationId = step.context().invocationId();
    Integer lastEnd = integer(boundary == null ? null : boundary.visualValue());
    Integer outputSize = outputSize(output);
    Integer length = inputLength(input);
    boolean truncated = truncated(input);
    boolean completeIdentity = inputId != null && invocationId != null;
    Progress progress = completeIdentity
        ? progressByFrame.computeIfAbsent(
            new FrameKey(binding.index(), step.context().threadId(), invocationId, inputId),
            unused -> new Progress())
        : new Progress();

    String effect = truncated ? "TRUNCATED" : "UNKNOWN";
    Integer candidateIndex = null;
    Candidate candidate = new Candidate(null, null, null, "UNKNOWN");
    String decision = "UNKNOWN";
    boolean confirmed = completeIdentity && confirmPending(progress, step, lastEnd, output,
        outputSize, pattern);
    if (confirmed) effect = "OBSERVED_AFTER_WRITE";

    if (point == binding.guard()) {
      if (progress.pending != null) progress.unknown = true;
      candidateIndex = cursor(hint, step);
      if (candidateIndex == null) candidateIndex = progress.nextOrdinal;
      progress.nextOrdinal = Math.max(progress.nextOrdinal, candidateIndex + 1);
      candidate = candidate(input, pattern, candidateIndex);
      if (candidate.start() != null && candidate.end() != null && lastEnd != null
          && !truncated && completeIdentity) {
        if (candidate.start() > candidate.end()) progress.invalidDuration = true;
        if (candidate.start().equals(candidate.end())
            && pattern.overlapPolicy() == ActivitySelectionPattern.OverlapPolicy.HALF_OPEN)
          progress.zeroLengthHalfOpen = true;
        if (progress.previousCandidateEnd != null
            && candidate.end() < progress.previousCandidateEnd) progress.invalidOrder = true;
        progress.previousCandidateEnd = candidate.end();
        decision = (pattern.firstCandidateGuard() && progress.decisions == 0
            || accepts(pattern, candidate.start(), lastEnd)) ? "ACCEPT" : "SKIP";
        progress.decisions++;
        if (decision.equals("ACCEPT")) progress.acceptedDecisions++;
        progress.pending = new Pending(candidateIndex, candidate.start(), candidate.end(),
            candidate.elementId(), decision, lastEnd, outputSize, step.sequence());
        effect = "PENDING";
      } else {
        progress.unknown = true;
        effect = truncated ? "TRUNCATED" : "UNKNOWN";
      }
    }

    boolean atReturn = point instanceof ReturnStmt;
    if (atReturn && fullyObserved(progress, length, truncated)
        && outputSize != null && outputSize != progress.selected.size())
      progress.invalidOutput = true;
    String orderStatus = progress.invalidOrder || progress.invalidDuration ? "VIOLATED"
        : atReturn && fullyObserved(progress, length, truncated) ? "RUNTIME_VERIFIED" : "UNKNOWN";
    String overlapStatus = progress.invalidOverlap || progress.invalidDuration ? "VIOLATED"
        : atReturn && fullyObserved(progress, length, truncated) ? "RUNTIME_VERIFIED" : "UNKNOWN";
    String outputStatus = progress.invalidOutput ? "VIOLATED"
        : atReturn && fullyObserved(progress, length, truncated)
            && progress.selected.size() == progress.acceptedDecisions
            && outputSize != null && outputSize == progress.selected.size()
            ? "RUNTIME_VERIFIED" : "UNKNOWN";
    String optimality = !progress.zeroLengthHalfOpen && !orderStatus.equals("VIOLATED")
        && (orderStatus.equals("RUNTIME_VERIFIED")
            || pattern.orderingEvidence() == ActivitySelectionPattern.OrderingEvidence.SOURCE_PROVEN)
        && overlapStatus.equals("RUNTIME_VERIFIED") && outputStatus.equals("RUNTIME_VERIFIED")
        ? "VERIFIED_FOR_RUN" : "UNKNOWN";
    if (candidateIndex == null && progress.pending != null) {
      candidateIndex = progress.pending.index();
      candidate = new Candidate(progress.pending.start(), progress.pending.end(),
          progress.pending.elementId(), "AVAILABLE");
      decision = progress.pending.decision();
    }
    ActivitySelectionFrame frame = new ActivitySelectionFrame(invocationId, inputId,
        candidateIndex, candidate.start(), candidate.end(), lastEnd,
        pattern.overlapPolicy().name(), decision, effect, progress.selected,
        pattern.orderingEvidence().name(), orderStatus, overlapStatus, outputStatus, optimality,
        step.snapshotPhase());
    String status = truncated ? "TRUNCATED"
        : optimality.equals("VERIFIED_FOR_RUN") ? "VERIFIED"
        : completeIdentity ? "PARTIAL" : "UNKNOWN";
    return new VisualState("hint-" + binding.index(), binding.index(), hint.type(),
        hint.visualPlan(), status, null, Map.of(), Map.of(), null, List.of(),
        null, null, null, null, frame);
  }

  private boolean confirmPending(Progress progress, ExecutionObservation step,
      Integer lastEnd, ObservationResult output, Integer outputSize,
      ActivitySelectionPattern pattern) {
    Pending pending = progress.pending;
    if (pending == null || pending.sequence() >= step.sequence()
        || pending.oldOutputSize() == null || outputSize == null || lastEnd == null) return false;
    boolean confirmed;
    if (pending.decision().equals("ACCEPT")) {
      confirmed = Objects.equals(lastEnd, pending.end())
          && outputSize == pending.oldOutputSize() + 1
          && outputContains(output, pending.elementId());
      if (confirmed) {
        if (progress.previousSelectedEnd != null
            && !accepts(pattern, pending.start(), progress.previousSelectedEnd))
          progress.invalidOverlap = true;
        progress.previousSelectedEnd = pending.end();
        progress.selected.add(pending.index());
      }
    } else {
      confirmed = Objects.equals(lastEnd, pending.oldLastEnd())
          && outputSize.equals(pending.oldOutputSize());
    }
    if (confirmed) progress.pending = null;
    return confirmed;
  }

  private static boolean outputContains(ObservationResult output, Long elementId) {
    if (output == null) return false;
    if (output.visualValue() instanceof Number) return true;
    if (!(output.visualValue() instanceof List<?> values) || values.isEmpty() || elementId == null)
      return false;
    Object last = values.get(values.size() - 1);
    return last instanceof Map<?, ?> map && Objects.equals(map.get("referenceId"), elementId);
  }

  private static boolean accepts(ActivitySelectionPattern pattern, int start, int lastEnd) {
    return pattern.overlapPolicy() == ActivitySelectionPattern.OverlapPolicy.HALF_OPEN
        ? start >= lastEnd : start > lastEnd;
  }

  private static boolean fullyObserved(Progress progress, Integer length, boolean truncated) {
    return !truncated && !progress.unknown && progress.pending == null && length != null
        && progress.decisions == length;
  }

  private static ObservationResult value(AlgorithmHint hint, ExecutionObservation step, String role) {
    String id = hint.variableDeclarationIds().get(role);
    String name = hint.variables().get(role);
    if (id == null || name == null) return null;
    return step.variables().stream()
        .filter(v -> id.equals(v.astNodeId()) && name.equals(v.variableName()))
        .findFirst().orElse(null);
  }

  private static Integer cursor(AlgorithmHint hint, ExecutionObservation step) {
    ObservationResult observed = value(hint, step, "cursor");
    return integer(observed == null ? null : observed.visualValue());
  }

  private static Integer outputSize(ObservationResult output) {
    if (output == null) return null;
    if (output.visualValue() instanceof Number number) return number.intValue();
    if (!(output.visualValue() instanceof List<?> values)) return null;
    if (!values.isEmpty() && values.get(values.size() - 1) instanceof Map<?, ?> marker
        && Boolean.TRUE.equals(marker.get("truncated"))) return null;
    return values.size();
  }

  private static Integer inputLength(ObservationResult input) {
    if (input == null) return null;
    if (input.visualValue() instanceof Map<?, ?> map)
      return integer(map.get("length"));
    if (input.visualValue() instanceof List<?> rows) {
      if (!rows.isEmpty() && rows.get(rows.size() - 1) instanceof Map<?, ?> marker
          && Boolean.TRUE.equals(marker.get("truncated"))) return integer(marker.get("length"));
      return rows.size();
    }
    return null;
  }

  private static boolean truncated(ObservationResult input) {
    if (input == null) return false;
    if (input.visualValue() instanceof Map<?, ?> map)
      return Boolean.TRUE.equals(map.get("truncated"));
    return input.visualValue() instanceof List<?> rows && !rows.isEmpty()
        && rows.get(rows.size() - 1) instanceof Map<?, ?> marker
        && Boolean.TRUE.equals(marker.get("truncated"));
  }

  private static Candidate candidate(ObservationResult input,
      ActivitySelectionPattern pattern, int index) {
    if (input == null || index < 0) return new Candidate(null, null, null, "UNKNOWN");
    if (pattern.representation().equals("INTERVAL_LIST")
        && input.visualValue() instanceof Map<?, ?> list
        && list.get("values") instanceof List<?> values && index < values.size()
        && values.get(index) instanceof Map<?, ?> pair) {
      return new Candidate(integer(pair.get("start")), integer(pair.get("end")),
          longValue(pair.get("elementObjectId")), Objects.toString(pair.get("status"), "UNKNOWN"));
    }
    if (pattern.representation().equals("INT_MATRIX")
        && input.visualValue() instanceof List<?> rows && index < rows.size()
        && rows.get(index) instanceof List<?> row) {
      int startColumn = Integer.parseInt(pattern.roleValues().get("start"));
      int endColumn = Integer.parseInt(pattern.roleValues().get("end"));
      return startColumn < row.size() && endColumn < row.size()
          ? new Candidate(integer(row.get(startColumn)), integer(row.get(endColumn)), null, "AVAILABLE")
          : new Candidate(null, null, null, "UNKNOWN");
    }
    return new Candidate(null, null, null, "UNKNOWN");
  }

  private static Integer integer(Object value) {
    return value instanceof Number number && number.longValue() >= Integer.MIN_VALUE
        && number.longValue() <= Integer.MAX_VALUE ? number.intValue() : null;
  }

  private static Long longValue(Object value) {
    return value instanceof Number number ? number.longValue() : null;
  }
}
