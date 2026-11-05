package com.aisc.algoviz.ast.engine.service;

import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.aisc.algoviz.ast.engine.dto.AstNode;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import java.util.ArrayList;
import java.util.List;

public class AstConverter {

  public AstNode convert(Node node) {
    AstNode astNode = createNode(node);

    for (Node child : childrenFor(node)) {
      astNode.addChild(convert(child));
    }

    return astNode;
  }

  AstNode createNode(Node node) {
    AstNode astNode =
        node.getRange()
            .map(range -> new AstNode(typeOf(node), nameOf(node), range.begin.line, range.begin.column))
            .orElseGet(() -> new AstNode(typeOf(node), nameOf(node)));
    astNode.setAstNodeId(AstAnalyzer.nodeId(node));
    node.getRange()
        .ifPresent(
            range -> {
              astNode.setStartLine(range.begin.line);
              astNode.setEndLine(range.end.line);
            });
    return astNode;
  }

  private List<Node> childrenFor(Node node) {

    // =========================
    // COMPILATION UNIT (ROOT)
    // =========================
    // This was the missing case. Without it, the root CompilationUnit
    // fell through to the default branch and returned List.of(),
    // which is why the API always came back with children: [].

    if (node instanceof CompilationUnit compilationUnit) {
      return new ArrayList<>(compilationUnit.getTypes());
    }

    // =========================
    // CLASS
    // =========================

    if (node instanceof ClassOrInterfaceDeclaration clazz) {
      return new ArrayList<>(clazz.getMembers());
    }

    // =========================
    // METHOD
    // =========================

    if (node instanceof MethodDeclaration method) {
      List<Node> children = new ArrayList<>();

      children.add(method.getType());

      children.addAll(method.getParameters());

      method.getBody().ifPresent(children::add);

      return children;
    }

    // =========================
    // PARAMETER
    // =========================

    if (node instanceof Parameter parameter) {
      return List.of(parameter.getType());
    }

    // =========================
    // VARIABLE DECLARATION (e.g. "int x = 10;" or "int a = 1, b = 2;")
    // =========================
    // This was missing, so every VariableDeclarationExpr fell through
    // to the default branch and lost its VariableDeclarator children.

    if (node instanceof VariableDeclarationExpr variableDeclaration) {
      return new ArrayList<>(variableDeclaration.getVariables());
    }

    // =========================
    // VARIABLE
    // =========================

    if (node instanceof VariableDeclarator variable) {
      List<Node> children = new ArrayList<>();

      children.add(variable.getType());

      variable.getInitializer().ifPresent(children::add);

      return children;
    }

    // =========================
    // BLOCK
    // =========================

    if (node instanceof BlockStmt block) {
      return new ArrayList<>(block.getStatements());
    }

    // =========================
    // EXPRESSION STATEMENT
    // =========================

    if (node instanceof ExpressionStmt expressionStmt) {
      return List.of(expressionStmt.getExpression());
    }

    // =========================
    // BINARY
    // =========================

    if (node instanceof BinaryExpr binary) {
      return List.of(binary.getLeft(), binary.getRight());
    }

    // =========================
    // ASSIGNMENT
    // =========================

    if (node instanceof AssignExpr assignment) {
      return List.of(assignment.getTarget(), assignment.getValue());
    }

    // =========================
    // IF
    // =========================

    if (node instanceof IfStmt ifStmt) {
      List<Node> children = new ArrayList<>();

      children.add(ifStmt.getCondition());
      children.add(ifStmt.getThenStmt());

      ifStmt.getElseStmt().ifPresent(children::add);

      return children;
    }

    // =========================
    // FOR
    // =========================

    if (node instanceof ForStmt forStmt) {
      List<Node> children = new ArrayList<>();

      children.addAll(forStmt.getInitialization());

      forStmt.getCompare().ifPresent(children::add);

      children.addAll(forStmt.getUpdate());

      children.add(forStmt.getBody());

      return children;
    }

    // =========================
    // WHILE
    // =========================

    if (node instanceof WhileStmt whileStmt) {
      return List.of(whileStmt.getCondition(), whileStmt.getBody());
    }

    // =========================
    // DO-WHILE
    // =========================

    if (node instanceof DoStmt doStmt) {
      return List.of(doStmt.getBody(), doStmt.getCondition());
    }

    // =========================
    // RETURN
    // =========================

    if (node instanceof ReturnStmt returnStmt) {
      return returnStmt.getExpression().<List<Node>>map(List::of).orElseGet(List::of);
    }

    // =========================
    // UNARY
    // =========================

    if (node instanceof UnaryExpr unary) {
      return List.of(unary.getExpression());
    }

    // =========================
    // ENCLOSED (parenthesized expression, e.g. "(low + high) / 2")
    // =========================
    // Unwrapped directly to its inner expression so parentheses
    // don't add a meaningless extra layer to the tree.

    if (node instanceof EnclosedExpr enclosed) {
      return List.of(enclosed.getInner());
    }

    // =========================
    // ARRAY ACCESS (e.g. "arr[i]")
    // =========================

    if (node instanceof ArrayAccessExpr arrayAccess) {
      return List.of(arrayAccess.getName(), arrayAccess.getIndex());
    }

    // =========================
    // ARRAY INITIALIZER (e.g. "{5, 3, 8, 1}")
    // =========================

    if (node instanceof ArrayInitializerExpr arrayInitializer) {
      return new ArrayList<>(arrayInitializer.getValues());
    }

    // =========================
    // ARRAY CREATION (e.g. "new int[]{5, 3, 8, 1}" or "new int[5]")
    // =========================

    if (node instanceof ArrayCreationExpr arrayCreation) {
      List<Node> children = new ArrayList<>();

      arrayCreation.getInitializer().ifPresent(children::add);

      return children;
    }

    // =========================
    // METHOD CALL
    // =========================

    if (node instanceof MethodCallExpr methodCall) {
      List<Node> children = new ArrayList<>();

      methodCall.getScope().ifPresent(children::add);

      children.addAll(methodCall.getArguments());

      return children;
    }

    // =========================
    // FIELD ACCESS (e.g. "System.out")
    // =========================
    // Also missing previously: the scope of a FieldAccessExpr
    // (e.g. "System" in "System.out") was silently dropped.

    if (node instanceof FieldAccessExpr fieldAccess) {
      return List.of(fieldAccess.getScope());
    }

    // =========================
    // LITERALS / NAMES
    // =========================

    if (node instanceof IntegerLiteralExpr
        || node instanceof DoubleLiteralExpr
        || node instanceof StringLiteralExpr
        || node instanceof BooleanLiteralExpr
        || node instanceof NameExpr) {

      return List.of();
    }

    // =========================
    // TYPE (generic JavaParser Type: PrimitiveType, ClassOrInterfaceType,
    // ArrayType, VoidType, etc. all implement this interface)
    // =========================

    if (node instanceof Type) {
      return List.of();
    }

    // =========================
    // DEFAULT
    // =========================

    return List.of();
  }

  private String typeOf(Node node) {

    if (node instanceof CompilationUnit) {
      return "CompilationUnit";
    }

    if (node instanceof ClassOrInterfaceDeclaration) {
      return "Class";
    }

    if (node instanceof MethodDeclaration) {
      return "Method";
    }

    if (node instanceof Parameter) {
      return "Parameter";
    }

    if (node instanceof VariableDeclarator) {
      return "VariableDeclaration";
    }

    if (node instanceof BinaryExpr) {
      return "BinaryExpression";
    }

    if (node instanceof AssignExpr) {
      return "Assignment";
    }

    if (node instanceof IfStmt) {
      return "If";
    }

    if (node instanceof ForStmt) {
      return "For";
    }

    if (node instanceof WhileStmt) {
      return "While";
    }

    if (node instanceof DoStmt) {
      return "DoWhile";
    }

    if (node instanceof ArrayAccessExpr) {
      return "ArrayAccess";
    }

    if (node instanceof ArrayInitializerExpr) {
      return "ArrayInitializer";
    }

    if (node instanceof ArrayCreationExpr) {
      return "ArrayCreation";
    }

    if (node instanceof EnclosedExpr) {
      return "Enclosed";
    }

    if (node instanceof ReturnStmt) {
      return "Return";
    }

    if (node instanceof UnaryExpr) {
      return "UnaryExpression";
    }

    if (node instanceof MethodCallExpr) {
      return "MethodCall";
    }

    if (node instanceof IntegerLiteralExpr) {
      return "IntegerLiteral";
    }

    if (node instanceof DoubleLiteralExpr) {
      return "DoubleLiteral";
    }

    if (node instanceof StringLiteralExpr) {
      return "StringLiteral";
    }

    if (node instanceof BooleanLiteralExpr) {
      return "BooleanLiteral";
    }

    if (node instanceof NameExpr) {
      return "VariableReference";
    }

    if (node instanceof BlockStmt) {
      return "Block";
    }

    if (node instanceof ExpressionStmt) {
      return "Expression";
    }

    if (node instanceof PrimitiveType) {
      return "PrimitiveType";
    }

    // Non-primitive JavaParser types (ClassOrInterfaceType, ArrayType,
    // VoidType, ...) share one normalized node type.
    if (node instanceof Type) {
      return "Type";
    }

    return stripSuffix(node.getClass().getSimpleName());
  }

  private String nameOf(Node node) {

    if (node instanceof CompilationUnit) {
      return "CompilationUnit";
    }

    if (node instanceof ClassOrInterfaceDeclaration clazz) {
      return clazz.getNameAsString();
    }

    if (node instanceof MethodDeclaration method) {
      return method.getNameAsString();
    }

    if (node instanceof Parameter parameter) {
      return parameter.getNameAsString();
    }

    if (node instanceof VariableDeclarator variable) {
      return variable.getNameAsString();
    }

    if (node instanceof AssignExpr assignment) {
      return assignment.getOperator().asString();
    }

    if (node instanceof BinaryExpr binary) {
      return binary.getOperator().asString();
    }

    if (node instanceof IfStmt) {
      return "if";
    }

    if (node instanceof ForStmt) {
      return "for";
    }

    if (node instanceof WhileStmt) {
      return "while";
    }

    if (node instanceof DoStmt) {
      return "do";
    }

    if (node instanceof ArrayCreationExpr arrayCreation) {
      return arrayCreation.getElementType().asString() + "[]";
    }

    if (node instanceof UnaryExpr unary) {
      return unary.getOperator().asString();
    }

    if (node instanceof IntegerLiteralExpr literal) {
      return literal.getValue();
    }

    if (node instanceof DoubleLiteralExpr literal) {
      return literal.getValue();
    }

    if (node instanceof StringLiteralExpr literal) {
      return literal.getValue();
    }

    if (node instanceof BooleanLiteralExpr literal) {
      return String.valueOf(literal.getValue());
    }

    if (node instanceof NameExpr name) {
      return name.getNameAsString();
    }

    if (node instanceof MethodCallExpr methodCall) {
      return methodCall.getNameAsString();
    }

    if (node instanceof FieldAccessExpr fieldAccess) {
      return fieldAccess.getNameAsString();
    }

    // Generic JavaParser Type: covers PrimitiveType ("int"),
    // ClassOrInterfaceType ("String"), ArrayType ("String[]"), etc.
    if (node instanceof Type type) {
      return type.asString();
    }

    return typeOf(node);
  }

  private String stripSuffix(String simpleName) {

    if (simpleName.endsWith("Expr")) {
      return simpleName.substring(0, simpleName.length() - 4);
    }

    if (simpleName.endsWith("Stmt")) {
      return simpleName.substring(0, simpleName.length() - 4);
    }

    return simpleName;
  }
}
