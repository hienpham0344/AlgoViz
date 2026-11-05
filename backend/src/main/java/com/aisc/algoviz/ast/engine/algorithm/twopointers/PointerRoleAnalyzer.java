package com.aisc.algoviz.ast.engine.algorithm.twopointers;

import com.aisc.algoviz.ast.engine.algorithm.LocalVariableBindings;
import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Resolves two indexed accesses to one source declaration, never from spelling alone. */
public final class PointerRoleAnalyzer {
  public record Access(String kind, String astNodeId, String indexName,
                       String indexDeclarationId) {}

  public record Candidate(String dataName, String dataDeclarationId,
                          String firstName, String firstDeclarationId,
                          String secondName, String secondDeclarationId,
                          String representation, List<Access> accesses) {
    public Candidate { accesses = List.copyOf(accesses); }
  }

  private record DataAccess(String dataName, String dataId, String representation, Access access) {}

  public List<Candidate> candidates(CompilationUnit unit, Node loop) {
    Expression guard = condition(loop);
    if (guard == null || LocalVariableBindings.callable(loop) == null) return List.of();
    List<DataAccess> found = new ArrayList<>();
    boolean unresolved = false;
    for (ArrayAccessExpr access : loop.findAll(ArrayAccessExpr.class)) {
      if (nearestLoop(access) != loop) continue;
      DataAccess parsed = arrayAccess(unit, access);
      if (parsed == null) unresolved = true;
      else found.add(parsed);
    }
    for (MethodCallExpr call : loop.findAll(MethodCallExpr.class)) {
      if (nearestLoop(call) != loop) continue;
      if (call.getNameAsString().equals("size") && safeListSize(unit, call)) continue;
      if (safeIntegerEquals(unit, call)) continue;
      DataAccess parsed = Set.of("get", "set").contains(call.getNameAsString())
          ? listAccess(unit, call) : helperAccess(unit, call);
      if (parsed == null) unresolved = true;
      else found.add(parsed);
    }
    if (unresolved || found.size() < 2) return List.of();
    Set<String> dataIds = new LinkedHashSet<>();
    Set<String> indexIds = new LinkedHashSet<>();
    for (DataAccess access : found) {
      dataIds.add(access.dataId());
      indexIds.add(access.access().indexDeclarationId());
    }
    if (dataIds.size() != 1 || indexIds.size() != 2) return List.of();
    String dataId = dataIds.iterator().next();
    boolean rebound = LocalVariableBindings.callable(loop).findAll(AssignExpr.class).stream()
        .filter(assignment -> assignment.getTarget() instanceof NameExpr
            && assignment.getBegin().isPresent() && loop.getEnd().isPresent()
            && !assignment.getBegin().get().isAfter(loop.getEnd().get()))
        .map(assignment -> (NameExpr) assignment.getTarget())
        .anyMatch(name -> LocalVariableBindings.resolve(name).filter(dataId::equals).isPresent());
    if (rebound) return List.of();
    for (DataAccess access : found) {
      boolean shadowedGuardName = guard.findAll(NameExpr.class).stream()
          .filter(name -> name.getNameAsString().equals(access.access().indexName()))
          .anyMatch(name -> LocalVariableBindings.resolve(name)
              .filter(access.access().indexDeclarationId()::equals).isEmpty());
      if (shadowedGuardName) return List.of();
    }
    DataAccess firstAccess = found.get(0);
    String firstId = firstAccess.access().indexDeclarationId();
    String originalFirstId = firstId;
    DataAccess secondAccess = found.stream()
        .filter(a -> !a.access().indexDeclarationId().equals(originalFirstId))
        .findFirst().orElseThrow();
    String secondId = secondAccess.access().indexDeclarationId();
    String initialFirstId = firstId;
    String initialSecondId = secondId;
    boolean firstInGuard = guard.findAll(NameExpr.class).stream()
        .anyMatch(n -> LocalVariableBindings.resolve(n).filter(initialFirstId::equals).isPresent());
    boolean secondInGuard = guard.findAll(NameExpr.class).stream()
        .anyMatch(n -> LocalVariableBindings.resolve(n).filter(initialSecondId::equals).isPresent());
    if (!firstInGuard && !secondInGuard) return List.of();
    // Source access order is irrelevant: the guard establishes the left/scanned role.
    boolean swapRoles = !firstInGuard && secondInGuard;
    if (firstInGuard && secondInGuard && unwrap(guard) instanceof BinaryExpr relation) {
      Expression left = unwrap(relation.getLeft());
      Expression right = unwrap(relation.getRight());
      NameExpr canonical = relation.getOperator() == BinaryExpr.Operator.LESS
          || relation.getOperator() == BinaryExpr.Operator.LESS_EQUALS
              ? left instanceof NameExpr name ? name : null
              : right instanceof NameExpr name ? name : null;
      if (canonical != null)
        swapRoles = !LocalVariableBindings.resolve(canonical).orElse("").equals(firstId);
    }
    if (swapRoles) {
      DataAccess swap = firstAccess;
      firstAccess = secondAccess;
      secondAccess = swap;
      firstId = firstAccess.access().indexDeclarationId();
      secondId = secondAccess.access().indexDeclarationId();
    }
    List<Access> accesses = found.stream().map(DataAccess::access).toList();
    return List.of(new Candidate(firstAccess.dataName(), firstAccess.dataId(),
        firstAccess.access().indexName(), firstId,
        secondAccess.access().indexName(), secondId,
        firstAccess.representation(), accesses));
  }

  private boolean safeIntegerEquals(CompilationUnit unit, MethodCallExpr call) {
    if (!call.getNameAsString().equals("equals") || call.getScope().isEmpty()
        || call.getArguments().size() != 1
        || !(unwrap(call.getScope().get()) instanceof MethodCallExpr left)
        || !(unwrap(call.getArgument(0)) instanceof MethodCallExpr right)) return false;
    DataAccess first = listAccess(unit, left);
    DataAccess second = listAccess(unit, right);
    return first != null && second != null && first.dataId().equals(second.dataId())
        && first.access().kind().equals("GET") && second.access().kind().equals("GET");
  }

  private static boolean safeListSize(CompilationUnit unit, MethodCallExpr call) {
    if (call.getArguments().size() != 0 || call.getScope().isEmpty()
        || !(unwrap(call.getScope().get()) instanceof NameExpr name)) return false;
    return LocalVariableBindings.resolve(name)
        .filter(id -> "INTEGER_LIST".equals(representation(unit, id))).isPresent();
  }

  private DataAccess arrayAccess(CompilationUnit unit, ArrayAccessExpr array) {
    Expression data = unwrap(array.getName());
    NameExpr index = sourceIndex(unit, indexName(array.getIndex()));
    if (!(data instanceof NameExpr name) || index == null) return null;
    Optional<String> dataId = LocalVariableBindings.resolve(name);
    Optional<String> indexId = LocalVariableBindings.resolve(index);
    if (dataId.isEmpty() || indexId.isEmpty()
        || !"INT_ARRAY".equals(representation(unit, dataId.get()))) return null;
    String kind = array.getParentNode().filter(p -> p instanceof AssignExpr assignment
        && assignment.getTarget() == array).isPresent() ? "SET" : "GET";
    return new DataAccess(name.getNameAsString(), dataId.get(), "INT_ARRAY",
        new Access(kind, AstAnalyzer.nodeId(array), index.getNameAsString(), indexId.get()));
  }

  private DataAccess listAccess(CompilationUnit unit, MethodCallExpr call) {
    if (call.getScope().isEmpty() || call.getArguments().isEmpty()) return null;
    Expression scope = unwrap(call.getScope().get());
    NameExpr index = sourceIndex(unit, indexName(call.getArgument(0)));
    if (!(scope instanceof NameExpr name) || index == null
        || (call.getNameAsString().equals("get") && call.getArguments().size() != 1)
        || (call.getNameAsString().equals("set") && call.getArguments().size() != 2)) return null;
    Optional<String> dataId = LocalVariableBindings.resolve(name);
    Optional<String> indexId = LocalVariableBindings.resolve(index);
    if (dataId.isEmpty() || indexId.isEmpty()
        || !"INTEGER_LIST".equals(representation(unit, dataId.get()))) return null;
    return new DataAccess(name.getNameAsString(), dataId.get(), "INTEGER_LIST",
        new Access(call.getNameAsString().toUpperCase(), AstAnalyzer.nodeId(call),
            index.getNameAsString(), indexId.get()));
  }

  private DataAccess helperAccess(CompilationUnit unit, MethodCallExpr call) {
    if (call.getScope().isPresent()
        && !(unwrap(call.getScope().get()) instanceof ThisExpr)) return null;
    TypeDeclaration<?> owner = call.findAncestor(TypeDeclaration.class).orElse(null);
    if (owner == null) return null;
    List<MethodDeclaration> matches = owner.getMethodsByName(call.getNameAsString());
    if (matches.size() != 1) return null;
    MethodDeclaration method = matches.get(0);
    if (!(method.isPrivate() || method.isStatic() || method.isFinal())
        || method.getParameters().size() != call.getArguments().size()
        || method.getBody().isEmpty() || method.getBody().get().getStatements().size() != 1
        || !(method.getBody().get().getStatement(0) instanceof ReturnStmt returned)
        || returned.getExpression().isEmpty()) return null;
    Expression body = unwrap(returned.getExpression().get());
    NameExpr data;
    NameExpr index;
    if (body instanceof ArrayAccessExpr array && unwrap(array.getName()) instanceof NameExpr name) {
      data = name;
      index = indexName(array.getIndex());
    } else if (body instanceof MethodCallExpr get && get.getNameAsString().equals("get")
        && get.getArguments().size() == 1 && get.getScope().isPresent()
        && unwrap(get.getScope().get()) instanceof NameExpr name) {
      data = name;
      index = indexName(get.getArgument(0));
    } else return null;
    if (index == null) return null;
    int dataPosition = -1, indexPosition = -1;
    for (int i = 0; i < method.getParameters().size(); i++) {
      Parameter parameter = method.getParameter(i);
      String id = AstAnalyzer.nodeId(parameter);
      if (LocalVariableBindings.resolve(data).filter(id::equals).isPresent()) dataPosition = i;
      if (LocalVariableBindings.resolve(index).filter(id::equals).isPresent()) indexPosition = i;
    }
    if (dataPosition < 0 || indexPosition < 0
        || !(unwrap(call.getArgument(dataPosition)) instanceof NameExpr actualData)
        || !(unwrap(call.getArgument(indexPosition)) instanceof NameExpr actualIndex)) return null;
    Optional<String> dataId = LocalVariableBindings.resolve(actualData);
    Optional<String> indexId = LocalVariableBindings.resolve(actualIndex);
    if (dataId.isEmpty() || indexId.isEmpty()) return null;
    String representation = representation(unit, dataId.get());
    if (representation == null) return null;
    return new DataAccess(actualData.getNameAsString(), dataId.get(), representation,
        new Access("GET", AstAnalyzer.nodeId(call), actualIndex.getNameAsString(), indexId.get()));
  }

  private static String representation(CompilationUnit unit, String declarationId) {
    for (Parameter parameter : unit.findAll(Parameter.class)) {
      if (AstAnalyzer.nodeId(parameter).equals(declarationId))
        return typeRepresentation(unit, parameter.getType().asString());
    }
    for (VariableDeclarator variable : unit.findAll(VariableDeclarator.class)) {
      if (AstAnalyzer.nodeId(variable).equals(declarationId))
        return typeRepresentation(unit, variable.getType().asString());
    }
    return null;
  }

  private static String typeRepresentation(CompilationUnit unit, String type) {
    String compact = type.replaceAll("\\s+", "");
    if (compact.equals("int[]")) return "INT_ARRAY";
    if (!compact.matches("(?:java\\.util\\.)?(?:List|ArrayList|LinkedList)<Integer>"))
      return null;
    String name = compact.substring(0, compact.indexOf('<'));
    String simple = name.substring(name.lastIndexOf('.') + 1);
    if (unit.findAll(TypeDeclaration.class).stream()
        .anyMatch(typeDeclaration -> typeDeclaration.getNameAsString().equals(simple)
            || typeDeclaration.getNameAsString().equals("Integer"))) return null;
    if (unit.getImports().stream().anyMatch(imported -> !imported.isStatic()
        && imported.getNameAsString().endsWith(".Integer")
        && !imported.getNameAsString().equals("java.lang.Integer"))) return null;
    if (name.startsWith("java.util.")) return "INTEGER_LIST";
    if (unit.getImports().stream().anyMatch(imported -> !imported.isStatic()
        && (imported.isAsterisk() && imported.getNameAsString().equals("java.util")
            || imported.getNameAsString().equals("java.util." + simple))))
      return "INTEGER_LIST";
    return null;
  }

  static Expression condition(Node loop) {
    if (loop instanceof ForStmt statement) return statement.getCompare().orElse(null);
    if (loop instanceof WhileStmt statement) return statement.getCondition();
    if (loop instanceof DoStmt statement) return statement.getCondition();
    return null;
  }

  static Expression unwrap(Expression expression) {
    while (expression instanceof EnclosedExpr enclosed) expression = enclosed.getInner();
    return expression;
  }

  static NameExpr indexName(Expression expression) {
    Expression value = unwrap(expression);
    if (value instanceof NameExpr name) return name;
    if (value instanceof UnaryExpr unary
        && unary.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
        && unwrap(unary.getExpression()) instanceof NameExpr name) return name;
    return null;
  }

  private static NameExpr sourceIndex(CompilationUnit unit, NameExpr index) {
    if (index == null) return null;
    NameExpr current = index;
    for (int depth = 0; depth < 4; depth++) {
      Optional<String> id = LocalVariableBindings.resolve(current);
      if (id.isEmpty()) return null;
      VariableDeclarator declaration = unit.findAll(VariableDeclarator.class).stream()
          .filter(v -> AstAnalyzer.nodeId(v).equals(id.get())).findFirst().orElse(null);
      if (declaration == null || declaration.getInitializer().isEmpty()) return current;
      Expression initial = unwrap(declaration.getInitializer().get());
      if (!(initial instanceof NameExpr origin)) return current;
      Node callable = LocalVariableBindings.callable(declaration);
      boolean reassigned = callable.findAll(AssignExpr.class).stream()
          .map(AssignExpr::getTarget).filter(NameExpr.class::isInstance)
          .map(NameExpr.class::cast)
          .anyMatch(name -> LocalVariableBindings.resolve(name).filter(id.get()::equals)
              .isPresent());
      reassigned |= callable.findAll(UnaryExpr.class).stream()
          .filter(u -> u.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT
              || u.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
              || u.getOperator() == UnaryExpr.Operator.PREFIX_DECREMENT
              || u.getOperator() == UnaryExpr.Operator.POSTFIX_DECREMENT)
          .map(UnaryExpr::getExpression).filter(NameExpr.class::isInstance)
          .map(NameExpr.class::cast)
          .anyMatch(name -> LocalVariableBindings.resolve(name).filter(id.get()::equals)
              .isPresent());
      if (reassigned) return null;
      String sourceId = LocalVariableBindings.resolve(origin).orElse(null);
      if (sourceId == null || sourceMutated(callable, sourceId, declaration, index)) return null;
      current = origin;
    }
    return null;
  }

  private static boolean sourceMutated(Node callable, String sourceId,
                                       VariableDeclarator alias, NameExpr access) {
    Node loop = nearestLoop(access);
    Node limit = nearestLoop(alias) == loop ? access : loop;
    if (alias.getBegin().isEmpty() || limit == null || limit.getEnd().isEmpty()) return true;
    return callable.findAll(Node.class).stream()
        .filter(node -> node instanceof AssignExpr || node instanceof UnaryExpr)
        .filter(node -> node.getBegin().isPresent()
            && node.getBegin().get().isAfter(alias.getBegin().get())
            && !node.getBegin().get().isAfter(limit.getEnd().get()))
        .anyMatch(node -> {
          NameExpr target = null;
          if (node instanceof AssignExpr assignment
              && assignment.getTarget() instanceof NameExpr name) target = name;
          if (node instanceof UnaryExpr unary
              && (unary.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT
                  || unary.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
                  || unary.getOperator() == UnaryExpr.Operator.PREFIX_DECREMENT
                  || unary.getOperator() == UnaryExpr.Operator.POSTFIX_DECREMENT)
              && unary.getExpression() instanceof NameExpr name) target = name;
          return target != null && LocalVariableBindings.resolve(target)
              .filter(sourceId::equals).isPresent();
        });
  }

  private static Node nearestLoop(Node node) {
    for (Node parent = node.getParentNode().orElse(null); parent != null;
        parent = parent.getParentNode().orElse(null)) {
      if (parent instanceof ForStmt || parent instanceof WhileStmt || parent instanceof DoStmt)
        return parent;
    }
    return null;
  }
}
