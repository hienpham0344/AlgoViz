package com.aisc.algoviz.ast.engine.algorithm;

import com.aisc.algoviz.ast.engine.algorithm.array.ArrayPatternsDetector;
import com.aisc.algoviz.ast.engine.algorithm.backtracking.BacktrackingDetector;
import com.aisc.algoviz.ast.engine.algorithm.dp.LinearDynamicProgrammingDetector;
import com.aisc.algoviz.ast.engine.algorithm.dp.RollingFibonacciDetector;
import com.aisc.algoviz.ast.engine.algorithm.dp.SequenceDynamicProgrammingDetector;
import com.aisc.algoviz.ast.engine.algorithm.graph.DijkstraDetector;
import com.aisc.algoviz.ast.engine.algorithm.graph.GraphPathsDetector;
import com.aisc.algoviz.ast.engine.algorithm.graph.GraphTraversalDetector;
import com.aisc.algoviz.ast.engine.algorithm.graph.UnionFindDetector;
import com.aisc.algoviz.ast.engine.algorithm.greedy.ActivityCountDetector;
import com.aisc.algoviz.ast.engine.algorithm.greedy.JumpGameDetector;
import com.aisc.algoviz.ast.engine.algorithm.search.GenericBinarySearchDetector;
import com.aisc.algoviz.ast.engine.algorithm.string.KmpDetector;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.github.javaparser.ast.body.MethodDeclaration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ordered, immutable strategy registry. Repeated evidence for one source pattern is deduplicated.
 */
public final class MethodPatternRegistry {
  private final List<MethodPatternDetector> detectors;

  public MethodPatternRegistry(List<MethodPatternDetector> detectors) {
    this.detectors = List.copyOf(detectors);
  }

  public static MethodPatternRegistry defaults() {
    return new MethodPatternRegistry(
        List.of(
            new ArrayPatternsDetector(),
            new GenericBinarySearchDetector(),
            new GraphTraversalDetector(),
            new GraphPathsDetector(),
            new LinearDynamicProgrammingDetector(),
            new RollingFibonacciDetector(),
            new SequenceDynamicProgrammingDetector(),
            new BacktrackingDetector(),
            new KmpDetector(),
            new DijkstraDetector(),
            new UnionFindDetector(),
            new JumpGameDetector(),
            new ActivityCountDetector()));
  }

  public List<AlgorithmHint> detect(MethodDeclaration method) {
    Map<String, AlgorithmHint> matches = new LinkedHashMap<>();
    for (var detector : detectors)
      for (var hint : detector.detect(method))
        matches.putIfAbsent(hint.type() + ":" + hint.patternAstNodeId(), hint);
    return List.copyOf(matches.values());
  }
}
