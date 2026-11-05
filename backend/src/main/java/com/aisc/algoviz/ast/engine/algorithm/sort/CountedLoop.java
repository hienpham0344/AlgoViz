package com.aisc.algoviz.ast.engine.algorithm.sort;

import com.aisc.algoviz.ast.engine.algorithm.LocalVariableBindings;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The counter and logical body of a canonical forward `for` or `while` loop. */
record CountedLoop(
    Node node,
    String counter,
    String counterDeclarationId,
    Expression condition,
    List<Statement> bodyStatements) {
  static Optional<CountedLoop> match(Node node, String initialValue) {
    if (node instanceof ForStmt loop) {
      String name = SortAstSupport.counter(loop, initialValue);
      if (name == null || loop.getCompare().isEmpty()) return Optional.empty();
      var ids = LocalVariableBindings.roles(loop, Map.of("counter", name));
      if (ids.isEmpty()) return Optional.empty();
      return Optional.of(
          new CountedLoop(
              loop, name, ids.get().get("counter"), loop.getCompare().get(), statements(loop.getBody())));
    }
    if (!(node instanceof WhileStmt loop)
        || !(loop.getParentNode().orElse(null) instanceof BlockStmt parent)
        || !(loop.getBody() instanceof BlockStmt body)
        || body.getStatements().isEmpty()) return Optional.empty();
    int index = parent.getStatements().indexOf(loop);
    if (index <= 0
        || !(parent.getStatement(index - 1) instanceof ExpressionStmt previous)
        || !(previous.getExpression() instanceof VariableDeclarationExpr declaration)
        || declaration.getVariables().size() != 1) return Optional.empty();
    var variable = declaration.getVariable(0);
    if (!variable.getType().asString().equals("int")
        || !SortAstSupport.matches(variable.getInitializer().orElse(null), initialValue))
      return Optional.empty();
    String name = variable.getNameAsString();
    if (loop.getCondition().findAll(NameExpr.class).stream()
            .noneMatch(reference -> reference.getNameAsString().equals(name))) return Optional.empty();
    Statement last = body.getStatement(body.getStatements().size() - 1);
    if (!(last instanceof ExpressionStmt update) || !increments(update.getExpression(), name))
      return Optional.empty();
    for (Statement statement : body.getStatements().subList(0, body.getStatements().size() - 1)) {
      if (writesCounter(statement, name)) return Optional.empty();
    }
    var ids = LocalVariableBindings.roles(loop, Map.of("counter", name));
    if (ids.isEmpty()) return Optional.empty();
    return Optional.of(
        new CountedLoop(
            loop,
            name,
            ids.get().get("counter"),
            loop.getCondition(),
            List.copyOf(body.getStatements().subList(0, body.getStatements().size() - 1))));
  }

  private static List<Statement> statements(Statement body) {
    return body instanceof BlockStmt block ? List.copyOf(block.getStatements()) : List.of(body);
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

  private static boolean writesCounter(Statement statement, String name) {
    boolean assignment =
        statement.findAll(AssignExpr.class).stream()
            .anyMatch(write -> SortAstSupport.matches(write.getTarget(), name));
    boolean unary =
        statement.findAll(UnaryExpr.class).stream()
            .anyMatch(
                write ->
                    SortAstSupport.matches(write.getExpression(), name)
                        && (write.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
                            || write.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT
                            || write.getOperator() == UnaryExpr.Operator.POSTFIX_DECREMENT
                            || write.getOperator() == UnaryExpr.Operator.PREFIX_DECREMENT));
    return assignment || unary;
  }
}
