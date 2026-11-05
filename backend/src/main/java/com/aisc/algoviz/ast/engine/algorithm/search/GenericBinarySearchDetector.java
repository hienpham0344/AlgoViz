package com.aisc.algoviz.ast.engine.algorithm.search;

import static com.aisc.algoviz.ast.engine.algorithm.PatternSupport.*;

import com.aisc.algoviz.ast.engine.algorithm.LocalValueOrigins;
import com.aisc.algoviz.ast.engine.algorithm.MethodPatternDetector;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;
import java.util.List;
import java.util.Map;

/** Comparable search requires midpoint equality and both correctly directed interval reductions. */
public final class GenericBinarySearchDetector implements MethodPatternDetector {
  @Override
  public List<AlgorithmHint> detect(MethodDeclaration method) {
    for (IfStmt equality : owned(method, IfStmt.class)) {
      Expression tested = comparison(equality.getCondition(), BinaryExpr.Operator.EQUALS);
      if (tested == null
          || !(LocalValueOrigins.initializer(tested) instanceof MethodCallExpr compare)
          || !compare.getNameAsString().equals("compareTo")
          || compare.getScope().isEmpty()
          || compare.getArguments().size() != 1
          || equality.getElseStmt().isEmpty()
          || !(single(equality.getElseStmt().get()) instanceof IfStmt direction)
          || direction.getElseStmt().isEmpty()) continue;
      Expression signed = comparison(direction.getCondition(), BinaryExpr.Operator.LESS);
      if (signed == null || !same(LocalValueOrigins.initializer(signed), compare)) continue;
      boolean arrayFirst = unwrap(compare.getScope().get()) instanceof ArrayAccessExpr;
      Expression element = arrayFirst ? compare.getScope().get() : compare.getArgument(0);
      if (!(unwrap(element) instanceof ArrayAccessExpr access)
          || name(access.getName()) == null
          || name(access.getIndex()) == null
          || !(single(equality.getThenStmt()) instanceof ReturnStmt found)
          || found.getExpression().filter(e -> same(e, access.getIndex())).isEmpty()) continue;
      var midpoint = declaration(method, access.getIndex());
      if (midpoint.isEmpty() || midpoint.get().getInitializer().isEmpty()) continue;
      Expression value = unwrap(midpoint.get().getInitializer().get());
      if (!(value instanceof BinaryExpr average)
          || !(average.getOperator() == BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT
                  && integer(average.getRight(), 1)
              || average.getOperator() == BinaryExpr.Operator.DIVIDE
                  && integer(average.getRight(), 2))
          || !(unwrap(average.getLeft()) instanceof BinaryExpr sum)
          || sum.getOperator() != BinaryExpr.Operator.PLUS
          || name(sum.getLeft()) == null
          || name(sum.getRight()) == null
          || same(sum.getLeft(), sum.getRight())) continue;
      if (owned(method, AssignExpr.class).stream()
              .anyMatch(a -> same(a.getTarget(), access.getIndex()))
          || owned(method, UnaryExpr.class).stream()
              .anyMatch(u -> same(u.getExpression(), access.getIndex()))) continue;
      for (Expression low : List.of(sum.getLeft(), sum.getRight())) {
        Expression high = low == sum.getLeft() ? sum.getRight() : sum.getLeft();
        Statement lower = arrayFirst ? direction.getThenStmt() : direction.getElseStmt().get();
        Statement upper = arrayFirst ? direction.getElseStmt().get() : direction.getThenStmt();
        Node scope = loop(equality).orElse(null);
        boolean iterative =
            scope instanceof WhileStmt scan
                && loop(midpoint.get()).orElse(null) == scan
                && loop(compare).orElse(null) == scan
                && bound(scan.getCondition(), low, high, BinaryExpr.Operator.LESS_EQUALS)
                && update(lower, low, access.getIndex(), 1)
                && update(upper, high, access.getIndex(), -1)
                && owned(scan, AssignExpr.class).stream()
                        .filter(a -> same(a.getTarget(), low) || same(a.getTarget(), high))
                        .count()
                    == 2
                && owned(scan, UnaryExpr.class).stream()
                    .noneMatch(u -> same(u.getExpression(), low) || same(u.getExpression(), high));
        boolean recursive =
            scope == null
                && baseCase(method, equality, low, high)
                && recurse(method, lower, low, high, access.getIndex(), true)
                && recurse(method, upper, low, high, access.getIndex(), false);
        if (!iterative && !recursive) continue;
        return hint(
                iterative ? scope : method,
                "binary-search",
                Map.of(
                    "array",
                    name(access.getName()),
                    "left",
                    name(low),
                    "right",
                    name(high),
                    "mid",
                    name(access.getIndex())),
                "A midpoint Comparable comparison returns on equality and reduces the correct half on both remaining branches")
            .map(List::of)
            .orElse(List.of());
      }
    }
    return List.of();
  }

  private Expression comparison(Expression expression, BinaryExpr.Operator operator) {
    return unwrap(expression) instanceof BinaryExpr binary
            && binary.getOperator() == operator
            && integer(binary.getRight(), 0)
        ? binary.getLeft()
        : null;
  }

  private boolean bound(
      Expression condition, Expression left, Expression right, BinaryExpr.Operator operator) {
    return unwrap(condition) instanceof BinaryExpr binary
        && binary.getOperator() == operator
        && same(binary.getLeft(), left)
        && same(binary.getRight(), right);
  }

  private boolean update(Statement statement, Expression target, Expression mid, int step) {
    return single(statement) instanceof ExpressionStmt expression
        && expression.getExpression() instanceof AssignExpr write
        && write.getOperator() == AssignExpr.Operator.ASSIGN
        && same(write.getTarget(), target)
        && offset(write.getValue(), mid, step);
  }

  private boolean baseCase(
      MethodDeclaration method, IfStmt equality, Expression low, Expression high) {
    return owned(method, IfStmt.class).stream()
        .anyMatch(
            branch ->
                branch.getBegin().orElseThrow().isBefore(equality.getBegin().orElseThrow())
                    && bound(branch.getCondition(), high, low, BinaryExpr.Operator.LESS)
                    && single(branch.getThenStmt()) instanceof ReturnStmt);
  }

  private boolean recurse(
      MethodDeclaration method,
      Statement statement,
      Expression low,
      Expression high,
      Expression mid,
      boolean advanceLow) {
    if (!(single(statement) instanceof ReturnStmt returned)
        || returned.getExpression().isEmpty()
        || !(unwrap(returned.getExpression().get()) instanceof MethodCallExpr call)
        || !recursive(call, method)) return false;
    boolean hasLow = false, hasHigh = false;
    for (int i = 0; i < method.getParameters().size(); i++) {
      String parameter = method.getParameter(i).getNameAsString();
      Expression expected = new NameExpr(parameter), actual = call.getArgument(i);
      if (named(low, parameter)) {
        hasLow = true;
        if (advanceLow ? !offset(actual, mid, 1) : !same(actual, expected)) return false;
      } else if (named(high, parameter)) {
        hasHigh = true;
        if (!advanceLow ? !offset(actual, mid, -1) : !same(actual, expected)) return false;
      } else if (!same(actual, expected)) return false;
    }
    return hasLow && hasHigh;
  }

  private Statement single(Statement statement) {
    return statement instanceof BlockStmt block && block.getStatements().size() == 1
        ? block.getStatement(0)
        : statement;
  }
}
