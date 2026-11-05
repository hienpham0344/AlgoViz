package com.aisc.algoviz.ast.engine.algorithm;

import static com.aisc.algoviz.ast.engine.algorithm.PatternSupport.*;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UnaryExpr;

/**
 * Reads through immutable local aliases only; never guesses across assignments or call boundaries.
 */
public final class LocalValueOrigins {
  private LocalValueOrigins() {}

  public static Expression initializer(Expression expression) {
    Expression current = unwrap(expression);
    for (int depth = 0; depth < 16 && current instanceof NameExpr reference; depth++) {
      if (!(LocalVariableBindings.callable(reference) instanceof MethodDeclaration method)) break;
      var declaration = declaration(method, reference);
      if (declaration.isEmpty() || declaration.get().getInitializer().isEmpty()) break;
      var id = LocalVariableBindings.resolve(reference).orElseThrow();
      boolean written =
          owned(method, NameExpr.class).stream()
              .filter(n -> LocalVariableBindings.resolve(n).filter(id::equals).isPresent())
              .anyMatch(
                  n ->
                      n.getParentNode()
                          .filter(
                              p ->
                                  p instanceof AssignExpr a && a.getTarget() == n
                                      || p instanceof UnaryExpr u
                                          && switch (u.getOperator()) {
                                            case POSTFIX_INCREMENT,
                                                PREFIX_INCREMENT,
                                                POSTFIX_DECREMENT,
                                                PREFIX_DECREMENT ->
                                                true;
                                            default -> false;
                                          })
                          .isPresent());
      if (written) break;
      current = unwrap(declaration.get().getInitializer().orElseThrow());
    }
    return current;
  }
}
