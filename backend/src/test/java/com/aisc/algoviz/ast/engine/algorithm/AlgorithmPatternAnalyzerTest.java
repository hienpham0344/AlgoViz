package com.aisc.algoviz.ast.engine.algorithm;

import static org.assertj.core.api.Assertions.assertThat;

import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlgorithmPatternAnalyzerTest {
  private final AstAnalyzer parser = new AstAnalyzer();
  private final AlgorithmPatternAnalyzer analyzer = new AlgorithmPatternAnalyzer();

  private List<AlgorithmHint> detect(String body) {
    return analyzer.analyze(
        parser.parseCompilationUnit(
            "class Sample { void solve(int[] data, int target, int k) {\n" + body + "\n} }"));
  }

  private String search(String midpoint) {
    return """
        int lo = 0, hi = data.length - 1;
        while (lo <= hi) {
          int pivot = %s;
          if (data[pivot] == target) return;
          if (data[pivot] < target) lo = pivot + 1;
          else hi = pivot - 1;
        }
        """
        .formatted(midpoint);
  }

  @Test
  void detectsBinarySearchWithRenamedVariablesAndExactLoopRange() {
    var hints = detect(search("lo + (hi - lo) / 2"));
    assertThat(hints).hasSize(1);
    var hint = hints.get(0);
    assertThat(hint.type()).isEqualTo("binary-search");
    assertThat(hint.variables())
        .containsEntry("left", "lo")
        .containsEntry("right", "hi")
        .containsEntry("mid", "pivot")
        .containsEntry("array", "data");
    assertThat(hint.startLine()).isEqualTo(3);
    assertThat(hint.endLine()).isEqualTo(8);
    assertThat(hint.evidence()).isNotEmpty();
  }

  @Test
  void supportsCommonMidpointForms() {
    for (String expression :
        List.of("(lo + hi) / 2", "(hi + lo) >>> 1", "lo + ((hi - lo) >> 1)", "((lo) + (hi)) / 2")) {
      assertThat(detect(search(expression)))
          .extracting(AlgorithmHint::type)
          .containsExactly("binary-search");
    }
  }

  @Test
  void supportsHalfOpenBinarySearchAndForLoop() {
    String source =
        search("(lo + hi) / 2")
            .replace("while (lo <= hi)", "for (; lo < hi;)")
            .replace("hi = pivot - 1", "hi = pivot");
    assertThat(detect(source)).extracting(AlgorithmHint::type).containsExactly("binary-search");
  }

  @Test
  void rejectsIncompleteBinarySearchEvidence() {
    assertThat(detect(search("lo + hi"))).isEmpty();
    assertThat(detect(search("(lo + hi) / 2").replace("hi = pivot - 1", "hi = target"))).isEmpty();
    assertThat(detect(search("(lo + hi) / 2").replace("data[pivot]", "target"))).isEmpty();
  }

  @Test
  void detectsOpposingPointersWithDifferentUpdateSyntax() {
    assertThat(
            detect(
                """
        int a = 0, b = data.length - 1;
        for (; a < b; a += 1, b = b - 1) {
          int tmp = data[a]; data[a] = data[b]; data[b] = tmp;
        }
        """))
        .singleElement()
        .satisfies(
            hint -> {
              assertThat(hint.type()).isEqualTo("two-pointers");
              assertThat(hint.variables()).containsEntry("left", "a").containsEntry("right", "b");
            });
  }

  @Test
  void rejectsDifferentArraysAndMissingMovement() {
    String body = "while (a < b) { int x = data[a] + other[b]; a++; b--; }";
    assertThat(detect(body)).isEmpty();
    assertThat(detect(body.replace("other", "data").replace("b--;", ""))).isEmpty();
  }

  @Test
  void detectsVariableSumWindow() {
    assertThat(
            detect(
                """
        int begin = 0, total = 0;
        for (int end = 0; end < data.length; end++) {
          total += data[end];
          while (total >= target) {
            total -= data[begin];
            begin++;
          }
        }
        """))
        .singleElement()
        .satisfies(
            hint -> {
              assertThat(hint.type()).isEqualTo("sliding-window");
              assertThat(hint.visualPlan()).isEqualTo("array-window");
              assertThat(hint.variables())
                  .containsEntry("left", "begin")
                  .containsEntry("right", "end")
                  .containsEntry("accumulator", "total");
            });
  }

  @Test
  void detectsFixedSumWindow() {
    assertThat(
            detect(
                """
        int total = 0;
        for (int end = 0; end < data.length; end++) {
          total += data[end];
          if (end >= k) total -= data[end - k];
        }
        """))
        .singleElement()
        .satisfies(
            hint -> {
              assertThat(hint.type()).isEqualTo("sliding-window");
              assertThat(hint.variables()).containsEntry("windowSize", "k");
              assertThat(hint.variables()).doesNotContainKey("left");
            });
  }

  @Test
  void detectsConditionalLeftWindowShrink() {
    assertThat(
            detect(
                """
        int total = 0, begin = 0;
        for (int end = 0; end < data.length; end++) {
          total += data[end];
          if (end - begin + 1 > k) { total -= data[begin]; begin++; }
        }
        """))
        .extracting(AlgorithmHint::type)
        .containsExactly("sliding-window");
  }

  @Test
  void rejectsUnrelatedAccumulatorAndChangingWindowSize() {
    String body =
        """
        for (int end = 0; end < data.length; end++) {
          total += data[end];
          if (end >= k) other -= data[end - k];
        }
        """;
    assertThat(detect(body)).isEmpty();
    assertThat(detect(body.replace("other", "total").replace("end++)", "end++, k++)"))).isEmpty();
  }

  @Test
  void ignoresNamesCommentsAndStringLiteralsAsEvidence() {
    assertThat(
            detect(
                """
        // binary search: left right mid sliding window
        String text = "while (left < right) { left++; right--; }";
        for (int left = 0; left < data.length; left++) { int mid = data[left]; }
        """))
        .isEmpty();
  }

  @Test
  void doesNotBorrowUpdatesFromNestedLoopsOrLambdas() {
    assertThat(
            detect(
                """
        while (a < b) {
          int x = data[a] + data[b];
          while (ready) { a++; b--; }
        }
        """))
        .isEmpty();
    assertThat(
            detect(
                """
        while (a < b) {
          int x = data[a] + data[b];
          Runnable r = () -> { a++; b--; };
        }
        """))
        .isEmpty();
  }

  @Test
  void keepsMethodsIndependentAndResultsDeterministic() {
    var unit =
        parser.parseCompilationUnit(
            """
        class Sample {
          void first(int[] data, int a, int b) { while (a < b) { int x = data[a] + data[b]; a++; } }
          void second(int[] data, int a, int b) { while (a < b) { b--; } }
          void third(int[] data) { int a = 0, b = data.length - 1; while (a < b) { int x = data[a] + data[b]; a++; b--; } }
        }
        """);
    assertThat(analyzer.analyze(unit)).hasSize(1).isEqualTo(analyzer.analyze(unit));
    assertThat(analyzer.analyze(unit).get(0).startLine()).isEqualTo(4);
  }

  @Test
  void handlesLoopsWithoutConditionsAndUnrangedNodes() {
    assertThat(detect("for (;;) { break; }")).isEmpty();
    var unit = parser.parseCompilationUnit("class Sample { void f() { while (a < b) {} } }");
    unit.walk(node -> node.setRange(null));
    assertThat(analyzer.analyze(unit)).isEmpty();
  }

  @Test
  void doesNotTreatForInitializationAsRepeatedMovement() {
    assertThat(
            detect(
                """
        for (a++, b--; a < b;) {
          int value = data[a] + data[b];
        }
        """))
        .isEmpty();
  }
}
