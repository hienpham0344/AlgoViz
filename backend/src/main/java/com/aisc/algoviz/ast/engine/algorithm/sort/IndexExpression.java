package com.aisc.algoviz.ast.engine.algorithm.sort;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Conservative affine normalization for integer loop bounds. */
final class IndexExpression {
  private IndexExpression() {}

  static boolean sameCondition(Expression actual, Expression expected) {
    return inequality(actual).flatMap(left -> inequality(expected).map(left::equals)).orElse(false);
  }

  private static Optional<Affine> inequality(Expression expression) {
    if (!(SortAstSupport.unwrap(expression) instanceof BinaryExpr comparison)) return Optional.empty();
    Optional<Affine> left = affine(comparison.getLeft());
    Optional<Affine> right = affine(comparison.getRight());
    if (left.isEmpty() || right.isEmpty()) return Optional.empty();
    return switch (comparison.getOperator()) {
      case LESS -> Optional.of(left.get().minus(right.get()).plus(Affine.constant(1)));
      case LESS_EQUALS -> Optional.of(left.get().minus(right.get()));
      case GREATER -> Optional.of(right.get().minus(left.get()).plus(Affine.constant(1)));
      case GREATER_EQUALS -> Optional.of(right.get().minus(left.get()));
      default -> Optional.empty();
    };
  }

  private static Optional<Affine> affine(Expression expression) {
    Expression value = SortAstSupport.unwrap(expression);
    if (value instanceof IntegerLiteralExpr integer) {
      try {
        return Optional.of(Affine.constant(integer.asNumber().longValue()));
      } catch (RuntimeException ex) {
        return Optional.empty();
      }
    }
    if (value instanceof NameExpr name) return Optional.of(Affine.symbol(name.getNameAsString()));
    if (value instanceof FieldAccessExpr field
        && field.getNameAsString().equals("length")
        && SortAstSupport.unwrap(field.getScope()) instanceof NameExpr array) {
      return Optional.of(Affine.symbol(array.getNameAsString() + ".length"));
    }
    if (value instanceof UnaryExpr unary) {
      Optional<Affine> inner = affine(unary.getExpression());
      if (unary.getOperator() == UnaryExpr.Operator.PLUS) return inner;
      if (unary.getOperator() == UnaryExpr.Operator.MINUS)
        return inner.map(item -> Affine.constant(0).minus(item));
    }
    if (value instanceof BinaryExpr binary) {
      Optional<Affine> left = affine(binary.getLeft());
      Optional<Affine> right = affine(binary.getRight());
      if (left.isEmpty() || right.isEmpty()) return Optional.empty();
      if (binary.getOperator() == BinaryExpr.Operator.PLUS)
        return Optional.of(left.get().plus(right.get()));
      if (binary.getOperator() == BinaryExpr.Operator.MINUS)
        return Optional.of(left.get().minus(right.get()));
    }
    return Optional.empty();
  }

  private record Affine(Map<String, Long> terms, long constant) {
    static Affine constant(long value) {
      return new Affine(Map.of(), value);
    }

    static Affine symbol(String name) {
      return new Affine(Map.of(name, 1L), 0);
    }

    Affine plus(Affine other) {
      Map<String, Long> combined = new HashMap<>(terms);
      other.terms.forEach((name, coefficient) -> combined.merge(name, coefficient, Long::sum));
      combined.values().removeIf(value -> value == 0);
      return new Affine(Map.copyOf(combined), constant + other.constant);
    }

    Affine minus(Affine other) {
      Map<String, Long> inverted = new HashMap<>();
      other.terms.forEach((name, coefficient) -> inverted.put(name, -coefficient));
      return plus(new Affine(inverted, -other.constant));
    }
  }
}
