package com.aisc.algoviz.ast.engine.algorithm.twopointers;

import com.aisc.algoviz.ast.engine.algorithm.LocalVariableBindings;
import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.aisc.algoviz.ast.engine.model.TwoPointersPattern;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Proves safe inward or forward progress for a conservative loop subset. */
public final class PointerProgressAnalyzer {
  public record Proof(TwoPointersPattern.Movement movement,
                      Map<String, String> operationAstNodeIds,
                      List<String> evidence) {
    public Proof {
      operationAstNodeIds = Map.copyOf(operationAstNodeIds);
      evidence = List.copyOf(evidence);
    }
  }

  private record Update(String declarationId, int delta, Node node, boolean header) {}
  private record Path(boolean first, boolean second, boolean stopped, boolean exitsLoop) {}

  public Optional<Proof> prove(Node loop, PointerRoleAnalyzer.Candidate candidate) {
    Expression guard = PointerRoleAnalyzer.condition(loop);
    if (guard == null || !(PointerRoleAnalyzer.unwrap(guard) instanceof BinaryExpr binary))
      return Optional.empty();
    if (loop instanceof DoStmt && !hasEntryGuard(loop, candidate)) return Optional.empty();
    List<Update> updates = updates(loop);
    List<Update> first = updates.stream()
        .filter(u -> u.declarationId().equals(candidate.firstDeclarationId())).toList();
    List<Update> second = updates.stream()
        .filter(u -> u.declarationId().equals(candidate.secondDeclarationId())).toList();
    if (first.isEmpty() || second.isEmpty()) return Optional.empty();
    if (first.stream().anyMatch(u -> !directUpdate(u))
        || second.stream().anyMatch(u -> !directUpdate(u) && !postfixWriteIndex(u))) return Optional.empty();
    boolean opposing = compares(binary, candidate.firstDeclarationId(),
        candidate.secondDeclarationId());
    if (opposing) {
      if (second.stream().anyMatch(u -> !directUpdate(u))
          || !initial(candidate.firstDeclarationId(), loop, candidate, false, false)
          || !initial(candidate.secondDeclarationId(), loop, candidate, true, false)
          || first.stream().anyMatch(u -> u.delta() != 1)
          || second.stream().anyMatch(u -> u.delta() != -1)
          || !allPathsProgress(loop, first, second, false)) return Optional.empty();
      return Optional.of(proof(TwoPointersPattern.Movement.OPPOSING, guard, first, second));
    }
    if (!bound(binary, candidate.firstDeclarationId(), candidate)
        || !initial(candidate.firstDeclarationId(), loop, candidate, false, true)
        || !initial(candidate.secondDeclarationId(), loop, candidate, false, false)
        || first.stream().anyMatch(u -> u.delta() != 1)
        || second.stream().anyMatch(u -> u.delta() != 1)
        || second.size() != 1 || second.get(0).header()
        || !conditionalSlowUpdate(loop, candidate, second.get(0))
        || !allPathsProgress(loop, first, second, true)) return Optional.empty();
    return Optional.of(proof(TwoPointersPattern.Movement.SAME_DIRECTION, guard, first, second));
  }

  private static Proof proof(TwoPointersPattern.Movement movement, Expression guard,
                             List<Update> first, List<Update> second) {
    Map<String, String> operations = new LinkedHashMap<>();
    operations.put("guard", AstAnalyzer.nodeId(guard));
    operations.put("firstUpdate", AstAnalyzer.nodeId(first.get(0).node()));
    operations.put("secondUpdate", AstAnalyzer.nodeId(second.get(0).node()));
    return new Proof(movement, operations,
        List.of("Two indexed positions belong to one input",
            "The loop guard and pointer updates establish " + movement.name() + " movement"));
  }

  private static boolean initial(String id, Node loop, PointerRoleAnalyzer.Candidate candidate,
                                 boolean end, boolean allowOne) {
    Node callable = LocalVariableBindings.callable(loop);
    if (callable == null) return false;
    for (VariableDeclarator variable : callable.findAll(VariableDeclarator.class)) {
      if (!AstAnalyzer.nodeId(variable).equals(id) || variable.getInitializer().isEmpty()) continue;
      Expression value = PointerRoleAnalyzer.unwrap(variable.getInitializer().get());
      if (mutatedBefore(callable, loop, id)) return false;
      if (!end) return value instanceof IntegerLiteralExpr literal
          && (literal.asNumber().intValue() == 0
              || allowOne && literal.asNumber().intValue() == 1);
      if (!(value instanceof BinaryExpr difference)
          || difference.getOperator() != BinaryExpr.Operator.MINUS
          || !one(difference.getRight())) return false;
      return length(difference.getLeft(), candidate);
    }
    return false;
  }

  private static boolean mutatedBefore(Node callable, Node loop, String id) {
    if (loop.getBegin().isEmpty()) return true;
    for (AssignExpr assignment : callable.findAll(AssignExpr.class)) {
      if (assignment.getTarget() instanceof NameExpr name
          && assignment.getBegin().isPresent()
          && assignment.getBegin().get().isBefore(loop.getBegin().get())
          && LocalVariableBindings.resolve(name).filter(id::equals).isPresent()) return true;
    }
    for (UnaryExpr update : callable.findAll(UnaryExpr.class)) {
      if (update.getExpression() instanceof NameExpr name
          && update.getBegin().isPresent()
          && update.getBegin().get().isBefore(loop.getBegin().get())
          && LocalVariableBindings.resolve(name).filter(id::equals).isPresent()) return true;
    }
    return false;
  }

  private static boolean directUpdate(Update update) {
    if (update.header()) return true;
    return update.node().getParentNode().filter(parent -> parent instanceof ExpressionStmt stmt
        && stmt.getExpression() == update.node()).isPresent();
  }

  private static boolean postfixWriteIndex(Update update) {
    if (!(update.node() instanceof UnaryExpr unary)
        || unary.getOperator() != UnaryExpr.Operator.POSTFIX_INCREMENT) return false;
    Node parent = unary.getParentNode().orElse(null);
    if (!(parent instanceof com.github.javaparser.ast.expr.ArrayAccessExpr access)
        || access.getIndex() != unary) return false;
    return access.getParentNode().filter(n -> n instanceof AssignExpr assignment
        && assignment.getTarget() == access && assignment.getOperator() == AssignExpr.Operator.ASSIGN).isPresent();
  }

  private static boolean conditionalSlowUpdate(Node loop,
                                               PointerRoleAnalyzer.Candidate candidate,
                                               Update update) {
    IfStmt branch = update.node().findAncestor(IfStmt.class).orElse(null);
    if (branch == null || !within(update.node(), branch.getThenStmt())
        || !within(branch, loop)) return false;
    if (initialValue(candidate.firstDeclarationId(), loop) == 1) return true;
    if (postfixWriteIndex(update)
        && indexed(branch.getCondition(), candidate.firstDeclarationId(), candidate)
        && !indexed(branch.getCondition(), candidate.secondDeclarationId(), candidate)) return true;
    return definitelyDifferentAtSameIndex(branch.getCondition(), candidate);
  }

  private static int initialValue(String id, Node loop) {
    Node callable = LocalVariableBindings.callable(loop);
    if (callable == null) return -1;
    return callable.findAll(VariableDeclarator.class).stream()
        .filter(variable -> AstAnalyzer.nodeId(variable).equals(id))
        .findFirst().flatMap(VariableDeclarator::getInitializer)
        .filter(IntegerLiteralExpr.class::isInstance)
        .map(IntegerLiteralExpr.class::cast).map(value -> value.asNumber().intValue())
        .orElse(-1);
  }

  private static boolean definitelyDifferentAtSameIndex(Expression expression,
                                                         PointerRoleAnalyzer.Candidate candidate) {
    Expression value = PointerRoleAnalyzer.unwrap(expression);
    if (!(value instanceof BinaryExpr binary)) return false;
    if (binary.getOperator() == BinaryExpr.Operator.AND)
      return definitelyDifferentAtSameIndex(binary.getLeft(), candidate)
          || definitelyDifferentAtSameIndex(binary.getRight(), candidate);
    if (binary.getOperator() != BinaryExpr.Operator.NOT_EQUALS) return false;
    return indexed(binary.getLeft(), candidate.firstDeclarationId(), candidate)
        && indexed(binary.getRight(), candidate.secondDeclarationId(), candidate)
        || indexed(binary.getLeft(), candidate.secondDeclarationId(), candidate)
            && indexed(binary.getRight(), candidate.firstDeclarationId(), candidate);
  }

  private static boolean indexed(Node expression, String id,
                                 PointerRoleAnalyzer.Candidate candidate) {
    return candidate.accesses().stream().filter(access -> access.indexDeclarationId().equals(id))
        .anyMatch(access -> expression.findAll(Node.class).stream()
            .anyMatch(node -> AstAnalyzer.nodeId(node).equals(access.astNodeId())));
  }

  private static boolean within(Node child, Node parent) {
    for (Node node = child; node != null; node = node.getParentNode().orElse(null))
      if (node == parent) return true;
    return false;
  }

  private static boolean compares(BinaryExpr guard, String first, String second) {
    boolean direct = guard.getOperator() == BinaryExpr.Operator.LESS
        || guard.getOperator() == BinaryExpr.Operator.LESS_EQUALS;
    boolean reversed = guard.getOperator() == BinaryExpr.Operator.GREATER
        || guard.getOperator() == BinaryExpr.Operator.GREATER_EQUALS;
    return (direct && boundName(guard.getLeft(), first)
        && boundName(guard.getRight(), second))
        || (reversed && boundName(guard.getLeft(), second)
            && boundName(guard.getRight(), first));
  }

  private static boolean bound(BinaryExpr guard, String first,
                               PointerRoleAnalyzer.Candidate candidate) {
    boolean forward = guard.getOperator() == BinaryExpr.Operator.LESS;
    boolean reversed = guard.getOperator() == BinaryExpr.Operator.GREATER;
    return (forward && boundName(guard.getLeft(), first)
        && length(guard.getRight(), candidate))
        || (reversed && boundName(guard.getRight(), first)
            && length(guard.getLeft(), candidate));
  }

  private static boolean length(Expression expression, PointerRoleAnalyzer.Candidate candidate) {
    Expression value = PointerRoleAnalyzer.unwrap(expression);
    if (value instanceof FieldAccessExpr field && field.getNameAsString().equals("length")
        && field.getScope() instanceof NameExpr name)
      return LocalVariableBindings.resolve(name).filter(candidate.dataDeclarationId()::equals)
          .isPresent();
    if (value instanceof MethodCallExpr call && call.getNameAsString().equals("size")
        && call.getArguments().isEmpty() && call.getScope().isPresent()
        && call.getScope().get() instanceof NameExpr name)
      return LocalVariableBindings.resolve(name).filter(candidate.dataDeclarationId()::equals)
          .isPresent();
    return false;
  }

  private static boolean boundName(Expression expression, String id) {
    return PointerRoleAnalyzer.unwrap(expression) instanceof NameExpr name
        && LocalVariableBindings.resolve(name).filter(id::equals).isPresent();
  }

  private static boolean one(Expression expression) {
    return PointerRoleAnalyzer.unwrap(expression) instanceof IntegerLiteralExpr literal
        && literal.asNumber().intValue() == 1;
  }

  private static List<Update> updates(Node loop) {
    List<Update> result = new ArrayList<>();
    for (UnaryExpr unary : loop.findAll(UnaryExpr.class)) {
      if (!(unary.getExpression() instanceof NameExpr name)) continue;
      int delta = switch (unary.getOperator()) {
        case PREFIX_INCREMENT, POSTFIX_INCREMENT -> 1;
        case PREFIX_DECREMENT, POSTFIX_DECREMENT -> -1;
        default -> 0;
      };
      if (delta != 0) LocalVariableBindings.resolve(name).ifPresent(id ->
          result.add(new Update(id, delta, unary, header(unary, loop))));
    }
    for (AssignExpr assignment : loop.findAll(AssignExpr.class)) {
      if (!(assignment.getTarget() instanceof NameExpr name)) continue;
      int delta = assignmentDelta(assignment, name);
      LocalVariableBindings.resolve(name).ifPresent(id ->
          result.add(new Update(id, delta, assignment, header(assignment, loop))));
    }
    return result;
  }

  private static int assignmentDelta(AssignExpr assignment, NameExpr target) {
    if (assignment.getOperator() == AssignExpr.Operator.PLUS && one(assignment.getValue()))
      return 1;
    if (assignment.getOperator() == AssignExpr.Operator.MINUS && one(assignment.getValue()))
      return -1;
    if (assignment.getOperator() != AssignExpr.Operator.ASSIGN
        || !(PointerRoleAnalyzer.unwrap(assignment.getValue()) instanceof BinaryExpr binary)
        || !one(binary.getRight())
        || !(PointerRoleAnalyzer.unwrap(binary.getLeft()) instanceof NameExpr left)
        || !LocalVariableBindings.resolve(left).equals(LocalVariableBindings.resolve(target)))
      return 0;
    if (binary.getOperator() == BinaryExpr.Operator.PLUS) return 1;
    if (binary.getOperator() == BinaryExpr.Operator.MINUS) return -1;
    return 0;
  }

  private static boolean header(Node update, Node loop) {
    if (!(loop instanceof ForStmt statement)) return false;
    return statement.getUpdate().stream().anyMatch(expr -> expr == update);
  }

  private static boolean allPathsProgress(Node loop, List<Update> first, List<Update> second,
                                          boolean requireFirst) {
    Statement body;
    if (loop instanceof ForStmt statement) body = statement.getBody();
    else if (loop instanceof WhileStmt statement) body = statement.getBody();
    else if (loop instanceof DoStmt statement) body = statement.getBody();
    else return false;
    List<Path> paths = walk(body, List.of(new Path(false, false, false, false)), first, second);
    boolean headerFirst = first.stream().anyMatch(Update::header);
    boolean headerSecond = second.stream().anyMatch(Update::header);
    for (Path path : paths) {
      if (path.exitsLoop()) continue;
      boolean movedFirst = path.first() || headerFirst;
      boolean movedSecond = path.second() || headerSecond;
      if (requireFirst ? !movedFirst : !(movedFirst || movedSecond)) return false;
    }
    return true;
  }

  private static List<Path> walk(Statement statement, List<Path> incoming,
                                 List<Update> first, List<Update> second) {
    if (statement instanceof BlockStmt block) {
      List<Path> states = incoming;
      for (Statement child : block.getStatements()) states = walk(child, states, first, second);
      return states;
    }
    List<Path> result = new ArrayList<>();
    for (Path path : incoming) {
      if (path.stopped()) { result.add(path); continue; }
      if (statement instanceof IfStmt branch) {
        result.addAll(walk(branch.getThenStmt(), List.of(path), first, second));
        if (branch.getElseStmt().isPresent())
          result.addAll(walk(branch.getElseStmt().get(), List.of(path), first, second));
        else result.add(path);
      } else if (statement instanceof ReturnStmt || statement instanceof BreakStmt) {
        result.add(new Path(path.first(), path.second(), true, true));
      } else if (statement instanceof ContinueStmt) {
        result.add(new Path(path.first(), path.second(), true, false));
      } else if (statement instanceof ForStmt || statement instanceof WhileStmt
          || statement instanceof DoStmt) {
        // Nested loops need a separate control-flow proof.
        result.add(new Path(false, false, true, false));
      } else {
        boolean movedFirst = first.stream().filter(u -> !u.header())
            .anyMatch(u -> u.node() == statement || u.node().findAncestor(Statement.class)
                .filter(statement::equals).isPresent());
        boolean movedSecond = second.stream().filter(u -> !u.header())
            .anyMatch(u -> u.node() == statement || u.node().findAncestor(Statement.class)
                .filter(statement::equals).isPresent());
        result.add(new Path(path.first() || movedFirst, path.second() || movedSecond,
            false, false));
      }
    }
    return result;
  }

  private static boolean hasEntryGuard(Node loop, PointerRoleAnalyzer.Candidate candidate) {
    // A do/while executes its body once before checking the trailing condition.
    Node previous = loop.getParentNode().orElse(null);
    if (!(previous instanceof com.github.javaparser.ast.stmt.BlockStmt block)) return false;
    int index = block.getStatements().indexOf(loop);
    if (index <= 0) return false;
    Node preceding = block.getStatement(index - 1);
    if (!(preceding instanceof IfStmt branch) || branch.getElseStmt().isPresent()
        || !(PointerRoleAnalyzer.unwrap(branch.getCondition()) instanceof BinaryExpr condition))
      return false;
    Statement exit = branch.getThenStmt();
    if (exit instanceof BlockStmt body) {
      if (body.getStatements().size() != 1) return false;
      exit = body.getStatement(0);
    }
    if (!(exit instanceof ReturnStmt || exit instanceof ThrowStmt)) return false;
    Expression left = PointerRoleAnalyzer.unwrap(condition.getLeft());
    Expression right = PointerRoleAnalyzer.unwrap(condition.getRight());
    return (length(left, candidate) && smallLengthGuard(condition.getOperator(), right, false))
        || (length(right, candidate)
            && smallLengthGuard(condition.getOperator(), left, true));
  }

  private static boolean smallLengthGuard(BinaryExpr.Operator operator,
                                          Expression constant, boolean reversed) {
    if (!(PointerRoleAnalyzer.unwrap(constant) instanceof IntegerLiteralExpr literal))
      return false;
    int bound = literal.asNumber().intValue();
    if (!reversed)
      return operator == BinaryExpr.Operator.LESS && bound == 2
          || operator == BinaryExpr.Operator.LESS_EQUALS && bound == 1;
    return operator == BinaryExpr.Operator.GREATER && bound == 2
        || operator == BinaryExpr.Operator.GREATER_EQUALS && bound == 1;
  }
}
