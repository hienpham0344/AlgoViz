package com.aisc.algoviz.ast.engine.algorithm.dp;

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
import com.github.javaparser.ast.stmt.IfStmt;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Recognizes one-dimensional DP through exact predecessor/target relationships. */
public final class LinearDynamicProgrammingDetector implements MethodPatternDetector {
  private record Choice(String operation, Expression candidate) {}

  @Override
  public List<AlgorithmHint> detect(MethodDeclaration method) {
    List<AlgorithmHint> result = new ArrayList<>();
    for (AssignExpr write : owned(method, AssignExpr.class)) {
      if (write.getOperator() != AssignExpr.Operator.ASSIGN
          || !(unwrap(write.getTarget()) instanceof ArrayAccessExpr target)
          || name(target.getName()) == null
          || name(target.getIndex()) == null
          || loop(write).isEmpty()) continue;
      Expression value = unwrap(write.getValue());
      Optional<AlgorithmHint> match = fibonacci(write, target, value);
      if (match.isEmpty()) match = optimize(write, target, value);
      match.ifPresent(result::add);
    }
    return result;
  }

  private Optional<AlgorithmHint> fibonacci(
      AssignExpr write, ArrayAccessExpr target, Expression value) {
    if (!(value instanceof BinaryExpr sum) || sum.getOperator() != BinaryExpr.Operator.PLUS)
      return Optional.empty();
    for (Expression term : List.of(sum.getLeft(), sum.getRight())) {
      Expression other = term == sum.getLeft() ? sum.getRight() : sum.getLeft();
      if (unwrap(term) instanceof ArrayAccessExpr first
          && unwrap(other) instanceof ArrayAccessExpr second
          && same(first.getName(), target.getName())
          && same(second.getName(), target.getName())
          && offset(first.getIndex(), target.getIndex(), -1)
          && offset(second.getIndex(), target.getIndex(), -2))
        return hint(
            loop(write).orElseThrow(),
            "fibonacci-dp",
            Map.of("table", name(target.getName())),
            "Each table entry sums the two immediately preceding entries");
    }
    return Optional.empty();
  }

  private Optional<AlgorithmHint> optimize(
      AssignExpr write, ArrayAccessExpr target, Expression value) {
    for (Choice choice : choices(write, target, value)) {
      if (!(unwrap(choice.candidate()) instanceof BinaryExpr sum)
          || sum.getOperator() != BinaryExpr.Operator.PLUS) continue;
      for (Expression part : List.of(sum.getLeft(), sum.getRight())) {
        Expression increment = part == sum.getLeft() ? sum.getRight() : sum.getLeft();
        if (!(LocalValueOrigins.initializer(part) instanceof ArrayAccessExpr previous)
            || !same(previous.getName(), target.getName())) continue;
        if (choice.operation().equals("min")
            && integer(increment, 1)
            && unwrap(previous.getIndex()) instanceof BinaryExpr difference
            && difference.getOperator() == BinaryExpr.Operator.MINUS
            && same(difference.getLeft(), target.getIndex())
            && name(difference.getRight()) != null)
          return hint(
              loop(write).orElseThrow(),
              "coin-change",
              Map.of("table", name(target.getName())),
              "Minimum coin count reuses the state at amount minus the current denomination and adds one");
        if (!choice.operation().equals("max")) continue;
        if (integer(increment, 1)
            && name(previous.getIndex()) != null
            && !same(previous.getIndex(), target.getIndex())
            && increasingGuard(write, target, previous))
          return hint(
              loop(write).orElseThrow(),
              "longest-increasing-subsequence",
              Map.of("table", name(target.getName())),
              "An increasing input pair extends the predecessor subsequence length by one");
        if (unwrap(previous.getIndex()) instanceof BinaryExpr difference
            && difference.getOperator() == BinaryExpr.Operator.MINUS
            && same(difference.getLeft(), target.getIndex())
            && unwrap(difference.getRight()) instanceof ArrayAccessExpr weight
            && unwrap(increment) instanceof ArrayAccessExpr profit
            && same(weight.getIndex(), profit.getIndex())
            && name(weight.getName()) != null
            && name(profit.getName()) != null
            && !same(weight.getName(), profit.getName()))
          return hint(
              loop(write).orElseThrow(),
              "knapsack",
              Map.of(
                  "table",
                  name(target.getName()),
                  "weights",
                  name(weight.getName()),
                  "values",
                  name(profit.getName())),
              "Capacity state chooses skipping an item or taking its value from capacity minus its weight");
      }
    }
    return Optional.empty();
  }

  private List<Choice> choices(AssignExpr write, ArrayAccessExpr target, Expression value) {
    if (math(value, "min") || math(value, "max")) {
      MethodCallExpr best = value.asMethodCallExpr();
      if (same(best.getArgument(0), target))
        return List.of(new Choice(best.getNameAsString(), best.getArgument(1)));
      if (same(best.getArgument(1), target))
        return List.of(new Choice(best.getNameAsString(), best.getArgument(0)));
    }
    var branch = write.findAncestor(IfStmt.class);
    if (branch.isPresent()
        && contains(branch.get().getThenStmt(), write)
        && lessGuard(branch.get().getCondition(), value, target))
      return List.of(new Choice("min", value));
    return List.of();
  }

  private boolean lessGuard(Expression condition, Expression candidate, Expression target) {
    if (!(unwrap(condition) instanceof BinaryExpr binary)) return false;
    if (binary.getOperator() == BinaryExpr.Operator.AND)
      return lessGuard(binary.getLeft(), candidate, target)
          || lessGuard(binary.getRight(), candidate, target);
    return binary.getOperator() == BinaryExpr.Operator.LESS
        && same(binary.getLeft(), candidate)
        && same(binary.getRight(), target);
  }

  private boolean increasingGuard(Node write, ArrayAccessExpr target, ArrayAccessExpr previous) {
    for (Node n = write;
        n != null && !(n instanceof MethodDeclaration);
        n = n.getParentNode().orElse(null)) {
      if (!(n instanceof IfStmt branch)
          || !contains(branch.getThenStmt(), write)
          || !(unwrap(branch.getCondition()) instanceof BinaryExpr comparison)) continue;
      if (!(unwrap(comparison.getLeft()) instanceof ArrayAccessExpr left)
          || !(unwrap(comparison.getRight()) instanceof ArrayAccessExpr right)
          || !same(left.getName(), right.getName())
          || same(left.getName(), target.getName())) continue;
      if (comparison.getOperator() == BinaryExpr.Operator.LESS
              && same(left.getIndex(), previous.getIndex())
              && same(right.getIndex(), target.getIndex())
          || comparison.getOperator() == BinaryExpr.Operator.GREATER
              && same(right.getIndex(), previous.getIndex())
              && same(left.getIndex(), target.getIndex())) return true;
    }
    return false;
  }
}
