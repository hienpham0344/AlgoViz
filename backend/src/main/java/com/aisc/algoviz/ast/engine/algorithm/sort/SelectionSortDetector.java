package com.aisc.algoviz.ast.engine.algorithm.sort;

import com.aisc.algoviz.ast.engine.algorithm.LocalVariableBindings;
import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.aisc.algoviz.ast.engine.model.SortPattern;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Recognizes complete min/max selection passes with bound local roles. */
public final class SelectionSortDetector implements SortPatternDetector {
  public SelectionSortDetector() {}

  @Override
  public Optional<AlgorithmHint> detect(Node node) {
    var outer = CountedLoop.match(node, "0");
    if (outer.isEmpty()) return Optional.empty();
    List<Statement> body = outer.get().bodyStatements();
    if (body.size() < 3
        || !(body.get(0) instanceof ExpressionStmt selectedStatement)
        || !(selectedStatement.getExpression() instanceof VariableDeclarationExpr selectedDeclaration)
        || selectedDeclaration.getVariables().size() != 1) return Optional.empty();
    VariableDeclarator selected = selectedDeclaration.getVariable(0);
    String pass = outer.get().counter();
    String selectedIndex = selected.getNameAsString();
    if (!selected.getType().asString().equals("int")
        || !SortAstSupport.matches(selected.getInitializer().orElse(null), pass))
      return Optional.empty();
    int innerPosition = body.get(1) instanceof ForStmt ? 1 : 2;
    if (body.size() != innerPosition + 4 && body.size() != innerPosition + 2
        || innerPosition == 2
            && (!(body.get(1) instanceof ExpressionStmt scanDeclaration)
                || !(scanDeclaration.getExpression() instanceof VariableDeclarationExpr))
        || !(body.get(innerPosition) instanceof ForStmt || body.get(innerPosition) instanceof WhileStmt))
      return Optional.empty();
    Node innerNode = body.get(innerPosition);
    var inner = CountedLoop.match(innerNode, pass + " + 1");
    if (inner.isEmpty() || inner.get().bodyStatements().size() != 1
        || !(inner.get().bodyStatements().get(0) instanceof IfStmt branch)
        || branch.getElseStmt().isPresent()
        || !(SortAstSupport.single(branch.getThenStmt()) instanceof ExpressionStmt update)
        || !(update.getExpression() instanceof AssignExpr assign)
        || assign.getOperator() != AssignExpr.Operator.ASSIGN
        || !SortAstSupport.matches(assign.getTarget(), selectedIndex)
        || !SortAstSupport.matches(assign.getValue(), inner.get().counter())
        || !(SortAstSupport.unwrap(branch.getCondition()) instanceof BinaryExpr comparison)
        || comparison.getOperator() != BinaryExpr.Operator.LESS
            && comparison.getOperator() != BinaryExpr.Operator.GREATER
        || !(SortAstSupport.unwrap(comparison.getLeft()) instanceof ArrayAccessExpr first)
        || !(SortAstSupport.unwrap(first.getName()) instanceof NameExpr arrayName))
      return Optional.empty();
    String array = arrayName.getNameAsString();
    String scan = inner.get().counter();
    boolean direct =
        SortAstSupport.matches(first, array + "[" + scan + "]")
            && SortAstSupport.matches(comparison.getRight(), array + "[" + selectedIndex + "]");
    boolean reversed =
        SortAstSupport.matches(first, array + "[" + selectedIndex + "]")
            && SortAstSupport.matches(comparison.getRight(), array + "[" + scan + "]");
    if (!direct && !reversed) return Optional.empty();
    if (!IndexExpression.sameCondition(
            outer.get().condition(),
            StaticJavaParser.parseExpression(pass + " < " + array + ".length - 1"))
        || !IndexExpression.sameCondition(
            inner.get().condition(),
            StaticJavaParser.parseExpression(scan + " < " + array + ".length")))
      return Optional.empty();
    List<Statement> swapStatements = body.subList(innerPosition + 1, body.size());
    var swap = swapStatements.size() == 3
        ? ArrayEffects.temporarySwap(swapStatements, array, pass, selectedIndex)
        : ArrayEffects.helperSwap(swapStatements.get(0), array, pass, selectedIndex);
    if (swap.isEmpty()) return Optional.empty();
    Node method = LocalVariableBindings.callable(node);
    Map<String, String> roles = new LinkedHashMap<>();
    roles.put("array", array);
    roles.put("pass", pass);
    roles.put("scan", scan);
    roles.put("selected", selectedIndex);
    if (swap.get().helperMethodAstNodeId() == null) roles.put("temp", swap.get().tempName());
    var bound = LocalVariableBindings.roles(node, roles);
    if (method == null
        || bound.isEmpty()
        || !SortAstSupport.intArray(method, bound.get().get("array"))
        || !outer.get().counterDeclarationId().equals(bound.get().get("pass"))
        || !inner.get().counterDeclarationId().equals(bound.get().get("scan")))
      return Optional.empty();
    var ids = bound.get();
    var range = node.getRange().orElseThrow();
    boolean ascending = direct == (comparison.getOperator() == BinaryExpr.Operator.LESS);
    Map<String, String> operationIds = new LinkedHashMap<>();
    operationIds.put("selectedIndexUpdate", AstAnalyzer.nodeId(SortAstSupport.single(branch.getThenStmt())));
    if (swap.get().helperMethodAstNodeId() != null) {
      operationIds.put("swapCall", swap.get().callAstNodeId());
      operationIds.put("swapHelper", swap.get().helperMethodAstNodeId());
    }
    var pattern =
        new SortPattern(
            1,
            ascending ? SortPattern.Direction.ASCENDING : SortPattern.Direction.DESCENDING,
            AstAnalyzer.nodeId(innerNode),
            AstAnalyzer.nodeId(comparison),
            AstAnalyzer.nodeId(branch),
            swap.get().operationAstNodeIds(),
            new SortPattern.Operand(ids.get("scan"), 0),
            new SortPattern.Operand(ids.get("selected"), 0),
            operationIds);
    return Optional.of(
        new AlgorithmHint(
            "selection-sort",
            0.96,
            "array-sort",
            List.of(
                "An outer pass initializes a selected index from the current boundary",
                "An inner forward scan compares array values and conditionally replaces that index",
                "The pass ends with a complete three-statement temporary swap"),
            roles,
            range.begin.line,
            range.end.line,
            AstAnalyzer.nodeId(node),
            AstAnalyzer.nodeId(method),
            ids,
            pattern));
  }
}
