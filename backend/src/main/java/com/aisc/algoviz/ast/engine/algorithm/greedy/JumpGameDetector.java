package com.aisc.algoviz.ast.engine.algorithm.greedy;

import static com.aisc.algoviz.ast.engine.algorithm.PatternSupport.*;

import com.aisc.algoviz.ast.engine.algorithm.MethodPatternDetector;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import java.util.List;
import java.util.Map;

/** Greedy reachable-frontier expansion for the jump game. */
public final class JumpGameDetector implements MethodPatternDetector {
  @Override
  public List<AlgorithmHint> detect(MethodDeclaration method) {
    for (Node loop : loops(method)) {
      Expression condition =
          loop instanceof ForStmt f
              ? f.getCompare().orElse(null)
              : loop instanceof WhileStmt w ? w.getCondition() : null;
      if (condition == null) continue;
      for (AssignExpr write : owned(loop, AssignExpr.class)) {
        String reach = name(write.getTarget());
        if (reach == null
            || write.getOperator() != AssignExpr.Operator.ASSIGN
            || !math(write.getValue(), "max")) continue;
        var maximum = unwrap(write.getValue()).asMethodCallExpr();
        for (Expression term : maximum.getArguments()) {
          Expression candidate = maximum.getArgument(term == maximum.getArgument(0) ? 1 : 0);
          if (!named(term, reach)
              || !(unwrap(candidate) instanceof BinaryExpr sum)
              || sum.getOperator() != BinaryExpr.Operator.PLUS
              || !(unwrap(sum.getRight()) instanceof ArrayAccessExpr element)
              || name(element.getName()) == null
              || name(element.getIndex()) == null
              || !same(sum.getLeft(), element.getIndex())) continue;
          boolean reachable =
              owned(condition, BinaryExpr.class).stream()
                  .anyMatch(
                      b ->
                          b.getOperator() == BinaryExpr.Operator.LESS_EQUALS
                              && same(b.getLeft(), element.getIndex())
                              && named(b.getRight(), reach));
          if (reachable)
            return hint(
                    loop,
                    "jump-game",
                    Map.of("array", name(element.getName()), "reach", reach),
                    "Only reachable positions are scanned and the maximum frontier expands by index plus jump length")
                .map(List::of)
                .orElse(List.of());
        }
      }
    }
    return List.of();
  }
}
