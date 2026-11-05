package com.aisc.algoviz.ast.engine.visualization;

import com.aisc.algoviz.ast.engine.algorithm.LocalVariableBindings;
import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.aisc.algoviz.ast.engine.model.ExecutionContext;
import com.aisc.algoviz.ast.engine.model.ExecutionObservation;
import com.aisc.algoviz.ast.engine.model.ObservationResult;
import com.aisc.algoviz.ast.engine.model.SortFrame;
import com.aisc.algoviz.ast.engine.model.VisualEvent;
import com.aisc.algoviz.ast.engine.model.VisualState;
import com.aisc.algoviz.ast.engine.model.VisualState.ArrayValue;
import com.aisc.algoviz.ast.engine.model.VisualState.IndexRange;
import com.aisc.algoviz.ast.engine.model.VisualState.Pointer;
import com.aisc.algoviz.ast.engine.model.VisualState.Scalar;
import com.aisc.algoviz.ast.engine.visualization.sort.SortTraceBuilder;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Builds independent states from snapshots; never carries missing variable values forward. */
@Component
public class VisualTraceBuilder {
  private final SortTraceBuilder sortTraceBuilder = new SortTraceBuilder();

  public List<ExecutionObservation> build(
      AstAnalyzer.Analysis analysis, List<AlgorithmHint> hints, List<ExecutionObservation> steps) {
    Map<String, Node> nodes = new LinkedHashMap<>();
    analysis.compilationUnit().walk(n -> nodes.put(AstAnalyzer.nodeId(n), n));
    List<Binding> bindings = bindings(analysis, hints, nodes);
    QuickTraceBuilder quickTraceBuilder = new QuickTraceBuilder(hints, nodes);
    MergeTraceBuilder mergeTraceBuilder = new MergeTraceBuilder(hints, nodes);
    HeapTraceBuilder heapTraceBuilder = new HeapTraceBuilder(hints, nodes);
    ActivitySelectionTraceBuilder activitySelectionTraceBuilder =
        new ActivitySelectionTraceBuilder(hints, nodes);
    TwoPointersTraceBuilder twoPointersTraceBuilder = new TwoPointersTraceBuilder(hints, nodes);
    List<ExecutionObservation> result = new ArrayList<>();
    ExecutionObservation previous = null;
    for (ExecutionObservation step : steps) {
      List<VisualState> states = new ArrayList<>();
      Node point = nodes.get(step.statementAstNodeId());
      for (Binding binding : bindings) {
        if (applies(binding, step, previous, point, analysis.mainClassName())) {
          states.add(state(binding, step, previous, point));
        }
      }
      states.addAll(quickTraceBuilder.states(step, point, analysis.mainClassName()));
      states.addAll(mergeTraceBuilder.states(step, point, analysis.mainClassName()));
      states.addAll(heapTraceBuilder.states(step, point, analysis.mainClassName()));
      states.addAll(activitySelectionTraceBuilder.states(step, point, analysis.mainClassName()));
      states.addAll(twoPointersTraceBuilder.states(step, point, analysis.mainClassName()));
      List<VisualEvent> events = events(previous, step, states);
      ExecutionObservation enriched = step.withVisualization(states, events);
      result.add(enriched);
      previous = enriched;
    }
    return List.copyOf(result);
  }

  private List<Binding> bindings(
      AstAnalyzer.Analysis analysis, List<AlgorithmHint> hints, Map<String, Node> nodes) {
    List<Node> loops =
        analysis
            .compilationUnit()
            .findAll(Node.class, n -> n instanceof ForStmt || n instanceof WhileStmt);
    List<Binding> result = new ArrayList<>();
    for (int index = 0; index < hints.size(); index++) {
      AlgorithmHint hint = hints.get(index);
      if (hint.twoPointers() != null) continue;
      if (!List.of(
              "binary-search",
              "two-pointers",
              "sliding-window",
              "bubble-sort",
              "selection-sort",
              "insertion-sort")
          .contains(hint.type())) continue;
      if (hint.patternAstNodeId() != null) {
        Node loop = nodes.get(hint.patternAstNodeId());
        if (!(loop instanceof ForStmt || loop instanceof WhileStmt)
            || owner(loop) == null
            || !AstAnalyzer.nodeId(owner(loop)).equals(hint.methodAstNodeId())
            || !hint.variableDeclarationIds().keySet().equals(hint.variables().keySet())) continue;
        boolean valid =
            hint.variableDeclarationIds().entrySet().stream()
                .allMatch(
                    entry ->
                        analysis.variables().stream()
                            .anyMatch(
                                v ->
                                    v.astNodeId().equals(entry.getValue())
                                        && v.name().equals(hint.variables().get(entry.getKey()))));
        if (valid)
          result.add(
              new Binding(index, hint, loop, hint.variableDeclarationIds(), helper(hint, nodes)));
        continue;
      }
      List<Node> matches =
          loops.stream()
              .filter(
                  n ->
                      n.getRange()
                          .map(
                              r -> r.begin.line == hint.startLine() && r.end.line == hint.endLine())
                          .orElse(false))
              .toList();
      // Compatibility for older hints without node IDs; never guess ambiguous source lines.
      if (matches.size() != 1) continue;
      Node loop = matches.get(0);
      if (!(owner(loop) instanceof CallableDeclaration<?>)) continue;
      Map<String, String> declarations =
          LocalVariableBindings.roles(loop, hint.variables()).orElse(Map.of());
      result.add(new Binding(index, hint, loop, declarations, null));
    }
    return result;
  }

  private boolean applies(
      Binding binding,
      ExecutionObservation step,
      ExecutionObservation previous,
      Node point,
      String mainClass) {
    if (point == null || step.context() == null) return false;
    if (binding.helper() != null && owner(point) == binding.helper().method()) {
      if (previous == null
          || previous.context() == null
          || step.context().threadId() != previous.context().threadId()
          || !step.context().className().equals(mainClass)
          || !step.context().methodName().equals(binding.helper().method().getNameAsString()))
        return false;
      boolean entering =
          (previous.statementAstNodeId().equals(binding.hint().sort().conditionStatementAstNodeId())
                  || previous
                      .statementAstNodeId()
                      .equals(binding.hint().sort().operationAstNodeIds().get("swapCall")))
              && previous.context().stackDepth() + 1 == step.context().stackDepth();
      boolean continuing =
          previous.context().stackDepth() == step.context().stackDepth()
              && previous.context().methodName().equals(step.context().methodName())
              && previous.visualStates().stream()
                  .anyMatch(state -> state.hintIndex() == binding.index());
      return entering || continuing;
    }
    if (owner(point) != owner(binding.loop())) return false;
    var callable = (CallableDeclaration<?>) owner(binding.loop());
    String method = callable.isMethodDeclaration() ? callable.getNameAsString() : "<init>";
    boolean inside = false;
    for (Node current = point; current != null; current = current.getParentNode().orElse(null)) {
      if (current == binding.loop()) inside = true;
    }
    boolean terminalSortPoint =
        binding.hint().type().endsWith("-sort") && isImmediatelyAfter(binding.loop(), point);
    if (!inside && !terminalSortPoint) return false;
    return step.context().className().equals(mainClass)
        && step.context().methodName().equals(method)
        && (terminalSortPoint
            || step.lineNumber() >= binding.hint().startLine()
                && step.lineNumber() <= binding.hint().endLine());
  }

  private boolean isImmediatelyAfter(Node loop, Node point) {
    Node parent = loop.getParentNode().orElse(null);
    if (!(parent instanceof com.github.javaparser.ast.stmt.BlockStmt block)) return false;
    int index = -1;
    for (int i = 0; i < block.getStatements().size(); i++) {
      if (block.getStatement(i) == loop) {
        index = i;
        break;
      }
    }
    return index >= 0
        && index + 1 < block.getStatements().size()
        && block.getStatement(index + 1) == point;
  }

  private static Node owner(Node node) {
    for (Node current = node; current != null; current = current.getParentNode().orElse(null)) {
      if (current instanceof CallableDeclaration<?> || current instanceof LambdaExpr)
        return current;
    }
    return null;
  }

  private VisualState state(
      Binding binding, ExecutionObservation step, ExecutionObservation previous, Node point) {
    ArrayValue array = array(binding, step);
    Map<String, Pointer> pointers = new LinkedHashMap<>();
    for (String role : List.of("left", "right", "mid", "pass", "scan", "selected")) {
      if (!binding.hint().variables().containsKey(role)) continue;
      ObservationResult variable = variable(binding, step, role);
      Long index = variable == null ? null : integer(variable.visualValue());
      String status = status(variable);
      if (status.equals("AVAILABLE")) {
        status =
            index == null
                ? "INVALID"
                : array.length() == null
                    ? "ARRAY_UNAVAILABLE"
                    : index < 0 || index >= array.length()
                        ? "OUT_OF_BOUNDS"
                        : index >= array.values().size() ? "NOT_CAPTURED" : "VALID";
      }
      pointers.put(
          role,
          new Pointer(roleName(binding, step, role), roleId(binding, step, role), index, status));
    }
    Map<String, Scalar> scalars = new LinkedHashMap<>();
    for (String role : List.of("accumulator", "windowSize", "key")) {
      if (!binding.hint().variables().containsKey(role)) continue;
      ObservationResult variable = variable(binding, step, role);
      String status = status(variable);
      Object value = variable == null ? null : variable.visualValue();
      if (status.equals("AVAILABLE")) {
        boolean valid =
            role.equals("windowSize")
                ? integer(value) != null && integer(value) > 0
                : role.equals("key")
                    ? value instanceof Number && Double.isFinite(((Number) value).doubleValue())
                    : value instanceof Number n && Double.isFinite(n.doubleValue());
        if (!valid) status = "INVALID";
      }
      scalars.put(
          role,
          new Scalar(roleName(binding, step, role), roleId(binding, step, role), value, status));
    }
    IndexRange range = range(array, pointers, scalars);
    List<String> notes = new ArrayList<>();
    if (binding.declarations().size() != binding.hint().variables().size())
      notes.add("UNRESOLVED_VARIABLE_BINDING");
    if (binding.hint().type().equals("sliding-window"))
      notes.add("RANGE_DOES_NOT_PROVE_ACCUMULATOR_MEMBERSHIP");
    if (binding.hint().type().equals("binary-search"))
      notes.add("POINTER_SPAN_DOES_NOT_DECLARE_SEARCH_BOUND_CONVENTION");
    if (array.truncated()) notes.add("ARRAY_TRUNCATED");
    SortFrame sortFrame =
        sortTraceBuilder.build(
            binding.hint(),
            previous,
            step,
            previous == null ? null : array(binding, previous),
            array,
            role -> variable(binding, step, role),
            role -> previous == null ? null : variable(binding, previous, role),
            isImmediatelyAfter(binding.loop(), point));
    boolean ready =
        array.status().equals("AVAILABLE")
            && !array.truncated()
            && pointers.values().stream().allMatch(p -> p.status().equals("VALID"))
            && scalars.values().stream().allMatch(s -> s.status().equals("AVAILABLE"))
            && (range.status().equals("VALID") || range.status().equals("EMPTY"));
    String status =
        !array.status().equals("AVAILABLE") ? "UNAVAILABLE" : ready ? "READY" : "PARTIAL";
    return new VisualState(
        "hint-" + binding.index(),
        binding.index(),
        binding.hint().type(),
        binding.hint().visualPlan(),
        status,
        array,
        pointers,
        scalars,
        range,
        notes,
        sortFrame);
  }

  private ObservationResult variable(Binding binding, ExecutionObservation step, String role) {
    String id = roleId(binding, step, role);
    if (id == null) return null;
    String name = roleName(binding, step, role);
    List<ObservationResult> matches =
        step.variables().stream()
            .filter(v -> id.equals(v.astNodeId()) && name.equals(v.variableName()))
            .toList();
    return matches.size() == 1 ? matches.get(0) : null;
  }

  private boolean inHelper(Binding binding, ExecutionObservation step) {
    return binding.helper() != null
        && step.context() != null
        && step.context().methodName().equals(binding.helper().method().getNameAsString());
  }

  private String roleId(Binding binding, ExecutionObservation step, String role) {
    return inHelper(binding, step)
        ? binding.helper().declarations().get(role)
        : binding.declarations().get(role);
  }

  private String roleName(Binding binding, ExecutionObservation step, String role) {
    return inHelper(binding, step)
        ? binding.helper().names().get(role)
        : binding.hint().variables().get(role);
  }

  private String status(ObservationResult variable) {
    if (variable == null) return "MISSING";
    if (variable.visualType().equals("unavailable")) return "UNAVAILABLE";
    if (variable.visualType().equals("null")) return "NULL";
    return "AVAILABLE";
  }

  private ArrayValue array(Binding binding, ExecutionObservation step) {
    String role = binding.hint().twoPointers() == null ? "array" : "data";
    ObservationResult variable = variable(binding, step, role);
    String status = status(variable);
    List<Object> values = new ArrayList<>();
    Integer length = null;
    boolean truncated = false;
    if (status.equals("AVAILABLE")) {
      if (!variable.visualType().equals("array") || !(variable.visualValue() instanceof List<?>)) {
        status = "UNSUPPORTED";
      } else {
        List<?> captured = (List<?>) variable.visualValue();
        for (Object element : captured) {
          if (element instanceof Map<?, ?> marker
              && Boolean.TRUE.equals(marker.get("truncated"))
              && marker.get("length") instanceof Integer total
              && total > values.size()
              && values.size() == captured.size() - 1) {
            length = total;
            truncated = true;
          } else if (element == null
              || element instanceof String
              || element instanceof Boolean
              || element instanceof Number n && Double.isFinite(n.doubleValue())) {
            values.add(element);
          } else {
            status = "UNSUPPORTED";
            break;
          }
        }
        if (status.equals("AVAILABLE") && !truncated) length = values.size();
      }
    }
    if (!status.equals("AVAILABLE")) {
      values.clear();
      length = null;
      truncated = false;
    }
    return new ArrayValue(
        roleName(binding, step, role),
        roleId(binding, step, role),
        status,
        values,
        length,
        truncated);
  }

  private static Long integer(Object value) {
    if (value instanceof Byte
        || value instanceof Short
        || value instanceof Integer
        || value instanceof Long) {
      long number = ((Number) value).longValue();
      if (number >= Integer.MIN_VALUE && number <= Integer.MAX_VALUE) return number;
    }
    return null;
  }

  private IndexRange range(
      ArrayValue array, Map<String, Pointer> pointers, Map<String, Scalar> scalars) {
    Pointer left = pointers.get("left");
    Pointer right = pointers.get("right");
    boolean fixed = scalars.containsKey("windowSize");
    String kind = fixed ? "FIXED_SIZE_CANDIDATE" : "POINTER_SPAN";
    if (array.length() == null
        || right == null
        || right.index() == null
        || !List.of("VALID", "NOT_CAPTURED").contains(right.status())) {
      return new IndexRange(kind, null, null, "UNKNOWN");
    }
    long start;
    if (fixed) {
      Scalar size = scalars.get("windowSize");
      if (!size.status().equals("AVAILABLE")) return new IndexRange(kind, null, null, "UNKNOWN");
      start = Math.max(0, right.index() - integer(size.value()) + 1);
    } else {
      if (left == null
          || left.index() == null
          || !List.of("VALID", "NOT_CAPTURED", "OUT_OF_BOUNDS").contains(left.status())) {
        return new IndexRange(kind, null, null, "UNKNOWN");
      }
      start = left.index();
    }
    long end = right.index() + 1;
    if (start < 0 || start > array.length()) return new IndexRange(kind, start, end, "INVALID");
    if (start > end) return new IndexRange(kind, start, end, "INVALID");
    return new IndexRange(kind, start, end, start == end ? "EMPTY" : "VALID");
  }

  private List<VisualEvent> events(
      ExecutionObservation previous, ExecutionObservation step, List<VisualState> states) {
    List<VisualEvent> events = new ArrayList<>();
    boolean comparable = comparable(previous, step);
    Map<String, VisualState> oldStates = new LinkedHashMap<>();
    if (previous != null) previous.visualStates().forEach(s -> oldStates.put(s.id(), s));
    for (VisualState state : states) {
      VisualState old = oldStates.remove(state.id());
      if (old == null || !comparable) {
        events.add(
            new VisualEvent(
                state.id(),
                old == null ? "STATE_ENTERED" : "STATE_RESET",
                "state",
                null,
                step.sequence(),
                null,
                null));
        continue;
      }
      change(events, state, "ARRAY_CHANGED", "array", old.array(), state.array(), previous, step);
      for (String role : state.pointers().keySet())
        change(
            events,
            state,
            "POINTER_CHANGED",
            role,
            old.pointers().get(role),
            state.pointers().get(role),
            previous,
            step);
      for (String role : state.scalars().keySet())
        change(
            events,
            state,
            "VALUE_CHANGED",
            role,
            old.scalars().get(role),
            state.scalars().get(role),
            previous,
            step);
      change(events, state, "RANGE_CHANGED", "range", old.range(), state.range(), previous, step);
    }
    for (String id : oldStates.keySet()) {
      events.add(
          new VisualEvent(
              id, "STATE_LEFT", "state", previous.sequence(), step.sequence(), null, null));
    }
    return List.copyOf(events);
  }

  private void change(
      List<VisualEvent> events,
      VisualState state,
      String type,
      String target,
      Object before,
      Object after,
      ExecutionObservation previous,
      ExecutionObservation step) {
    if (!Objects.equals(before, after)) {
      events.add(
          new VisualEvent(
              state.id(), type, target, previous.sequence(), step.sequence(), before, after));
    }
  }

  private boolean comparable(ExecutionObservation previous, ExecutionObservation step) {
    if (previous == null || previous.sequence() + 1 != step.sequence()) return false;
    ExecutionContext a = previous.context();
    ExecutionContext b = step.context();
    return a != null
        && b != null
        && a.threadId() == b.threadId()
        && a.stackDepth() == b.stackDepth()
        && Objects.equals(a.className(), b.className())
        && Objects.equals(a.methodName(), b.methodName())
        && Objects.equals(a.methodSignature(), b.methodSignature())
        // Rewinds may be a loop back-edge or a new invocation at the same stack depth.
        && a.codeIndex() >= 0
        && b.codeIndex() > a.codeIndex();
  }

  private Helper helper(AlgorithmHint hint, Map<String, Node> nodes) {
    if (hint.sort() == null) return null;
    String methodId = hint.sort().operationAstNodeIds().get("swapHelper");
    if (!(nodes.get(methodId) instanceof MethodDeclaration method)
        || method.getParameters().size() != 3
        || method.getBody().isEmpty()
        || method.getBody().orElseThrow().getStatements().isEmpty()
        || !(method.getBody().orElseThrow().getStatement(0) instanceof ExpressionStmt statement)
        || !(statement.getExpression() instanceof VariableDeclarationExpr declaration)
        || declaration.getVariables().size() != 1) return null;
    VariableDeclarator temp = declaration.getVariable(0);
    String leftRole = hint.type().equals("selection-sort") ? "pass" : "scan";
    String rightRole = hint.type().equals("selection-sort") ? "selected" : null;
    Map<String, String> names = new LinkedHashMap<>();
    Map<String, String> declarations = new LinkedHashMap<>();
    names.put("array", method.getParameter(0).getNameAsString());
    declarations.put("array", AstAnalyzer.nodeId(method.getParameter(0)));
    names.put(leftRole, method.getParameter(1).getNameAsString());
    declarations.put(leftRole, AstAnalyzer.nodeId(method.getParameter(1)));
    if (rightRole != null) {
      names.put(rightRole, method.getParameter(2).getNameAsString());
      declarations.put(rightRole, AstAnalyzer.nodeId(method.getParameter(2)));
    }
    names.put("temp", temp.getNameAsString());
    declarations.put("temp", AstAnalyzer.nodeId(temp));
    return new Helper(method, Map.copyOf(names), Map.copyOf(declarations));
  }

  private record Helper(
      MethodDeclaration method, Map<String, String> names, Map<String, String> declarations) {}

  private record Binding(
      int index, AlgorithmHint hint, Node loop, Map<String, String> declarations, Helper helper) {}
}
