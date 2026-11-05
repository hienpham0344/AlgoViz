package com.aisc.algoviz.ast.engine.ast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aisc.algoviz.ast.engine.exception.CodeParseException;
import org.junit.jupiter.api.Test;

class AstAnalyzerTest {

  private final AstAnalyzer analyzer = new AstAnalyzer();

  @Test
  void extractsExecutableStatementsAndScopedVariablesFromAJavaSourceFile() {
    AstAnalyzer.Analysis analysis = analyzer.analyze(sampleSource());

    assertThat(analysis.mainClassName()).isEqualTo("demo.ScopeSample");
    assertThat(analysis.observationPoints())
        .extracting(point -> point.lineNumber())
        .contains(4, 5, 6, 8, 9);
    assertThat(analysis.variables()).filteredOn(variable -> variable.name().equals("value")).hasSize(2);
    assertThat(analysis.variables())
        .filteredOn(variable -> variable.name().equals("value"))
        .extracting(variable -> variable.astNodeId())
        .doesNotHaveDuplicates();
    assertThat(analysis.variables())
        .filteredOn(variable -> variable.name().equals("value"))
        .allSatisfy(
            variable -> {
              assertThat(variable.scopeStartLine()).isLessThanOrEqualTo(variable.declarationLine());
              assertThat(variable.scopeEndLine()).isGreaterThanOrEqualTo(variable.declarationLine());
            });
  }

  @Test
  void exposesAStableIdForTheSameSourceRange() {
    AstAnalyzer.Analysis first = analyzer.analyze(sampleSource());
    AstAnalyzer.Analysis second = analyzer.analyze(sampleSource());

    assertThat(first.observationPoints())
        .extracting(point -> point.astNodeId())
        .containsExactlyElementsOf(
            second.observationPoints().stream().map(point -> point.astNodeId()).toList());
  }

  @Test
  void rejectsInvalidCompilationUnits() {
    assertThatThrownBy(() -> analyzer.analyze("public class Broken {"))
        .isInstanceOf(CodeParseException.class)
        .extracting("code")
        .isEqualTo("INVALID_JAVA_CODE");
  }

  @Test
  void resolvesTheTypeOfASimpleVarInitializerForRuntimeMapping() {
    AstAnalyzer.Analysis analysis = analyzer.analyze(varSource());

    assertThat(analysis.variables())
        .filteredOn(variable -> variable.name().equals("value"))
        .singleElement()
        .extracting(variable -> variable.declaredType())
        .isEqualTo("int");
  }

  @Test
  void extractsCatchParametersWithinTheirCatchScope() {
    AstAnalyzer.Analysis analysis = analyzer.analyze(catchSource());

    assertThat(analysis.variables())
        .filteredOn(variable -> variable.name().equals("error"))
        .singleElement()
        .satisfies(
            variable -> {
              assertThat(variable.declaredType()).isEqualTo("RuntimeException");
              assertThat(variable.scopeStartLine()).isEqualTo(5);
              assertThat(variable.scopeEndLine()).isEqualTo(7);
            });
  }

  @Test
  void boundsForLoopLocalsToTheForStatement() {
    AstAnalyzer.Analysis analysis = analyzer.analyze(forSource());

    assertThat(analysis.variables())
        .filteredOn(variable -> variable.name().equals("index"))
        .singleElement()
        .satisfies(
            variable -> {
              assertThat(variable.scopeStartLine()).isEqualTo(3);
              assertThat(variable.scopeEndLine()).isEqualTo(5);
            });
  }

  @Test
  void excludesStatementsInsideNestedClassesFromTheTopLevelTarget() {
    AstAnalyzer.Analysis analysis = analyzer.analyze(nestedClassSource());

    assertThat(analysis.observationPoints())
        .extracting(point -> point.lineNumber())
        .contains(3)
        .doesNotContain(7);
  }

  private String sampleSource() {
    return """
        package demo;
        public class ScopeSample {
          public static void main(String[] args) {
            if (args.length == 0) {
              int value = 10;
              System.out.println(value);
            } else {
              int value = 20;
              System.out.println(value);
            }
          }
        }
        """;
  }

  private String varSource() {
    return """
        public class VarSample {
          public static void main(String[] args) {
            var value = 10;
            System.out.println(value);
          }
        }
        """;
  }

  private String catchSource() {
    return """
        public class CatchSample {
          public static void main(String[] args) {
            try {
              throw new RuntimeException();
            } catch (RuntimeException error) {
              System.out.println(error);
            }
          }
        }
        """;
  }

  private String forSource() {
    return """
        public class ForScopeSample {
          public static void main(String[] args) {
            for (int index = 0; index < 1; index++) {
              System.out.println(index);
            }
            System.out.println("done");
          }
        }
        """;
  }

  private String nestedClassSource() {
    return """
        public class OuterSample {
          public static void main(String[] args) {
            System.out.println("outer");
          }
          static class Inner {
            void run() {
              System.out.println("inner");
            }
          }
        }
        """;
  }
}
