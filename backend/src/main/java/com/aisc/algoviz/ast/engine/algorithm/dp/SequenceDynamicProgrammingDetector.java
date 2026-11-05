package com.aisc.algoviz.ast.engine.algorithm.dp;

import static com.aisc.algoviz.ast.engine.algorithm.PatternSupport.*;

import com.aisc.algoviz.ast.engine.algorithm.LocalValueOrigins;
import com.aisc.algoviz.ast.engine.algorithm.MethodPatternDetector;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Two-sequence DP requires both matching-character and mismatch recurrences. */
public final class SequenceDynamicProgrammingDetector implements MethodPatternDetector {
  private record Cell(Expression table, Expression row, Expression column) {}

  private record Element(Expression sequence, Expression index) {}

  private record Cost(Expression state, int increment) {}

  @Override
  public List<AlgorithmHint> detect(MethodDeclaration method) {
    List<AlgorithmHint> result = new ArrayList<>();
    for (IfStmt branch : owned(method, IfStmt.class)) {
      if (branch.getElseStmt().isEmpty() || loop(branch).isEmpty()) continue;
      List<Expression> equality = equalityOperands(branch.getCondition());
      if (equality.size() != 2) continue;
      List<AssignExpr> matches = owned(branch.getThenStmt(), AssignExpr.class);
      List<AssignExpr> mismatches =
          owned(branch.getElseStmt().get(), AssignExpr.class).stream()
              .filter(a -> unwrap(a.getTarget()) instanceof ArrayAccessExpr)
              .toList();
      if (matches.size() != 1 || mismatches.size() != 1) continue;
      AssignExpr match = matches.get(0), mismatch = mismatches.get(0);
      Cell target = cell(match.getTarget());
      if (target == null
          || name(target.table()) == null
          || !same(match.getTarget(), mismatch.getTarget())
          || match.getOperator() != AssignExpr.Operator.ASSIGN
          || mismatch.getOperator() != AssignExpr.Operator.ASSIGN
          || !characters(equality.get(0), equality.get(1), target)) continue;
      Expression equal = unwrap(match.getValue()), different = unwrap(mismatch.getValue());
      if (equal instanceof BinaryExpr sum
          && sum.getOperator() == BinaryExpr.Operator.PLUS
          && ((integer(sum.getRight(), 1) && predecessor(sum.getLeft(), target, -1, -1))
              || (integer(sum.getLeft(), 1) && predecessor(sum.getRight(), target, -1, -1)))
          && math(different, "max")) {
        var arguments = different.asMethodCallExpr().getArguments();
        if (arguments.stream().anyMatch(e -> predecessor(e, target, -1, 0))
            && arguments.stream().anyMatch(e -> predecessor(e, target, 0, -1)))
          hint(
                  branch,
                  "longest-common-subsequence",
                  Map.of("table", name(target.table())),
                  "Matching characters extend the diagonal LCS; mismatches take the maximum of the adjacent states")
              .ifPresent(result::add);
      } else if (predecessor(equal, target, -1, -1)) {
        List<Cost> states = minimumCosts(branch.getElseStmt().orElseThrow(), mismatch);
        if (states.size() == 3
            && states.stream().allMatch(c -> c.increment() == 1)
            && states.stream().anyMatch(c -> predecessor(c.state(), target, -1, -1))
            && states.stream().anyMatch(c -> predecessor(c.state(), target, -1, 0))
            && states.stream().anyMatch(c -> predecessor(c.state(), target, 0, -1)))
          hint(
                  branch,
                  "edit-distance",
                  Map.of("table", name(target.table())),
                  "Matching characters retain diagonal cost; mismatches add one to minimum insert, delete and substitute costs")
              .ifPresent(result::add);
      }
    }
    return result;
  }

  private Cell cell(Expression expression) {
    if (unwrap(expression) instanceof ArrayAccessExpr column
        && unwrap(column.getName()) instanceof ArrayAccessExpr row)
      return new Cell(row.getName(), row.getIndex(), column.getIndex());
    return null;
  }

  private boolean predecessor(Expression expression, Cell target, int row, int column) {
    Cell previous = cell(expression);
    return previous != null
        && same(previous.table(), target.table())
        && (row == 0
            ? same(previous.row(), target.row())
            : previousIndex(previous.row(), target.row()))
        && (column == 0
            ? same(previous.column(), target.column())
            : previousIndex(previous.column(), target.column()));
  }

  private boolean characters(Expression first, Expression second, Cell target) {
    Element a = element(first), b = element(second);
    if (a == null || b == null || same(a.sequence(), b.sequence())) return false;
    return previousIndex(a.index(), target.row()) && previousIndex(b.index(), target.column())
        || previousIndex(b.index(), target.row()) && previousIndex(a.index(), target.column());
  }

  private boolean previousIndex(Expression previous, Expression current) {
    return offset(previous, current, -1) || offset(current, previous, 1);
  }

  private Element element(Expression expression) {
    Expression value = LocalValueOrigins.initializer(expression);
    if (value instanceof ArrayAccessExpr array)
      return new Element(array.getName(), array.getIndex());
    if (value instanceof MethodCallExpr call
        && call.getNameAsString().equals("charAt")
        && call.getArguments().size() == 1
        && call.getScope().isPresent())
      return new Element(call.getScope().get(), call.getArgument(0));
    return null;
  }

  private List<Expression> equalityOperands(Expression condition) {
    if (unwrap(condition) instanceof BinaryExpr binary
        && binary.getOperator() == BinaryExpr.Operator.EQUALS)
      return List.of(binary.getLeft(), binary.getRight());
    if (unwrap(condition) instanceof MethodCallExpr call
        && call.getNameAsString().equals("equals")
        && call.getArguments().size() == 1
        && call.getScope().isPresent()) return List.of(call.getScope().get(), call.getArgument(0));
    return List.of();
  }

  private List<Cost> minimumCosts(Statement branch, AssignExpr result) {
    List<Statement> statements =
        branch instanceof BlockStmt block ? block.getStatements() : List.of(branch);
    Map<String, List<Cost>> values = new HashMap<>();
    for (int index = 0; index < statements.size(); index++) {
      if (!(statements.get(index) instanceof ExpressionStmt statement)) return List.of();
      Expression expression = statement.getExpression();
      if (expression.isVariableDeclarationExpr()) {
        for (var variable : expression.asVariableDeclarationExpr().getVariables()) {
          if (variable.getInitializer().isEmpty() || values.containsKey(variable.getNameAsString()))
            return List.of();
          values.put(variable.getNameAsString(), costs(variable.getInitializer().get(), values, 0));
        }
      } else if (expression instanceof AssignExpr write
          && write.getOperator() == AssignExpr.Operator.ASSIGN) {
        if (write == result)
          return index == statements.size() - 1 ? costs(write.getValue(), values, 0) : List.of();
        String local = name(write.getTarget());
        if (local == null || !values.containsKey(local)) return List.of();
        values.put(local, costs(write.getValue(), values, 0));
      } else return List.of();
    }
    return List.of();
  }

  private List<Cost> costs(Expression expression, Map<String, List<Cost>> values, int depth) {
    if (depth > 16) return List.of();
    if (name(expression) != null && values.containsKey(name(expression)))
      return values.get(name(expression));
    Expression value = LocalValueOrigins.initializer(expression);
    if (math(value, "min")) {
      var call = value.asMethodCallExpr();
      List<Cost> left = costs(call.getArgument(0), values, depth + 1);
      List<Cost> right = costs(call.getArgument(1), values, depth + 1);
      if (left.isEmpty() || right.isEmpty() || left.size() + right.size() > 3) return List.of();
      List<Cost> result = new ArrayList<>(left);
      result.addAll(right);
      return result;
    }
    if (value instanceof BinaryExpr sum && sum.getOperator() == BinaryExpr.Operator.PLUS) {
      Expression other =
          integer(sum.getLeft(), 1)
              ? sum.getRight()
              : integer(sum.getRight(), 1) ? sum.getLeft() : null;
      if (other != null)
        return costs(other, values, depth + 1).stream()
            .map(c -> new Cost(c.state(), c.increment() + 1))
            .toList();
    }
    return List.of(new Cost(value, 0));
  }
}
