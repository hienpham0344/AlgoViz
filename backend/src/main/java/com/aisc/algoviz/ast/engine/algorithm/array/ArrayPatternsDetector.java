package com.aisc.algoviz.ast.engine.algorithm.array;

import static com.aisc.algoviz.ast.engine.algorithm.PatternSupport.*;

import com.aisc.algoviz.ast.engine.algorithm.LocalValueOrigins;
import com.aisc.algoviz.ast.engine.algorithm.MethodPatternDetector;
import com.aisc.algoviz.ast.engine.algorithm.PatternSupport;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Array recurrences and direct sequential search, without identifier-name heuristics. */
public final class ArrayPatternsDetector implements MethodPatternDetector {
  @Override
  public List<AlgorithmHint> detect(MethodDeclaration method) {
    List<AlgorithmHint> result = new ArrayList<>();
    for (Node loop : loops(method)) {
      Optional<AlgorithmHint> match = prefix(loop);
      if (match.isEmpty()) match = kadane(loop);
      if (match.isEmpty()) match = resettingKadane(loop);
      if (match.isEmpty()) match = linear(loop);
      match.ifPresent(result::add);
    }
    return result;
  }

  private Optional<AlgorithmHint> prefix(Node loop) {
    for (AssignExpr write : owned(loop, AssignExpr.class)) {
      if (loop(write).orElse(null) != loop
          || write.getOperator() != AssignExpr.Operator.ASSIGN
          || !(unwrap(write.getTarget()) instanceof ArrayAccessExpr target)
          || name(target.getName()) == null
          || !(unwrap(write.getValue()) instanceof BinaryExpr sum)
          || sum.getOperator() != BinaryExpr.Operator.PLUS) continue;
      for (Expression term : List.of(sum.getLeft(), sum.getRight())) {
        Expression other = term == sum.getLeft() ? sum.getRight() : sum.getLeft();
        if (!(unwrap(term) instanceof ArrayAccessExpr previous)
            || !(unwrap(other) instanceof ArrayAccessExpr input)
            || name(input.getName()) == null
            || !same(previous.getName(), target.getName())) continue;
        boolean scan = ForwardArrayScan.matches(loop, input.getName(), input.getIndex(), -1);
        if (!scan) continue;
        if ((offset(target.getIndex(), previous.getIndex(), 1)
                && same(input.getIndex(), previous.getIndex()))
            || (offset(previous.getIndex(), target.getIndex(), -1)
                && same(input.getIndex(), target.getIndex())))
          return hint(
              loop,
              "prefix-sum",
              Map.of("input", name(input.getName()), "prefix", name(target.getName())),
              "Each prefix cell adds one input element to its preceding prefix cell");
      }
    }
    return Optional.empty();
  }

  private Optional<AlgorithmHint> kadane(Node loop) {
    List<AssignExpr> writes =
        owned(loop, AssignExpr.class).stream()
            .filter(write -> loop(write).orElse(null) == loop)
            .toList();
    for (AssignExpr write : writes) {
      String running = name(write.getTarget());
      if (running == null
          || write.getOperator() != AssignExpr.Operator.ASSIGN
          || !math(write.getValue(), "max")) continue;
      var maximum = unwrap(write.getValue()).asMethodCallExpr();
      for (Expression element : maximum.getArguments()) {
        Expression other = maximum.getArgument(element == maximum.getArgument(0) ? 1 : 0);
        if (!(unwrap(element) instanceof ArrayAccessExpr access)
            || name(access.getName()) == null
            || !(unwrap(other) instanceof BinaryExpr sum)
            || sum.getOperator() != BinaryExpr.Operator.PLUS
            || !((named(sum.getLeft(), running) && same(sum.getRight(), element))
                || (named(sum.getRight(), running) && same(sum.getLeft(), element)))) continue;
        if (!ForwardArrayScan.matches(loop, access.getName(), access.getIndex(), 1)
            && !ForwardArrayScan.matches(loop, access.getName(), access.getIndex(), 0)) continue;
        for (AssignExpr best : writes) {
          String global = name(best.getTarget());
          if (global == null || global.equals(running) || !math(best.getValue(), "max")) continue;
          var args = unwrap(best.getValue()).asMethodCallExpr().getArguments();
          if (args.stream().anyMatch(e -> named(e, global))
              && args.stream().anyMatch(e -> named(e, running)))
            return hint(
                loop,
                "kadane",
                Map.of("array", name(access.getName()), "running", running, "best", global),
                "Running maximum chooses restarting at this element or extending the previous subarray",
                "A separate maximum retains the best subarray seen");
        }
      }
    }
    return Optional.empty();
  }

  private Optional<AlgorithmHint> linear(Node loop) {
    if (owned(loop, Node.class).stream().filter(PatternSupport::isLoop).count() != 1)
      return Optional.empty();
    for (IfStmt branch : owned(loop, IfStmt.class)) {
      for (BinaryExpr comparison : equalities(branch.getCondition())) {
        for (Expression term : List.of(comparison.getLeft(), comparison.getRight())) {
          Expression origin = LocalValueOrigins.initializer(term);
          if (origin instanceof MethodCallExpr call
              && call.getNameAsString().equals("compareTo")
              && call.getArguments().size() == 1
              && call.getScope().isPresent()) {
            Expression other =
                term == comparison.getLeft() ? comparison.getRight() : comparison.getLeft();
            if (!integer(other, 0)) continue;
            origin = LocalValueOrigins.initializer(call.getScope().get());
          }
          if (!(origin instanceof ArrayAccessExpr access)
              || name(access.getName()) == null
              || name(access.getIndex()) == null) continue;
          String index = name(access.getIndex());
          boolean advancing =
              ForwardArrayScan.matches(loop, access.getName(), access.getIndex(), 0);
          if (advancing
              && owned(branch.getThenStmt(), ReturnStmt.class).stream()
                  .anyMatch(
                      r ->
                          r.getExpression()
                              .filter(e -> named(e, index) || same(e, access))
                              .isPresent()))
            return hint(
                loop,
                "linear-search",
                Map.of("array", name(access.getName()), "index", index),
                "Sequential index scan returns the matching index or element after an equality test");
        }
      }
    }
    return Optional.empty();
  }

  private List<BinaryExpr> equalities(Expression condition) {
    if (!(unwrap(condition) instanceof BinaryExpr binary)) return List.of();
    if (binary.getOperator() == BinaryExpr.Operator.EQUALS) return List.of(binary);
    if (binary.getOperator() != BinaryExpr.Operator.AND) return List.of();
    List<BinaryExpr> result = new ArrayList<>(equalities(binary.getLeft()));
    result.addAll(equalities(binary.getRight()));
    return result;
  }

  private Optional<AlgorithmHint> resettingKadane(Node loop) {
    if (!(loop instanceof ForEachStmt scan)
        || name(scan.getIterable()) == null
        || !(scan.getBody() instanceof BlockStmt body)
        || body.getStatements().size() != 3
        || !(body.getStatement(0) instanceof ExpressionStmt first)
        || !(first.getExpression() instanceof AssignExpr add)
        || add.getOperator() != AssignExpr.Operator.PLUS
        || name(add.getTarget()) == null
        || !named(add.getValue(), scan.getVariable().getVariable(0).getNameAsString())
        || !(body.getStatement(1) instanceof ExpressionStmt second)
        || !(second.getExpression() instanceof AssignExpr best)
        || best.getOperator() != AssignExpr.Operator.ASSIGN
        || name(best.getTarget()) == null
        || same(best.getTarget(), add.getTarget())
        || !math(best.getValue(), "max")
        || !(body.getStatement(2) instanceof IfStmt reset)
        || reset.getElseStmt().isPresent()
        || !(unwrap(reset.getCondition()) instanceof BinaryExpr negative)
        || negative.getOperator() != BinaryExpr.Operator.LESS
        || !same(negative.getLeft(), add.getTarget())
        || !integer(negative.getRight(), 0)) return Optional.empty();
    var arguments = unwrap(best.getValue()).asMethodCallExpr().getArguments();
    if (arguments.stream().noneMatch(e -> same(e, best.getTarget()))
        || arguments.stream().noneMatch(e -> same(e, add.getTarget()))) return Optional.empty();
    var resetBody = reset.getThenStmt();
    if (resetBody instanceof BlockStmt block && block.getStatements().size() == 1)
      resetBody = block.getStatement(0);
    if (!(resetBody instanceof ExpressionStmt statement)
        || !(statement.getExpression() instanceof AssignExpr zero)
        || zero.getOperator() != AssignExpr.Operator.ASSIGN
        || !same(zero.getTarget(), add.getTarget())
        || !integer(zero.getValue(), 0)) return Optional.empty();
    return hint(
        loop,
        "kadane",
        Map.of(
            "array",
            name(scan.getIterable()),
            "running",
            name(add.getTarget()),
            "best",
            name(best.getTarget())),
        "A contiguous running sum records its maximum before discarding a negative prefix");
  }
}
