package com.aisc.algoviz.ast.engine.algorithm.sort;

import com.aisc.algoviz.ast.engine.algorithm.LocalVariableBindings;
import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.aisc.algoviz.ast.engine.model.MergePattern;
import com.aisc.algoviz.ast.engine.model.SortPattern.Direction;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
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

/** Complete top-down Merge Sort with inline auxiliary writes and copy-back. */
public final class MergeSortDetector implements SortPatternDetector {
  @Override
  public Optional<AlgorithmHint> detect(Node node) {
    if (!(node instanceof WhileStmt mergeLoop)) return Optional.empty();
    MethodDeclaration method = node.findAncestor(MethodDeclaration.class).orElse(null);
    ClassOrInterfaceDeclaration owner =
        method == null ? null : method.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null);
    if (method == null
        || owner == null
        || method.getBody().isEmpty()
        || method.getParameters().size() != 3
        || (!method.isPrivate() && !method.isStatic() && !method.isFinal() && !owner.isFinal())
        || !method.getType().isVoidType()
        || !method.getParameter(0).getType().asString().equals("int[]")
        || !method.getParameter(1).getType().asString().equals("int")
        || !method.getParameter(2).getType().asString().equals("int")) return Optional.empty();
    String array = method.getParameter(0).getNameAsString();
    String low = method.getParameter(1).getNameAsString();
    String high = method.getParameter(2).getNameAsString();
    var recursiveBody = SortAstSupport.recursiveBody(method, low, high);
    if (recursiveBody.isEmpty()) return Optional.empty();
    List<Statement> body = recursiveBody.get().statements();
    if ((body.size() != 12 && body.size() != 13) || body.get(8) != mergeLoop)
      return Optional.empty();
    IfStmt guard = recursiveBody.get().guard();
    var mid = declaration(body.get(1), "int");
    var aux = declaration(body.get(4), "int[]");
    var left = declaration(body.get(5), "int");
    var right = declaration(body.get(6), "int");
    var write = declaration(body.get(7), "int");
    if (mid.isEmpty() || aux.isEmpty() || left.isEmpty() || right.isEmpty() || write.isEmpty())
      return Optional.empty();
    String midName = mid.get().getNameAsString();
    String auxName = aux.get().getNameAsString();
    String leftName = left.get().getNameAsString();
    String rightName = right.get().getNameAsString();
    String writeName = write.get().getNameAsString();
    if (!(SortAstSupport.matches(
                mid.get().getInitializer().orElse(null),
                low + " + (" + high + " - " + low + ") / 2")
            || SortAstSupport.matches(
                mid.get().getInitializer().orElse(null), "(" + low + " + " + high + ") / 2"))
        || !recursive(body.get(2), method, array, low, midName)
        || !recursive(body.get(3), method, array, midName + " + 1", high)
        || !(aux.get().getInitializer().orElse(null) instanceof ArrayCreationExpr allocation)
        || !allocation.getElementType().asString().equals("int")
        || allocation.getLevels().size() != 1
        || allocation.getLevels().get(0).getDimension().isEmpty()
        || !SortAstSupport.matches(
            allocation.getLevels().get(0).getDimension().get(), high + " - " + low + " + 1")
        || !SortAstSupport.matches(left.get().getInitializer().orElse(null), low)
        || !SortAstSupport.matches(right.get().getInitializer().orElse(null), midName + " + 1")
        || !SortAstSupport.matches(write.get().getInitializer().orElse(null), "0"))
      return Optional.empty();
    if (!SortAstSupport.matches(
            mergeLoop.getCondition(),
            leftName + " <= " + midName + " && " + rightName + " <= " + high)
        || !mergeLoop.getBody().isBlockStmt()
        || mergeLoop.getBody().asBlockStmt().getStatements().size() != 2
        || !(mergeLoop.getBody().asBlockStmt().getStatement(0) instanceof IfStmt choice)
        || choice.getElseStmt().isEmpty()
        || !(SortAstSupport.unwrap(choice.getCondition()) instanceof BinaryExpr comparison))
      return Optional.empty();
    Direction direction =
        SortAstSupport.relationDirection(
                comparison, array + "[" + leftName + "]", array + "[" + rightName + "]")
            .orElse(null);
    if (direction == null) return Optional.empty();
    List<Statement> leftBranch = statements(choice.getThenStmt());
    List<Statement> rightBranch = statements(choice.getElseStmt().orElseThrow());
    if (leftBranch.size() != 2
        || rightBranch.size() != 2
        || !SortAstSupport.assignment(
            leftBranch.get(0), auxName + "[" + writeName + "]", array + "[" + leftName + "]")
        || !increments(leftBranch.get(1), leftName)
        || !SortAstSupport.assignment(
            rightBranch.get(0), auxName + "[" + writeName + "]", array + "[" + rightName + "]")
        || !increments(rightBranch.get(1), rightName)
        || !increments(mergeLoop.getBody().asBlockStmt().getStatement(1), writeName))
      return Optional.empty();
    if (!(body.get(9) instanceof WhileStmt leftRemainder)
        || !(body.get(10) instanceof WhileStmt rightRemainder)
        || !remainder(leftRemainder, leftName, midName, array, auxName, writeName)
        || !remainder(rightRemainder, rightName, high, array, auxName, writeName))
      return Optional.empty();
    Node copyNode = body.get(body.size() - 1);
    if ((body.size() == 12 && !(copyNode instanceof ForStmt))
        || (body.size() == 13 && !(copyNode instanceof WhileStmt))) return Optional.empty();
    var copy = CountedLoop.match(copyNode, low);
    if (copy.isEmpty() || copy.get().bodyStatements().size() != 1) return Optional.empty();
    String index = copy.get().counter();
    if (!(SortAstSupport.matches(copy.get().condition(), index + " <= " + high)
            || SortAstSupport.matches(copy.get().condition(), index + " < " + high + " + 1"))
        || !SortAstSupport.assignment(
            copy.get().bodyStatements().get(0),
            array + "[" + index + "]",
            auxName + "[" + index + " - " + low + "]")) return Optional.empty();
    Map<String, String> roles =
        Map.of(
            "array", array, "low", low, "high", high, "mid", midName, "aux", auxName, "left",
            leftName, "right", rightName, "write", writeName, "index", index);
    var ids = LocalVariableBindings.roles(method, roles);
    if (ids.isEmpty()
        || !copy.get().counterDeclarationId().equals(ids.get().get("index"))
        || !AstAnalyzer.nodeId(mid.get()).equals(ids.get().get("mid"))
        || !AstAnalyzer.nodeId(aux.get()).equals(ids.get().get("aux"))) return Optional.empty();
    Map<String, String> operations = new LinkedHashMap<>();
    operations.put("guard", AstAnalyzer.nodeId(guard));
    operations.put("split", AstAnalyzer.nodeId(body.get(1)));
    operations.put("leftCall", AstAnalyzer.nodeId(body.get(2)));
    operations.put("rightCall", AstAnalyzer.nodeId(body.get(3)));
    operations.put("auxAllocation", AstAnalyzer.nodeId(body.get(4)));
    operations.put("comparison", AstAnalyzer.nodeId(comparison));
    operations.put("leftWrite", AstAnalyzer.nodeId(leftBranch.get(0)));
    operations.put("rightWrite", AstAnalyzer.nodeId(rightBranch.get(0)));
    operations.put(
        "leftRemainderWrite", AstAnalyzer.nodeId(statements(leftRemainder.getBody()).get(0)));
    operations.put(
        "rightRemainderWrite", AstAnalyzer.nodeId(statements(rightRemainder.getBody()).get(0)));
    operations.put("copyBackLoop", AstAnalyzer.nodeId(copyNode));
    operations.put("copyBackWrite", AstAnalyzer.nodeId(copy.get().bodyStatements().get(0)));
    var range = method.getRange().orElseThrow();
    return Optional.of(
        new AlgorithmHint(
            "merge-sort",
            0.97,
            "merge-auxiliary",
            List.of(
                "Inclusive recursive split covers both halves",
                "Every selected and remaining value enters the auxiliary array",
                "Complete auxiliary range copies back to the source array"),
            roles,
            range.begin.line,
            range.end.line,
            AstAnalyzer.nodeId(node),
            AstAnalyzer.nodeId(method),
            ids.get(),
            null,
            null,
            new MergePattern(1, direction, operations)));
  }

  private static Optional<VariableDeclarator> declaration(Statement statement, String type) {
    if (!(statement instanceof ExpressionStmt expression)
        || !(expression.getExpression() instanceof VariableDeclarationExpr declaration)
        || declaration.getVariables().size() != 1
        || !declaration.getVariable(0).getType().asString().equals(type)) return Optional.empty();
    return Optional.of(declaration.getVariable(0));
  }

  private static boolean recursive(
      Statement statement, MethodDeclaration method, String array, String first, String second) {
    if (!(statement instanceof ExpressionStmt expression)
        || !(expression.getExpression() instanceof MethodCallExpr call)
        || !SortAstSupport.sameClassRecursiveCall(call, method)) return false;
    return SortAstSupport.matches(call.getArgument(0), array)
        && SortAstSupport.matches(call.getArgument(1), first)
        && SortAstSupport.matches(call.getArgument(2), second);
  }

  private static boolean remainder(
      WhileStmt loop, String cursor, String bound, String array, String aux, String write) {
    List<Statement> statements = statements(loop.getBody());
    return SortAstSupport.matches(loop.getCondition(), cursor + " <= " + bound)
        && statements.size() == 3
        && SortAstSupport.assignment(
            statements.get(0), aux + "[" + write + "]", array + "[" + cursor + "]")
        && increments(statements.get(1), cursor)
        && increments(statements.get(2), write);
  }

  private static List<Statement> statements(Statement statement) {
    return statement.isBlockStmt()
        ? List.copyOf(statement.asBlockStmt().getStatements())
        : List.of(statement);
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
}
