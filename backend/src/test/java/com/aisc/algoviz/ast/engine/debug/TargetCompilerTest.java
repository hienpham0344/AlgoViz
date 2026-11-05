package com.aisc.algoviz.ast.engine.debug;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;

class TargetCompilerTest {

  private final AstAnalyzer analyzer = new AstAnalyzer();
  private final TargetCompiler compiler = new TargetCompiler();

  @Test
  void compilesACompleteSourceFileWithDebugInformation() throws Exception {
    try (TargetCompiler.CompiledTarget target = compiler.compile(validSource(), analyzer.analyze(validSource()))) {
      assertThat(target.mainClassName()).isEqualTo("demo.DebugTarget");
      assertThat(target.classesDirectory().resolve("demo/DebugTarget.class")).exists();
      assertThat(Files.isDirectory(target.workspaceDirectory())).isTrue();
    }
  }

  @Test
  void rejectsAClassWithoutAMainMethod() {
    String source = "package demo; public class NoMain { int count = 1; }";

    assertThatThrownBy(() -> compiler.compile(source, analyzer.analyze(source)))
        .isInstanceOf(DebugException.class)
        .hasMessageContaining("main");
  }

  private String validSource() {
    return """
        package demo;
        public class DebugTarget {
          public static void main(String[] args) {
            int total = 2;
            System.out.println(total);
          }
        }
        """;
  }
}
