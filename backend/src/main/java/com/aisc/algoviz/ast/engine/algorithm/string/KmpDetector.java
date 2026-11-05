package com.aisc.algoviz.ast.engine.algorithm.string;

import static com.aisc.algoviz.ast.engine.algorithm.PatternSupport.*;

import com.aisc.algoviz.ast.engine.algorithm.MethodPatternDetector;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import java.util.List;
import java.util.Map;

/**
 * KMP search evidence links mismatches, prefix fallback and subsequent matching-character advance.
 */
public final class KmpDetector implements MethodPatternDetector {
  @Override
  public List<AlgorithmHint> detect(MethodDeclaration method) {
    for (WhileStmt fallback : owned(method, WhileStmt.class)) {
      if (loop(fallback).isEmpty()) continue;
      for (AssignExpr update : owned(fallback, AssignExpr.class)) {
        String matched = name(update.getTarget());
        if (matched == null
            || update.getOperator() != AssignExpr.Operator.ASSIGN
            || !(unwrap(update.getValue()) instanceof ArrayAccessExpr table)
            || name(table.getName()) == null
            || !offset(table.getIndex(), update.getTarget(), -1)) continue;
        boolean positive =
            owned(fallback.getCondition(), BinaryExpr.class).stream()
                .anyMatch(
                    b ->
                        b.getOperator() == BinaryExpr.Operator.GREATER
                            && named(b.getLeft(), matched)
                            && integer(b.getRight(), 0));
        if (!positive) continue;
        for (BinaryExpr mismatch : owned(fallback.getCondition(), BinaryExpr.class)) {
          if (mismatch.getOperator() != BinaryExpr.Operator.NOT_EQUALS
              || !(unwrap(mismatch.getLeft()) instanceof MethodCallExpr text)
              || !(unwrap(mismatch.getRight()) instanceof MethodCallExpr pattern)
              || !character(text)
              || !character(pattern)
              || !named(pattern.getArgument(0), matched)
              || same(text.getScope().orElse(null), pattern.getScope().orElse(null))) continue;
          for (IfStmt match : owned(loop(fallback).orElseThrow(), IfStmt.class)) {
            if (!(unwrap(match.getCondition()) instanceof BinaryExpr equality)
                || equality.getOperator() != BinaryExpr.Operator.EQUALS
                || !(same(equality.getLeft(), text) && same(equality.getRight(), pattern)
                    || same(equality.getRight(), text) && same(equality.getLeft(), pattern)))
              continue;
            boolean advances =
                owned(match.getThenStmt(), UnaryExpr.class).stream()
                    .anyMatch(
                        u ->
                            named(u.getExpression(), matched)
                                && (u.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
                                    || u.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT));
            if (advances
                && name(text.getScope().orElse(null)) != null
                && name(pattern.getScope().orElse(null)) != null)
              return hint(
                      loop(fallback).orElseThrow(),
                      "kmp",
                      Map.of(
                          "text",
                          name(text.getScope().orElseThrow()),
                          "pattern",
                          name(pattern.getScope().orElseThrow()),
                          "failureTable",
                          name(table.getName()),
                          "matched",
                          matched),
                      "Mismatch follows the prefix-table fallback at matched minus one",
                      "A matching text/pattern character then extends the matched prefix")
                  .map(List::of)
                  .orElse(List.of());
          }
        }
      }
    }
    return List.of();
  }

  private boolean character(MethodCallExpr call) {
    return call.getNameAsString().equals("charAt")
        && call.getArguments().size() == 1
        && call.getScope().isPresent();
  }
}
