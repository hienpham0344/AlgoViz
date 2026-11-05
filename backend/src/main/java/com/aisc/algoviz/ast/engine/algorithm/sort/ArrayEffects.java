package com.aisc.algoviz.ast.engine.algorithm.sort;

import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import java.util.List;
import java.util.Optional;

/** Conservative summaries of supported array mutations. */
final class ArrayEffects {
  private ArrayEffects() {}

  record Swap(
      List<String> operationAstNodeIds,
      String tempName,
      String helperMethodAstNodeId,
      String callAstNodeId) {}

  static Optional<Swap> temporarySwap(
      List<Statement> statements, String array, String left, String right) {
    if (statements.size() != 3
        || !(statements.get(0) instanceof ExpressionStmt first)
        || !(first.getExpression() instanceof VariableDeclarationExpr declaration)
        || declaration.getVariables().size() != 1) return Optional.empty();
    var temp = declaration.getVariable(0);
    String saved = temp.getNameAsString();
    if (!temp.getType().asString().equals("int")
        || !SortAstSupport.matches(temp.getInitializer().orElse(null), array + "[" + left + "]")
        || !SortAstSupport.assignment(
            statements.get(1), array + "[" + left + "]", array + "[" + right + "]")
        || !SortAstSupport.assignment(
            statements.get(2), array + "[" + right + "]", saved)) return Optional.empty();
    return Optional.of(
        new Swap(statements.stream().map(AstAnalyzer::nodeId).toList(), saved, null, null));
  }

  static Optional<Swap> helperSwap(
      Statement statement, String array, String left, String right) {
    if (!(statement instanceof ExpressionStmt expression)
        || !(expression.getExpression() instanceof MethodCallExpr call)
        || call.getArguments().size() != 3
        || call.getScope().filter(scope -> !(scope instanceof ThisExpr)).isPresent()
        || !SortAstSupport.matches(call.getArgument(0), array)
        || !SortAstSupport.matches(call.getArgument(1), left)
        || !SortAstSupport.matches(call.getArgument(2), right)) return Optional.empty();
    TypeDeclaration<?> owner = call.findAncestor(TypeDeclaration.class).orElse(null);
    if (owner == null) return Optional.empty();
    List<MethodDeclaration> candidates =
        owner.getMethodsByName(call.getNameAsString()).stream()
            .filter(method -> method.getParameters().size() == 3)
            .filter(method -> method.getType().isVoidType())
            .filter(method -> method.getParameter(0).getType().asString().equals("int[]"))
            .filter(method -> method.getParameter(1).getType().asString().equals("int"))
            .filter(method -> method.getParameter(2).getType().asString().equals("int"))
            .toList();
    if (candidates.size() != 1 || candidates.get(0).getBody().isEmpty()) return Optional.empty();
    MethodDeclaration helper = candidates.get(0);
    // A virtual call can dispatch to a subclass implementation with different effects.
    if (!helper.isStatic()
        && !helper.isPrivate()
        && !helper.isFinal()
        && !(owner instanceof ClassOrInterfaceDeclaration declaration && declaration.isFinal()))
      return Optional.empty();
    String helperArray = helper.getParameter(0).getNameAsString();
    String helperLeft = helper.getParameter(1).getNameAsString();
    String helperRight = helper.getParameter(2).getNameAsString();
    return temporarySwap(
            helper.getBody().orElseThrow().getStatements(), helperArray, helperLeft, helperRight)
        .map(
            swap ->
                new Swap(
                    swap.operationAstNodeIds(),
                    swap.tempName(),
                    AstAnalyzer.nodeId(helper),
                    AstAnalyzer.nodeId(statement)));
  }
}
