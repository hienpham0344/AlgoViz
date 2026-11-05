package com.aisc.algoviz.ast.engine.algorithm.graph;

import static com.aisc.algoviz.ast.engine.algorithm.PatternSupport.*;

import com.aisc.algoviz.ast.engine.algorithm.MethodPatternDetector;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Traversal evidence connects the worklist, extracted vertex, adjacency and discovery guard. */
public final class GraphTraversalDetector implements MethodPatternDetector {
  @Override
  public List<AlgorithmHint> detect(MethodDeclaration method) {
    List<AlgorithmHint> result = new ArrayList<>();
    for (ForEachStmt neighbors : owned(method, ForEachStmt.class)) {
      String next = neighbors.getVariableDeclarator().getNameAsString();
      Expression vertex = adjacencyVertex(neighbors.getIterable());
      if (vertex == null || name(vertex) == null) continue;
      Optional<AlgorithmHint> match = dfs(method, neighbors, vertex, next);
      if (match.isEmpty()) match = worklist(method, neighbors, vertex, next);
      match.ifPresent(result::add);
    }
    return result;
  }

  private Optional<AlgorithmHint> dfs(
      MethodDeclaration method, ForEachStmt neighbors, Expression vertex, String next) {
    int parameter = -1;
    for (int i = 0; i < method.getParameters().size(); i++)
      if (method.getParameter(i).getNameAsString().equals(name(vertex))) parameter = i;
    if (parameter < 0) return Optional.empty();
    for (IfStmt branch : owned(neighbors, IfStmt.class)) {
      ArrayAccessExpr seen = unseen(branch.getCondition(), next);
      if (seen == null || name(seen.getName()) == null) continue;
      boolean marksCurrent =
          owned(method, AssignExpr.class).stream()
              .anyMatch(
                  a ->
                      a.getEnd().orElseThrow().isBefore(neighbors.getBegin().orElseThrow())
                          && a.getParentNode().flatMap(n -> n.getParentNode()).orElse(null)
                              == neighbors.getParentNode().orElse(null)
                          && a.getOperator() == AssignExpr.Operator.ASSIGN
                          && a.getValue().isBooleanLiteralExpr()
                          && a.getValue().asBooleanLiteralExpr().getValue()
                          && unwrap(a.getTarget()) instanceof ArrayAccessExpr access
                          && same(access.getName(), seen.getName())
                          && same(access.getIndex(), vertex));
      boolean clears =
          owned(method, AssignExpr.class).stream()
              .anyMatch(
                  a ->
                      a.getTarget() instanceof ArrayAccessExpr access
                          && same(access.getName(), seen.getName())
                          && a.getValue().isBooleanLiteralExpr()
                          && !a.getValue().asBooleanLiteralExpr().getValue());
      int argument = parameter;
      if (marksCurrent
          && !clears
          && owned(branch.getThenStmt(), MethodCallExpr.class).stream()
              .anyMatch(c -> recursive(c, method) && named(c.getArgument(argument), next)))
        return hint(
            method,
            "depth-first-search",
            Map.of("vertex", name(vertex), "visited", name(seen.getName())),
            "Current vertex is marked and unvisited adjacent vertices are explored recursively");
    }
    return Optional.empty();
  }

  private Optional<AlgorithmHint> worklist(
      MethodDeclaration method, ForEachStmt neighbors, Expression vertex, String next) {
    Optional<VariableDeclarator> current = declaration(method, vertex);
    if (current.isEmpty()
        || current.get().getInitializer().isEmpty()
        || !(unwrap(current.get().getInitializer().get()) instanceof MethodCallExpr take)
        || !List.of("poll", "remove", "removeFirst", "pollFirst").contains(take.getNameAsString())
        || take.getScope().isEmpty()
        || name(take.getScope().get()) == null) return Optional.empty();
    Expression queue = take.getScope().get();
    Node workLoop = loop(neighbors).orElse(null);
    if (!(workLoop instanceof WhileStmt repeat)
        || !contains(repeat, current.get())
        || !(unwrap(repeat.getCondition()) instanceof UnaryExpr notEmpty)
        || notEmpty.getOperator() != UnaryExpr.Operator.LOGICAL_COMPLEMENT
        || !(unwrap(notEmpty.getExpression()) instanceof MethodCallExpr empty)
        || !callOn(empty, queue, "isEmpty")
        || !empty.getArguments().isEmpty()) return Optional.empty();
    Optional<VariableDeclarator> worklist = declaration(method, queue);
    if (worklist.isEmpty()
        || worklist.get().getInitializer().isEmpty()
        || !(unwrap(worklist.get().getInitializer().get()) instanceof ObjectCreationExpr creation)
        || !List.of("ArrayDeque", "LinkedList").contains(creation.getType().getNameAsString())
        || !standardType(
            creation,
            creation.getType().getNameWithScope(),
            "java.util." + creation.getType().getNameAsString())) return Optional.empty();
    if (owned(method, MethodCallExpr.class).stream()
        .anyMatch(
            c ->
                callOn(
                    c, queue, "push", "pop", "addFirst", "offerFirst", "pollLast", "removeLast")))
      return Optional.empty();
    for (IfStmt branch : owned(neighbors, IfStmt.class)) {
      boolean enqueues =
          owned(branch.getThenStmt(), MethodCallExpr.class).stream()
              .anyMatch(
                  c ->
                      callOn(c, queue, "offer", "add", "addLast", "offerLast")
                          && c.getArguments().size() == 1
                          && named(c.getArgument(0), next));
      if (!enqueues) continue;
      ArrayAccessExpr visited = unseen(branch.getCondition(), next);
      if (visited != null
          && name(visited.getName()) != null
          && owned(branch.getThenStmt(), AssignExpr.class).stream()
              .anyMatch(
                  a ->
                      same(a.getTarget(), visited)
                          && a.getOperator() == AssignExpr.Operator.ASSIGN
                          && a.getValue().isBooleanLiteralExpr()
                          && a.getValue().asBooleanLiteralExpr().getValue()))
        return hint(
            method,
            "breadth-first-search",
            Map.of("queue", name(queue), "visited", name(visited.getName())),
            "FIFO worklist supplies the vertex whose neighbors are scanned",
            "Unvisited neighbors are marked before being enqueued");
      if (unwrap(branch.getCondition()) instanceof BinaryExpr zero
          && zero.getOperator() == BinaryExpr.Operator.EQUALS) {
        Expression test =
            integer(zero.getRight(), 0)
                ? zero.getLeft()
                : integer(zero.getLeft(), 0) ? zero.getRight() : null;
        if (unwrap(test) instanceof ArrayAccessExpr degree
            && named(degree.getIndex(), next)
            && name(degree.getName()) != null
            && owned(neighbors, UnaryExpr.class).stream()
                .anyMatch(
                    u ->
                        same(u.getExpression(), degree)
                            && (u.getOperator() == UnaryExpr.Operator.POSTFIX_DECREMENT
                                || u.getOperator() == UnaryExpr.Operator.PREFIX_DECREMENT)))
          return hint(
              method,
              "topological-sort",
              Map.of("queue", name(queue), "indegree", name(degree.getName())),
              "A FIFO worklist releases adjacent vertices when their decremented indegree reaches zero");
      }
    }
    return Optional.empty();
  }

  private Expression adjacencyVertex(Expression expression) {
    expression = unwrap(expression);
    if (expression instanceof ArrayAccessExpr access) return access.getIndex();
    if (expression instanceof MethodCallExpr call
        && call.getNameAsString().equals("get")
        && call.getArguments().size() == 1
        && call.getScope().isPresent()) return call.getArgument(0);
    return null;
  }

  private ArrayAccessExpr unseen(Expression expression, String next) {
    expression = unwrap(expression);
    if (expression instanceof UnaryExpr negation
        && negation.getOperator() == UnaryExpr.Operator.LOGICAL_COMPLEMENT
        && unwrap(negation.getExpression()) instanceof ArrayAccessExpr access
        && named(access.getIndex(), next)) return access;
    if (expression instanceof BinaryExpr equality
        && equality.getOperator() == BinaryExpr.Operator.EQUALS) {
      Expression other =
          equality.getRight().isBooleanLiteralExpr()
                  && !equality.getRight().asBooleanLiteralExpr().getValue()
              ? equality.getLeft()
              : null;
      if (unwrap(other) instanceof ArrayAccessExpr access && named(access.getIndex(), next))
        return access;
    }
    return null;
  }
}
