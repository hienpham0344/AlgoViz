package com.aisc.algoviz.ast.engine.algorithm.array;

import static com.aisc.algoviz.ast.engine.algorithm.PatternSupport.*;

import com.aisc.algoviz.ast.engine.algorithm.LocalVariableBindings;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.WhileStmt;

/** Canonical unit-stride array scans shared by search, prefix sums and Kadane. */
final class ForwardArrayScan {
  private ForwardArrayScan() {}

  static boolean matches(Node loop, Expression array, Expression index, int start) {
    if (name(array) == null || name(index) == null) return false;
    Expression condition =
        loop instanceof ForStmt f
            ? f.getCompare().orElse(null)
            : loop instanceof WhileStmt w ? w.getCondition() : null;
    if (!(unwrap(condition) instanceof BinaryExpr bound) || !same(bound.getLeft(), index))
      return false;
    boolean fullArray =
        bound.getOperator() == BinaryExpr.Operator.LESS
            && unwrap(bound.getRight()) instanceof FieldAccessExpr length
            && length.getNameAsString().equals("length")
            && same(length.getScope(), array);
    boolean subrange =
        start < 0
            && name(bound.getRight()) != null
            && (bound.getOperator() == BinaryExpr.Operator.LESS
                || bound.getOperator() == BinaryExpr.Operator.LESS_EQUALS);
    if (!fullArray && !subrange) return false;
    Expression update;
    if (loop instanceof ForStmt scan) {
      if (scan.getInitialization().size() != 1
          || !scan.getInitialization().get(0).isVariableDeclarationExpr()
          || scan.getInitialization().get(0).asVariableDeclarationExpr().getVariables().size() != 1
          || scan.getUpdate().size() != 1) return false;
      var variable = scan.getInitialization().get(0).asVariableDeclarationExpr().getVariable(0);
      if (!variable.getNameAsString().equals(name(index))
          || variable.getInitializer().filter(e -> initial(e, start)).isEmpty()) return false;
      update = scan.getUpdate().get(0);
    } else if (loop instanceof WhileStmt scan
        && scan.getBody() instanceof BlockStmt body
        && !body.getStatements().isEmpty()
        && body.getStatement(body.getStatements().size() - 1) instanceof ExpressionStmt last) {
      Node owner = LocalVariableBindings.callable(loop);
      if (!(owner instanceof MethodDeclaration method)) return false;
      var variable = declaration(method, index);
      if (variable.isEmpty()
          || variable.get().getInitializer().filter(e -> initial(e, start)).isEmpty()) return false;
      String id = com.aisc.algoviz.ast.engine.ast.AstAnalyzer.nodeId(variable.get());
      if (owned(method, NameExpr.class).stream()
          .anyMatch(
              n ->
                  n.getBegin().orElseThrow().isBefore(loop.getBegin().orElseThrow())
                      && LocalVariableBindings.resolve(n).filter(id::equals).isPresent()
                      && (n.getParentNode()
                          .filter(
                              p ->
                                  p instanceof AssignExpr a && a.getTarget() == n
                                      || p instanceof UnaryExpr u && mutation(u))
                          .isPresent()))) return false;
      update = last.getExpression();
    } else return false;
    if (!increments(update, index)) return false;
    return owned(loop, AssignExpr.class).stream()
            .noneMatch(a -> a != update && same(a.getTarget(), index))
        && owned(loop, UnaryExpr.class).stream()
            .noneMatch(u -> u != update && mutation(u) && same(u.getExpression(), index));
  }

  private static boolean increments(Expression expression, Expression index) {
    if (unwrap(expression) instanceof UnaryExpr u)
      return same(u.getExpression(), index)
          && (u.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
              || u.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT);
    return unwrap(expression) instanceof AssignExpr a
        && same(a.getTarget(), index)
        && (a.getOperator() == AssignExpr.Operator.PLUS && integer(a.getValue(), 1)
            || a.getOperator() == AssignExpr.Operator.ASSIGN && offset(a.getValue(), index, 1));
  }

  private static boolean initial(Expression expression, int start) {
    // A negative requested start permits a literal subrange origin for prefix accumulation.
    if (start >= 0) return integer(expression, start);
    if (!(unwrap(expression) instanceof IntegerLiteralExpr literal)) return false;
    try {
      return literal.asNumber().longValue() >= 0;
    } catch (NumberFormatException unsupportedLiteral) {
      return false;
    }
  }

  private static boolean mutation(UnaryExpr unary) {
    return switch (unary.getOperator()) {
      case PREFIX_INCREMENT, POSTFIX_INCREMENT, PREFIX_DECREMENT, POSTFIX_DECREMENT -> true;
      default -> false;
    };
  }
}
