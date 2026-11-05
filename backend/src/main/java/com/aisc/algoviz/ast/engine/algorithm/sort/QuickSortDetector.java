package com.aisc.algoviz.ast.engine.algorithm.sort;

import com.aisc.algoviz.ast.engine.algorithm.LocalVariableBindings;
import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.aisc.algoviz.ast.engine.model.QuickPattern;
import com.aisc.algoviz.ast.engine.model.SortPattern.Direction;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The complete, directly written inclusive-range Lomuto Quick Sort family. */
public final class QuickSortDetector implements SortPatternDetector {
  @Override
  public Optional<AlgorithmHint> detect(Node node) {
    if (!(node instanceof ForStmt || node instanceof WhileStmt)) return Optional.empty();
    MethodDeclaration method = node.findAncestor(MethodDeclaration.class).orElse(null);
    if (method == null || method.getBody().isEmpty() || method.getParameters().size() != 3)
      return Optional.empty();
    ClassOrInterfaceDeclaration owner = method.findAncestor(ClassOrInterfaceDeclaration.class)
        .orElse(null);
    if (owner == null || (!method.isPrivate() && !method.isStatic() && !method.isFinal()
        && !owner.isFinal())) return Optional.empty();
    if (!method.getType().isVoidType()
        || !method.getParameter(0).getType().asString().equals("int[]")
        || !method.getParameter(1).getType().asString().equals("int")
        || !method.getParameter(2).getType().asString().equals("int")) return Optional.empty();
    String array = method.getParameter(0).getNameAsString();
    String low = method.getParameter(1).getNameAsString();
    String high = method.getParameter(2).getNameAsString();
    var recursiveBody = SortAstSupport.recursiveBody(method, low, high);
    if (recursiveBody.isEmpty()) return Optional.empty();
    List<Statement> body = recursiveBody.get().statements();
    int loopPosition = body.indexOf(node);
    if (loopPosition != 3 && loopPosition != 4) return Optional.empty();
    if (body.size() != loopPosition + 6) return Optional.empty();
    if (loopPosition == 4 && !(node instanceof WhileStmt)) return Optional.empty();
    IfStmt guard = recursiveBody.get().guard();
    var pivot = declaration(body.get(1), "int");
    var store = declaration(body.get(2), "int");
    if (pivot.isEmpty() || store.isEmpty()) return Optional.empty();
    String pivotName = pivot.get().getNameAsString();
    String storeName = store.get().getNameAsString();
    if (!SortAstSupport.matches(pivot.get().getInitializer().orElse(null), array + "[" + high + "]")
        || !SortAstSupport.matches(store.get().getInitializer().orElse(null), low))
      return Optional.empty();
    if (loopPosition == 4 && declaration(body.get(3), "int").isEmpty())
      return Optional.empty();
    var scanLoop = CountedLoop.match(node, low);
    if (scanLoop.isEmpty()
        || !SortAstSupport.matches(scanLoop.get().condition(), scanLoop.get().counter() + " < " + high)
        || scanLoop.get().bodyStatements().size() != 1
        || !(scanLoop.get().bodyStatements().get(0) instanceof IfStmt branch)
        || branch.getElseStmt().isPresent()
        || !(SortAstSupport.unwrap(branch.getCondition()) instanceof BinaryExpr comparison))
      return Optional.empty();
    String scan = scanLoop.get().counter();
    Direction direction = SortAstSupport.relationDirection(comparison,
        array + "[" + scan + "]", pivotName).orElse(null);
    if (direction == null) return Optional.empty();
    List<Statement> branchBody = branch.getThenStmt().isBlockStmt()
        ? List.copyOf(branch.getThenStmt().asBlockStmt().getStatements())
        : List.of(branch.getThenStmt());
    if (branchBody.size() != 4 || !increments(branchBody.get(3), storeName))
      return Optional.empty();
    var innerSwap = ArrayEffects.temporarySwap(branchBody.subList(0, 3), array, storeName, scan);
    var finalSwap = ArrayEffects.temporarySwap(body.subList(loopPosition + 1, loopPosition + 4),
        array, storeName, high);
    if (innerSwap.isEmpty() || finalSwap.isEmpty()) return Optional.empty();
    Statement leftCall = body.get(loopPosition + 4);
    Statement rightCall = body.get(loopPosition + 5);
    if (!recursive(leftCall, method, array, low, storeName + " - 1")
        || !recursive(rightCall, method, array, storeName + " + 1", high))
      return Optional.empty();
    Map<String, String> roles = Map.of("array", array, "low", low, "high", high,
        "pivot", pivotName, "store", storeName, "scan", scan);
    var ids = LocalVariableBindings.roles(method, roles);
    if (ids.isEmpty()
        || !scanLoop.get().counterDeclarationId().equals(ids.get().get("scan"))
        || !AstAnalyzer.nodeId(pivot.get()).equals(ids.get().get("pivot"))
        || !AstAnalyzer.nodeId(store.get()).equals(ids.get().get("store")))
      return Optional.empty();
    Map<String, String> operations = new LinkedHashMap<>();
    operations.put("guard", AstAnalyzer.nodeId(guard));
    operations.put("pivotRead", AstAnalyzer.nodeId(body.get(1)));
    operations.put("scanLoop", AstAnalyzer.nodeId(node));
    operations.put("comparison", AstAnalyzer.nodeId(comparison));
    for (int index = 0; index < 3; index++) {
      operations.put(new String[] {"innerSwapSave", "innerSwapLeftWrite", "innerSwapRightWrite"}[index],
          innerSwap.get().operationAstNodeIds().get(index));
      operations.put(new String[] {"finalSwapSave", "finalSwapLeftWrite", "finalSwapRightWrite"}[index],
          finalSwap.get().operationAstNodeIds().get(index));
    }
    operations.put("leftCall", AstAnalyzer.nodeId(leftCall));
    operations.put("rightCall", AstAnalyzer.nodeId(rightCall));
    var range = method.getRange().orElseThrow();
    return Optional.of(new AlgorithmHint(
        "quick-sort", 0.97, "quick-partition",
        List.of("Inclusive range guard and stable pivot at high",
            "Complete Lomuto partition with proven swaps",
            "Both recursive subranges exclude the final pivot"),
        roles, range.begin.line, range.end.line, AstAnalyzer.nodeId(node),
        AstAnalyzer.nodeId(method), ids.get(), null,
        new QuickPattern(1, direction, operations)));
  }

  private static Optional<com.github.javaparser.ast.body.VariableDeclarator> declaration(
      Statement statement, String type) {
    if (!(statement instanceof ExpressionStmt expression)
        || !(expression.getExpression() instanceof VariableDeclarationExpr declaration)
        || declaration.getVariables().size() != 1
        || !declaration.getVariable(0).getType().asString().equals(type))
      return Optional.empty();
    return Optional.of(declaration.getVariable(0));
  }

  private static boolean increments(Statement statement, String name) {
    if (!(statement instanceof ExpressionStmt expression)) return false;
    if (expression.getExpression() instanceof UnaryExpr unary)
      return SortAstSupport.matches(unary.getExpression(), name)
          && (unary.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
              || unary.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT);
    return SortAstSupport.matches(expression.getExpression(), name + " += 1")
        || SortAstSupport.matches(expression.getExpression(), name + " = " + name + " + 1");
  }

  private static boolean recursive(Statement statement, MethodDeclaration method, String array,
      String first, String second) {
    if (!(statement instanceof ExpressionStmt expression)
        || !(expression.getExpression() instanceof MethodCallExpr call)
        || !SortAstSupport.sameClassRecursiveCall(call, method)) return false;
    return SortAstSupport.matches(call.getArgument(0), array)
        && SortAstSupport.matches(call.getArgument(1), first)
        && SortAstSupport.matches(call.getArgument(2), second);
  }
}
