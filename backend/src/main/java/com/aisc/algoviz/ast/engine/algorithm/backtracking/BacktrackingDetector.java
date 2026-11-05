package com.aisc.algoviz.ast.engine.algorithm.backtracking;

import static com.aisc.algoviz.ast.engine.algorithm.PatternSupport.*;

import com.aisc.algoviz.ast.engine.algorithm.MethodPatternDetector;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import java.util.List;
import java.util.Map;

/** The choose/explore/undo pattern shared by combinations, permutations and path enumeration. */
public final class BacktrackingDetector implements MethodPatternDetector {
  @Override
  public List<AlgorithmHint> detect(MethodDeclaration method) {
    boolean base =
        owned(method, IfStmt.class).stream()
            .anyMatch(
                branch ->
                    !owned(branch.getThenStmt(), ReturnStmt.class).isEmpty()
                        && loop(branch).isEmpty());
    if (!base) return List.of();
    for (var loop : loops(method)) {
      List<MethodCallExpr> calls = owned(loop, MethodCallExpr.class);
      for (MethodCallExpr choose : calls) {
        if (!choose.getNameAsString().equals("add")
            || choose.getArguments().size() != 1
            || choose.getScope().isEmpty()
            || name(choose.getScope().get()) == null) continue;
        Expression path = choose.getScope().get();
        for (MethodCallExpr explore : calls) {
          if (!recursive(explore, method)
              || !before(choose, explore)
              || explore.getArguments().stream().noneMatch(a -> same(a, path))) continue;
          for (MethodCallExpr undo : calls) {
            if (!callOn(undo, path, "remove")
                || !before(explore, undo)
                || !sameBlock(choose, explore, undo)
                || undo.getArguments().size() != 1
                || !undoChoice(method, path, choose.getArgument(0), undo.getArgument(0))) continue;
            return hint(
                    method,
                    "backtracking",
                    Map.of("path", name(path)),
                    "A base case terminates exploration",
                    "Each loop adds a choice, recurses with the same collection, then removes that choice")
                .map(List::of)
                .orElse(List.of());
          }
        }
      }
    }
    return List.of();
  }

  private boolean undoChoice(
      MethodDeclaration method, Expression path, Expression choice, Expression undo) {
    if (unwrap(undo) instanceof BinaryExpr last
        && last.getOperator() == BinaryExpr.Operator.MINUS
        && integer(last.getRight(), 1)
        && unwrap(last.getLeft()) instanceof MethodCallExpr size
        && callOn(size, path, "size")
        && size.getArguments().isEmpty())
      return collectionType(method, path, List.of("List", "ArrayList", "LinkedList"));
    if (!same(choice, undo)) return false;
    return collectionType(
        method,
        path,
        List.of("Set", "SortedSet", "NavigableSet", "HashSet", "TreeSet", "LinkedHashSet"));
  }

  private boolean collectionType(MethodDeclaration method, Expression path, List<String> types) {
    var type =
        method.getParameters().stream()
            .filter(p -> p.getNameAsString().equals(name(path)))
            .map(p -> p.getType())
            .findFirst()
            .or(() -> declaration(method, path).map(d -> d.getType()));
    return type.filter(t -> t.isClassOrInterfaceType())
        .map(t -> t.asClassOrInterfaceType())
        .filter(
            t ->
                types.stream()
                    .anyMatch(
                        simple -> standardType(t, t.getNameWithScope(), "java.util." + simple)))
        .isPresent();
  }

  private boolean before(MethodCallExpr first, MethodCallExpr second) {
    return first.getBegin().isPresent()
        && second.getBegin().isPresent()
        && first.getBegin().get().isBefore(second.getBegin().get());
  }

  private boolean sameBlock(MethodCallExpr choose, MethodCallExpr explore, MethodCallExpr undo) {
    var block = choose.getParentNode().flatMap(n -> n.getParentNode()).orElse(null);
    return block instanceof com.github.javaparser.ast.stmt.BlockStmt
        && explore.getParentNode().flatMap(n -> n.getParentNode()).orElse(null) == block
        && undo.getParentNode().flatMap(n -> n.getParentNode()).orElse(null) == block;
  }
}
