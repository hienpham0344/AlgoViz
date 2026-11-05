package com.aisc.algoviz.ast.engine.algorithm.sort;

import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.github.javaparser.ast.Node;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Ordered sort detector set; the first complete structural match owns the loop. */
public final class SortPatternRegistry {
  private final List<SortPatternDetector> detectors;

  public SortPatternRegistry(List<SortPatternDetector> detectors) {
    this.detectors = List.copyOf(detectors);
    if (this.detectors.isEmpty() || this.detectors.stream().anyMatch(Objects::isNull))
      throw new IllegalArgumentException("At least one non-null sort detector is required");
  }

  public static SortPatternRegistry defaults() {
    return new SortPatternRegistry(
        List.of(
            new BubbleSortDetector(),
            new SelectionSortDetector(),
            new InsertionSortDetector(),
            new QuickSortDetector(),
            new MergeSortDetector(),
            new HeapSortDetector()));
  }

  public Optional<AlgorithmHint> detect(Node loop) {
    return detectors.stream()
        .map(detector -> detector.detect(loop))
        .flatMap(Optional::stream)
        .findFirst();
  }
}
