package com.aisc.algoviz.ast.engine.algorithm.greedy;

import com.aisc.algoviz.ast.engine.algorithm.LocalVariableBindings;
import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Resolves interval endpoints from their use in the selection guard and state update. */
public record IntervalRoles(
    String representation,
    String inputDeclarationId,
    String candidateDeclarationId,
    Integer startColumn,
    Integer endColumn,
    String intervalType,
    String startField,
    String endField,
    String startReadName,
    String endReadName) {

  public static Optional<IntervalRoles> resolve(CompilationUnit unit, Node loop) {
    Candidate candidate = candidate(unit, loop);
    if (candidate == null) return Optional.empty();

    List<IntervalRoles> matches = new ArrayList<>();
    for (IfStmt branch : loop.findAll(IfStmt.class)) {
      if (nearestLoop(branch) != loop || branch.getElseStmt().isPresent()) continue;
      for (BinaryExpr comparison : branch.getCondition().findAll(BinaryExpr.class)) {
        if (!isBoundaryComparison(comparison)) continue;
        Endpoint left = endpoint(unit, comparison.getLeft(), candidate);
        Endpoint right = endpoint(unit, comparison.getRight(), candidate);
        if ((left == null) == (right == null)) continue;
        Endpoint start = left != null ? left : right;
        Expression boundaryExpr = left != null ? comparison.getRight() : comparison.getLeft();
        String boundary = simpleName(boundaryExpr);
        if (boundary == null) continue;
        for (AssignExpr update : branch.getThenStmt().findAll(AssignExpr.class)) {
          if (update.getOperator() != AssignExpr.Operator.ASSIGN
              || !boundary.equals(simpleName(update.getTarget()))) continue;
          Endpoint end = endpoint(unit, update.getValue(), candidate);
          if (end == null || start.same(end)) continue;
          matches.add(candidate.toRoles(start, end));
        }
      }
    }
    if (matches.size() != 1) return Optional.empty();
    return Optional.of(matches.get(0));
  }

  public static boolean matchesEndpoint(
      CompilationUnit unit, Expression expression, String candidateName,
      IntervalRoles roles, boolean start) {
    Candidate candidate = new Candidate(roles.representation().equals("INT_MATRIX"),
        candidateName, roles.candidateDeclarationId(), roles.inputDeclarationId(),
        roles.intervalType());
    Endpoint endpoint = endpoint(unit, expression, candidate);
    if (endpoint == null) return false;
    return candidate.matrix()
        ? endpoint.column() != null && endpoint.column().equals(
            start ? roles.startColumn() : roles.endColumn())
        : endpoint.field() != null && endpoint.field().equals(
            start ? roles.startField() : roles.endField());
  }

  public static String runtimeType(CompilationUnit unit, String simpleName) {
    var matches = unit.findAll(TypeDeclaration.class).stream()
        .filter(type -> type.getNameAsString().equals(simpleName))
        .toList();
    if (matches.size() != 1) return null;
    List<String> names = new ArrayList<>();
    for (Node node = matches.get(0); node != null; node = node.getParentNode().orElse(null))
      if (node instanceof TypeDeclaration<?> type) names.add(0, type.getNameAsString());
    String packagePrefix = unit.getPackageDeclaration()
        .map(declaration -> declaration.getNameAsString() + ".").orElse("");
    return packagePrefix + String.join("$", names);
  }

  private static Candidate candidate(CompilationUnit unit, Node loop) {
    if (loop instanceof ForEachStmt each) {
      VariableDeclarator variable = each.getVariable().getVariable(0);
      if (!(each.getIterable() instanceof NameExpr input)) return null;
      String inputId = LocalVariableBindings.resolve(input).orElse(null);
      if (inputId == null) return null;
      String type = variable.getType().asString();
      if (type.equals("int[]") && "int[][]".equals(declarationType(unit, inputId)))
        return new Candidate(true, variable.getNameAsString(), AstAnalyzer.nodeId(variable),
            inputId, null);
      if (findRecord(unit, type) == null && findFinalIntervalClass(unit, type) == null) return null;
      String inputType = declarationType(unit, inputId);
      if (inputType == null || !isIntervalList(inputType)
          || !inputType.contains("<" + type + ">")) return null;
      return new Candidate(false, variable.getNameAsString(), AstAnalyzer.nodeId(variable),
          inputId, type);
    }

    for (VariableDeclarator variable : loop.findAll(VariableDeclarator.class)) {
      if (nearestLoop(variable) != loop || variable.getInitializer().isEmpty()) continue;
      Expression initializer = unwrap(variable.getInitializer().orElseThrow());
      if (!variable.getType().asString().equals("int[]") || !(initializer instanceof ArrayAccessExpr row)
          || !(unwrap(row.getName()) instanceof NameExpr input)) continue;
      String inputId = LocalVariableBindings.resolve(input).orElse(null);
      if (inputId == null || !"int[][]".equals(declarationType(unit, inputId))) continue;
      return new Candidate(true, variable.getNameAsString(), AstAnalyzer.nodeId(variable),
          inputId, null);
    }
    for (VariableDeclarator variable : loop.findAll(VariableDeclarator.class)) {
      if (nearestLoop(variable) != loop || variable.getInitializer().isEmpty()
          || !(unwrap(variable.getInitializer().orElseThrow()) instanceof MethodCallExpr get)
          || !get.getNameAsString().equals("get") || get.getArguments().size() != 1
          || get.getScope().filter(NameExpr.class::isInstance).isEmpty()) continue;
      String type = variable.getType().asString();
      if (findRecord(unit, type) == null && findFinalIntervalClass(unit, type) == null) continue;
      NameExpr input = (NameExpr) get.getScope().orElseThrow();
      String inputId = LocalVariableBindings.resolve(input).orElse(null);
      String inputType = inputId == null ? null : declarationType(unit, inputId);
      if (inputType == null || !isIntervalList(inputType) || !inputType.contains("<" + type + ">")) continue;
      return new Candidate(false, variable.getNameAsString(), AstAnalyzer.nodeId(variable), inputId, type);
    }
    return null;
  }

  private static Endpoint endpoint(CompilationUnit unit, Expression expression, Candidate candidate) {
    Expression value = unwrap(expression);
    if (value instanceof NameExpr alias) {
      String id = LocalVariableBindings.resolve(alias).orElse(null);
      VariableDeclarator declaration = id == null ? null : unit.findAll(VariableDeclarator.class)
          .stream().filter(variable -> AstAnalyzer.nodeId(variable).equals(id))
          .findFirst().orElse(null);
      if (declaration == null || !declaration.getType().asString().equals("int")
          || declaration.getInitializer().isEmpty()
          || nearestLoop(declaration) == null
          || nearestLoop(declaration) != nearestLoop(alias)
          || declaration.getBegin().isEmpty() || alias.getBegin().isEmpty()
          || !declaration.getBegin().orElseThrow().isBefore(alias.getBegin().orElseThrow())
          || aliasWritten(nearestLoop(alias), alias.getNameAsString())) return null;
      return endpoint(unit, declaration.getInitializer().orElseThrow(), candidate);
    }
    if (candidate.matrix()) {
      if (value instanceof MethodCallExpr helper)
        return helperEndpoint(unit, helper, candidate);
      if (!(value instanceof ArrayAccessExpr access)
          || !candidate.name().equals(simpleName(access.getName()))
          || !(unwrap(access.getIndex()) instanceof IntegerLiteralExpr literal)) return null;
      try {
        int column = Integer.parseInt(literal.getValue());
        return column >= 0 ? new Endpoint(column, null, null) : null;
      } catch (NumberFormatException ignored) {
        return null;
      }
    }
    if (value instanceof FieldAccessExpr field
        && candidate.name().equals(simpleName(field.getScope()))) {
      return intervalField(unit, candidate.intervalType(), field.getNameAsString(), false);
    }
    if (value instanceof MethodCallExpr call
        && call.getScope().filter(scope -> candidate.name().equals(simpleName(scope))).isPresent()
        && call.getArguments().isEmpty()) {
      return intervalField(unit, candidate.intervalType(), call.getNameAsString(), true);
    }
    if (value instanceof MethodCallExpr call)
      return helperEndpoint(unit, call, candidate);
    return null;
  }

  private static Endpoint helperEndpoint(
      CompilationUnit unit, MethodCallExpr call, Candidate candidate) {
    if (call.getArguments().size() != 1
        || !candidate.name().equals(simpleName(call.getArgument(0)))
        || call.getScope().filter(scope -> !(unwrap(scope) instanceof ThisExpr)).isPresent())
      return null;
    ClassOrInterfaceDeclaration owner = call.findAncestor(ClassOrInterfaceDeclaration.class)
        .orElse(null);
    if (owner == null) return null;
    List<MethodDeclaration> methods = owner.getMethodsByName(call.getNameAsString());
    if (methods.size() != 1) return null;
    MethodDeclaration method = methods.get(0);
    if (!(method.isPrivate() || method.isStatic() || method.isFinal() || owner.isFinal())
        || !method.getType().asString().equals("int")
        || method.getParameters().size() != 1
        || !method.getParameter(0).getType().asString().equals(
            candidate.matrix() ? "int[]" : candidate.intervalType())
        || method.getBody().isEmpty() || method.getBody().orElseThrow().getStatements().size() != 1
        || !method.getBody().orElseThrow().getStatement(0).isReturnStmt()) return null;
    Expression returned = method.getBody().orElseThrow().getStatement(0).asReturnStmt()
        .getExpression().orElse(null);
    if (returned == null || !(unwrap(returned) instanceof ArrayAccessExpr
        || unwrap(returned) instanceof FieldAccessExpr
        || unwrap(returned) instanceof MethodCallExpr accessor
            && accessor.getScope().filter(scope -> method.getParameter(0).getNameAsString()
                .equals(simpleName(scope))).isPresent())) return null;
    return endpoint(unit, returned, new Candidate(candidate.matrix(),
        method.getParameter(0).getNameAsString(), candidate.declarationId(),
        candidate.inputId(), candidate.intervalType()));
  }

  private static boolean aliasWritten(Node loop, String alias) {
    return loop.findAll(AssignExpr.class).stream()
        .filter(assignment -> nearestLoop(assignment) == loop)
        .anyMatch(assignment -> alias.equals(simpleName(assignment.getTarget())))
        || loop.findAll(UnaryExpr.class).stream()
            .filter(unary -> nearestLoop(unary) == loop)
            .anyMatch(unary -> alias.equals(simpleName(unary.getExpression()))
                && (unary.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
                    || unary.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT
                    || unary.getOperator() == UnaryExpr.Operator.POSTFIX_DECREMENT
                    || unary.getOperator() == UnaryExpr.Operator.PREFIX_DECREMENT));
  }

  private static Endpoint intervalField(
      CompilationUnit unit, String type, String accessor, boolean called) {
    RecordDeclaration record = findRecord(unit, type);
    if (record != null) {
      boolean component = record.getParameters().stream()
          .anyMatch(parameter -> parameter.getNameAsString().equals(accessor));
      if (!component || !called && !record.getMethodsByName(accessor).isEmpty()) return null;
      return record.getMethodsByName(accessor).stream()
              .allMatch(method -> method.getParameters().isEmpty()
                  && method.getBody().filter(body -> body.getStatements().size() == 1
                      && body.getStatement(0).isReturnStmt()
                      && body.getStatement(0).asReturnStmt().getExpression()
                          .map(expression -> accessor.equals(returnedField(expression)))
                          .orElse(false)).isPresent())
          ? new Endpoint(null, accessor, accessor) : null;
    }
    ClassOrInterfaceDeclaration declaration = findFinalIntervalClass(unit, type);
    if (declaration == null) return null;
    if (!called) return isFinalIntField(declaration, accessor)
        ? new Endpoint(null, accessor, accessor) : null;
    for (var method : declaration.getMethodsByName(accessor)) {
      if (method.getParameters().isNonEmpty() || method.getBody().isEmpty()
          || method.getBody().orElseThrow().getStatements().size() != 1
          || !method.getBody().orElseThrow().getStatement(0).isReturnStmt()) continue;
      Expression returned = method.getBody().orElseThrow().getStatement(0).asReturnStmt()
          .getExpression().orElse(null);
      String fieldName = returnedField(returned);
      if (fieldName != null && isFinalIntField(declaration, fieldName))
        return new Endpoint(null, fieldName, accessor);
    }
    return null;
  }

  private static boolean isFinalIntField(ClassOrInterfaceDeclaration declaration, String name) {
    return declaration.getFields().stream().filter(field -> field.isFinal() && !field.isStatic())
        .flatMap(field -> field.getVariables().stream())
        .anyMatch(field -> field.getType().asString().equals("int") && field.getNameAsString().equals(name));
  }

  private static ClassOrInterfaceDeclaration findFinalIntervalClass(CompilationUnit unit, String name) {
    return unit.findAll(ClassOrInterfaceDeclaration.class).stream()
        .filter(type -> type.getNameAsString().equals(name) && type.isFinal())
        .filter(type -> type.getFields().stream().filter(field -> field.isFinal() && !field.isStatic())
            .flatMap(field -> field.getVariables().stream())
            .filter(field -> field.getType().asString().equals("int"))
            .count() == 2)
        .findFirst().orElse(null);
  }

  private static RecordDeclaration findRecord(CompilationUnit unit, String name) {
    return unit.findAll(RecordDeclaration.class).stream()
        .filter(type -> type.getNameAsString().equals(name))
        .filter(type -> type.getParameters().size() == 2
            && type.getParameters().stream().allMatch(parameter -> parameter.getType().asString().equals("int")))
        .findFirst().orElse(null);
  }

  private static boolean isBoundaryComparison(BinaryExpr expression) {
    return expression.getOperator() == BinaryExpr.Operator.GREATER_EQUALS
        || expression.getOperator() == BinaryExpr.Operator.GREATER
        || expression.getOperator() == BinaryExpr.Operator.LESS_EQUALS
        || expression.getOperator() == BinaryExpr.Operator.LESS;
  }

  private static String declarationType(CompilationUnit unit, String id) {
    for (Parameter parameter : unit.findAll(Parameter.class))
      if (AstAnalyzer.nodeId(parameter).equals(id)) return parameter.getType().asString();
    for (VariableDeclarator variable : unit.findAll(VariableDeclarator.class))
      if (AstAnalyzer.nodeId(variable).equals(id)) return variable.getType().asString();
    return null;
  }

  private static boolean isIntervalList(String type) {
    return type.startsWith("List<") || type.startsWith("java.util.List<")
        || type.startsWith("ArrayList<") || type.startsWith("LinkedList<")
        || type.startsWith("java.util.ArrayList<") || type.startsWith("java.util.LinkedList<");
  }

  private static Node nearestLoop(Node node) {
    for (Node parent = node.getParentNode().orElse(null); parent != null;
        parent = parent.getParentNode().orElse(null))
      if (parent instanceof ForStmt || parent instanceof WhileStmt || parent instanceof ForEachStmt) return parent;
    return null;
  }

  private static String simpleName(Expression expression) {
    Expression value = unwrap(expression);
    if (value instanceof NameExpr name) return name.getNameAsString();
    return null;
  }

  private static String returnedField(Expression expression) {
    if (expression == null) return null;
    Expression value = unwrap(expression);
    if (value instanceof NameExpr name) return name.getNameAsString();
    if (value instanceof FieldAccessExpr field && unwrap(field.getScope()) instanceof ThisExpr)
      return field.getNameAsString();
    return null;
  }

  private static Expression unwrap(Expression expression) {
    Expression value = expression;
    while (value instanceof EnclosedExpr enclosed) value = enclosed.getInner();
    return value;
  }

  private record Endpoint(Integer column, String field, String readName) {
    boolean same(Endpoint other) {
      return column != null ? column.equals(other.column) : field.equals(other.field);
    }
  }

  private record Candidate(
      boolean matrix, String name, String declarationId, String inputId, String intervalType) {
    IntervalRoles toRoles(Endpoint start, Endpoint end) {
      return new IntervalRoles(matrix ? "INT_MATRIX" : "INTERVAL_LIST", inputId, declarationId,
          start.column(), end.column(), intervalType, start.field(), end.field(),
          start.readName(), end.readName());
    }
  }
}
