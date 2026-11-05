package com.aisc.algoviz.ast.engine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aisc.algoviz.ast.engine.dto.AstNode;
import com.aisc.algoviz.ast.engine.exception.CodeParseException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AstServiceTest {

  private final AstService astService = new AstService();

  @Test
  void parsesVariableDeclarationWithPrimitiveTypeAndIntegerLiteral() {
    AstNode root = astService.parse("int x = 10;");

    assertThat(root.getType()).isEqualTo("CompilationUnit");
    assertThat(findNode(root, "VariableDeclaration", "x")).isPresent();
    assertThat(findNode(root, "PrimitiveType", "int")).isPresent();
    assertThat(findNode(root, "IntegerLiteral", "10")).isPresent();
  }

  @Test
  void parsesBinaryExpressionFromVariableInitializer() {
    AstNode root = astService.parse("int x = 10 + 20;");

    assertThat(findNode(root, "BinaryExpression", "+")).isPresent();
  }

  @Test
  void parsesIfStatementConditionAsBinaryExpression() {
    AstNode root = astService.parse("if (x > 10) { x++; }");

    assertThat(findNode(root, "If", "if")).isPresent();
    assertThat(findNode(root, "BinaryExpression", ">")).isPresent();
  }

  @Test
  void parsesForStatementWithVariableDeclarationAndBinaryExpression() {
    AstNode root =
        astService.parse(
            """
                for (int i = 0; i < 5; i++) {
                    System.out.println(i);
                }
                """);

    assertThat(findNode(root, "For", "for")).isPresent();
    assertThat(findNode(root, "VariableDeclaration", "i")).isPresent();
    assertThat(findNode(root, "BinaryExpression", "<")).isPresent();
  }

  @Test
  void rejectsInvalidJavaCodeWithStableErrorCode() {
    assertThatThrownBy(() -> astService.parse("int x ="))
        .isInstanceOf(CodeParseException.class)
        .extracting("code")
        .isEqualTo("INVALID_JAVA_CODE");
  }

  private Optional<AstNode> findNode(AstNode node, String type, String name) {
    if (type.equals(node.getType()) && name.equals(node.getName())) {
      return Optional.of(node);
    }

    return node.getChildren().stream()
        .map(child -> findNode(child, type, name))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }
}
