package com.aisc.algoviz.ast.engine.algorithm.graph;

import static com.aisc.algoviz.ast.engine.algorithm.PatternSupport.*;

import com.aisc.algoviz.ast.engine.algorithm.MethodPatternDetector;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Matrix shortest-path recurrence. Other path algorithms are added as separate evidence rules. */
public final class GraphPathsDetector implements MethodPatternDetector {
  @Override
  public List<AlgorithmHint> detect(MethodDeclaration method) {
    List<AlgorithmHint> result = new ArrayList<>();
    for (AssignExpr write : owned(method, AssignExpr.class)) {
      if (write.getOperator() != AssignExpr.Operator.ASSIGN
          || !math(write.getValue(), "min")
          || !(unwrap(write.getTarget()) instanceof ArrayAccessExpr target)
          || !(unwrap(target.getName()) instanceof ArrayAccessExpr row)
          || name(row.getName()) == null) continue;
      var call = unwrap(write.getValue()).asMethodCallExpr();
      for (Expression term : call.getArguments()) {
        Expression other = call.getArgument(term == call.getArgument(0) ? 1 : 0);
        if (!same(term, target)
            || !(unwrap(other) instanceof BinaryExpr sum)
            || sum.getOperator() != BinaryExpr.Operator.PLUS
            || !(unwrap(sum.getLeft()) instanceof ArrayAccessExpr left)
            || !(unwrap(left.getName()) instanceof ArrayAccessExpr leftRow)
            || !(unwrap(sum.getRight()) instanceof ArrayAccessExpr right)
            || !(unwrap(right.getName()) instanceof ArrayAccessExpr rightRow)) continue;
        if (!same(row.getName(), leftRow.getName())
            || !same(row.getName(), rightRow.getName())
            || !same(row.getIndex(), leftRow.getIndex())
            || !same(target.getIndex(), right.getIndex())
            || !same(left.getIndex(), rightRow.getIndex())
            || same(left.getIndex(), row.getIndex())
            || same(left.getIndex(), target.getIndex())) continue;
        Node outer = write;
        int depth = 0;
        for (Node n = write; n != method; n = n.getParentNode().orElseThrow())
          if (isLoop(n)) {
            depth++;
            outer = n;
          }
        boolean intermediateFirst =
            outer instanceof com.github.javaparser.ast.stmt.ForStmt scan
                && scan.getInitialization().size() == 1
                && scan.getInitialization().get(0).isVariableDeclarationExpr()
                && scan.getInitialization().get(0).asVariableDeclarationExpr().getVariables().size()
                    == 1
                && scan.getInitialization()
                    .get(0)
                    .asVariableDeclarationExpr()
                    .getVariable(0)
                    .getNameAsString()
                    .equals(name(left.getIndex()));
        if (depth == 3 && intermediateFirst)
          hint(
                  outer,
                  "floyd-warshall",
                  Map.of("distance", name(row.getName())),
                  "Three nested loops relax d[i][j] through the same intermediate vertex d[i][k] + d[k][j]")
              .ifPresent(result::add);
      }
    }
    return result;
  }
}
