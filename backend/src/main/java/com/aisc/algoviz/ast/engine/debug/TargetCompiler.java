package com.aisc.algoviz.ast.engine.debug;

import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.springframework.stereotype.Component;

/** Compiles an uploaded Java source file into an isolated, debug-enabled temporary workspace. */
@Component
public class TargetCompiler {

  public CompiledTarget compile(String sourceCode, AstAnalyzer.Analysis analysis) {
    if (!analysis.hasMainMethod()) {
      throw new DebugException(
          "The uploaded class must declare public static void main(String[] args) before it can be debugged.");
    }

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      throw new DebugException("A full JDK with javac is required to compile the debug target.");
    }

    Path workspace = createWorkspace();
    try {
      Path sourceFile = writeSourceFile(workspace, analysis.mainClassName(), sourceCode);
      Path classesDirectory = workspace.resolve("classes");
      Files.createDirectories(classesDirectory);
      compileSource(compiler, sourceFile, classesDirectory);
      return new CompiledTarget(workspace, classesDirectory, analysis.mainClassName());
    } catch (IOException ex) {
      deleteWorkspace(workspace);
      throw new DebugException("Unable to prepare the uploaded source for compilation.", ex);
    } catch (RuntimeException ex) {
      deleteWorkspace(workspace);
      throw ex;
    }
  }

  private Path createWorkspace() {
    try {
      return Files.createTempDirectory("ast-jdi-target-");
    } catch (IOException ex) {
      throw new DebugException("Unable to create an isolated workspace for the debug target.", ex);
    }
  }

  private Path writeSourceFile(Path workspace, String mainClassName, String sourceCode)
      throws IOException {
    int finalDot = mainClassName.lastIndexOf('.');
    String packageName = finalDot < 0 ? "" : mainClassName.substring(0, finalDot);
    String simpleClassName = finalDot < 0 ? mainClassName : mainClassName.substring(finalDot + 1);
    Path sourceDirectory = workspace.resolve("source");
    if (!packageName.isBlank()) {
      sourceDirectory = sourceDirectory.resolve(packageName.replace('.', '/'));
    }
    Files.createDirectories(sourceDirectory);
    Path sourceFile = sourceDirectory.resolve(simpleClassName + ".java");
    Files.writeString(sourceFile, sourceCode, StandardCharsets.UTF_8);
    return sourceFile;
  }

  private void compileSource(JavaCompiler compiler, Path sourceFile, Path classesDirectory) {
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    StandardJavaFileManager fileManager =
        compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8);
    boolean compilationSucceeded = false;
    try {
      Iterable<? extends JavaFileObject> compilationUnits =
          fileManager.getJavaFileObjectsFromPaths(List.of(sourceFile));
      List<String> options = List.of("-g", "--release", "17", "-proc:none", "-classpath", classesDirectory.toString(), "-d", classesDirectory.toString());
      Boolean success = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits).call();
      if (!Boolean.TRUE.equals(success)) {
        throw new DebugException("Target compilation failed: " + formatDiagnostics(diagnostics));
      }
      compilationSucceeded = true;
    } finally {
      try {
        fileManager.close();
      } catch (IOException closeFailure) {
        if (!compilationSucceeded) {
          throw new DebugException("Unable to close javac resources for the debug target.", closeFailure);
        }
        // JDK 24 in the sandbox can reject ZipFileSystem cleanup for a read-only classpath JAR
        // after javac has successfully emitted classes. The target output is valid at this point.
      }
    }
  }

  private String formatDiagnostics(DiagnosticCollector<JavaFileObject> diagnostics) {
    return diagnostics.getDiagnostics().stream()
        .map(
            diagnostic ->
                "line " + diagnostic.getLineNumber() + ": " + diagnostic.getMessage(Locale.ROOT))
        .reduce((left, right) -> left + "; " + right)
        .orElse("No compiler diagnostic was provided.");
  }

  private static void deleteWorkspace(Path workspace) {
    if (workspace == null || !Files.exists(workspace)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(workspace)) {
      paths.sorted(Comparator.reverseOrder())
          .forEach(
              path -> {
                try {
                  Files.deleteIfExists(path);
                } catch (IOException ex) {
                  throw new UncheckedIOException(ex);
                }
              });
    } catch (IOException | UncheckedIOException ignored) {
      // Cleanup must never hide a compile or JDI error. The OS temp directory can reclaim leftovers.
    }
  }

  public record CompiledTarget(Path workspaceDirectory, Path classesDirectory, String mainClassName)
      implements AutoCloseable {

    @Override
    public void close() {
      deleteWorkspace(workspaceDirectory);
    }
  }
}
