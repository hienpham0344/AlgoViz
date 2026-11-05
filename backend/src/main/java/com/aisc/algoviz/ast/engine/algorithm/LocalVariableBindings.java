package com.aisc.algoviz.ast.engine.algorithm;

import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.TryStmt;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Lexical bindings for supported locals and parameters; unresolved references stay unresolved. */
public final class LocalVariableBindings {
  private LocalVariableBindings() {}

  public static Node callable(Node node) {
    for (Node current = node; current != null; current = current.getParentNode().orElse(null)) {
      if (current instanceof LambdaExpr || current instanceof TypeDeclaration<?>) return null;
      if (current instanceof CallableDeclaration<?>) return current;
    }
    return null;
  }

  public static Optional<String> resolve(NameExpr reference) {
    Node owner = callable(reference);
    if (owner == null || reference.getBegin().isEmpty()) return Optional.empty();
    List<Node> candidates = new ArrayList<>();
    for (VariableDeclarator declaration : owner.findAll(VariableDeclarator.class)) {
      if (!declaration.getNameAsString().equals(reference.getNameAsString())
          || callable(declaration) != owner
          || declaration.findAncestor(FieldDeclaration.class).isPresent()
          || declaration.getBegin().isEmpty()
          || declaration.getBegin().get().isAfter(reference.getBegin().get())) continue;
      Node scope = scope(declaration);
      if (distance(reference, scope) >= 0) candidates.add(declaration);
    }
    for (Parameter parameter : owner.findAll(Parameter.class)) {
      if (!parameter.getNameAsString().equals(reference.getNameAsString())
          || callable(parameter) != owner) continue;
      Node scope = parameter.getParentNode().orElse(null);
      if ((scope instanceof CallableDeclaration<?> || scope instanceof CatchClause)
          && distance(reference, scope) >= 0) candidates.add(parameter);
    }
    int nearest = Integer.MAX_VALUE;
    Node selected = null;
    boolean ambiguous = false;
    for (Node candidate : candidates) {
      Node scope =
          candidate instanceof Parameter
              ? candidate.getParentNode().orElse(null)
              : scope(candidate);
      int depth = distance(reference, scope);
      if (depth < nearest) {
        nearest = depth;
        selected = candidate;
        ambiguous = false;
      } else if (depth == nearest) ambiguous = true;
    }
    return selected == null || ambiguous
        ? Optional.empty()
        : Optional.of(AstAnalyzer.nodeId(selected));
  }

  public static Optional<Map<String, String>> roles(Node loop, Map<String, String> names) {
    Map<String, String> result = new LinkedHashMap<>();
    for (var role : names.entrySet()) {
      List<NameExpr> references =
          loop.findAll(NameExpr.class).stream()
              .filter(
                  n -> n.getNameAsString().equals(role.getValue()) && callable(n) == callable(loop))
              .toList();
      if (references.isEmpty()) return Optional.empty();
      String expected = null;
      for (NameExpr reference : references) {
        Optional<String> resolved = resolve(reference);
        if (resolved.isEmpty() || expected != null && !expected.equals(resolved.get()))
          return Optional.empty();
        expected = resolved.get();
      }
      result.put(role.getKey(), expected);
    }
    return Optional.of(Map.copyOf(result));
  }

  private static Node scope(Node declaration) {
    for (Node current = declaration.getParentNode().orElse(null);
        current != null;
        current = current.getParentNode().orElse(null)) {
      if (current instanceof BlockStmt
          || current instanceof ForStmt
          || current instanceof ForEachStmt) return current;
      if (current instanceof SwitchEntry || current instanceof TryStmt) return null;
      if (current instanceof CallableDeclaration<?> || current instanceof LambdaExpr) return null;
    }
    return null;
  }

  private static int distance(Node reference, Node scope) {
    if (scope == null) return -1;
    int distance = 0;
    for (Node current = reference;
        current != null;
        current = current.getParentNode().orElse(null)) {
      if (current == scope) return distance;
      distance++;
    }
    return -1;
  }
}
