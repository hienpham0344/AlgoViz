package com.aisc.algoviz.ast.engine.algorithm.greedy;

import com.aisc.algoviz.ast.engine.algorithm.LocalVariableBindings;
import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.aisc.algoviz.ast.engine.model.ActivitySelectionPattern.OrderingEvidence;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import java.util.List;

/** Proves only a simple, pure ascending-by-end comparator applied before the scan. */
public final class EndOrderingAnalyzer {
  public Result classify(CompilationUnit unit, Node loop, IntervalRoles roles) {
    Node callable = LocalVariableBindings.callable(loop);
    if (callable == null || loop.getBegin().isEmpty()) return Result.precondition();
    for (MethodCallExpr call : callable.findAll(MethodCallExpr.class)) {
      if (!isSortCall(call) || call.getRange().isEmpty()
          || !call.getRange().orElseThrow().end.isBefore(loop.getBegin().orElseThrow())
          || !guaranteedBefore(call, loop)) continue;
      if (isKnownSorter(call) && !standardUtility(unit, call.getScope().orElseThrow(),
          ((NameExpr) call.getScope().orElseThrow()).getNameAsString())) continue;
      Expression input = sortedInput(call);
      if (!sameInput(input, roles.inputDeclarationId())) continue;
      Expression comparator = comparator(call);
      ComparatorOrder order = comparatorOrder(unit, comparator, roles);
      if (order == ComparatorOrder.ASCENDING)
        return new Result(OrderingEvidence.SOURCE_PROVEN, AstAnalyzer.nodeId(call));
      if (order == ComparatorOrder.DESCENDING || order == ComparatorOrder.UNSAFE
          || order == ComparatorOrder.OTHER)
        return new Result(OrderingEvidence.CONTRADICTED, AstAnalyzer.nodeId(call));
    }
    return Result.precondition();
  }

  private static boolean guaranteedBefore(MethodCallExpr call, Node loop) {
    if (!(call.getParentNode().orElse(null) instanceof ExpressionStmt sortStatement)
        || sortStatement.getExpression() != call
        || !(sortStatement.getParentNode().orElse(null) instanceof BlockStmt block)
        || loop.getParentNode().orElse(null) != block) return false;
    int sortPosition = block.getStatements().indexOf(sortStatement);
    int loopPosition = block.getStatements().indexOf(loop);
    if (sortPosition < 0 || loopPosition <= sortPosition) return false;
    for (int index = sortPosition + 1; index < loopPosition; index++) {
      Statement statement = block.getStatement(index);
      if (!statement.isExpressionStmt()
          || !statement.asExpressionStmt().getExpression().isVariableDeclarationExpr()
          || !statement.findAll(MethodCallExpr.class).isEmpty()
          || !statement.findAll(com.github.javaparser.ast.expr.AssignExpr.class).isEmpty())
        return false;
    }
    return true;
  }

  private static boolean isSortCall(MethodCallExpr call) {
    String name = call.getNameAsString();
    if (name.equals("sort") && call.getScope().isPresent()) return true;
    return name.equals("sort") && call.getScope().filter(NameExpr.class::isInstance)
        .map(scope -> ((NameExpr) scope).getNameAsString().equals("Arrays")
            || ((NameExpr) scope).getNameAsString().equals("Collections")).orElse(false);
  }

  private static Expression sortedInput(MethodCallExpr call) {
    if (call.getScope().isPresent() && !isKnownSorter(call)) return call.getScope().orElseThrow();
    return call.getArguments().isEmpty() ? null : call.getArgument(0);
  }

  private static boolean isKnownSorter(MethodCallExpr call) {
    return call.getScope().filter(NameExpr.class::isInstance)
        .map(scope -> List.of("Arrays", "Collections").contains(((NameExpr) scope).getNameAsString()))
        .orElse(false);
  }

  private static Expression comparator(MethodCallExpr call) {
    if (call.getScope().isPresent() && !isKnownSorter(call))
      return call.getArguments().isEmpty() ? null : call.getArgument(0);
    return call.getArguments().size() < 2 ? null : call.getArgument(1);
  }

  private static boolean sameInput(Expression expression, String declarationId) {
    Expression value = unwrap(expression);
    return value instanceof NameExpr name
        && LocalVariableBindings.resolve(name).filter(declarationId::equals).isPresent();
  }

  private static ComparatorOrder comparatorOrder(
      CompilationUnit unit, Expression expression, IntervalRoles roles) {
    Expression comparator = unwrap(expression);
    if (comparator instanceof MethodCallExpr comparingInt
        && comparingInt.getNameAsString().equals("comparingInt")
        && comparingInt.getScope().filter(scope -> standardUtility(unit,
            scope, "Comparator")).isPresent()
        && comparingInt.getArguments().size() == 1) {
      Expression key = unwrap(comparingInt.getArgument(0));
      if (key instanceof MethodReferenceExpr reference)
        return endpointName(reference.getIdentifier(), roles, true)
            ? ComparatorOrder.ASCENDING
            : endpointName(reference.getIdentifier(), roles, false)
                ? ComparatorOrder.OTHER : ComparatorOrder.UNKNOWN;
      if (key instanceof LambdaExpr lambda && lambda.getParameters().size() == 1)
        return lambdaExpression(lambda).map(body -> isEnd(body, lambda.getParameter(0).getNameAsString(), roles)
                ? ComparatorOrder.ASCENDING
                : isStart(body, lambda.getParameter(0).getNameAsString(), roles)
                    ? ComparatorOrder.OTHER : ComparatorOrder.UNKNOWN)
            .orElse(ComparatorOrder.UNKNOWN);
    }
    LambdaExpr lambda = comparator instanceof LambdaExpr value ? value : null;
    if (lambda == null || lambda.getParameters().size() != 2) return ComparatorOrder.UNKNOWN;
    Expression body = lambdaExpression(lambda).map(EndOrderingAnalyzer::unwrap).orElse(null);
    if (body instanceof MethodCallExpr compare && compare.getNameAsString().equals("compare")
        && compare.getScope().filter(scope -> standardUtility(unit,
            scope, "Integer")).isPresent()
        && compare.getArguments().size() == 2) {
      String first = lambda.getParameter(0).getNameAsString();
      String second = lambda.getParameter(1).getNameAsString();
      if (isEnd(compare.getArgument(0), first, roles) && isEnd(compare.getArgument(1), second, roles))
        return ComparatorOrder.ASCENDING;
      if (isEnd(compare.getArgument(0), second, roles) && isEnd(compare.getArgument(1), first, roles))
        return ComparatorOrder.DESCENDING;
      if (isStart(compare.getArgument(0), first, roles) && isStart(compare.getArgument(1), second, roles))
        return ComparatorOrder.OTHER;
      if (isStart(compare.getArgument(0), second, roles) && isStart(compare.getArgument(1), first, roles))
        return ComparatorOrder.OTHER;
      if (mentionsEnd(compare, roles)) return ComparatorOrder.UNKNOWN;
    }
    if (body instanceof BinaryExpr subtraction && subtraction.getOperator() == BinaryExpr.Operator.MINUS
        && isEnd(subtraction.getLeft(), lambda.getParameter(0).getNameAsString(), roles)
        && isEnd(subtraction.getRight(), lambda.getParameter(1).getNameAsString(), roles))
      return ComparatorOrder.UNSAFE;
    return ComparatorOrder.UNKNOWN;
  }

  private static boolean endpointName(String name, IntervalRoles roles, boolean end) {
    if (roles.representation().equals("INT_MATRIX")) return false;
    return name.equals(end ? roles.endReadName() : roles.startReadName());
  }

  private static boolean standardUtility(
      CompilationUnit unit, Expression scope, String simpleName) {
    String qualified = simpleName.equals("Integer") ? "java.lang.Integer" : "java.util." + simpleName;
    if (scope.toString().equals(qualified)) return true;
    if (!(scope instanceof NameExpr name) || !name.getNameAsString().equals(simpleName)
        || LocalVariableBindings.resolve(name).isPresent()
        || unit.findAll(ClassOrInterfaceDeclaration.class).stream()
            .anyMatch(type -> type.getNameAsString().equals(simpleName))) return false;
    if (simpleName.equals("Integer"))
      return unit.getImports().stream().noneMatch(imported -> !imported.isAsterisk()
          && imported.getNameAsString().endsWith(".Integer")
          && !imported.getNameAsString().equals(qualified));
    return unit.getImports().stream().anyMatch(imported -> !imported.isStatic()
        && (imported.getNameAsString().equals(qualified)
            || imported.isAsterisk() && imported.getNameAsString().equals("java.util")));
  }

  private static java.util.Optional<Expression> lambdaExpression(LambdaExpr lambda) {
    return lambda.getBody() instanceof ExpressionStmt statement
        ? java.util.Optional.of(statement.getExpression()) : java.util.Optional.empty();
  }

  private static boolean isEnd(Expression expression, String variable, IntervalRoles roles) {
    return endpoint(expression, variable, roles, false);
  }

  private static boolean isStart(Expression expression, String variable, IntervalRoles roles) {
    return endpoint(expression, variable, roles, true);
  }

  private static boolean endpoint(Expression expression, String variable, IntervalRoles roles, boolean start) {
    Expression value = unwrap(expression);
    if (roles.representation().equals("INT_MATRIX") && value instanceof com.github.javaparser.ast.expr.ArrayAccessExpr access)
      return name(access.getName()).equals(variable) && integer(access.getIndex()) != null
          && integer(access.getIndex()).equals(start ? roles.startColumn() : roles.endColumn());
    if (value instanceof MethodCallExpr call)
      return call.getScope().filter(scope -> name(scope).equals(variable)).isPresent()
          && call.getNameAsString().equals(start ? roles.startReadName() : roles.endReadName())
          && call.getArguments().isEmpty();
    if (value instanceof com.github.javaparser.ast.expr.FieldAccessExpr field)
      return name(field.getScope()).equals(variable) && field.getNameAsString().equals(start ? roles.startField() : roles.endField());
    return false;
  }

  private static boolean mentionsEnd(Expression expression, IntervalRoles roles) {
    if (roles.representation().equals("INT_MATRIX")) return expression.toString().contains("[" + roles.endColumn() + "]");
    return expression.toString().contains("." + roles.endReadName() + "(")
        || expression.toString().contains("." + roles.endField());
  }

  private static Integer integer(Expression expression) {
    try { return Integer.valueOf(unwrap(expression).toString()); }
    catch (NumberFormatException ignored) { return null; }
  }

  private static String name(Expression expression) {
    Expression value = unwrap(expression);
    return value instanceof NameExpr name ? name.getNameAsString() : "";
  }

  private static Expression unwrap(Expression expression) {
    while (expression instanceof EnclosedExpr enclosed) expression = enclosed.getInner();
    return expression;
  }

  public record Result(OrderingEvidence evidence, String sortAstNodeId) {
    private static Result precondition() {
      return new Result(OrderingEvidence.INPUT_PRECONDITION, null);
    }
  }

  private enum ComparatorOrder { ASCENDING, DESCENDING, UNSAFE, OTHER, UNKNOWN }
}
