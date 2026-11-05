package com.aisc.algoviz.ast.engine.visualization;

import com.aisc.algoviz.ast.engine.algorithm.LocalVariableBindings;
import com.aisc.algoviz.ast.engine.algorithm.sort.SortNameResolution;
import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;

/** Conservatively links a call in the target class to one of its methods. */
final class SortCallSupport {
  private SortCallSupport() {}

  static boolean sameClassCall(MethodCallExpr call, MethodDeclaration target) {
    ClassOrInterfaceDeclaration owner =
        target.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null);
    if (owner == null || call.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null) != owner)
      return false;
    if (call.getScope().isEmpty()) return true;
    Expression scope = unwrap(call.getScope().orElseThrow());
    if (scope.isThisExpr()) return true;
    if (scope instanceof ObjectCreationExpr creation)
      return creation.getType().asString().equals(owner.getNameAsString());
    if (!(scope instanceof NameExpr name)) return false;
    String declarationId = LocalVariableBindings.resolve(name).orElse(null);
    if (declarationId == null)
      return target.isStatic() && SortNameResolution.unshadowedClassName(name, owner);
    return owner.findAll(VariableDeclarator.class).stream()
        .anyMatch(
            variable ->
                AstAnalyzer.nodeId(variable).equals(declarationId)
                    && variable.getType().asString().equals(owner.getNameAsString()));
  }

  private static Expression unwrap(Expression expression) {
    while (expression instanceof EnclosedExpr enclosed) expression = enclosed.getInner();
    return expression;
  }
}
