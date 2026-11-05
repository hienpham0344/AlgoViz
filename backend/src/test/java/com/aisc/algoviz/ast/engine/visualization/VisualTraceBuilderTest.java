package com.aisc.algoviz.ast.engine.visualization;

import static org.assertj.core.api.Assertions.assertThat;

import com.aisc.algoviz.ast.engine.algorithm.AlgorithmPatternAnalyzer;
import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.aisc.algoviz.ast.engine.model.ExecutionContext;
import com.aisc.algoviz.ast.engine.model.ExecutionObservation;
import com.aisc.algoviz.ast.engine.model.ObservationResult;
import com.aisc.algoviz.ast.engine.model.VisualEvent;
import com.aisc.algoviz.ast.engine.model.VisualState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VisualTraceBuilderTest {
  private final AstAnalyzer.Analysis analysis =
      new AstAnalyzer()
          .analyze(
              """
      public class Sample {
        public static void main(String[] args) {
          int[] nums = {1, 2, 3};
          int left = 0, right = nums.length - 1;
          while (left < right) {
            int temp = nums[left];
            nums[left] = nums[right];
            nums[right] = temp;
            left++;
            right--;
          }
          System.out.println(nums[0]);
        }
      }
      """);
  private final List<AlgorithmHint> hints =
      new AlgorithmPatternAnalyzer().analyze(analysis.compilationUnit()).stream()
          .map(h -> new AlgorithmHint(h.type(), h.confidence(), h.visualPlan(), h.evidence(),
              java.util.Map.of("array", h.variables().get("data"), "left", h.variables().get("left"),
                  "right", h.variables().get("right")),
              h.startLine(), h.endLine(), h.patternAstNodeId(),
              h.methodAstNodeId(), java.util.Map.of("array", h.variableDeclarationIds().get("data"),
                  "left", h.variableDeclarationIds().get("left"),
                  "right", h.variableDeclarationIds().get("right")), h.sort(), h.quick(),
              h.merge(), h.heap(), h.activitySelection()))
          .toList();
  private final VisualTraceBuilder builder = new VisualTraceBuilder();

  private ObservationResult variable(String name, String type, Object value) {
    var declaration =
        analysis.variables().stream().filter(v -> v.name().equals(name)).findFirst().orElseThrow();
    return new ObservationResult(
        declaration.astNodeId(),
        name,
        declaration.declaredType(),
        String.valueOf(value),
        type,
        value,
        6);
  }

  private List<ObservationResult> variables(Object array, Object left, Object right) {
    return List.of(
        variable("nums", "array", array),
        variable("left", "primitive", left),
        variable("right", "primitive", right));
  }

  private ExecutionContext context(long thread, String method, int depth, long pc) {
    return new ExecutionContext(
        thread, "main", "Sample", method, "([Ljava/lang/String;)V", depth, pc);
  }

  private ExecutionObservation step(
      long sequence, int line, ExecutionContext context, List<ObservationResult> values) {
    var point =
        analysis.observationPoints().stream()
            .filter(p -> p.lineNumber() == line)
            .findFirst()
            .orElseThrow();
    return new ExecutionObservation(
        sequence,
        point.astNodeId(),
        line,
        point.startLine(),
        point.endLine(),
        point.startColumn(),
        point.endColumn(),
        point.statementKind(),
        point.code(),
        values,
        context);
  }

  private List<ExecutionObservation> build(ExecutionObservation... steps) {
    return builder.build(analysis, hints, List.of(steps));
  }

  private VisualState single(List<ObservationResult> variables) {
    return build(step(1, 6, context(1, "main", 1, 10), variables)).get(0).visualStates().get(0);
  }

  @Test
  void producesIndependentStateWithoutChangingRawSnapshot() {
    var original = step(1, 6, context(1, "main", 1, 10), variables(List.of(1, 2, 3), 0, 2));
    var enriched = build(original).get(0);
    var state = enriched.visualStates().get(0);
    assertThat(state.status()).isEqualTo("READY");
    assertThat(state.hintIndex()).isZero();
    assertThat(state.array().values()).containsExactly(1, 2, 3);
    assertThat(state.pointers().get("left").index()).isZero();
    assertThat(state.range())
        .isEqualTo(new VisualState.IndexRange("POINTER_SPAN", 0L, 3L, "VALID"));
    assertThat(enriched.variables()).isEqualTo(original.variables());
    assertThat(enriched.context()).isEqualTo(original.context());
    assertThat(original.visualStates()).isEmpty();
    assertThat(build(original)).isEqualTo(build(original));
    assertThat(enriched.visualEvents())
        .extracting(VisualEvent::type)
        .containsExactly("STATE_ENTERED");
  }

  @Test
  void emitsOnlyObservedDifferencesBetweenAdjacentForwardSnapshots() {
    var values = variables(List.of(1, 2, 3), 0, 2);
    var first = step(1, 6, context(1, "main", 1, 10), values);
    var second = step(2, 7, context(1, "main", 1, 15), variables(List.of(3, 2, 3), 1, 2));
    var result = build(first, second);
    assertThat(result.get(1).visualEvents())
        .extracting(VisualEvent::type)
        .containsExactly("ARRAY_CHANGED", "POINTER_CHANGED", "RANGE_CHANGED");
    assertThat(result.get(1).visualEvents())
        .allSatisfy(
            e -> {
              assertThat(e.fromSequence()).isEqualTo(1);
              assertThat(e.toSequence()).isEqualTo(2);
            });
    assertThat(build(first, step(2, 7, context(1, "main", 1, 15), values)).get(1).visualEvents())
        .isEmpty();
  }

  @Test
  void missingValuesAreNotCarriedForwardAndWrongDeclarationIsRejected() {
    var first = step(1, 6, context(1, "main", 1, 10), variables(List.of(1, 2, 3), 0, 2));
    var wrongLeft =
        new ObservationResult("wrong-declaration", "left", "int", "1", "primitive", 1, 7);
    var second =
        step(
            2,
            7,
            context(1, "main", 1, 15),
            List.of(variable("nums", "array", List.of(1, 2, 3)), wrongLeft));
    var state = build(first, second).get(1).visualStates().get(0);
    assertThat(state.status()).isEqualTo("PARTIAL");
    assertThat(state.pointers().get("left").status()).isEqualTo("MISSING");
    assertThat(state.pointers().get("left").index()).isNull();
    assertThat(state.pointers().get("right").index()).isNull();
    assertThat(state.range().status()).isEqualTo("UNKNOWN");
  }

  @Test
  void resetsAcrossThreadDepthRewindAndSequenceGaps() {
    var first = step(1, 6, context(1, "main", 1, 10), variables(List.of(1, 2, 3), 0, 2));
    for (ExecutionContext next :
        List.of(
            context(2, "main", 1, 15),
            context(1, "main", 2, 15),
            context(1, "main", 1, 10),
            context(1, "main", 1, 5),
            new ExecutionContext(1, "main", "Sample", "main", "()V", 1, 15))) {
      var events =
          build(first, step(2, 7, next, variables(List.of(3, 2, 1), 1, 1))).get(1).visualEvents();
      assertThat(events).extracting(VisualEvent::type).containsExactly("STATE_RESET");
      assertThat(events.get(0).fromSequence()).isNull();
    }
    assertThat(
            build(first, step(4, 7, context(1, "main", 1, 15), first.variables()))
                .get(1)
                .visualEvents())
        .extracting(VisualEvent::type)
        .containsExactly("STATE_RESET");
  }

  @Test
  void leavesScopeAndDoesNotReuseStateAfterReturning() {
    var first = step(1, 6, context(1, "main", 1, 10), variables(List.of(1, 2, 3), 0, 2));
    var outside = step(2, 12, context(1, "main", 1, 30), first.variables());
    var result = build(first, outside, step(3, 6, context(1, "main", 1, 10), first.variables()));
    assertThat(result.get(1).visualStates()).isEmpty();
    assertThat(result.get(1).visualEvents())
        .extracting(VisualEvent::type)
        .containsExactly("STATE_LEFT");
    assertThat(result.get(2).visualEvents())
        .extracting(VisualEvent::type)
        .containsExactly("STATE_ENTERED");
    assertThat(
            build(step(1, 6, context(1, "other", 1, 10), first.variables())).get(0).visualStates())
        .isEmpty();
    assertThat(build(step(1, 6, null, first.variables())).get(0).visualStates()).isEmpty();
  }

  @Test
  void handlesTruncationWithoutRenderingMarkerAsArrayElement() {
    var state = single(variables(List.of(1, 2, Map.of("truncated", true, "length", 200)), 0, 150));
    assertThat(state.array().length()).isEqualTo(200);
    assertThat(state.array().values()).containsExactly(1, 2);
    assertThat(state.array().truncated()).isTrue();
    assertThat(state.pointers().get("right").status()).isEqualTo("NOT_CAPTURED");
    assertThat(state.status()).isEqualTo("PARTIAL");
    assertThat(state.notes()).contains("ARRAY_TRUNCATED");
  }

  @Test
  void preservesRawOutOfBoundsIndicesWithoutClampingOrInventingAnArrayCell() {
    var state = single(variables(List.of(1, 2, 3), -1, 3));
    assertThat(state.pointers().get("left").index()).isEqualTo(-1);
    assertThat(state.pointers().get("right").index()).isEqualTo(3);
    assertThat(state.pointers().values())
        .allSatisfy(p -> assertThat(p.status()).isEqualTo("OUT_OF_BOUNDS"));
    assertThat(state.range().status()).isEqualTo("UNKNOWN");
    assertThat(single(variables(List.of(), 0, -1)).array().length()).isZero();
    assertThat(single(variables(List.of(), 0, -1)).status()).isEqualTo("PARTIAL");
  }

  @Test
  void distinguishesMissingNullUnavailableAndUnsupportedArrays() {
    assertThat(single(List.of()).array().status()).isEqualTo("MISSING");
    assertThat(single(List.of(variable("nums", "null", null))).array().status()).isEqualTo("NULL");
    assertThat(single(List.of(variable("nums", "unavailable", null))).array().status())
        .isEqualTo("UNAVAILABLE");
    assertThat(single(variables(List.of(List.of(1)), 0, 0)).array().status())
        .isEqualTo("UNSUPPORTED");
    assertThat(single(variables(Map.of("referenceId", 1), 0, 0)).array().status())
        .isEqualTo("UNSUPPORTED");
  }

  @Test
  void rejectsNonIntegralAndOversizedPointerValues() {
    for (Object value : List.of(1.5, "1", Long.MAX_VALUE, Double.NaN, true)) {
      assertThat(single(variables(List.of(1, 2), value, 1)).pointers().get("left").status())
          .isEqualTo("INVALID");
    }
  }

  @Test
  void copiesArrayValuesIncludingNullElements() {
    List<Object> values = new ArrayList<>(Arrays.asList(1, null, 3));
    var state = single(variables(values, 0, 2));
    values.set(0, 9);
    assertThat(state.array().values()).containsExactly(1, null, 3);
  }

  @Test
  void returnsEmptyVisualizationForNoHintsAndAmbiguousSameLineLoops() {
    var step = step(1, 6, context(1, "main", 1, 10), variables(List.of(1, 2), 0, 1));
    assertThat(builder.build(analysis, List.of(), List.of(step)).get(0).visualStates()).isEmpty();
    var other =
        new AstAnalyzer()
            .analyze(
                "class Sample { void f(int[] a, int x, int y) { int l=0,r=a.length-1; while(l<r) { int z=a[l]+a[r]; l++; r--; } { int left=0,right=a.length-1; while(left<right) { int z=a[left]+a[right]; left++; right--; } } } }");
    var otherHints = new AlgorithmPatternAnalyzer().analyze(other.compilationUnit());
    assertThat(otherHints).hasSize(2);
    var point = other.observationPoints().stream()
        .filter(p -> p.code().contains("int z=a[l]+a[r]"))
        .findFirst().orElseThrow();
    var otherStep =
        new ExecutionObservation(
            1,
            point.astNodeId(),
            1,
            point.startLine(),
            point.endLine(),
            point.startColumn(),
            point.endColumn(),
            point.statementKind(),
            point.code(),
            List.of(),
            new ExecutionContext(1, "main", "Sample", "f", "([III)V", 1, 10));
    assertThat(builder.build(other, otherHints, List.of(otherStep)).get(0).visualStates())
        .hasSize(1);
    var legacy =
        otherHints.stream()
            .map(
                h ->
                    new AlgorithmHint(
                        h.type(),
                        h.confidence(),
                        h.visualPlan(),
                        h.evidence(),
                        h.variables(),
                        h.startLine(),
                        h.endLine()))
            .toList();
    assertThat(builder.build(other, legacy, List.of(otherStep)).get(0).visualStates()).isEmpty();
    assertThat(otherHints).extracting(AlgorithmHint::patternAstNodeId).doesNotHaveDuplicates();
    for (int index = 0; index < otherHints.size(); index++) {
      var hint = otherHints.get(index);
      var loopPoint =
          other.observationPoints().stream()
              .filter(p -> p.astNodeId().equals(hint.patternAstNodeId()))
              .findFirst()
              .orElseThrow();
      var loopStep =
          new ExecutionObservation(
              1,
              loopPoint.astNodeId(),
              1,
              1,
              1,
              loopPoint.startColumn(),
              loopPoint.endColumn(),
              loopPoint.statementKind(),
              loopPoint.code(),
              List.of(),
              otherStep.context());
      assertThat(builder.build(other, otherHints, List.of(loopStep)).get(0).visualStates())
          .extracting(VisualState::hintIndex)
          .containsExactly(index);
    }
  }

  @Test
  void rejectsScalarWindowSizeAndAccumulatorThatCannotBeRendered() {
    var hint = hints.get(0);
    var window =
        new AlgorithmHint(
            "sliding-window",
            0.8,
            "array-window",
            List.of(),
            Map.of("array", "nums", "right", "right", "windowSize", "left", "accumulator", "temp"),
            hint.startLine(),
            hint.endLine());
    var vars = new ArrayList<>(variables(List.of(1, 2, 3), 0, 2));
    vars.add(variable("temp", "primitive", Double.NaN));
    var state =
        builder
            .build(analysis, List.of(window), List.of(step(1, 7, context(1, "main", 1, 10), vars)))
            .get(0)
            .visualStates()
            .get(0);
    assertThat(state.scalars().get("windowSize").status()).isEqualTo("INVALID");
    assertThat(state.scalars().get("accumulator").status()).isEqualTo("INVALID");
    assertThat(state.range().status()).isEqualTo("UNKNOWN");
    assertThat(state.status()).isEqualTo("PARTIAL");
  }
}
