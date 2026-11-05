package com.aisc.algoviz.ast.engine.algorithm.sort;

import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.github.javaparser.ast.Node;
import java.util.Optional;

/** Detects one conservative sorting pattern from a loop subtree. */
public interface SortPatternDetector {
  Optional<AlgorithmHint> detect(Node loop);
}
