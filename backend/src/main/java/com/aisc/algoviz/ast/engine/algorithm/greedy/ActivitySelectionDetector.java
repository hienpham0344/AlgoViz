package com.aisc.algoviz.ast.engine.algorithm.greedy;

import com.aisc.algoviz.ast.engine.algorithm.LocalVariableBindings;
import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.aisc.algoviz.ast.engine.model.ActivitySelectionPattern;
import com.aisc.algoviz.ast.engine.model.ActivitySelectionPattern.OverlapPolicy;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Conservative proof of the Activity Selection scan and selection effects. */
public final class ActivitySelectionDetector {
  public Optional<AlgorithmHint> detect(CompilationUnit unit, Node loop) {
    if (!(loop instanceof ForStmt || loop instanceof WhileStmt || loop instanceof ForEachStmt))
      return Optional.empty();
    var roles = IntervalRoles.resolve(unit, loop).orElse(null);
    if (roles == null) return Optional.empty();
    EndOrderingAnalyzer.Result ordering = new EndOrderingAnalyzer().classify(unit, loop, roles);
    if (!fullScan(loop, roles)) return Optional.empty();

    String candidateName = declarationName(unit, roles.candidateDeclarationId());
    if (candidateName == null) return Optional.empty();
    IfStmt accepted = null;
    String boundary = null;
    BinaryExpr decision = null;
    AssignExpr update = null;
    boolean closed = false;
    for (IfStmt branch : loop.findAll(IfStmt.class)) {
      if (nearestLoop(branch) != loop || branch.getElseStmt().isPresent()) continue;
      for (BinaryExpr comparison : branch.getCondition().findAll(BinaryExpr.class)) {
        Decision candidateDecision = decision(comparison, candidateName, roles);
        if (candidateDecision == null) continue;
        AssignExpr candidateUpdate = branch.getThenStmt().findAll(AssignExpr.class).stream()
            .filter(a -> a.getOperator() == AssignExpr.Operator.ASSIGN)
            .filter(a -> candidateDecision.boundary().equals(name(a.getTarget())))
            .filter(a -> isEnd(a.getValue(), candidateName, roles))
            .findFirst().orElse(null);
        if (candidateUpdate == null || accepted != null) return Optional.empty();
        accepted = branch;
        boundary = candidateDecision.boundary();
        decision = comparison;
        update = candidateUpdate;
        closed = candidateDecision.closed();
      }
    }
    if (accepted == null || !oneBoundaryWrite(loop, boundary, update)) return Optional.empty();
    Effect effect = selectionEffect(accepted, candidateName);
    if (effect == null || !validGuard(accepted.getCondition(), decision, effect.outputName(), closed)
        || !validSelectedBranch(accepted, update, effect)
        || !onlyOutputEffect(loop, effect)
        || !safeInitialState(unit, loop, boundary, effect.outputName(), closed)
        || hasEarlyExit(loop) || mutatesInput(loop, roles, candidateName, effect.outputName()))
      return Optional.empty();
    if (effect.outputName() != null && !returnsOutput(unit, loop, effect)) return Optional.empty();

    Node method = LocalVariableBindings.callable(loop);
    if (method == null || loop.getRange().isEmpty()) return Optional.empty();
    String inputName = inputName(unit, roles.inputDeclarationId());
    if (inputName == null) return Optional.empty();
    Map<String, String> variables = new LinkedHashMap<>();
    variables.put("input", inputName);
    variables.put("candidate", candidateName);
    variables.put("lastEnd", boundary);
    variables.put("output", effect.outputName());
    String cursor = cursor(loop, inputName);
    if (cursor != null) variables.put("cursor", cursor);
    var declarationIds = LocalVariableBindings.roles(loop, variables).orElse(null);
    if (declarationIds == null) return Optional.empty();
    Map<String, String> operationIds = new LinkedHashMap<>();
    operationIds.put("scan", AstAnalyzer.nodeId(loop));
    operationIds.put("decision", AstAnalyzer.nodeId(decision));
    operationIds.put("lastEndUpdate", AstAnalyzer.nodeId(update));
    operationIds.put("selectionEffect", effect.astNodeId());
    if (ordering.sortAstNodeId() != null) operationIds.put("sort", ordering.sortAstNodeId());
    Map<String, String> roleValues = new LinkedHashMap<>();
    roleValues.put("start", roleValue(roles, true));
    roleValues.put("end", roleValue(roles, false));
    if (roles.representation().equals("INTERVAL_LIST")) {
      String runtimeType = IntervalRoles.runtimeType(unit, roles.intervalType());
      if (runtimeType == null) return Optional.empty();
      roleValues.put("runtimeType", runtimeType);
    }
    var metadata = new ActivitySelectionPattern(1, roles.representation(),
        closed ? OverlapPolicy.CLOSED : OverlapPolicy.HALF_OPEN,
        ordering.evidence(),
        Map.of("input", roles.inputDeclarationId(), "candidate", roles.candidateDeclarationId(),
            "lastEnd", declarationIds.get("lastEnd")),
        roleValues,
        operationIds, closed);
    var range = loop.getRange().orElseThrow();
    AlgorithmHint hint = new AlgorithmHint("activity-selection", 0.95, "activity-selection",
        List.of("Candidates are scanned once", "Selection requires the configured non-overlap rule",
            "The accepted candidate end becomes the next boundary"), variables,
        range.begin.line, range.end.line, AstAnalyzer.nodeId(loop), AstAnalyzer.nodeId(method),
        declarationIds, null, null, null, null, metadata);
    return Optional.of(hint);
  }

  private boolean fullScan(Node loop, IntervalRoles roles) {
    if (loop.findAll(Node.class).stream().anyMatch(node -> node != loop
        && (node instanceof ForStmt || node instanceof WhileStmt
            || node instanceof ForEachStmt || node instanceof DoStmt))) return false;
    if (loop instanceof ForEachStmt each)
      return each.getIterable() instanceof NameExpr input
          && name(input).equals(inputNameForLoop(each, roles));
    String input = inputNameForLoop(loop, roles);
    if (input == null) return false;
    if (loop instanceof ForStmt forLoop) {
      if (forLoop.getInitialization().size() != 1 || forLoop.getUpdate().size() != 1
          || !(forLoop.getInitialization().get(0) instanceof com.github.javaparser.ast.expr.VariableDeclarationExpr init)
          || init.getVariables().size() != 1) return false;
      VariableDeclarator index = init.getVariable(0);
      String counter = index.getNameAsString();
      return index.getInitializer().filter(e -> "0".equals(e.toString())).isPresent()
          && forLoop.getCompare().filter(e -> isUpperBound(e, counter, input, roles)).isPresent()
          && increments(forLoop.getUpdate().get(0), counter)
          && candidateUsesCursor(loop, roles, counter, input)
          && cursorWrites(forLoop.getBody(), counter) == 0;
    }
    if (!(loop instanceof WhileStmt whileLoop)) return false;
    String counter = whileLoop.getCondition().findAll(NameExpr.class).stream()
        .map(NameExpr::getNameAsString).filter(n -> !n.equals(input)).findFirst().orElse(null);
    if (counter == null || !isUpperBound(whileLoop.getCondition(), counter, input, roles)
        || !(whileLoop.getBody() instanceof BlockStmt body) || body.getStatements().isEmpty()
        || !candidateUsesCursor(loop, roles, counter, input)
        || !zeroInitialized(loop, counter)) return false;
    Statement finalStatement = body.getStatement(body.getStatements().size() - 1);
    return finalStatement instanceof ExpressionStmt expression
        && increments(expression.getExpression(), counter)
        && cursorWrites(body, counter) == 1;
  }

  private static boolean isUpperBound(Expression expression, String counter, String input, IntervalRoles roles) {
    if (!(unwrap(expression) instanceof BinaryExpr comparison)) return false;
    return comparison.getOperator() == BinaryExpr.Operator.LESS
        && name(comparison.getLeft()).equals(counter)
        && isLength(comparison.getRight(), input, roles);
  }

  private static boolean candidateUsesCursor(
      Node loop, IntervalRoles roles, String cursor, String input) {
    VariableDeclarator candidate = loop.findAll(VariableDeclarator.class).stream()
        .filter(v -> AstAnalyzer.nodeId(v).equals(roles.candidateDeclarationId()))
        .findFirst().orElse(null);
    if (candidate == null || candidate.getInitializer().isEmpty()) return false;
    Expression source = unwrap(candidate.getInitializer().orElseThrow());
    if (source instanceof ArrayAccessExpr row)
      return roles.representation().equals("INT_MATRIX")
          && name(row.getName()).equals(input) && name(row.getIndex()).equals(cursor);
    if (source instanceof MethodCallExpr get)
      return roles.representation().equals("INTERVAL_LIST") && get.getNameAsString().equals("get")
          && get.getArguments().size() == 1 && name(get.getArgument(0)).equals(cursor)
          && get.getScope().filter(scope -> name(scope).equals(input)).isPresent();
    return false;
  }

  private static boolean zeroInitialized(Node loop, String cursor) {
    Node owner = LocalVariableBindings.callable(loop);
    if (owner == null) return false;
    return owner.findAll(VariableDeclarator.class).stream()
        .filter(v -> v.getNameAsString().equals(cursor) && v.getBegin().isPresent()
            && loop.getBegin().isPresent() && v.getBegin().orElseThrow().isBefore(loop.getBegin().orElseThrow()))
        .anyMatch(v -> v.getInitializer().filter(value -> "0".equals(value.toString())).isPresent()
            && stableInitial(v, loop, cursor));
  }

  private static long cursorWrites(Node body, String cursor) {
    long assignments = body.findAll(AssignExpr.class).stream()
        .filter(a -> name(a.getTarget()).equals(cursor)).count();
    long unary = body.findAll(UnaryExpr.class).stream()
        .filter(u -> name(u.getExpression()).equals(cursor)
            && (u.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
                || u.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT
                || u.getOperator() == UnaryExpr.Operator.POSTFIX_DECREMENT
                || u.getOperator() == UnaryExpr.Operator.PREFIX_DECREMENT)).count();
    return assignments + unary;
  }

  private static boolean isLength(Expression expression, String input, IntervalRoles roles) {
    Expression value = unwrap(expression);
    if (value instanceof FieldAccessExpr field && field.getNameAsString().equals("length")
        && name(field.getScope()).equals(input)) return roles.representation().equals("INT_MATRIX");
    if (value instanceof MethodCallExpr call && call.getNameAsString().equals("size")
        && call.getArguments().isEmpty() && call.getScope().filter(s -> name(s).equals(input)).isPresent())
      return roles.representation().equals("INTERVAL_LIST");
    return false;
  }

  private static boolean increments(Expression expression, String counter) {
    Expression value = unwrap(expression);
    if (value instanceof UnaryExpr unary && name(unary.getExpression()).equals(counter))
      return unary.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
          || unary.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT;
    if (value instanceof AssignExpr assignment && name(assignment.getTarget()).equals(counter))
      return assignment.getOperator() == AssignExpr.Operator.PLUS
          && "1".equals(assignment.getValue().toString());
    return false;
  }

  private static Decision decision(BinaryExpr comparison, String candidate, IntervalRoles roles) {
    boolean left = isStart(comparison.getLeft(), candidate, roles);
    boolean right = isStart(comparison.getRight(), candidate, roles);
    if (left == right) return null;
    Expression boundExpr = left ? comparison.getRight() : comparison.getLeft();
    String boundary = name(boundExpr);
    if (boundary == null) return null;
    boolean accepts = left
        ? comparison.getOperator() == BinaryExpr.Operator.GREATER_EQUALS
            || comparison.getOperator() == BinaryExpr.Operator.GREATER
        : comparison.getOperator() == BinaryExpr.Operator.LESS_EQUALS
            || comparison.getOperator() == BinaryExpr.Operator.LESS;
    boolean rejects = left
        ? comparison.getOperator() == BinaryExpr.Operator.LESS_EQUALS
            || comparison.getOperator() == BinaryExpr.Operator.LESS
        : comparison.getOperator() == BinaryExpr.Operator.GREATER_EQUALS
            || comparison.getOperator() == BinaryExpr.Operator.GREATER;
    if (!accepts || rejects) return null;
    return new Decision(boundary,
        comparison.getOperator() == BinaryExpr.Operator.GREATER
            || comparison.getOperator() == BinaryExpr.Operator.LESS);
  }

  private static boolean isStart(Expression expression, String candidate, IntervalRoles roles) {
    return endpoint(expression, candidate, roles, true);
  }

  private static boolean isEnd(Expression expression, String candidate, IntervalRoles roles) {
    return endpoint(expression, candidate, roles, false);
  }

  private static boolean endpoint(Expression expression, String candidate, IntervalRoles roles, boolean start) {
    Expression value = unwrap(expression);
    if (value instanceof NameExpr alias) {
      CompilationUnit unit = alias.findCompilationUnit().orElse(null);
      String id = LocalVariableBindings.resolve(alias).orElse(null);
      VariableDeclarator declaration = unit == null || id == null ? null
          : unit.findAll(VariableDeclarator.class).stream()
              .filter(variable -> AstAnalyzer.nodeId(variable).equals(id))
              .findFirst().orElse(null);
      if (declaration == null || !declaration.getType().asString().equals("int")
          || declaration.getInitializer().isEmpty()
          || nearestLoop(declaration) == null || nearestLoop(declaration) != nearestLoop(alias)
          || declaration.getBegin().isEmpty() || alias.getBegin().isEmpty()
          || !declaration.getBegin().orElseThrow().isBefore(alias.getBegin().orElseThrow())
          || aliasWritten(nearestLoop(alias), alias.getNameAsString())) return false;
      return endpoint(declaration.getInitializer().orElseThrow(), candidate, roles, start);
    }
    if (roles.representation().equals("INT_MATRIX") && value instanceof ArrayAccessExpr access) {
      Integer column = integer(access.getIndex());
      return name(access.getName()).equals(candidate)
          && column != null && column.equals(start ? roles.startColumn() : roles.endColumn());
    }
    String field = start ? roles.startField() : roles.endField();
    if (value instanceof FieldAccessExpr access)
      return name(access.getScope()).equals(candidate) && access.getNameAsString().equals(field);
    if (value instanceof MethodCallExpr call)
      return call.getScope().filter(scope -> name(scope).equals(candidate)).isPresent()
          && call.getNameAsString().equals(start ? roles.startReadName() : roles.endReadName())
          && call.getArguments().isEmpty()
          || value.findCompilationUnit().filter(unit -> IntervalRoles.matchesEndpoint(
              unit, value, candidate, roles, start)).isPresent();
    return false;
  }

  private static boolean aliasWritten(Node loop, String alias) {
    return loop.findAll(AssignExpr.class).stream()
        .filter(assignment -> nearestLoop(assignment) == loop)
        .anyMatch(assignment -> name(assignment.getTarget()).equals(alias))
        || loop.findAll(UnaryExpr.class).stream()
            .filter(unary -> nearestLoop(unary) == loop)
            .anyMatch(unary -> name(unary.getExpression()).equals(alias)
                && (unary.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
                    || unary.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT
                    || unary.getOperator() == UnaryExpr.Operator.POSTFIX_DECREMENT
                    || unary.getOperator() == UnaryExpr.Operator.PREFIX_DECREMENT));
  }

  private static boolean oneBoundaryWrite(Node loop, String boundary, AssignExpr acceptedUpdate) {
    List<AssignExpr> writes = loop.findAll(AssignExpr.class).stream()
        .filter(assignment -> nearestLoop(assignment) == loop)
        .filter(assignment -> name(assignment.getTarget()).equals(boundary)).toList();
    boolean unaryWrite = loop.findAll(UnaryExpr.class).stream()
        .filter(unary -> nearestLoop(unary) == loop)
        .anyMatch(unary -> name(unary.getExpression()).equals(boundary)
            && unary.getOperator() != UnaryExpr.Operator.LOGICAL_COMPLEMENT);
    return !unaryWrite && writes.size() == 1 && writes.get(0) == acceptedUpdate;
  }

  private static boolean onlyOutputEffect(Node loop, Effect effect) {
    String output = effect.outputName();
    if (output == null || output.isEmpty()) return false;
    long writes = loop.findAll(UnaryExpr.class).stream()
        .filter(unary -> nearestLoop(unary) == loop)
        .filter(unary -> name(unary.getExpression()).equals(output))
        .filter(unary -> unary.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
            || unary.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT
            || unary.getOperator() == UnaryExpr.Operator.POSTFIX_DECREMENT
            || unary.getOperator() == UnaryExpr.Operator.PREFIX_DECREMENT).count();
    writes += loop.findAll(AssignExpr.class).stream()
        .filter(assignment -> nearestLoop(assignment) == loop)
        .filter(assignment -> name(assignment.getTarget()).equals(output)).count();
    writes += loop.findAll(MethodCallExpr.class).stream()
        .filter(call -> nearestLoop(call) == loop)
        .filter(call -> call.getScope().filter(scope -> name(scope).equals(output)).isPresent())
        .count();
    return writes == 1;
  }

  private static Effect selectionEffect(IfStmt accepted, String candidate) {
    List<UnaryExpr> increments = accepted.getThenStmt().findAll(UnaryExpr.class).stream()
        .filter(unary -> unary.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
            || unary.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT)
        .toList();
    List<AssignExpr> counts = accepted.getThenStmt().findAll(AssignExpr.class).stream()
        .filter(assignment -> assignment.getOperator() == AssignExpr.Operator.PLUS
            && "1".equals(assignment.getValue().toString())).toList();
    List<MethodCallExpr> additions = accepted.getThenStmt().findAll(MethodCallExpr.class).stream()
        .filter(call -> call.getNameAsString().equals("add") && call.getArguments().size() == 1
            && name(call.getArguments().get(0)).equals(candidate) && call.getScope().isPresent()).toList();
    if (increments.size() + counts.size() + additions.size() != 1) return null;
    if (increments.size() == 1)
      return new Effect(name(increments.get(0).getExpression()), AstAnalyzer.nodeId(increments.get(0)));
    if (counts.size() == 1)
      return new Effect(name(counts.get(0).getTarget()), AstAnalyzer.nodeId(counts.get(0)));
    if (additions.size() == 1)
      return new Effect(name(additions.get(0).getScope().orElseThrow()), AstAnalyzer.nodeId(additions.get(0)));
    return null;
  }

  private static boolean validGuard(
      Expression condition, BinaryExpr comparison, String output, boolean closed) {
    Expression root = unwrap(condition);
    if (root == comparison) return !closed;
    if (!(root instanceof BinaryExpr combined) || combined.getOperator() != BinaryExpr.Operator.OR
        || !closed) return false;
    Expression other = unwrap(combined.getLeft()) == comparison ? combined.getRight()
        : unwrap(combined.getRight()) == comparison ? combined.getLeft() : null;
    if (other == null || !(unwrap(other) instanceof BinaryExpr first)
        || first.getOperator() != BinaryExpr.Operator.EQUALS) return false;
    return name(first.getLeft()).equals(output) && integer(first.getRight()) != null
            && integer(first.getRight()) == 0
        || name(first.getRight()).equals(output) && integer(first.getLeft()) != null
            && integer(first.getLeft()) == 0;
  }

  private static boolean validSelectedBranch(IfStmt accepted, AssignExpr update, Effect effect) {
    if (!(accepted.getThenStmt() instanceof BlockStmt block) || block.getStatements().size() != 2)
      return false;
    return block.getStatements().stream().allMatch(statement ->
        statement instanceof ExpressionStmt expression
            && (AstAnalyzer.nodeId(expression.getExpression()).equals(AstAnalyzer.nodeId(update))
                || AstAnalyzer.nodeId(expression.getExpression()).equals(effect.astNodeId())));
  }

  private static boolean safeInitialState(
      CompilationUnit unit, Node loop, String boundary, String output, boolean closed) {
    Node callable = LocalVariableBindings.callable(loop);
    if (callable == null) return false;
    VariableDeclarator boundaryDeclaration = precedingDeclaration(callable, loop, boundary);
    VariableDeclarator outputDeclaration = precedingDeclaration(callable, loop, output);
    if (boundaryDeclaration == null || outputDeclaration == null
        || !boundaryDeclaration.getType().asString().equals("int")
        || boundaryDeclaration.getInitializer().isEmpty()
        || outputDeclaration.getInitializer().isEmpty()
        || !stableInitial(boundaryDeclaration, loop, boundary)
        || !stableInitial(outputDeclaration, loop, output)) return false;
    String boundaryInitial = boundaryDeclaration.getInitializer().orElseThrow().toString();
    if (!closed && !boundaryInitial.equals("Integer.MIN_VALUE")
        && !boundaryInitial.equals("-2147483648")) return false;
    Expression outputInitial = outputDeclaration.getInitializer().orElseThrow();
    boolean zeroCount = outputDeclaration.getType().asString().equals("int")
        && "0".equals(outputInitial.toString());
    boolean emptyList = outputInitial instanceof com.github.javaparser.ast.expr.ObjectCreationExpr created
        && created.getArguments().isEmpty()
        && List.of("ArrayList", "LinkedList").contains(created.getType().getNameAsString());
    return zeroCount || emptyList;
  }

  private static VariableDeclarator precedingDeclaration(Node callable, Node loop, String name) {
    return callable.findAll(VariableDeclarator.class).stream()
        .filter(v -> v.getNameAsString().equals(name) && v.getBegin().isPresent()
            && loop.getBegin().isPresent()
            && v.getBegin().orElseThrow().isBefore(loop.getBegin().orElseThrow()))
        .findFirst().orElse(null);
  }

  private static boolean stableInitial(VariableDeclarator declaration, Node loop, String name) {
    if (!(loop.getParentNode().orElse(null) instanceof BlockStmt block)) return false;
    ExpressionStmt declarationStatement = declaration.findAncestor(ExpressionStmt.class).orElse(null);
    if (declarationStatement == null || declarationStatement.getParentNode().orElse(null) != block)
      return false;
    int first = block.getStatements().indexOf(declarationStatement);
    int last = block.getStatements().indexOf(loop);
    if (first < 0 || last <= first) return false;
    for (int index = first + 1; index < last; index++) {
      Statement statement = block.getStatement(index);
      if (statement.findAll(NameExpr.class).stream()
          .anyMatch(reference -> reference.getNameAsString().equals(name))) return false;
    }
    return true;
  }

  private static boolean returnsOutput(CompilationUnit unit, Node loop, Effect effect) {
    Node callable = LocalVariableBindings.callable(loop);
    if (!(callable instanceof com.github.javaparser.ast.body.MethodDeclaration method)
        || method.getBody().isEmpty()
        || loop.getParentNode().orElse(null) != method.getBody().orElseThrow()) return false;
    BlockStmt body = method.getBody().orElseThrow();
    int index = body.getStatements().indexOf(loop);
    return index >= 0 && index == body.getStatements().size() - 2
        && body.getStatement(index + 1) instanceof ReturnStmt returned
        && name(returned.getExpression().orElse(null)).equals(effect.outputName());
  }

  private static boolean hasEarlyExit(Node loop) {
    return loop.findAll(Node.class).stream().anyMatch(node -> nearestLoop(node) == loop
        && (node instanceof BreakStmt || node instanceof ContinueStmt || node instanceof ReturnStmt || node instanceof DoStmt));
  }

  private static boolean mutatesInput(
      Node loop, IntervalRoles roles, String candidate, String output) {
    String input = inputNameForLoop(loop, roles);
    if (input == null) return true;
    boolean assigned = loop.findAll(AssignExpr.class).stream()
        .filter(assignment -> nearestLoop(assignment) == loop)
        .anyMatch(assignment -> assignment.getTarget().findAll(NameExpr.class).stream()
            .anyMatch(name -> name.getNameAsString().equals(input)
                || name.getNameAsString().equals(candidate)));
    boolean unaryWrite = loop.findAll(UnaryExpr.class).stream()
        .filter(unary -> nearestLoop(unary) == loop)
        .filter(unary -> unary.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
            || unary.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT
            || unary.getOperator() == UnaryExpr.Operator.POSTFIX_DECREMENT
            || unary.getOperator() == UnaryExpr.Operator.PREFIX_DECREMENT)
        .anyMatch(unary -> unary.getExpression().findAll(NameExpr.class).stream()
            .anyMatch(name -> name.getNameAsString().equals(input)
                || name.getNameAsString().equals(candidate)));
    boolean uncertainCall = loop.findAll(MethodCallExpr.class).stream()
        .filter(call -> nearestLoop(call) == loop)
        .anyMatch(call -> {
          String scope = call.getScope().map(ActivitySelectionDetector::name).orElse("");
          if (scope.equals(input))
            return !(call.getNameAsString().equals("size") && call.getArguments().isEmpty()
                || call.getNameAsString().equals("get") && call.getArguments().size() == 1);
          if (scope.equals(candidate))
            return roles.representation().equals("INT_MATRIX")
                || !call.getArguments().isEmpty()
                || !(call.getNameAsString().equals(roles.startReadName())
                    || call.getNameAsString().equals(roles.endReadName()));
          if (scope.equals(output))
            return !call.getNameAsString().equals("add") || call.getArguments().size() != 1
                || !name(call.getArgument(0)).equals(candidate);
          if (call.findCompilationUnit().filter(unit ->
              IntervalRoles.matchesEndpoint(unit, call, candidate, roles, true)
                  || IntervalRoles.matchesEndpoint(unit, call, candidate, roles, false)).isPresent())
            return false;
          return true;
        });
    return assigned || unaryWrite || uncertainCall;
  }

  private static Node nearestLoop(Node node) {
    for (Node parent = node.getParentNode().orElse(null); parent != null; parent = parent.getParentNode().orElse(null))
      if (parent instanceof ForStmt || parent instanceof WhileStmt || parent instanceof ForEachStmt || parent instanceof DoStmt)
        return parent;
    return null;
  }

  private static String declarationName(CompilationUnit unit, String id) {
    return unit.findAll(VariableDeclarator.class).stream().filter(v -> AstAnalyzer.nodeId(v).equals(id))
        .map(v -> v.getNameAsString()).findFirst().orElseGet(() -> unit.findAll(Parameter.class).stream()
            .filter(p -> AstAnalyzer.nodeId(p).equals(id)).map(p -> p.getNameAsString()).findFirst().orElse(null));
  }

  private static String inputName(CompilationUnit unit, String id) { return declarationName(unit, id); }

  private static String inputNameForLoop(Node loop, IntervalRoles roles) { return inputName(loop.findCompilationUnit().orElseThrow(), roles.inputDeclarationId()); }

  private static String cursor(Node loop, String inputName) {
    if (loop instanceof ForStmt forLoop
        && forLoop.getInitialization().size() == 1
        && forLoop.getInitialization().get(0) instanceof com.github.javaparser.ast.expr.VariableDeclarationExpr init
        && init.getVariables().size() == 1)
      return init.getVariable(0).getNameAsString();
    if (loop instanceof WhileStmt whileLoop)
      return whileLoop.getCondition().findAll(NameExpr.class).stream()
          .map(NameExpr::getNameAsString).filter(name -> !name.equals(inputName))
          .findFirst().orElse(null);
    return null;
  }

  private static String roleValue(IntervalRoles roles, boolean start) {
    return roles.representation().equals("INT_MATRIX")
        ? Integer.toString(start ? roles.startColumn() : roles.endColumn())
        : (start ? roles.startField() : roles.endField());
  }

  private static Integer integer(Expression expression) {
    Expression value = unwrap(expression);
    if (!(value instanceof IntegerLiteralExpr literal)) return null;
    try { return Integer.parseInt(literal.getValue()); } catch (NumberFormatException ignored) { return null; }
  }

  private static String name(Expression expression) {
    Expression value = expression == null ? null : unwrap(expression);
    return value instanceof NameExpr name ? name.getNameAsString() : "";
  }

  private static Expression unwrap(Expression expression) {
    while (expression instanceof EnclosedExpr enclosed) expression = enclosed.getInner();
    return expression;
  }

  private record Decision(String boundary, boolean closed) {}
  private record Effect(String outputName, String astNodeId) {}
}
