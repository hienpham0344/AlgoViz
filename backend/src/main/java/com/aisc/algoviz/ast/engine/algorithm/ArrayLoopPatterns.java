package com.aisc.algoviz.ast.engine.algorithm;

import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Existing loop-level array rules, isolated from orchestration. */
final class ArrayLoopPatterns {
  Optional<AlgorithmHint> detect(Node loop) {
    Optional<AlgorithmHint> hint = binarySearch(loop);
    if (hint.isEmpty()) hint = slidingWindow(loop);
    return hint.flatMap(candidate -> bind(loop, candidate));
  }

  private Optional<AlgorithmHint> binarySearch(Node loop) {
    Optional<BinaryExpr> bounds = orderedBounds(loop);
    if (bounds.isEmpty()) return Optional.empty();
    String left = name(bounds.get().getLeft());
    String right = name(bounds.get().getRight());
    for (VariableDeclarator declaration : owned(loop, VariableDeclarator.class)) {
      String mid = declaration.getNameAsString();
      if (mid.equals(left)
          || mid.equals(right)
          || declaration.getInitializer().filter(e -> midpoint(e, left, right)).isEmpty()) continue;
      for (ArrayAccessExpr access : owned(loop, ArrayAccessExpr.class)) {
        String array = name(access.getName());
        if (array == null || !isName(access.getIndex(), mid)) continue;
        boolean compared =
            owned(loop, IfStmt.class).stream()
                .anyMatch(
                    branch ->
                        branch.getCondition().findAll(BinaryExpr.class).stream()
                            .anyMatch(
                                comparison ->
                                    isComparison(comparison)
                                        && comparison.findAll(ArrayAccessExpr.class).stream()
                                            .anyMatch(a -> a.equals(access))));
        boolean advance =
            owned(loop, AssignExpr.class).stream()
                .anyMatch(
                    a ->
                        a.getOperator() == AssignExpr.Operator.ASSIGN
                            && isName(a.getTarget(), left)
                            && offset(a.getValue(), mid, BinaryExpr.Operator.PLUS, 1)
                            && conditional(a, loop));
        boolean retreat =
            owned(loop, AssignExpr.class).stream()
                .anyMatch(
                    a ->
                        a.getOperator() == AssignExpr.Operator.ASSIGN
                            && isName(a.getTarget(), right)
                            && (isName(a.getValue(), mid)
                                || offset(a.getValue(), mid, BinaryExpr.Operator.MINUS, 1))
                            && conditional(a, loop));
        if (compared && advance && retreat) {
          return Optional.of(
              hint(
                  loop,
                  "binary-search",
                  0.9,
                  "array-pointers",
                  Map.of("array", array, "left", left, "right", right, "mid", mid),
                  "Loop compares two bounds",
                  "Midpoint is derived from both bounds",
                  "An array value at the midpoint is compared and bounds are updated conditionally"));
        }
      }
    }
    return Optional.empty();
  }

  private Optional<AlgorithmHint> bind(Node loop, AlgorithmHint hint) {
    Node method = LocalVariableBindings.callable(loop);
    if (method == null) return Optional.empty();
    return LocalVariableBindings.roles(loop, hint.variables())
        .map(
            ids ->
                new AlgorithmHint(
                    hint.type(),
                    hint.confidence(),
                    hint.visualPlan(),
                    hint.evidence(),
                    hint.variables(),
                    hint.startLine(),
                    hint.endLine(),
                    AstAnalyzer.nodeId(loop),
                    AstAnalyzer.nodeId(method),
                    ids));
  }

  private Optional<AlgorithmHint> slidingWindow(Node loop) {
    // Addition belongs to the outer loop; subtraction may belong to a shrink loop.
    for (AssignExpr addition : owned(loop, AssignExpr.class)) {
      String sum = name(addition.getTarget());
      if (sum == null
          || addition.getOperator() != AssignExpr.Operator.PLUS
          || !(unwrap(addition.getValue()) instanceof ArrayAccessExpr incoming)) continue;
      String array = name(incoming.getName());
      String right = name(incoming.getIndex());
      if (array == null
          || right == null
          || !moves(loop, right, true)
          || !arrayBound(loop, right, array)) continue;
      for (AssignExpr subtraction : loop.findAll(AssignExpr.class)) {
        if (!withinExecutionScope(subtraction, loop)
            || !isName(subtraction.getTarget(), sum)
            || subtraction.getOperator() != AssignExpr.Operator.MINUS
            || !(unwrap(subtraction.getValue()) instanceof ArrayAccessExpr outgoing)
            || !isName(outgoing.getName(), array)) continue;
        String left = name(outgoing.getIndex());
        if (left != null && !left.equals(right)) {
          Node owner = nearestLoop(subtraction);
          boolean shrink =
              owner == loop
                  ? conditional(subtraction, loop) && moves(loop, left, true)
                  : owner instanceof WhileStmt
                      && nearestLoop(owner) == loop
                      && mentions(condition(owner), sum)
                      && moves(owner, left, true);
          if (shrink) {
            return Optional.of(
                hint(
                    loop,
                    "sliding-window",
                    0.85,
                    "array-window",
                    Map.of("array", array, "left", left, "right", right, "accumulator", sum),
                    "Right index advances within the array bound",
                    "Accumulator adds the entering element and removes the leaving element",
                    "Left index advances during conditional shrinking"));
          }
        }
        Expression index = unwrap(outgoing.getIndex());
        if (nearestLoop(subtraction) == loop
            && conditional(subtraction, loop)
            && index instanceof BinaryExpr difference
            && difference.getOperator() == BinaryExpr.Operator.MINUS
            && isName(difference.getLeft(), right)
            && name(difference.getRight()) != null
            && !mentions(difference.getRight(), right)) {
          String size = name(difference.getRight());
          if (writes(loop, size)) continue;
          return Optional.of(
              hint(
                  loop,
                  "sliding-window",
                  0.8,
                  "array-window",
                  Map.of("array", array, "right", right, "windowSize", size, "accumulator", sum),
                  "Right index advances within the array bound",
                  "Accumulator adds array[right] and conditionally removes array[right - windowSize]"));
        }
      }
    }
    return Optional.empty();
  }

  private static boolean midpoint(Expression expression, String left, String right) {
    Expression value = unwrap(expression);
    if (!(value instanceof BinaryExpr binary)) return false;
    if (binary.getOperator() == BinaryExpr.Operator.PLUS && isName(binary.getLeft(), left)) {
      Expression half = unwrap(binary.getRight());
      return half instanceof BinaryExpr division
          && halves(division)
          && unwrap(division.getLeft()) instanceof BinaryExpr difference
          && difference.getOperator() == BinaryExpr.Operator.MINUS
          && isName(difference.getLeft(), right)
          && isName(difference.getRight(), left);
    }
    return halves(binary)
        && unwrap(binary.getLeft()) instanceof BinaryExpr sum
        && sum.getOperator() == BinaryExpr.Operator.PLUS
        && ((isName(sum.getLeft(), left) && isName(sum.getRight(), right))
            || (isName(sum.getLeft(), right) && isName(sum.getRight(), left)));
  }

  private static boolean halves(BinaryExpr binary) {
    return (binary.getOperator() == BinaryExpr.Operator.DIVIDE && literal(binary.getRight(), 2))
        || ((binary.getOperator() == BinaryExpr.Operator.SIGNED_RIGHT_SHIFT
                || binary.getOperator() == BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT)
            && literal(binary.getRight(), 1));
  }

  private static Optional<BinaryExpr> orderedBounds(Node loop) {
    Expression expression = unwrap(condition(loop));
    if (expression instanceof BinaryExpr binary
        && (binary.getOperator() == BinaryExpr.Operator.LESS
            || binary.getOperator() == BinaryExpr.Operator.LESS_EQUALS)
        && name(binary.getLeft()) != null
        && name(binary.getRight()) != null
        && !name(binary.getLeft()).equals(name(binary.getRight()))) return Optional.of(binary);
    return Optional.empty();
  }

  private static boolean arrayBound(Node loop, String index, String array) {
    return unwrap(condition(loop)) instanceof BinaryExpr binary
        && binary.getOperator() == BinaryExpr.Operator.LESS
        && isName(binary.getLeft(), index)
        && unwrap(binary.getRight()) instanceof FieldAccessExpr length
        && length.getNameAsString().equals("length")
        && isName(length.getScope(), array);
  }

  private static boolean moves(Node loop, String variable, boolean forward) {
    boolean unary =
        owned(loop, UnaryExpr.class).stream()
            .anyMatch(
                u ->
                    isName(u.getExpression(), variable)
                        && (forward
                            ? u.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
                                || u.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT
                            : u.getOperator() == UnaryExpr.Operator.POSTFIX_DECREMENT
                                || u.getOperator() == UnaryExpr.Operator.PREFIX_DECREMENT));
    return unary
        || owned(loop, AssignExpr.class).stream()
            .anyMatch(
                a ->
                    isName(a.getTarget(), variable)
                        && ((a.getOperator()
                                    == (forward
                                        ? AssignExpr.Operator.PLUS
                                        : AssignExpr.Operator.MINUS)
                                && literal(a.getValue(), 1))
                            || (a.getOperator() == AssignExpr.Operator.ASSIGN
                                && offset(
                                    a.getValue(),
                                    variable,
                                    forward ? BinaryExpr.Operator.PLUS : BinaryExpr.Operator.MINUS,
                                    1))));
  }

  private static boolean writes(Node loop, String variable) {
    return loop.findAll(AssignExpr.class).stream().anyMatch(a -> isName(a.getTarget(), variable))
        || loop.findAll(UnaryExpr.class).stream()
            .anyMatch(
                u ->
                    isName(u.getExpression(), variable)
                        && (u.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
                            || u.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT
                            || u.getOperator() == UnaryExpr.Operator.POSTFIX_DECREMENT
                            || u.getOperator() == UnaryExpr.Operator.PREFIX_DECREMENT));
  }

  private static boolean offset(
      Expression expression, String variable, BinaryExpr.Operator operator, int amount) {
    return unwrap(expression) instanceof BinaryExpr binary
        && binary.getOperator() == operator
        && isName(binary.getLeft(), variable)
        && literal(binary.getRight(), amount);
  }

  private static boolean literal(Expression expression, int value) {
    return unwrap(expression) instanceof IntegerLiteralExpr integer
        && integer.getValue().equals(Integer.toString(value));
  }

  private static boolean isComparison(BinaryExpr binary) {
    return switch (binary.getOperator()) {
      case EQUALS, NOT_EQUALS, LESS, LESS_EQUALS, GREATER, GREATER_EQUALS -> true;
      default -> false;
    };
  }

  private static boolean mentions(Expression expression, String variable) {
    return expression != null
        && expression.findAll(NameExpr.class).stream()
            .anyMatch(n -> n.getNameAsString().equals(variable));
  }

  private static String name(Expression expression) {
    return unwrap(expression) instanceof NameExpr n ? n.getNameAsString() : null;
  }

  private static boolean isName(Expression expression, String expected) {
    return expected.equals(name(expression));
  }

  private static Expression unwrap(Expression expression) {
    while (expression instanceof EnclosedExpr enclosed) expression = enclosed.getInner();
    return expression;
  }

  private static Expression condition(Node loop) {
    if (loop instanceof WhileStmt statement) return statement.getCondition();
    if (loop instanceof ForStmt statement) return statement.getCompare().orElse(null);
    return null;
  }

  private static boolean isLoop(Node node) {
    return node instanceof WhileStmt
        || node instanceof ForStmt
        || node instanceof ForEachStmt
        || node instanceof DoStmt;
  }

  private static Node nearestLoop(Node node) {
    Node parent = node.getParentNode().orElse(null);
    while (parent != null && !isLoop(parent)) parent = parent.getParentNode().orElse(null);
    return parent;
  }

  private static boolean withinExecutionScope(Node node, Node loop) {
    for (Node current = node; current != loop; current = current.getParentNode().orElse(null)) {
      if (current == null
          || current instanceof LambdaExpr
          || current instanceof TypeDeclaration<?>
          || current instanceof ObjectCreationExpr
          || current instanceof ForEachStmt
          || current instanceof DoStmt) return false;
    }
    return true;
  }

  private static <T extends Node> List<T> owned(Node loop, Class<T> type) {
    return loop.findAll(type).stream()
        .filter(n -> nearestLoop(n) == loop)
        .filter(n -> withinExecutionScope(n, loop))
        .filter(
            n ->
                !(loop instanceof ForStmt statement)
                    || statement.getInitialization().stream()
                        .noneMatch(init -> containsIdentity(init, n)))
        .toList();
  }

  private static boolean containsIdentity(Node root, Node node) {
    for (Node current = node; current != null; current = current.getParentNode().orElse(null)) {
      if (current == root) return true;
    }
    return false;
  }

  private static boolean conditional(Node node, Node loop) {
    for (Node current = node.getParentNode().orElse(null);
        current != null && current != loop;
        current = current.getParentNode().orElse(null)) {
      if (current instanceof IfStmt) return true;
    }
    return false;
  }

  private static AlgorithmHint hint(
      Node loop,
      String type,
      double confidence,
      String plan,
      Map<String, String> variables,
      String... evidence) {
    var range = loop.getRange().orElseThrow();
    return new AlgorithmHint(
        type, confidence, plan, List.of(evidence), variables, range.begin.line, range.end.line);
  }
}
