package com.aisc.algoviz.ast.engine.algorithm.graph;

import static com.aisc.algoviz.ast.engine.algorithm.PatternSupport.*;

import com.aisc.algoviz.ast.engine.algorithm.MethodPatternDetector;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import java.util.List;
import java.util.Map;

/** Priority-queue shortest paths with connected extraction, adjacency and relaxation evidence. */
public final class DijkstraDetector implements MethodPatternDetector {
  @Override
  public List<AlgorithmHint> detect(MethodDeclaration method) {
    for (IfStmt branch : owned(method, IfStmt.class)) {
      if (!(unwrap(branch.getCondition()) instanceof BinaryExpr comparison)) continue;
      Expression old =
          comparison.getOperator() == BinaryExpr.Operator.GREATER
              ? comparison.getLeft()
              : comparison.getOperator() == BinaryExpr.Operator.LESS ? comparison.getRight() : null;
      Expression candidate =
          old == comparison.getLeft() ? comparison.getRight() : comparison.getLeft();
      if (!(unwrap(old) instanceof ArrayAccessExpr distance)
          || name(distance.getName()) == null
          || !(unwrap(candidate) instanceof BinaryExpr sum)
          || sum.getOperator() != BinaryExpr.Operator.PLUS
          || !(unwrap(sum.getLeft()) instanceof ArrayAccessExpr origin)
          || !same(distance.getName(), origin.getName())
          || same(distance.getIndex(), origin.getIndex())) continue;
      boolean writes =
          owned(branch.getThenStmt(), AssignExpr.class).stream()
              .anyMatch(
                  a ->
                      a.getOperator() == AssignExpr.Operator.ASSIGN
                          && same(a.getTarget(), distance)
                          && same(a.getValue(), candidate));
      if (!writes) continue;
      for (MethodCallExpr enqueue : owned(branch.getThenStmt(), MethodCallExpr.class)) {
        if (!List.of("offer", "add").contains(enqueue.getNameAsString())
            || enqueue.getScope().isEmpty()
            || name(enqueue.getScope().get()) == null
            || enqueue.getArguments().size() != 1
            || !(unwrap(enqueue.getArgument(0)) instanceof ArrayCreationExpr payload)
            || payload.getInitializer().isEmpty()
            || payload.getInitializer().get().getValues().size() != 2) continue;
        var values = payload.getInitializer().get().getValues();
        if (!same(values.get(0), distance.getIndex()) || !same(values.get(1), distance)) continue;
        Expression queue = enqueue.getScope().get();
        var declaration = declaration(method, queue);
        if (declaration.isEmpty()
            || declaration.get().getInitializer().isEmpty()
            || !(unwrap(declaration.get().getInitializer().get())
                instanceof ObjectCreationExpr creation)
            || !standardType(
                creation, creation.getType().getNameWithScope(), "java.util.PriorityQueue")
            || !distancePriority(creation)) continue;
        if (!extractedVertex(method, branch, origin.getIndex(), queue)) continue;
        return hint(
                method,
                "dijkstra",
                Map.of("distance", name(distance.getName()), "queue", name(queue)),
                "Minimum-distance priority queue supplies the vertex whose adjacency is scanned",
                "A successful edge relaxation updates the same distance and enqueues that vertex with its new priority")
            .map(List::of)
            .orElse(List.of());
      }
    }
    return List.of();
  }

  private boolean distancePriority(ObjectCreationExpr creation) {
    return creation.getArguments().stream()
        .anyMatch(
            argument -> {
              if (!(unwrap(argument) instanceof MethodCallExpr comparator)
                  || !List.of("comparingInt", "comparingLong")
                      .contains(comparator.getNameAsString())
                  || comparator
                      .getScope()
                      .filter(
                          s ->
                              s.toString().equals("Comparator")
                                  || s.toString().equals("java.util.Comparator"))
                      .isEmpty()
                  || comparator.getArguments().size() != 1
                  || !comparator.getArgument(0).isLambdaExpr()) return false;
              LambdaExpr lambda = comparator.getArgument(0).asLambdaExpr();
              if (lambda.getParameters().size() != 1) return false;
              return lambda
                  .getExpressionBody()
                  .filter(
                      e ->
                          unwrap(e) instanceof ArrayAccessExpr access
                              && named(access.getName(), lambda.getParameter(0).getNameAsString())
                              && integer(access.getIndex(), 1))
                  .isPresent();
            });
  }

  private boolean extractedVertex(
      MethodDeclaration method, IfStmt branch, Expression vertex, Expression queue) {
    var current = declaration(method, vertex);
    if (current.isEmpty()
        || current.get().getInitializer().isEmpty()
        || !(unwrap(current.get().getInitializer().get()) instanceof ArrayAccessExpr state)
        || !integer(state.getIndex(), 0)) return false;
    var entry = declaration(method, state.getName());
    if (entry.isEmpty()
        || entry.get().getInitializer().isEmpty()
        || !(unwrap(entry.get().getInitializer().get()) instanceof MethodCallExpr take)
        || !callOn(take, queue, "poll", "remove")) return false;
    return branch
        .findAncestor(ForEachStmt.class)
        .filter(
            neighbors -> {
              Expression iterable = unwrap(neighbors.getIterable());
              return iterable instanceof MethodCallExpr call
                      && call.getNameAsString().equals("get")
                      && call.getArguments().size() == 1
                      && same(call.getArgument(0), vertex)
                  || iterable instanceof ArrayAccessExpr array && same(array.getIndex(), vertex);
            })
        .isPresent();
  }
}
