package com.aisc.algoviz.ast.engine.algorithm.graph;

import static com.aisc.algoviz.ast.engine.algorithm.PatternSupport.*;

import com.aisc.algoviz.ast.engine.algorithm.MethodPatternDetector;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import java.util.List;
import java.util.Map;

/** Recognizes disjoint-set find with recursive path compression. */
public final class UnionFindDetector implements MethodPatternDetector {
  @Override
  public List<AlgorithmHint> detect(MethodDeclaration method) {
    for (IfStmt branch : owned(method, IfStmt.class)) {
      if (!(unwrap(branch.getCondition()) instanceof BinaryExpr guard)
          || guard.getOperator() != BinaryExpr.Operator.NOT_EQUALS) continue;
      for (AssignExpr write : owned(branch.getThenStmt(), AssignExpr.class)) {
        if (write.getOperator() != AssignExpr.Operator.ASSIGN
            || !(unwrap(write.getTarget()) instanceof ArrayAccessExpr parent)
            || name(parent.getName()) == null
            || name(parent.getIndex()) == null
            || !(unwrap(write.getValue()) instanceof MethodCallExpr call)
            || !recursive(call, method)) continue;
        if (!(same(guard.getLeft(), parent) && same(guard.getRight(), parent.getIndex())
            || same(guard.getRight(), parent) && same(guard.getLeft(), parent.getIndex())))
          continue;
        boolean followsParent = false, preservesOthers = true;
        for (int i = 0; i < method.getParameters().size(); i++) {
          String parameter = method.getParameter(i).getNameAsString();
          if (parameter.equals(name(parent.getIndex())))
            followsParent = same(call.getArgument(i), parent);
          else preservesOthers &= named(call.getArgument(i), parameter);
        }
        if (followsParent
            && preservesOthers
            && owned(method, ReturnStmt.class).stream()
                .anyMatch(r -> r.getExpression().filter(e -> same(e, parent)).isPresent()))
          return hint(
                  method,
                  "union-find",
                  Map.of("parent", name(parent.getName()), "vertex", name(parent.getIndex())),
                  "Non-root vertices recursively follow their parent and compress the path to the returned root")
              .map(List::of)
              .orElse(List.of());
      }
    }
    return List.of();
  }
}
