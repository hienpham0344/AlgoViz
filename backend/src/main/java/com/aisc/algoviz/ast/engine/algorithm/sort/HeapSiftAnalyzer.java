package com.aisc.algoviz.ast.engine.algorithm.sort;

import com.aisc.algoviz.ast.engine.algorithm.LocalVariableBindings;
import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.aisc.algoviz.ast.engine.model.SortPattern.Direction;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;
import java.util.List;
import java.util.Optional;

/** Proves a bounded family of heap restoration bodies without relying on method names. */
final class HeapSiftAnalyzer {
  private HeapSiftAnalyzer() {}

  enum Kind {
    DOWN,
    UP
  }

  record HeapSiftProof(
      Kind kind,
      String arrayDeclarationId,
      String boundaryDeclarationId,
      String nodeDeclarationId,
      List<String> operationAstNodeIds,
      String helperMethodAstNodeId,
      String callAstNodeId,
      boolean inclusiveBoundary) {}

  static Optional<HeapSiftProof> prove(
      Node candidate,
      MethodDeclaration caller,
      String arrayId,
      Direction direction,
      String boundaryId) {
    if (candidate instanceof MethodCallExpr call) {
      return helper(call, caller, arrayId, direction, boundaryId);
    }
    if (!(candidate instanceof BlockStmt body) || caller.getParameters().size() != 3)
      return Optional.empty();
    Parameter array = parameter(caller, arrayId);
    Parameter boundary = parameter(caller, boundaryId);
    if (array == null
        || boundary == null
        || !array.getType().asString().equals("int[]")
        || !boundary.getType().asString().equals("int")) return Optional.empty();
    Parameter node =
        caller.getParameters().stream()
            .filter(p -> p != array && p != boundary && p.getType().asString().equals("int"))
            .findFirst()
            .orElse(null);
    if (node == null) return Optional.empty();
    return body(body, caller, array, boundary, node, direction, null, null);
  }

  static Optional<HeapSiftProof> proveInline(
      WhileStmt loop,
      MethodDeclaration method,
      String arrayId,
      String boundaryId,
      String nodeId,
      Direction direction) {
    String array = name(method, arrayId);
    String boundary = name(method, boundaryId);
    String node = name(method, nodeId);
    if (array == null || boundary == null || node == null || !distinct(array, boundary, node))
      return Optional.empty();
    Optional<Pattern> pattern =
        iterativeDownLoop(loop, array, boundary, node, direction, false)
            .or(() -> iterativeDownLoop(loop, array, boundary, node, direction, true))
            .or(() -> iterativeUpLoop(loop, array, boundary, node, direction));
    if (pattern.isEmpty()
        || LocalVariableBindings.roles(
                loop,
                pattern.get().kind() == Kind.UP
                    ? java.util.Map.of("array", array, "node", node)
                    : java.util.Map.of("array", array, "boundary", boundary, "node", node))
            .filter(
                ids ->
                    ids.get("array").equals(arrayId)
                        && ids.get("node").equals(nodeId)
                        && (pattern.get().kind() == Kind.UP
                            || ids.get("boundary").equals(boundaryId)))
            .isEmpty()) return Optional.empty();
    return Optional.of(
        new HeapSiftProof(
            pattern.get().kind(),
            arrayId,
            boundaryId,
            nodeId,
            pattern.get().operations(),
            null,
            AstAnalyzer.nodeId(loop),
            pattern.get().inclusiveBoundary()));
  }

  private static Optional<HeapSiftProof> helper(
      MethodCallExpr call,
      MethodDeclaration caller,
      String arrayId,
      Direction direction,
      String boundaryId) {
    var owner = caller.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null);
    if (owner == null
        || call.getArguments().size() != 3
        || call.getScope()
            .filter(
                scope ->
                    !(scope instanceof ThisExpr)
                        && !(scope instanceof NameExpr name
                            && caller.isStatic()
                            && SortNameResolution.unshadowedClassName(name, owner)))
            .isPresent()) return Optional.empty();
    List<MethodDeclaration> methods =
        owner.getMethodsByName(call.getNameAsString()).stream()
            .filter(
                m ->
                    m.getParameters().size() == 3
                        && m.getType().isVoidType()
                        && m.getBody().isPresent())
            .toList();
    if (methods.size() != 1) return Optional.empty();
    MethodDeclaration method = methods.get(0);
    if (!method.isStatic() && !method.isPrivate() && !method.isFinal() && !owner.isFinal())
      return Optional.empty();
    int arrayPosition = -1;
    int boundaryPosition = -1;
    boolean subtractsOne = false;
    for (int i = 0; i < 3; i++) {
      Expression argument = SortAstSupport.unwrap(call.getArgument(i));
      if (argument instanceof NameExpr name) {
        String id = LocalVariableBindings.resolve(name).orElse(null);
        if (arrayId.equals(id) && HeapIndexExpressions.arrayDeclarationId(name).isPresent())
          arrayPosition = i;
        if (boundaryId.equals(id)) boundaryPosition = i;
      } else if (argument instanceof BinaryExpr binary
          && binary.getLeft() instanceof NameExpr name
          && LocalVariableBindings.resolve(name).filter(boundaryId::equals).isPresent()
          && SortAstSupport.matches(argument, name.getNameAsString() + " - 1")) {
        boundaryPosition = i;
        subtractsOne = true;
      }
    }
    if (arrayPosition < 0 || boundaryPosition < 0 || arrayPosition == boundaryPosition)
      return Optional.empty();
    int nodePosition = 3 - arrayPosition - boundaryPosition;
    Parameter array = method.getParameter(arrayPosition);
    Parameter boundary = method.getParameter(boundaryPosition);
    Parameter node = method.getParameter(nodePosition);
    if (!array.getType().asString().equals("int[]")
        || !boundary.getType().asString().equals("int")
        || !node.getType().asString().equals("int")) return Optional.empty();
    boolean inclusiveArgument = subtractsOne;
    return body(
            method.getBody().orElseThrow(),
            method,
            array,
            boundary,
            node,
            direction,
            AstAnalyzer.nodeId(method),
            AstAnalyzer.nodeId(call))
        .filter(proof -> proof.inclusiveBoundary() == inclusiveArgument);
  }

  private static Optional<HeapSiftProof> body(
      BlockStmt body,
      MethodDeclaration method,
      Parameter array,
      Parameter boundary,
      Parameter node,
      Direction direction,
      String helperId,
      String callId) {
    String a = array.getNameAsString();
    String b = boundary.getNameAsString();
    String n = node.getNameAsString();
    Optional<Pattern> pattern =
        iterativeDown(body, a, b, n, direction, false)
            .or(() -> iterativeDown(body, a, b, n, direction, true))
            .or(() -> recursiveDown(body, method, a, b, n, direction, false))
            .or(() -> recursiveDown(body, method, a, b, n, direction, true))
            .or(() -> iterativeUp(body, a, b, n, direction));
    java.util.Map<String, String> names =
        pattern.isPresent() && pattern.get().kind() == Kind.UP
            ? java.util.Map.of("array", a, "node", n)
            : java.util.Map.of("array", a, "boundary", b, "node", n);
    if (pattern.isEmpty()
        || LocalVariableBindings.roles(body, names)
            .filter(
                ids ->
                    ids.get("array").equals(AstAnalyzer.nodeId(array))
                        && (pattern.get().kind() == Kind.UP
                            || ids.get("boundary").equals(AstAnalyzer.nodeId(boundary)))
                        && ids.get("node").equals(AstAnalyzer.nodeId(node)))
            .isEmpty()) return Optional.empty();
    return Optional.of(
        new HeapSiftProof(
            pattern.get().kind(),
            AstAnalyzer.nodeId(array),
            AstAnalyzer.nodeId(boundary),
            AstAnalyzer.nodeId(node),
            pattern.get().operations(),
            helperId,
            callId,
            pattern.get().inclusiveBoundary()));
  }

  private record Pattern(Kind kind, List<String> operations, boolean inclusiveBoundary) {}

  private static Optional<Pattern> iterativeDown(
      BlockStmt body, String a, String b, String n, Direction direction, boolean inclusive) {
    if (body.getStatements().size() != 1) return Optional.empty();
    return iterativeDownLoop(body.getStatement(0), a, b, n, direction, inclusive);
  }

  private static Optional<Pattern> iterativeDownLoop(
      Statement loop, String a, String b, String n, Direction direction, boolean inclusive) {
    Expression condition;
    Statement loopBody;
    if (loop instanceof WhileStmt whileLoop) {
      condition = whileLoop.getCondition();
      loopBody = whileLoop.getBody();
    } else if (loop instanceof ForStmt forLoop
        && forLoop.getInitialization().isEmpty()
        && forLoop.getUpdate().isEmpty()
        && forLoop.getCompare().isPresent()) {
      condition = forLoop.getCompare().orElseThrow();
      loopBody = forLoop.getBody();
    } else return Optional.empty();
    if (!(loopBody instanceof BlockStmt block) || block.getStatements().size() != 9)
      return Optional.empty();
    List<Statement> s = block.getStatements();
    String left = declaredName(s.get(0), "int");
    String right = declaredName(s.get(1), "int");
    String chosen = declaredName(s.get(2), "int");
    if (left == null
        || right == null
        || chosen == null
        || !distinct(n, left, right, chosen)
        || !HeapIndexExpressions.child(initializer(s.get(0)), n, null)
            .filter(side -> side == HeapIndexExpressions.ChildSide.LEFT)
            .isPresent()
        || !HeapIndexExpressions.child(initializer(s.get(1)), n, left)
            .filter(side -> side == HeapIndexExpressions.ChildSide.RIGHT)
            .isPresent()
        || !SortAstSupport.matches(initializer(s.get(2)), left)
        || !withinLeftChild(condition, n, b, inclusive)) return Optional.empty();
    if (!(s.get(3) instanceof IfStmt select)
        || select.getElseStmt().isPresent()
        || !(select.getCondition() instanceof BinaryExpr and)
        || and.getOperator() != BinaryExpr.Operator.AND
        || !within(and.getLeft(), right, b, inclusive)
        || !comparison(and.getRight(), a + "[" + right + "]", a + "[" + left + "]", direction, true)
        || !SortAstSupport.assignment(SortAstSupport.single(select.getThenStmt()), chosen, right))
      return Optional.empty();
    if (!(s.get(4) instanceof IfStmt stop)
        || stop.getElseStmt().isPresent()
        || !(SortAstSupport.single(stop.getThenStmt()) instanceof BreakStmt)
        || !comparison(
            stop.getCondition(), a + "[" + n + "]", a + "[" + chosen + "]", direction, false))
      return Optional.empty();
    var swap = ArrayEffects.temporarySwap(s.subList(5, 8), a, n, chosen);
    if (swap.isEmpty() || !SortAstSupport.assignment(s.get(8), n, chosen)) return Optional.empty();
    return Optional.of(new Pattern(Kind.DOWN, swap.get().operationAstNodeIds(), inclusive));
  }

  private static Optional<Pattern> recursiveDown(
      BlockStmt body,
      MethodDeclaration method,
      String a,
      String b,
      String n,
      Direction direction,
      boolean inclusive) {
    List<Statement> s = body.getStatements();
    if (s.size() != 6) return Optional.empty();
    String left = declaredName(s.get(0), "int");
    String right = declaredName(s.get(1), "int");
    String chosen = declaredName(s.get(2), "int");
    if (left == null
        || right == null
        || chosen == null
        || !distinct(n, left, right, chosen)
        || !HeapIndexExpressions.child(initializer(s.get(0)), n, null)
            .filter(side -> side == HeapIndexExpressions.ChildSide.LEFT)
            .isPresent()
        || !HeapIndexExpressions.child(initializer(s.get(1)), n, left)
            .filter(side -> side == HeapIndexExpressions.ChildSide.RIGHT)
            .isPresent()
        || !SortAstSupport.matches(initializer(s.get(2)), n)) return Optional.empty();
    if (!childSelection(s.get(3), left, b, a, chosen, direction, inclusive)
        || !childSelection(s.get(4), right, b, a, chosen, direction, inclusive)
        || !(s.get(5) instanceof IfStmt changed)
        || changed.getElseStmt().isPresent()
        || !(changed.getThenStmt() instanceof BlockStmt branch)
        || branch.getStatements().size() != 4
        || !(SortAstSupport.matches(changed.getCondition(), chosen + " != " + n)
            || SortAstSupport.matches(changed.getCondition(), n + " != " + chosen)))
      return Optional.empty();
    var swap = ArrayEffects.temporarySwap(branch.getStatements().subList(0, 3), a, n, chosen);
    if (swap.isEmpty()
        || !(branch.getStatement(3) instanceof ExpressionStmt recurse)
        || !(recurse.getExpression() instanceof MethodCallExpr call)
        || !SortAstSupport.sameClassRecursiveCall(call, method)
        || call.getArguments().size() != 3
        || !SortAstSupport.matches(call.getArgument(0), a)
        || !SortAstSupport.matches(call.getArgument(1), b)
        || !SortAstSupport.matches(call.getArgument(2), chosen)) return Optional.empty();
    return Optional.of(new Pattern(Kind.DOWN, swap.get().operationAstNodeIds(), inclusive));
  }

  private static Optional<Pattern> iterativeUp(
      BlockStmt body, String a, String b, String n, Direction direction) {
    if (body.getStatements().size() != 1) return Optional.empty();
    return iterativeUpLoop(body.getStatement(0), a, b, n, direction);
  }

  private static Optional<Pattern> iterativeUpLoop(
      Statement statement, String a, String b, String n, Direction direction) {
    if (!(statement instanceof WhileStmt loop)
        || !(loop.getBody() instanceof BlockStmt block)
        || block.getStatements().size() != 6
        || !(SortAstSupport.matches(loop.getCondition(), n + " > 0")
            || SortAstSupport.matches(loop.getCondition(), "0 < " + n))) return Optional.empty();
    List<Statement> s = block.getStatements();
    String parent = declaredName(s.get(0), "int");
    if (parent == null
        || parent.equals(n)
        || !HeapIndexExpressions.parent(initializer(s.get(0)), n)
        || !(s.get(1) instanceof IfStmt stop)
        || stop.getElseStmt().isPresent()
        || !(SortAstSupport.single(stop.getThenStmt()) instanceof BreakStmt)
        || !comparison(
            stop.getCondition(), a + "[" + parent + "]", a + "[" + n + "]", direction, false))
      return Optional.empty();
    var swap = ArrayEffects.temporarySwap(s.subList(2, 5), a, parent, n);
    if (swap.isEmpty() || !SortAstSupport.assignment(s.get(5), n, parent)) return Optional.empty();
    return Optional.of(new Pattern(Kind.UP, swap.get().operationAstNodeIds(), false));
  }

  private static boolean childSelection(
      Statement statement,
      String child,
      String bound,
      String array,
      String chosen,
      Direction direction,
      boolean inclusive) {
    if (!(statement instanceof IfStmt select)
        || select.getElseStmt().isPresent()
        || !(select.getCondition() instanceof BinaryExpr and)
        || and.getOperator() != BinaryExpr.Operator.AND) return false;
    return within(and.getLeft(), child, bound, inclusive)
        && comparison(
            and.getRight(), array + "[" + child + "]", array + "[" + chosen + "]", direction, true)
        && SortAstSupport.assignment(SortAstSupport.single(select.getThenStmt()), chosen, child);
  }

  private static boolean comparison(
      Expression expression, String first, String second, Direction sortDirection, boolean strict) {
    if (!(expression instanceof BinaryExpr binary)) return false;
    Direction expected =
        sortDirection == Direction.ASCENDING ? Direction.DESCENDING : Direction.ASCENDING;
    if (SortAstSupport.relationDirection(binary, first, second).filter(expected::equals).isEmpty())
      return false;
    return switch (binary.getOperator()) {
      case GREATER, LESS -> strict;
      case GREATER_EQUALS, LESS_EQUALS -> !strict;
      default -> false;
    };
  }

  private static boolean within(
      Expression expression, String left, String right, boolean inclusive) {
    return SortAstSupport.matches(expression, left + (inclusive ? " <= " : " < ") + right)
        || SortAstSupport.matches(expression, right + (inclusive ? " >= " : " > ") + left);
  }

  private static boolean withinLeftChild(
      Expression expression, String parent, String boundary, boolean inclusive) {
    if (!(expression instanceof BinaryExpr binary)) return false;
    boolean direct =
        SortAstSupport.matches(binary.getRight(), boundary)
            && HeapIndexExpressions.child(binary.getLeft(), parent, null)
                .filter(side -> side == HeapIndexExpressions.ChildSide.LEFT)
                .isPresent();
    boolean reversed =
        SortAstSupport.matches(binary.getLeft(), boundary)
            && HeapIndexExpressions.child(binary.getRight(), parent, null)
                .filter(side -> side == HeapIndexExpressions.ChildSide.LEFT)
                .isPresent();
    return direct
            && binary.getOperator()
                == (inclusive ? BinaryExpr.Operator.LESS_EQUALS : BinaryExpr.Operator.LESS)
        || reversed
            && binary.getOperator()
                == (inclusive ? BinaryExpr.Operator.GREATER_EQUALS : BinaryExpr.Operator.GREATER);
  }

  private static boolean distinct(String... names) {
    return java.util.Arrays.stream(names).distinct().count() == names.length;
  }

  private static Parameter parameter(MethodDeclaration method, String id) {
    return method.getParameters().stream()
        .filter(p -> AstAnalyzer.nodeId(p).equals(id))
        .findFirst()
        .orElse(null);
  }

  private static String name(MethodDeclaration method, String id) {
    Parameter parameter = parameter(method, id);
    if (parameter != null) return parameter.getNameAsString();
    return method.findAll(VariableDeclarator.class).stream()
        .filter(variable -> AstAnalyzer.nodeId(variable).equals(id))
        .map(VariableDeclarator::getNameAsString)
        .findFirst()
        .orElse(null);
  }

  private static String declaredName(Statement statement, String type) {
    if (!(statement instanceof ExpressionStmt expression)
        || !(expression.getExpression() instanceof VariableDeclarationExpr declaration)
        || declaration.getVariables().size() != 1
        || !declaration.getVariable(0).getType().asString().equals(type)) return null;
    return declaration.getVariable(0).getNameAsString();
  }

  private static Expression initializer(Statement statement) {
    if (!(statement instanceof ExpressionStmt expression)
        || !(expression.getExpression() instanceof VariableDeclarationExpr declaration)
        || declaration.getVariables().size() != 1) return null;
    return declaration.getVariable(0).getInitializer().orElse(null);
  }
}
