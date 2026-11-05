package com.aisc.algoviz.ast.engine.algorithm;

import java.util.List;

/**
 * Versioned recognition scope; a supported structural form is not a promise about every rewrite.
 */
public final class AlgorithmCatalog {
  public static final String VERSION = "intermediate-v1";

  public record Entry(String type, String family) {}

  private AlgorithmCatalog() {}

  public static List<Entry> entries() {
    return List.of(
        new Entry("binary-search", "search"),
        new Entry("linear-search", "search"),
        new Entry("bubble-sort", "sort"),
        new Entry("selection-sort", "sort"),
        new Entry("insertion-sort", "sort"),
        new Entry("quick-sort", "sort"),
        new Entry("merge-sort", "sort"),
        new Entry("heap-sort", "sort"),
        new Entry("breadth-first-search", "graph"),
        new Entry("depth-first-search", "graph"),
        new Entry("topological-sort", "graph"),
        new Entry("floyd-warshall", "graph"),
        new Entry("dijkstra", "graph"),
        new Entry("union-find", "graph"),
        new Entry("fibonacci-dp", "dp"),
        new Entry("coin-change", "dp"),
        new Entry("knapsack", "dp"),
        new Entry("longest-increasing-subsequence", "dp"),
        new Entry("longest-common-subsequence", "dp"),
        new Entry("edit-distance", "dp"),
        new Entry("activity-selection", "greedy"),
        new Entry("jump-game", "greedy"),
        new Entry("backtracking", "backtracking"),
        new Entry("prefix-sum", "array-string"),
        new Entry("kadane", "array-string"),
        new Entry("two-pointers", "array-string"),
        new Entry("sliding-window", "array-string"),
        new Entry("kmp", "array-string"));
  }
}
