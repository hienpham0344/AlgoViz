package com.aisc.algoviz.ast.engine.algorithm.sort;

import com.aisc.algoviz.ast.engine.algorithm.LocalVariableBindings;
import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.aisc.algoviz.ast.engine.model.HeapPattern;
import com.aisc.algoviz.ast.engine.model.SortPattern.Direction;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Complete bottom-up Heap Sort proof over a single int array. */
public final class HeapSortDetector implements SortPatternDetector {
  @Override
  public Optional<AlgorithmHint> detect(Node node) {
    if (!(node instanceof ForStmt || node instanceof WhileStmt)) return Optional.empty();
    Statement extraction = (Statement) node;
    MethodDeclaration method = node.findAncestor(MethodDeclaration.class).orElse(null);
    ClassOrInterfaceDeclaration owner =
        method == null ? null : method.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null);
    if (method == null
        || owner == null
        || method.getBody().isEmpty()
        || method.getParameters().size() != 1
        || !method.getType().isVoidType()
        || !method.getParameter(0).getType().asString().equals("int[]")
        || (!method.isPrivate() && !method.isStatic() && !method.isFinal() && !owner.isFinal()))
      return Optional.empty();
    String array = method.getParameter(0).getNameAsString();
    String sourceArrayId = AstAnalyzer.nodeId(method.getParameter(0));
    String arrayId = sourceArrayId;
    List<Statement> body = method.getBody().orElseThrow().getStatements();
    if (body.size() < 3 || body.size() > 6 || body.get(body.size() - 1) != extraction)
      return Optional.empty();
    int position = 0;
    VariableDeclarator alias = declaration(body.get(0));
    if (alias != null && alias.getType().asString().equals("int[]")) {
      if (!(alias.getInitializer().orElse(null) instanceof NameExpr source)
          || !source.getNameAsString().equals(array)
          || !LocalVariableBindings.resolve(source).filter(sourceArrayId::equals).isPresent())
        return Optional.empty();
      array = alias.getNameAsString();
      arrayId = AstAnalyzer.nodeId(alias);
      String aliasId = arrayId;
      if (method.findAll(NameExpr.class).stream()
          .filter(name -> LocalVariableBindings.resolve(name).filter(aliasId::equals).isPresent())
          .anyMatch(
              name ->
                  !HeapIndexExpressions.arrayDeclarationId(name)
                      .filter(sourceArrayId::equals)
                      .isPresent())) return Optional.empty();
      position = 1;
    }
    VariableDeclarator size = declaration(body.get(position++));
    if (size == null
        || !size.getType().asString().equals("int")
        || !SortAstSupport.matches(size.getInitializer().orElse(null), array + ".length"))
      return Optional.empty();
    String length = size.getNameAsString();
    String sizeId = AstAnalyzer.nodeId(size);
    Statement buildInitializer =
        body.get(position) instanceof ExpressionStmt ? body.get(position++) : null;
    if (position >= body.size() - 1) return Optional.empty();
    Statement build = body.get(position++);
    Statement extractInitializer =
        position < body.size() - 1 && body.get(position) instanceof ExpressionStmt
            ? body.get(position++)
            : null;
    if (position != body.size() - 1 || !(build instanceof ForStmt || build instanceof WhileStmt))
      return Optional.empty();
    Counter buildCounter = descending(build, buildInitializer, length + " / 2 - 1");
    boolean topDown = buildCounter == null;
    if (topDown) buildCounter = ascending(build, buildInitializer, "1");
    Counter endCounter = descending(extraction, extractInitializer, length + " - 1");
    if (buildCounter == null
        || endCounter == null
        || !(topDown
            ? lessThan(buildCounter.condition(), buildCounter.name(), length)
            : nonnegative(buildCounter.condition(), buildCounter.name()))
        || !positive(endCounter.condition(), endCounter.name())) return Optional.empty();
    List<Statement> buildBody = buildCounter.bodyStatements();
    List<Statement> extractBody = endCounter.bodyStatements();
    SiftSite buildSite = siftSite(buildBody, 0, buildCounter.name());
    SiftSite restoreSite = siftSite(extractBody, 3, "0");
    if (buildSite == null
        || restoreSite == null
        || buildBody.size() != buildSite.width()
        || extractBody.size() != 3 + restoreSite.width()) return Optional.empty();
    var rootSwap =
        ArrayEffects.temporarySwap(extractBody.subList(0, 3), array, "0", endCounter.name());
    if (rootSwap.isEmpty()) return Optional.empty();
    String buildId = buildCounter.declarationId();
    String endId = endCounter.declarationId();
    Map<String, String> roles =
        Map.of(
            "array",
            array,
            "size",
            length,
            "buildCursor",
            buildCounter.name(),
            "end",
            endCounter.name());
    var ids = LocalVariableBindings.roles(method, roles);
    if (ids.isEmpty()
        || !arrayId.equals(ids.get().get("array"))
        || !sizeId.equals(ids.get().get("size"))
        || !buildId.equals(ids.get().get("buildCursor"))
        || !endId.equals(ids.get().get("end"))) return Optional.empty();
    for (Direction direction : Direction.values()) {
      var buildProof = proof(buildSite, method, arrayId, sizeId, direction);
      var restoreProof = proof(restoreSite, method, arrayId, endId, direction);
      if (buildProof.isEmpty()
          || restoreProof.isEmpty()
          || buildProof.get().kind()
              != (topDown ? HeapSiftAnalyzer.Kind.UP : HeapSiftAnalyzer.Kind.DOWN)
          || restoreProof.get().kind() != HeapSiftAnalyzer.Kind.DOWN
          || !siteArguments(buildSite, owner, buildProof.get(), array, length, buildCounter.name())
          || !siteArguments(restoreSite, owner, restoreProof.get(), array, endCounter.name(), "0"))
        continue;
      Map<String, String> operations = new LinkedHashMap<>();
      operations.put("buildLoop", AstAnalyzer.nodeId(build));
      operations.put(
          "buildCall",
          AstAnalyzer.nodeId(buildSite.inline() ? buildSite.node() : buildBody.get(0)));
      operations.put("extractLoop", AstAnalyzer.nodeId(extraction));
      operations.put("rootSwapSave", rootSwap.get().operationAstNodeIds().get(0));
      operations.put("rootSwapRootWrite", rootSwap.get().operationAstNodeIds().get(1));
      operations.put("rootSwapEndWrite", rootSwap.get().operationAstNodeIds().get(2));
      operations.put(
          "restoreCall",
          AstAnalyzer.nodeId(restoreSite.inline() ? restoreSite.node() : extractBody.get(3)));
      List<String> siftIds = restoreProof.get().operationAstNodeIds();
      for (int i = 0; i < siftIds.size(); i++) operations.put("siftSwap" + i, siftIds.get(i));
      MethodDeclaration siftMethod =
          restoreSite.inline()
              ? method
              : owner.getMethods().stream()
                  .filter(
                      candidate ->
                          AstAnalyzer.nodeId(candidate)
                              .equals(restoreProof.get().helperMethodAstNodeId()))
                  .findFirst()
                  .orElse(null);
      if (siftMethod == null) continue;
      operations.put("siftMethod", AstAnalyzer.nodeId(siftMethod));
      if (restoreSite.inline()) operations.put("siftLoop", AstAnalyzer.nodeId(restoreSite.node()));
      List<IfStmt> siftBranches =
          restoreSite.inline()
              ? restoreSite.node().findAll(IfStmt.class)
              : siftMethod.findAll(IfStmt.class);
      if (!siftBranches.isEmpty())
        operations.put("siftChoice", AstAnalyzer.nodeId(siftBranches.get(0)));
      if (siftBranches.size() > 1 && siftMethod.findAll(WhileStmt.class).isEmpty())
        operations.put("siftChoiceRight", AstAnalyzer.nodeId(siftBranches.get(1)));
      MethodDeclaration buildSiftMethod =
          buildSite.inline()
              ? method
              : owner.getMethods().stream()
                  .filter(
                      candidate ->
                          AstAnalyzer.nodeId(candidate)
                              .equals(buildProof.get().helperMethodAstNodeId()))
                  .findFirst()
                  .orElse(null);
      if (buildSiftMethod == null) continue;
      operations.put("buildSiftMethod", AstAnalyzer.nodeId(buildSiftMethod));
      if (buildSite.inline()) operations.put("buildSiftLoop", AstAnalyzer.nodeId(buildSite.node()));
      List<IfStmt> buildBranches =
          buildSite.inline()
              ? buildSite.node().findAll(IfStmt.class)
              : buildSiftMethod.findAll(IfStmt.class);
      if (!buildBranches.isEmpty())
        operations.put("buildSiftChoice", AstAnalyzer.nodeId(buildBranches.get(0)));
      List<String> buildSiftIds = buildProof.get().operationAstNodeIds();
      for (int i = 0; i < buildSiftIds.size(); i++)
        operations.put("buildSiftSwap" + i, buildSiftIds.get(i));
      Map<String, String> allRoles = new LinkedHashMap<>(roles);
      Map<String, String> allIds = new LinkedHashMap<>(ids.get());
      addSiftRole(
          allRoles, allIds, "siftArray", siftMethod, restoreProof.get().arrayDeclarationId());
      addSiftRole(
          allRoles, allIds, "siftBoundary", siftMethod, restoreProof.get().boundaryDeclarationId());
      addSiftRole(allRoles, allIds, "siftNode", siftMethod, restoreProof.get().nodeDeclarationId());
      addSiftRole(
          allRoles,
          allIds,
          "buildSiftArray",
          buildSiftMethod,
          buildProof.get().arrayDeclarationId());
      addSiftRole(
          allRoles,
          allIds,
          "buildSiftBoundary",
          buildSiftMethod,
          buildProof.get().boundaryDeclarationId());
      addSiftRole(
          allRoles, allIds, "buildSiftNode", buildSiftMethod, buildProof.get().nodeDeclarationId());
      if (topDown) {
        Node buildSiftOwner = buildSite.inline() ? buildSite.node() : buildSiftMethod;
        buildSiftOwner.findAll(VariableDeclarator.class).stream()
            .filter(variable -> variable.getType().asString().equals("int"))
            .findFirst()
            .ifPresent(
                variable -> {
                  allRoles.put("buildSiftParent", variable.getNameAsString());
                  allIds.put("buildSiftParent", AstAnalyzer.nodeId(variable));
                });
      }
      Node siftOwner = restoreSite.inline() ? restoreSite.node() : siftMethod;
      List<VariableDeclarator> siftLocals =
          siftOwner.findAll(VariableDeclarator.class).stream()
              .filter(variable -> variable.getType().asString().equals("int"))
              .toList();
      if (siftLocals.size() >= 3 && !topDown) {
        for (int i = 0; i < 3; i++) {
          String role = new String[] {"siftLeft", "siftRight", "siftChosen"}[i];
          allRoles.put(role, siftLocals.get(i).getNameAsString());
          allIds.put(role, AstAnalyzer.nodeId(siftLocals.get(i)));
        }
      }
      var range = method.getRange().orElseThrow();
      return Optional.of(
          new AlgorithmHint(
              "heap-sort",
              0.97,
              "heap-in-place",
              List.of(
                  "Every internal node is restored in the build phase",
                  "Root is swapped with the old heap end before shrink and restoration"),
              allRoles,
              range.begin.line,
              range.end.line,
              AstAnalyzer.nodeId(extraction),
              AstAnalyzer.nodeId(method),
              allIds,
              null,
              null,
              null,
              new HeapPattern(
                  1,
                  direction,
                  topDown ? "TOP_DOWN" : "BOTTOM_UP",
                  operations,
                  restoreProof.get().inclusiveBoundary(),
                  buildProof.get().inclusiveBoundary())));
    }
    return Optional.empty();
  }

  private record Counter(
      String name, String declarationId, Expression condition, List<Statement> bodyStatements) {}

  private record SiftSite(Node node, String cursorId, int width, boolean inline) {}

  private static SiftSite siftSite(List<Statement> statements, int offset, String initialCursor) {
    if (statements.size() <= offset) return null;
    MethodCallExpr helper = call(statements.get(offset));
    if (helper != null) return new SiftSite(helper, null, 1, false);
    if (statements.size() <= offset + 1 || !(statements.get(offset + 1) instanceof WhileStmt loop))
      return null;
    VariableDeclarator cursor = declaration(statements.get(offset));
    if (cursor == null
        || !cursor.getType().asString().equals("int")
        || !SortAstSupport.matches(cursor.getInitializer().orElse(null), initialCursor))
      return null;
    return new SiftSite(loop, AstAnalyzer.nodeId(cursor), 2, true);
  }

  private static Optional<HeapSiftAnalyzer.HeapSiftProof> proof(
      SiftSite site,
      MethodDeclaration method,
      String arrayId,
      String boundaryId,
      Direction direction) {
    if (site.inline())
      return HeapSiftAnalyzer.proveInline(
          (WhileStmt) site.node(), method, arrayId, boundaryId, site.cursorId(), direction);
    return HeapSiftAnalyzer.prove(site.node(), method, arrayId, direction, boundaryId);
  }

  private static boolean siteArguments(
      SiftSite site,
      ClassOrInterfaceDeclaration owner,
      HeapSiftAnalyzer.HeapSiftProof proof,
      String array,
      String boundary,
      String node) {
    return site.inline()
        ? !proof.inclusiveBoundary()
        : arguments((MethodCallExpr) site.node(), owner, proof, array, boundary, node);
  }

  private static Counter descending(Statement loop, Statement initializer, String initial) {
    if (loop instanceof ForStmt counted && initializer == null) return descending(counted, initial);
    if (!(loop instanceof WhileStmt whileLoop)) return null;
    return whileCounter(whileLoop, initializer, initial, false);
  }

  private static Counter ascending(Statement loop, Statement initializer, String initial) {
    if (loop instanceof ForStmt counted && initializer == null) return ascending(counted, initial);
    if (!(loop instanceof WhileStmt whileLoop)) return null;
    return whileCounter(whileLoop, initializer, initial, true);
  }

  private static Counter whileCounter(
      WhileStmt loop, Statement initializer, String initial, boolean increasing) {
    VariableDeclarator counter = declaration(initializer);
    if (counter == null
        || !counter.getType().asString().equals("int")
        || !SortAstSupport.matches(counter.getInitializer().orElse(null), initial)
        || !(loop.getBody() instanceof BlockStmt block)
        || block.getStatements().size() < 2) return null;
    List<Statement> statements = block.getStatements();
    Statement finalStatement = statements.get(statements.size() - 1);
    if (!(finalStatement instanceof ExpressionStmt update)
        || !(increasing
            ? increments(update.getExpression(), counter.getNameAsString())
            : decrements(update.getExpression(), counter.getNameAsString()))) return null;
    return new Counter(
        counter.getNameAsString(),
        AstAnalyzer.nodeId(counter),
        loop.getCondition(),
        List.copyOf(statements.subList(0, statements.size() - 1)));
  }

  private static Counter descending(ForStmt loop, String initial) {
    if (loop.getInitialization().size() != 1
        || loop.getUpdate().size() != 1
        || loop.getCompare().isEmpty()
        || !(loop.getInitialization().get(0) instanceof VariableDeclarationExpr declaration)
        || declaration.getVariables().size() != 1) return null;
    VariableDeclarator counter = declaration.getVariable(0);
    String name = counter.getNameAsString();
    if (!counter.getType().asString().equals("int")
        || !SortAstSupport.matches(counter.getInitializer().orElse(null), initial)
        || !decrements(loop.getUpdate().get(0), name)) return null;
    return new Counter(
        name,
        AstAnalyzer.nodeId(counter),
        loop.getCompare().orElseThrow(),
        statements(loop.getBody()));
  }

  private static Counter ascending(ForStmt loop, String initial) {
    if (loop.getInitialization().size() != 1
        || loop.getUpdate().size() != 1
        || loop.getCompare().isEmpty()
        || !(loop.getInitialization().get(0) instanceof VariableDeclarationExpr declaration)
        || declaration.getVariables().size() != 1) return null;
    VariableDeclarator counter = declaration.getVariable(0);
    String name = counter.getNameAsString();
    if (!counter.getType().asString().equals("int")
        || !SortAstSupport.matches(counter.getInitializer().orElse(null), initial)
        || !increments(loop.getUpdate().get(0), name)) return null;
    return new Counter(
        name,
        AstAnalyzer.nodeId(counter),
        loop.getCompare().orElseThrow(),
        statements(loop.getBody()));
  }

  private static boolean increments(Expression expression, String name) {
    Expression update = SortAstSupport.unwrap(expression);
    if (update instanceof UnaryExpr unary)
      return SortAstSupport.matches(unary.getExpression(), name)
          && (unary.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
              || unary.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT);
    return SortAstSupport.matches(update, name + " += 1")
        || SortAstSupport.matches(update, name + " = " + name + " + 1");
  }

  private static boolean lessThan(Expression expression, String left, String right) {
    return SortAstSupport.matches(expression, left + " < " + right)
        || SortAstSupport.matches(expression, right + " > " + left);
  }

  private static boolean decrements(Expression expression, String name) {
    Expression update = SortAstSupport.unwrap(expression);
    if (update instanceof UnaryExpr unary)
      return SortAstSupport.matches(unary.getExpression(), name)
          && (unary.getOperator() == UnaryExpr.Operator.POSTFIX_DECREMENT
              || unary.getOperator() == UnaryExpr.Operator.PREFIX_DECREMENT);
    return SortAstSupport.matches(update, name + " -= 1")
        || SortAstSupport.matches(update, name + " = " + name + " - 1");
  }

  private static boolean nonnegative(Expression expression, String name) {
    return SortAstSupport.matches(expression, name + " >= 0")
        || SortAstSupport.matches(expression, "0 <= " + name);
  }

  private static boolean positive(Expression expression, String name) {
    return SortAstSupport.matches(expression, name + " > 0")
        || SortAstSupport.matches(expression, "0 < " + name);
  }

  private static List<Statement> statements(Statement body) {
    return body instanceof BlockStmt block ? List.copyOf(block.getStatements()) : List.of(body);
  }

  private static VariableDeclarator declaration(Statement statement) {
    if (!(statement instanceof ExpressionStmt expression)
        || !(expression.getExpression() instanceof VariableDeclarationExpr declaration)
        || declaration.getVariables().size() != 1) return null;
    return declaration.getVariable(0);
  }

  private static MethodCallExpr call(Statement statement) {
    return statement instanceof ExpressionStmt expression
            && expression.getExpression() instanceof MethodCallExpr method
        ? method
        : null;
  }

  private static boolean arguments(
      MethodCallExpr call,
      ClassOrInterfaceDeclaration owner,
      HeapSiftAnalyzer.HeapSiftProof proof,
      String array,
      String boundary,
      String node) {
    MethodDeclaration method =
        owner.getMethods().stream()
            .filter(
                candidate -> AstAnalyzer.nodeId(candidate).equals(proof.helperMethodAstNodeId()))
            .findFirst()
            .orElse(null);
    if (method == null || call.getArguments().size() != method.getParameters().size()) return false;
    for (int i = 0; i < method.getParameters().size(); i++) {
      String id = AstAnalyzer.nodeId(method.getParameter(i));
      String expected =
          id.equals(proof.arrayDeclarationId())
              ? array
              : id.equals(proof.boundaryDeclarationId())
                  ? proof.inclusiveBoundary() ? boundary + " - 1" : boundary
                  : id.equals(proof.nodeDeclarationId()) ? node : null;
      if (expected == null || !SortAstSupport.matches(call.getArgument(i), expected)) return false;
    }
    return true;
  }

  private static void addSiftRole(
      Map<String, String> names,
      Map<String, String> ids,
      String role,
      MethodDeclaration method,
      String declarationId) {
    method.getParameters().stream()
        .filter(parameter -> AstAnalyzer.nodeId(parameter).equals(declarationId))
        .findFirst()
        .ifPresent(
            parameter -> {
              names.put(role, parameter.getNameAsString());
              ids.put(role, declarationId);
            });
    method.findAll(VariableDeclarator.class).stream()
        .filter(variable -> AstAnalyzer.nodeId(variable).equals(declarationId))
        .findFirst()
        .ifPresent(
            variable -> {
              names.put(role, variable.getNameAsString());
              ids.put(role, declarationId);
            });
  }
}
