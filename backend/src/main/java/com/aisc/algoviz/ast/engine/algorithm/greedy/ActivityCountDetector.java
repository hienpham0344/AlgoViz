package com.aisc.algoviz.ast.engine.algorithm.greedy;

import static com.aisc.algoviz.ast.engine.algorithm.PatternSupport.*;

import com.aisc.algoviz.ast.engine.algorithm.MethodPatternDetector;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import java.util.List;
import java.util.Map;

/** Count-only interval scheduling with a directly indexed int[][] and proven end ordering. */
public final class ActivityCountDetector implements MethodPatternDetector {
  @Override
  public List<AlgorithmHint> detect(MethodDeclaration method) {
    for (ForStmt scan : owned(method, ForStmt.class))
      for (IfStmt branch : owned(scan, IfStmt.class)) {
        if (branch.getElseStmt().isPresent()
            || loop(branch).orElse(null) != scan
            || !(unwrap(branch.getCondition()) instanceof BinaryExpr guard)
            || guard.getOperator() != BinaryExpr.Operator.GREATER_EQUALS
            || name(guard.getRight()) == null
            || !(unwrap(guard.getLeft()) instanceof ArrayAccessExpr start)
            || !integer(start.getIndex(), 0)
            || !(unwrap(start.getName()) instanceof ArrayAccessExpr row)
            || name(row.getName()) == null
            || name(row.getIndex()) == null) continue;
        String boundary = name(guard.getRight());
        var boundaryDeclaration = declaration(method, guard.getRight());
        if (boundaryDeclaration.isEmpty()
            || boundaryDeclaration
                .get()
                .getInitializer()
                .filter(e -> e.toString().equals("Integer.MIN_VALUE"))
                .isEmpty()) continue;
        boolean fullScan =
            scan.getInitialization().size() == 1
                && scan.getInitialization().get(0).isVariableDeclarationExpr()
                && scan.getInitialization().get(0).asVariableDeclarationExpr().getVariables().size()
                    == 1
                && scan.getInitialization()
                    .get(0)
                    .asVariableDeclarationExpr()
                    .getVariable(0)
                    .getNameAsString()
                    .equals(name(row.getIndex()))
                && scan.getInitialization()
                    .get(0)
                    .asVariableDeclarationExpr()
                    .getVariable(0)
                    .getInitializer()
                    .filter(e -> integer(e, 0))
                    .isPresent()
                && scan.getCompare()
                    .filter(
                        e ->
                            e instanceof BinaryExpr b
                                && b.getOperator() == BinaryExpr.Operator.LESS
                                && same(b.getLeft(), row.getIndex())
                                && b.getRight() instanceof FieldAccessExpr length
                                && length.getNameAsString().equals("length")
                                && same(length.getScope(), row.getName()))
                    .isPresent()
                && scan.getUpdate().size() == 1
                && increments(scan.getUpdate().get(0), row.getIndex());
        if (!fullScan || !sortedByEnd(method, scan, row.getName())) continue;
        var writes = owned(scan, AssignExpr.class);
        if (writes.size() != 1 || !contains(branch.getThenStmt(), writes.get(0))) continue;
        AssignExpr update = writes.get(0);
        if (update.getOperator() != AssignExpr.Operator.ASSIGN
            || !named(update.getTarget(), boundary)
            || !(unwrap(update.getValue()) instanceof ArrayAccessExpr end)
            || !same(end.getName(), row)
            || !integer(end.getIndex(), 1)) continue;
        var effects = owned(branch.getThenStmt(), UnaryExpr.class);
        if (effects.size() != 1
            || !increments(effects.get(0), effects.get(0).getExpression())
            || name(effects.get(0).getExpression()) == null) continue;
        Expression count = effects.get(0).getExpression();
        if (declaration(method, count)
                .flatMap(d -> d.getInitializer())
                .filter(e -> integer(e, 0))
                .isEmpty()
            || owned(method, ReturnStmt.class).stream()
                .noneMatch(r -> r.getExpression().filter(e -> same(e, count)).isPresent()))
          continue;
        return hint(
                scan,
                "activity-selection",
                Map.of("input", name(row.getName()), "lastEnd", boundary, "count", name(count)),
                "Intervals are sorted by increasing end time before a complete scan",
                "Every compatible interval increments the result and replaces the last selected end")
            .map(List::of)
            .orElse(List.of());
      }
    return List.of();
  }

  private boolean increments(Expression expression, Expression target) {
    return unwrap(expression) instanceof UnaryExpr unary
        && same(unary.getExpression(), target)
        && (unary.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
            || unary.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT);
  }

  private boolean sortedByEnd(MethodDeclaration method, ForStmt scan, Expression input) {
    return owned(method, MethodCallExpr.class).stream()
        .anyMatch(
            call -> {
              if (!call.getNameAsString().equals("sort")
                  || call.getScope().filter(s -> standardUtility(s, "java.util.Arrays")).isEmpty()
                  || call.getArguments().size() != 2
                  || !same(call.getArgument(0), input)
                  || call.getParentNode().flatMap(n -> n.getParentNode()).orElse(null)
                      != scan.getParentNode().orElse(null)
                  || !call.getEnd().orElseThrow().isBefore(scan.getBegin().orElseThrow())
                  || !(unwrap(call.getArgument(1)) instanceof MethodCallExpr comparator)
                  || !comparator.getNameAsString().equals("comparingInt")
                  || comparator
                      .getScope()
                      .filter(s -> standardUtility(s, "java.util.Comparator"))
                      .isEmpty()
                  || comparator.getArguments().size() != 1
                  || !comparator.getArgument(0).isLambdaExpr()) return false;
              LambdaExpr key = comparator.getArgument(0).asLambdaExpr();
              return key.getParameters().size() == 1
                  && key.getExpressionBody()
                      .filter(
                          e ->
                              e instanceof ArrayAccessExpr field
                                  && integer(field.getIndex(), 1)
                                  && named(field.getName(), key.getParameter(0).getNameAsString()))
                      .isPresent();
            });
  }
}
