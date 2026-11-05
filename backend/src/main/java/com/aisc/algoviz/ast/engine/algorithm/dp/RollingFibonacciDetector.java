package com.aisc.algoviz.ast.engine.algorithm.dp;

import static com.aisc.algoviz.ast.engine.algorithm.PatternSupport.*;

import com.aisc.algoviz.ast.engine.algorithm.MethodPatternDetector;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import java.util.List;
import java.util.Map;

/** Constant-space Fibonacci: seed 0/1, form the sum, then rotate the two states. */
public final class RollingFibonacciDetector implements MethodPatternDetector {
  @Override
  public List<AlgorithmHint> detect(MethodDeclaration method) {
    for (ForStmt loop : owned(method, ForStmt.class)) {
      if (!(loop.getBody() instanceof BlockStmt body) || body.getStatements().size() != 3) continue;
      List<AssignExpr> writes =
          body.getStatements().stream()
              .filter(ExpressionStmt.class::isInstance)
              .map(ExpressionStmt.class::cast)
              .map(ExpressionStmt::getExpression)
              .filter(AssignExpr.class::isInstance)
              .map(AssignExpr.class::cast)
              .toList();
      if (writes.size() != 3
          || writes.stream()
              .anyMatch(
                  w ->
                      w.getOperator() != AssignExpr.Operator.ASSIGN || name(w.getTarget()) == null))
        continue;
      AssignExpr sum = writes.get(0), shift = writes.get(1), finish = writes.get(2);
      if (!(unwrap(sum.getValue()) instanceof BinaryExpr addition)
          || addition.getOperator() != BinaryExpr.Operator.PLUS
          || !same(finish.getValue(), sum.getTarget())
          || !same(shift.getValue(), finish.getTarget())
          || same(shift.getTarget(), finish.getTarget())
          || same(sum.getTarget(), shift.getTarget())
          || same(sum.getTarget(), finish.getTarget())
          || !((same(addition.getLeft(), shift.getTarget())
                  && same(addition.getRight(), finish.getTarget()))
              || (same(addition.getRight(), shift.getTarget())
                  && same(addition.getLeft(), finish.getTarget())))) continue;
      if (!seed(method, shift, 0) || !seed(method, finish, 1) || !unitLoop(loop)) continue;
      return hint(
              loop,
              "fibonacci-dp",
              Map.of("previous", name(shift.getTarget()), "current", name(finish.getTarget())),
              "Zero/one seeds advance by summing and rotating the two preceding Fibonacci states")
          .map(List::of)
          .orElse(List.of());
    }
    return List.of();
  }

  private boolean seed(MethodDeclaration method, AssignExpr rotation, int expected) {
    var local = declaration(method, rotation.getTarget());
    return local.isPresent()
        && local.get().getInitializer().filter(e -> integer(e, expected)).isPresent()
        && owned(method, AssignExpr.class).stream()
            .noneMatch(w -> w != rotation && same(w.getTarget(), rotation.getTarget()))
        && owned(method, UnaryExpr.class).stream()
            .noneMatch(u -> same(u.getExpression(), rotation.getTarget()));
  }

  private boolean unitLoop(ForStmt loop) {
    if (loop.getInitialization().size() != 1
        || !loop.getInitialization().get(0).isVariableDeclarationExpr()
        || loop.getInitialization().get(0).asVariableDeclarationExpr().getVariables().size() != 1
        || loop.getUpdate().size() != 1
        || loop.getCompare().isEmpty()) return false;
    var index = loop.getInitialization().get(0).asVariableDeclarationExpr().getVariable(0);
    Expression update = unwrap(loop.getUpdate().get(0));
    if (index.getInitializer().filter(e -> integer(e, 2)).isEmpty()
        || owned(loop.getBody(), AssignExpr.class).stream()
            .anyMatch(a -> named(a.getTarget(), index.getNameAsString()))
        || !(update instanceof UnaryExpr increment)
        || !named(increment.getExpression(), index.getNameAsString())
        || (increment.getOperator() != UnaryExpr.Operator.POSTFIX_INCREMENT
            && increment.getOperator() != UnaryExpr.Operator.PREFIX_INCREMENT)) return false;
    return unwrap(loop.getCompare().get()) instanceof BinaryExpr bound
        && named(bound.getLeft(), index.getNameAsString())
        && (bound.getOperator() == BinaryExpr.Operator.LESS
            || bound.getOperator() == BinaryExpr.Operator.LESS_EQUALS);
  }
}
