package com.aisc.algoviz.ast.engine.algorithm.sort;

import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.aisc.algoviz.ast.engine.model.SortPattern.Direction;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class SortAstSupport {
  private SortAstSupport() {}

  record RecursiveBody(IfStmt guard, List<Statement> statements) {}

  static Optional<RecursiveBody> recursiveBody(MethodDeclaration method, String low, String high) {
    if (method.getBody().isEmpty()) return Optional.empty();
    List<Statement> source = List.copyOf(method.getBody().orElseThrow().getStatements());
    if (source.isEmpty() || !(source.get(0) instanceof IfStmt guard)
        || guard.getElseStmt().isPresent()) return Optional.empty();
    boolean earlyReturn = single(guard.getThenStmt()) instanceof ReturnStmt stop
        && stop.getExpression().isEmpty()
        && (matches(guard.getCondition(), low + " >= " + high)
            || matches(guard.getCondition(), high + " <= " + low));
    if (earlyReturn) return Optional.of(new RecursiveBody(guard, source));
    if (source.size() != 1 || !(guard.getThenStmt() instanceof BlockStmt block)
        || !(matches(guard.getCondition(), low + " < " + high)
            || matches(guard.getCondition(), high + " > " + low)))
      return Optional.empty();
    List<Statement> normalized = new ArrayList<>();
    normalized.add(guard);
    normalized.addAll(block.getStatements());
    return Optional.of(new RecursiveBody(guard, List.copyOf(normalized)));
  }

  static boolean sameClassRecursiveCall(MethodCallExpr call, MethodDeclaration method) {
    if (!call.getNameAsString().equals(method.getNameAsString())
        || call.getArguments().size() != method.getParameters().size()) return false;
    if (call.getScope().isEmpty()) return true;
    Expression scope = unwrap(call.getScope().orElseThrow());
    if (scope.isThisExpr()) return !method.isStatic();
    ClassOrInterfaceDeclaration owner = method.findAncestor(ClassOrInterfaceDeclaration.class)
        .orElse(null);
    return method.isStatic() && owner != null && scope instanceof NameExpr name
        && SortNameResolution.unshadowedClassName(name, owner);
  }

  static Optional<Direction> relationDirection(
      BinaryExpr comparison, String first, String second) {
    boolean direct = matches(comparison.getLeft(), first)
        && matches(comparison.getRight(), second);
    boolean reversed = matches(comparison.getLeft(), second)
        && matches(comparison.getRight(), first);
    if (!direct && !reversed) return Optional.empty();
    return Optional.ofNullable(switch (comparison.getOperator()) {
      case LESS, LESS_EQUALS -> reversed ? Direction.DESCENDING : Direction.ASCENDING;
      case GREATER, GREATER_EQUALS -> reversed ? Direction.ASCENDING : Direction.DESCENDING;
      default -> null;
    });
  }

  static String counter(ForStmt loop, String initializer) {
    if (loop.getInitialization().size() != 1
        || loop.getUpdate().size() != 1
        || !(loop.getInitialization().get(0)
            instanceof com.github.javaparser.ast.expr.VariableDeclarationExpr declaration)
        || declaration.getVariables().size() != 1) return null;
    var variable = declaration.getVariable(0);
    String name = variable.getNameAsString();
    if (!variable.getType().asString().equals("int")
        || !matches(variable.getInitializer().orElse(null), initializer)) return null;
    Expression update = unwrap(loop.getUpdate().get(0));
    boolean increments =
        update instanceof UnaryExpr unary
            && matches(unary.getExpression(), name)
            && (unary.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
                || unary.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT);
    return increments
            || matches(update, name + " += 1")
            || matches(update, name + " = " + name + " + 1")
        ? name
        : null;
  }

  static Statement single(Statement statement) {
    if (statement instanceof BlockStmt block && block.getStatements().size() == 1)
      return block.getStatement(0);
    return statement;
  }

  static boolean assignment(Statement statement, String target, String value) {
    return statement instanceof ExpressionStmt expression
        && expression.getExpression() instanceof AssignExpr assign
        && assign.getOperator() == AssignExpr.Operator.ASSIGN
        && matches(assign.getTarget(), target)
        && matches(assign.getValue(), value);
  }

  static boolean intArray(Node callable, String declarationId) {
    return callable.findAll(VariableDeclarator.class).stream()
            .anyMatch(
                variable ->
                    AstAnalyzer.nodeId(variable).equals(declarationId)
                        && variable.getType().asString().equals("int[]"))
        || callable.findAll(Parameter.class).stream()
            .anyMatch(
                parameter ->
                    AstAnalyzer.nodeId(parameter).equals(declarationId)
                        && parameter.getType().asString().equals("int[]"));
  }

  static boolean matches(Expression actual, String expected) {
    if (actual == null) return false;
    Expression copy = unwrap(actual).clone();
    for (EnclosedExpr enclosed : copy.findAll(EnclosedExpr.class))
      enclosed.replace(unwrap(enclosed).clone());
    Expression expectedTree = StaticJavaParser.parseExpression(expected);
    for (EnclosedExpr enclosed : expectedTree.findAll(EnclosedExpr.class))
      enclosed.replace(unwrap(enclosed).clone());
    return copy.equals(unwrap(expectedTree));
  }

  static Expression unwrap(Expression expression) {
    while (expression instanceof EnclosedExpr enclosed) expression = enclosed.getInner();
    return expression;
  }
}
