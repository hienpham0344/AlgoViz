package com.aisc.algoviz.ast.engine.algorithm;

import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Small shared AST operations; no algorithm labels or recognition policy live here. */
public final class PatternSupport {
  private PatternSupport() {}

  public static Expression unwrap(Expression expression) {
    while (expression instanceof EnclosedExpr enclosed) expression = enclosed.getInner();
    return expression;
  }

  public static String name(Expression expression) {
    return unwrap(expression) instanceof NameExpr n ? n.getNameAsString() : null;
  }

  public static boolean same(Expression first, Expression second) {
    return first != null && second != null && unwrap(first).equals(unwrap(second));
  }

  public static boolean named(Expression expression, String name) {
    return name != null && name.equals(name(expression));
  }

  public static boolean integer(Expression expression, int value) {
    if (!(unwrap(expression) instanceof IntegerLiteralExpr literal)) return false;
    try {
      return literal.asNumber().longValue() == value;
    } catch (NumberFormatException unsupportedLiteral) {
      return false;
    }
  }

  public static boolean offset(Expression expression, Expression base, int value) {
    if (!(unwrap(expression) instanceof BinaryExpr binary)) return false;
    return same(binary.getLeft(), base)
        && integer(binary.getRight(), Math.abs(value))
        && binary.getOperator()
            == (value < 0 ? BinaryExpr.Operator.MINUS : BinaryExpr.Operator.PLUS);
  }

  public static boolean contains(Node root, Node child) {
    for (Node current = child; current != null; current = current.getParentNode().orElse(null))
      if (current == root) return true;
    return false;
  }

  public static <T extends Node> List<T> owned(Node scope, Class<T> type) {
    Node owner = LocalVariableBindings.callable(scope);
    return scope.findAll(type).stream()
        .filter(node -> LocalVariableBindings.callable(node) == owner)
        .toList();
  }

  public static boolean isLoop(Node node) {
    return node instanceof ForStmt
        || node instanceof WhileStmt
        || node instanceof ForEachStmt
        || node instanceof DoStmt;
  }

  public static Optional<Node> loop(Node node) {
    return node.findAncestor(Node.class, PatternSupport::isLoop);
  }

  public static List<Node> loops(MethodDeclaration method) {
    return owned(method, Node.class).stream().filter(PatternSupport::isLoop).toList();
  }

  public static boolean math(Expression expression, String operation) {
    return unwrap(expression) instanceof MethodCallExpr call
        && call.getNameAsString().equals(operation)
        && call.getScope().filter(s -> standardUtility(s, "java.lang.Math")).isPresent()
        && call.getArguments().size() == 2;
  }

  public static boolean standardUtility(Expression scope, String qualifiedName) {
    if (!standardType(scope, scope.toString(), qualifiedName)) return false;
    String root = scope.toString().split("\\.")[0];
    if (scope instanceof NameExpr name && LocalVariableBindings.resolve(name).isPresent())
      return false;
    return scope.findCompilationUnit().stream()
        .flatMap(unit -> unit.findAll(FieldDeclaration.class).stream())
        .flatMap(field -> field.getVariables().stream())
        .noneMatch(field -> field.getNameAsString().equals(root));
  }

  public static boolean standardType(Node context, String writtenName, String qualifiedName) {
    String simple = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
    if (!writtenName.equals(simple) && !writtenName.equals(qualifiedName)) return false;
    var unit = context.findCompilationUnit();
    if (unit.isEmpty()) return false;
    String root = writtenName.split("\\.")[0];
    if (unit.get().findAll(TypeDeclaration.class).stream()
        .anyMatch(type -> type.getNameAsString().equals(root))) return false;
    if (writtenName.equals(qualifiedName)) return true;
    return unit.get().getImports().stream()
        .noneMatch(
            imported ->
                !imported.isAsterisk()
                    && imported.getNameAsString().endsWith("." + simple)
                    && !imported.getNameAsString().equals(qualifiedName));
  }

  public static Optional<VariableDeclarator> declaration(
      MethodDeclaration method, Expression reference) {
    if (!(unwrap(reference) instanceof NameExpr n)) return Optional.empty();
    return LocalVariableBindings.resolve(n)
        .flatMap(
            id ->
                owned(method, VariableDeclarator.class).stream()
                    .filter(d -> AstAnalyzer.nodeId(d).equals(id))
                    .findFirst());
  }

  public static boolean callOn(MethodCallExpr call, Expression receiver, String... names) {
    return List.of(names).contains(call.getNameAsString())
        && call.getScope().filter(scope -> same(scope, receiver)).isPresent();
  }

  public static boolean recursive(MethodCallExpr call, MethodDeclaration method) {
    return call.getNameAsString().equals(method.getNameAsString())
        && call.getArguments().size() == method.getParameters().size()
        && call.getScope().map(Expression::isThisExpr).orElse(true)
        && method.getParentNode().stream()
                .flatMap(p -> p.getChildNodes().stream())
                .filter(MethodDeclaration.class::isInstance)
                .map(MethodDeclaration.class::cast)
                .filter(
                    m ->
                        m.getNameAsString().equals(method.getNameAsString())
                            && m.getParameters().size() == call.getArguments().size())
                .count()
            == 1;
  }

  public static Optional<AlgorithmHint> hint(
      Node scope, String type, Map<String, String> roles, String... evidence) {
    Node owner = LocalVariableBindings.callable(scope);
    if (owner == null || scope.getRange().isEmpty()) return Optional.empty();
    return LocalVariableBindings.roles(scope, roles)
        .map(
            ids ->
                new AlgorithmHint(
                    type,
                    0.85,
                    "source-pattern",
                    List.of(evidence),
                    roles,
                    scope.getRange().orElseThrow().begin.line,
                    scope.getRange().orElseThrow().end.line,
                    AstAnalyzer.nodeId(scope),
                    AstAnalyzer.nodeId(owner),
                    ids));
  }
}
