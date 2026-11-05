package com.aisc.algoviz.ast.engine.visualization;

import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;

/** Interprets the proven source comparison using operand order and observed values. */
final class SortRuntimeComparison {
  private SortRuntimeComparison() {}

  static boolean name(Expression expression, String expected) {
    return unwrap(expression) instanceof NameExpr actual
        && actual.getNameAsString().equals(expected);
  }

  static boolean arrayIndex(Expression expression, String expected) {
    return unwrap(expression) instanceof ArrayAccessExpr access
        && name(access.getIndex(), expected);
  }

  static boolean evaluate(BinaryExpr comparison, int first, int second, boolean reversed) {
    int left = reversed ? second : first;
    int right = reversed ? first : second;
    return switch (comparison.getOperator()) {
      case LESS -> left < right;
      case LESS_EQUALS -> left <= right;
      case GREATER -> left > right;
      case GREATER_EQUALS -> left >= right;
      default -> false;
    };
  }

  private static Expression unwrap(Expression expression) {
    while (expression instanceof EnclosedExpr enclosed) expression = enclosed.getInner();
    return expression;
  }
}
