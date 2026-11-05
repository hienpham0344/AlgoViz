package com.aisc.algoviz.ast.engine.algorithm.sort;

import com.aisc.algoviz.ast.engine.algorithm.LocalVariableBindings;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.NameExpr;

/** Rejects a class-looking qualifier when Java may resolve it as a variable. */
public final class SortNameResolution {
  private SortNameResolution() {}

  public static boolean unshadowedClassName(NameExpr reference, ClassOrInterfaceDeclaration owner) {
    String name = owner.getNameAsString();
    if (!reference.getNameAsString().equals(name)
        || LocalVariableBindings.resolve(reference).isPresent()) return false;
    for (Node current = reference;
        current != null;
        current = current.getParentNode().orElse(null)) {
      if (!(current instanceof ClassOrInterfaceDeclaration declaration)) continue;
      if (!declaration.getExtendedTypes().isEmpty()
          || !declaration.getImplementedTypes().isEmpty()
          || declaration.getFields().stream()
              .flatMap(field -> field.getVariables().stream())
              .anyMatch(variable -> variable.getNameAsString().equals(name))) return false;
    }
    return reference.findCompilationUnit().stream()
        .flatMap(unit -> unit.getImports().stream())
        .noneMatch(
            entry ->
                entry.isStatic()
                    && (entry.isAsterisk() || entry.getName().getIdentifier().equals(name)));
  }
}
