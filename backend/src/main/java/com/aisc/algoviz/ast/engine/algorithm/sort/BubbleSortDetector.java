package com.aisc.algoviz.ast.engine.algorithm.sort;

import com.aisc.algoviz.ast.engine.algorithm.LocalVariableBindings;
import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.aisc.algoviz.ast.engine.model.SortPattern;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Recognizes complete adjacent-swap passes with lexically bound local roles. */
public final class BubbleSortDetector implements SortPatternDetector {
  public BubbleSortDetector() {}

  @Override
  public Optional<AlgorithmHint> detect(Node node) {
    var outer = CountedLoop.match(node, "0");
    if (outer.isEmpty()) return Optional.empty();
    String pass = outer.get().counter();
    BubbleBody body = nestedLoop(outer.get().bodyStatements());
    if (body == null) return Optional.empty();
    Node innerNode = body.inner();
    var inner = CountedLoop.match(innerNode, "0");
    if (inner.isEmpty() || inner.get().counter().equals(pass)) return Optional.empty();
    String scan = inner.get().counter();
    if (inner.get().bodyStatements().size() != 1
        || !(inner.get().bodyStatements().get(0) instanceof IfStmt branch)
        || branch.getElseStmt().isPresent()
        || !(SortAstSupport.unwrap(branch.getCondition()) instanceof BinaryExpr comparison))
      return Optional.empty();
    if (comparison.getOperator() != BinaryExpr.Operator.GREATER
        && comparison.getOperator() != BinaryExpr.Operator.LESS) return Optional.empty();
    if (!(SortAstSupport.unwrap(comparison.getLeft()) instanceof ArrayAccessExpr first)
        || !(SortAstSupport.unwrap(first.getName()) instanceof NameExpr arrayRef))
      return Optional.empty();
    String array = arrayRef.getNameAsString();
    boolean direct =
        SortAstSupport.matches(first, array + "[" + scan + "]")
            && SortAstSupport.matches(comparison.getRight(), array + "[" + scan + " + 1]");
    boolean reversed =
        SortAstSupport.matches(first, array + "[" + scan + " + 1]")
            && SortAstSupport.matches(comparison.getRight(), array + "[" + scan + "]");
    if (!direct && !reversed) return Optional.empty();
    if (!IndexExpression.sameCondition(
            outer.get().condition(),
            StaticJavaParser.parseExpression(pass + " < " + array + ".length - 1")))
      return Optional.empty();
    boolean shrinking =
        IndexExpression.sameCondition(
            inner.get().condition(),
            StaticJavaParser.parseExpression(scan + " < " + array + ".length - 1 - " + pass));
    boolean full =
        IndexExpression.sameCondition(
            inner.get().condition(),
            StaticJavaParser.parseExpression(scan + " < " + array + ".length - 1"));
    if (!shrinking && !full) return Optional.empty();

    Statement then = branch.getThenStmt();
    List<Statement> swapStatements =
        then instanceof BlockStmt block ? List.copyOf(block.getStatements()) : List.of(then);
    if (body.flag() != null) {
      if (swapStatements.size() != 4
          || !SortAstSupport.assignment(swapStatements.get(3), body.flag(), "true"))
        return Optional.empty();
      swapStatements = swapStatements.subList(0, 3);
    }
    Optional<ArrayEffects.Swap> swap =
        ArrayEffects.temporarySwap(swapStatements, array, scan, scan + " + 1");
    if (swap.isEmpty() && swapStatements.size() == 1)
      swap = ArrayEffects.helperSwap(swapStatements.get(0), array, scan, scan + " + 1");
    if (swap.isEmpty()) return Optional.empty();

    Node method = LocalVariableBindings.callable(node);
    Map<String, String> roles = new LinkedHashMap<>();
    roles.put("array", array);
    roles.put("pass", pass);
    roles.put("scan", scan);
    if (body.flag() != null) roles.put("swapped", body.flag());
    if (swap.get().helperMethodAstNodeId() == null) roles.put("temp", swap.get().tempName());
    var bound = LocalVariableBindings.roles(node, roles);
    if (method == null
        || bound.isEmpty()
        || !SortAstSupport.intArray(method, bound.get().get("array"))) return Optional.empty();
    if (!outer.get().counterDeclarationId().equals(bound.get().get("pass"))
        || !inner.get().counterDeclarationId().equals(bound.get().get("scan")))
      return Optional.empty();
    var range = node.getRange().orElseThrow();
    Map<String, String> operationIds =
        swap.get().helperMethodAstNodeId() == null
            ? Map.of()
            : Map.of(
                "swapCall", swap.get().callAstNodeId(),
                "swapHelper", swap.get().helperMethodAstNodeId());
    boolean ascending =
        direct == (comparison.getOperator() == BinaryExpr.Operator.GREATER);
    var sort =
        new SortPattern(
            1,
            ascending ? SortPattern.Direction.ASCENDING : SortPattern.Direction.DESCENDING,
            AstAnalyzer.nodeId(innerNode),
            AstAnalyzer.nodeId(comparison),
            AstAnalyzer.nodeId(branch),
            swap.get().operationAstNodeIds(),
            new SortPattern.Operand(bound.get().get("scan"), 0),
            new SortPattern.Operand(bound.get().get("scan"), 1),
            operationIds);
    return Optional.of(
        new AlgorithmHint(
            "bubble-sort",
            0.95,
            "array-sort",
            List.of(
                "Nested forward loops compare adjacent array elements",
                "Strict comparison guards a complete temporary swap",
                "Array and counters resolve to local or parameter declarations"),
            roles,
            range.begin.line,
            range.end.line,
            AstAnalyzer.nodeId(node),
            AstAnalyzer.nodeId(method),
            bound.get(),
            sort));
  }

  private static BubbleBody nestedLoop(List<Statement> statements) {
    if (statements.size() == 1
        && (statements.get(0) instanceof ForStmt || statements.get(0) instanceof WhileStmt))
      return new BubbleBody(statements.get(0), null);
    if (statements.size() == 2
        && statements.get(0) instanceof ExpressionStmt expression
        && expression.getExpression() instanceof VariableDeclarationExpr
        && statements.get(1) instanceof WhileStmt)
      return new BubbleBody(statements.get(1), null);
    if ((statements.size() == 3 || statements.size() == 4)
        && statements.get(0) instanceof ExpressionStmt expression
        && expression.getExpression() instanceof VariableDeclarationExpr declaration
        && declaration.getVariables().size() == 1
        && declaration.getVariable(0).getType().asString().equals("boolean")
        && SortAstSupport.matches(declaration.getVariable(0).getInitializer().orElse(null), "false")
        && (statements.get(statements.size() - 2) instanceof ForStmt
            || statements.get(statements.size() - 2) instanceof WhileStmt)
        && statements.get(statements.size() - 1) instanceof IfStmt stop
        && stop.getElseStmt().isEmpty()
        && SortAstSupport.matches(
            stop.getCondition(), "!" + declaration.getVariable(0).getNameAsString())
        && SortAstSupport.single(stop.getThenStmt()) instanceof BreakStmt) {
      if (statements.size() == 4) {
        if (!(statements.get(1) instanceof ExpressionStmt counter)
            || !(counter.getExpression() instanceof VariableDeclarationExpr scan)
            || scan.getVariables().size() != 1
            || !scan.getVariable(0).getType().asString().equals("int")
            || !SortAstSupport.matches(scan.getVariable(0).getInitializer().orElse(null), "0")
            || !(statements.get(2) instanceof WhileStmt)) return null;
      }
      return new BubbleBody(
          statements.get(statements.size() - 2), declaration.getVariable(0).getNameAsString());
    }
    return null;
  }

  private record BubbleBody(Node inner, String flag) {}
}
