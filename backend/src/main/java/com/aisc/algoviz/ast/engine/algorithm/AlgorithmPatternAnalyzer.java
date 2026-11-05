package com.aisc.algoviz.ast.engine.algorithm;

import com.aisc.algoviz.ast.engine.algorithm.greedy.ActivitySelectionDetector;
import com.aisc.algoviz.ast.engine.algorithm.sort.SortPatternRegistry;
import com.aisc.algoviz.ast.engine.algorithm.twopointers.TwoPointersDetector;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Orchestrates legacy loop strategies and extensible method strategies without executing source.
 */
@Component
public class AlgorithmPatternAnalyzer {
  private final ArrayLoopPatterns arrayPatterns = new ArrayLoopPatterns();
  private final MethodPatternRegistry methodPatterns = MethodPatternRegistry.defaults();
  private final SortPatternRegistry sortPatterns;
  private final ActivitySelectionDetector activitySelectionDetector;
  private final TwoPointersDetector twoPointersDetector = new TwoPointersDetector();

  public AlgorithmPatternAnalyzer() {
    this(SortPatternRegistry.defaults(), new ActivitySelectionDetector());
  }

  public AlgorithmPatternAnalyzer(SortPatternRegistry sortPatterns) {
    this(sortPatterns, new ActivitySelectionDetector());
  }

  public AlgorithmPatternAnalyzer(
      SortPatternRegistry sortPatterns, ActivitySelectionDetector activitySelectionDetector) {
    this.sortPatterns = sortPatterns;
    this.activitySelectionDetector = activitySelectionDetector;
  }

  public List<AlgorithmHint> analyze(CompilationUnit unit) {
    List<AlgorithmHint> hints = new ArrayList<>();
    for (Node loop : unit.findAll(Node.class, AlgorithmPatternAnalyzer::isLoop)) {
      if (loop.getRange().isEmpty()) continue;
      if (loop instanceof DoStmt) {
        twoPointersDetector.detect(unit, loop).ifPresent(hints::add);
        continue;
      }
      Optional<AlgorithmHint> sort = sortPatterns.detect(loop);
      if (sort.isPresent()) {
        hints.add(sort.get());
        continue;
      }
      Optional<AlgorithmHint> activitySelection = activitySelectionDetector.detect(unit, loop);
      if (activitySelection.isPresent()) {
        hints.add(activitySelection.get());
        continue;
      }
      if (loop instanceof ForEachStmt) continue;
      Optional<AlgorithmHint> hint = arrayPatterns.detect(loop);
      if (hint.isPresent()) hints.add(hint.get());
      else twoPointersDetector.detect(unit, loop).ifPresent(hints::add);
    }
    for (var method : unit.findAll(MethodDeclaration.class)) {
      hints.addAll(methodPatterns.detect(method));
    }
    return List.copyOf(hints);
  }

  private static boolean isLoop(Node node) {
    return node instanceof WhileStmt
        || node instanceof ForStmt
        || node instanceof ForEachStmt
        || node instanceof DoStmt;
  }
}
