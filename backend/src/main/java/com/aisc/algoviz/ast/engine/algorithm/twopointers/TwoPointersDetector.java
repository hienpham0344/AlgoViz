package com.aisc.algoviz.ast.engine.algorithm.twopointers;

import com.aisc.algoviz.ast.engine.algorithm.LocalVariableBindings;
import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.aisc.algoviz.ast.engine.model.TwoPointersPattern;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.ForStmt;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Emits a two-pointer hint only when roles, control flow and a paired effect agree. */
public final class TwoPointersDetector {
  private final PointerRoleAnalyzer roles = new PointerRoleAnalyzer();
  private final PointerProgressAnalyzer progress = new PointerProgressAnalyzer();

  public Optional<AlgorithmHint> detect(CompilationUnit unit, Node loop) {
    Node callable = LocalVariableBindings.callable(loop);
    if (callable == null || loop.getRange().isEmpty()) return Optional.empty();
    for (PointerRoleAnalyzer.Candidate candidate : roles.candidates(unit, loop)) {
      Optional<PointerProgressAnalyzer.Proof> result = progress.prove(loop, candidate);
      if (result.isEmpty() || !pairedEffect(loop, candidate)) continue;
      var proof = result.get();
      if (proof.movement() == TwoPointersPattern.Movement.OPPOSING
          && accessAfterBodyUpdate(loop, candidate)) continue;
      Map<String, String> names = new LinkedHashMap<>();
      Map<String, String> ids = new LinkedHashMap<>();
      names.put("data", candidate.dataName());
      ids.put("data", candidate.dataDeclarationId());
      String first = proof.movement() == TwoPointersPattern.Movement.OPPOSING ? "left" : "scan";
      String second = proof.movement() == TwoPointersPattern.Movement.OPPOSING ? "right" : "slow";
      names.put(first, candidate.firstName());
      names.put(second, candidate.secondName());
      ids.put(first, candidate.firstDeclarationId());
      ids.put(second, candidate.secondDeclarationId());
      Map<String, String> operations = new LinkedHashMap<>(proof.operationAstNodeIds());
      operations.put("firstAccess", candidate.accesses().stream()
          .filter(a -> a.indexDeclarationId().equals(candidate.firstDeclarationId()))
          .findFirst().orElseThrow().astNodeId());
      operations.put("secondAccess", candidate.accesses().stream()
          .filter(a -> a.indexDeclarationId().equals(candidate.secondDeclarationId()))
          .findFirst().orElseThrow().astNodeId());
      TwoPointersPattern pattern = new TwoPointersPattern(1, proof.movement(),
          candidate.representation(), ids, operations, proof.evidence());
      var range = loop.getRange().orElseThrow();
      return Optional.of(new AlgorithmHint("two-pointers", 0.9, "array-pointers",
          proof.evidence(), names, range.begin.line, range.end.line,
          AstAnalyzer.nodeId(loop), AstAnalyzer.nodeId(callable), ids,
          null, null, null, null, null, pattern));
    }
    return Optional.empty();
  }

  private static boolean pairedEffect(Node loop, PointerRoleAnalyzer.Candidate candidate) {
    String first = candidate.firstDeclarationId();
    String second = candidate.secondDeclarationId();
    for (BinaryExpr expression : loop.findAll(BinaryExpr.class)) {
      if (containsIndex(expression.getLeft(), candidate, first)
          && containsIndex(expression.getRight(), candidate, second)
          || containsIndex(expression.getLeft(), candidate, second)
              && containsIndex(expression.getRight(), candidate, first)) return true;
    }
    for (AssignExpr assignment : loop.findAll(AssignExpr.class)) {
      if (containsIndex(assignment.getTarget(), candidate, first)
          && containsIndex(assignment.getValue(), candidate, second)
          || containsIndex(assignment.getTarget(), candidate, second)
              && containsIndex(assignment.getValue(), candidate, first)) return true;
    }
    for (MethodCallExpr call : loop.findAll(MethodCallExpr.class)) {
      if (call.getNameAsString().equals("equals") && call.getScope().isPresent()
          && call.getArguments().size() == 1
          && (containsIndex(call.getScope().get(), candidate, first)
              && containsIndex(call.getArgument(0), candidate, second)
              || containsIndex(call.getScope().get(), candidate, second)
                  && containsIndex(call.getArgument(0), candidate, first))) return true;
      if (!call.getNameAsString().equals("set") || call.getArguments().size() != 2) continue;
      if (indexArgument(call.getArgument(0), first)
          && containsIndex(call.getArgument(1), candidate, second)
          || indexArgument(call.getArgument(0), second)
              && containsIndex(call.getArgument(1), candidate, first)) return true;
    }
    return false;
  }

  private static boolean indexArgument(Node argument, String declarationId) {
    return argument instanceof NameExpr name
        && LocalVariableBindings.resolve(name).filter(declarationId::equals).isPresent();
  }

  private static boolean accessAfterBodyUpdate(Node loop,
                                               PointerRoleAnalyzer.Candidate candidate) {
    for (PointerRoleAnalyzer.Access access : candidate.accesses()) {
      Node accessNode = loop.findAll(Node.class).stream()
          .filter(n -> AstAnalyzer.nodeId(n).equals(access.astNodeId()))
          .findFirst().orElse(null);
      if (accessNode == null || accessNode.getBegin().isEmpty()) return true;
      for (Node node : loop.findAll(Node.class)) {
        if (node.getBegin().isEmpty() || !node.getBegin().get().isBefore(accessNode.getBegin().get())
            || node.getParentNode().filter(p -> p instanceof ForStmt).isPresent()) continue;
        NameExpr target = null;
        if (node instanceof UnaryExpr unary && unary.getExpression() instanceof NameExpr name)
          target = name;
        if (node instanceof AssignExpr assignment
            && assignment.getTarget() instanceof NameExpr name) target = name;
        if (target != null && LocalVariableBindings.resolve(target)
            .filter(access.indexDeclarationId()::equals).isPresent()) return true;
      }
    }
    return false;
  }

  private static boolean containsIndex(Node node, PointerRoleAnalyzer.Candidate candidate,
                                       String id) {
    return candidate.accesses().stream().anyMatch(access ->
        access.indexDeclarationId().equals(id) && node.findAll(Node.class).stream()
            .anyMatch(child -> AstAnalyzer.nodeId(child).equals(access.astNodeId())));
  }
}
