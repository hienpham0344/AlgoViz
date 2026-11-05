package com.aisc.algoviz.ast.engine.algorithm.sort;

import com.aisc.algoviz.ast.engine.algorithm.LocalVariableBindings;
import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import java.util.Optional;

/** Bounded equivalences for zero-based binary-heap indices and simple array aliases. */
final class HeapIndexExpressions {
  private HeapIndexExpressions() {}

  enum ChildSide {
    LEFT,
    RIGHT
  }

  record HeapBoundary(String exclusiveExpression) {}

  static Optional<ChildSide> child(Expression expression, String parent, String provenLeft) {
    if (matchesAny(
        expression, "2 * " + parent + " + 1", parent + " * 2 + 1", "(" + parent + " << 1) + 1"))
      return Optional.of(ChildSide.LEFT);
    if (matchesAny(
            expression, "2 * " + parent + " + 2", parent + " * 2 + 2", "(" + parent + " << 1) + 2")
        || provenLeft != null && SortAstSupport.matches(expression, provenLeft + " + 1"))
      return Optional.of(ChildSide.RIGHT);
    return Optional.empty();
  }

  static boolean parent(Expression expression, String child) {
    return SortAstSupport.matches(expression, "(" + child + " - 1) / 2");
  }

  static Optional<HeapBoundary> boundary(
      Expression expression, String sizeOrLast, boolean inclusive) {
    if (SortAstSupport.matches(expression, sizeOrLast))
      return Optional.of(new HeapBoundary(inclusive ? sizeOrLast + " + 1" : sizeOrLast));
    if (inclusive && SortAstSupport.matches(expression, sizeOrLast + " - 1"))
      return Optional.of(new HeapBoundary(sizeOrLast));
    return Optional.empty();
  }

  static Optional<String> arrayDeclarationId(NameExpr reference) {
    Node method = LocalVariableBindings.callable(reference);
    if (method == null) return Optional.empty();
    String id = LocalVariableBindings.resolve(reference).orElse(null);
    if (id == null || reassigned(method, id)) return Optional.empty();
    for (Parameter parameter : method.findAll(Parameter.class)) {
      if (AstAnalyzer.nodeId(parameter).equals(id)
          && parameter.getType().asString().equals("int[]")) return Optional.of(id);
    }
    for (VariableDeclarator variable : method.findAll(VariableDeclarator.class)) {
      if (!AstAnalyzer.nodeId(variable).equals(id)
          || !variable.getType().asString().equals("int[]")
          || variable.getInitializer().isEmpty()
          || !(SortAstSupport.unwrap(variable.getInitializer().orElseThrow())
              instanceof NameExpr source)) continue;
      String sourceId = LocalVariableBindings.resolve(source).orElse(null);
      if (sourceId == null || reassigned(method, sourceId)) return Optional.empty();
      return method.findAll(Parameter.class).stream()
          .filter(
              parameter ->
                  AstAnalyzer.nodeId(parameter).equals(sourceId)
                      && parameter.getType().asString().equals("int[]"))
          .map(AstAnalyzer::nodeId)
          .findFirst();
    }
    return Optional.empty();
  }

  private static boolean reassigned(Node method, String declarationId) {
    return method.findAll(AssignExpr.class).stream()
        .filter(write -> SortAstSupport.unwrap(write.getTarget()) instanceof NameExpr)
        .map(write -> (NameExpr) SortAstSupport.unwrap(write.getTarget()))
        .anyMatch(
            name -> LocalVariableBindings.resolve(name).filter(declarationId::equals).isPresent());
  }

  private static boolean matchesAny(Expression expression, String... expected) {
    for (String candidate : expected)
      if (SortAstSupport.matches(expression, candidate)) return true;
    return false;
  }
}
