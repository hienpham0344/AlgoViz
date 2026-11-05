package com.aisc.algoviz.ast.engine.ast;

import com.aisc.algoviz.ast.engine.exception.CodeParseException;
import com.aisc.algoviz.ast.engine.model.AstVariable;
import com.aisc.algoviz.ast.engine.model.ObservationPoint;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.LocalClassDeclarationStmt;
import com.github.javaparser.ast.stmt.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Parses a complete Java source file and extracts the source metadata required by the JDI layer.
 *
 * <p>The generated IDs are derived from source ranges instead of object identity. They therefore
 * remain stable whenever the same source is parsed again, including when the AST is rendered for
 * the UI in a separate request.
 */
@Component
public class AstAnalyzer {

  private final JavaParser javaParser;

  public AstAnalyzer() {
    ParserConfiguration configuration =
        new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
    this.javaParser = new JavaParser(configuration);
  }

  public Analysis analyze(String sourceCode) {
    CompilationUnit compilationUnit = parseCompilationUnit(sourceCode);
    TargetType targetType = findTargetType(compilationUnit);
    List<AstVariable> variables = extractVariables(compilationUnit);
    List<ObservationPoint> points =
        extractObservationPoints(targetType.declaration(), targetType.className(), sourceCode);
    return new Analysis(
        compilationUnit,
        targetType.className(),
        targetType.hasMainMethod(),
        List.copyOf(points),
        List.copyOf(variables));
  }

  public synchronized CompilationUnit parseCompilationUnit(String sourceCode) {
    ParseResult<CompilationUnit> result = javaParser.parse(sourceCode);
    if (result.isSuccessful() && result.getResult().isPresent()) {
      return result.getResult().get();
    }

    List<String> problems = new ArrayList<>();
    for (Problem problem : result.getProblems()) {
      problems.add(formatProblem(problem));
    }
    throw new CodeParseException("Unable to parse Java source code", "INVALID_JAVA_CODE", problems);
  }

  /** Returns the shared deterministic ID used by both the AST and runtime result payloads. */
  public static String nodeId(Node node) {
    return node.getRange()
        .map(
            range ->
                "ast-"
                    + node.getClass().getSimpleName()
                    + "-"
                    + range.begin.line
                    + "-"
                    + range.begin.column
                    + "-"
                    + range.end.line
                    + "-"
                    + range.end.column)
        .orElse("ast-" + node.getClass().getSimpleName() + "-unranged");
  }

  private TargetType findTargetType(CompilationUnit compilationUnit) {
    List<ClassOrInterfaceDeclaration> topLevelClasses =
        compilationUnit.getTypes().stream()
            .filter(ClassOrInterfaceDeclaration.class::isInstance)
            .map(ClassOrInterfaceDeclaration.class::cast)
            .filter(clazz -> !clazz.isInterface())
            .toList();

    ClassOrInterfaceDeclaration selected =
        topLevelClasses.stream()
            .filter(this::hasMainMethod)
            .findFirst()
            .or(() -> topLevelClasses.stream().findFirst())
            .orElseThrow(
                () ->
                    new CodeParseException(
                        "A Java source file must declare a top-level class",
                        "MISSING_TOP_LEVEL_CLASS",
                        List.of()));

    String packageName =
        compilationUnit.getPackageDeclaration().map(pkg -> pkg.getNameAsString() + ".").orElse("");
    return new TargetType(
        packageName + selected.getNameAsString(), hasMainMethod(selected), selected);
  }

  private boolean hasMainMethod(ClassOrInterfaceDeclaration declaration) {
    return declaration.getMethods().stream()
        .anyMatch(
            method ->
                method.getNameAsString().equals("main")
                    && method.isPublic()
                    && method.isStatic()
                    && method.getType().isVoidType()
                    && method.getParameters().size() == 1
                    && method.getParameter(0).getType().isArrayType());
  }

  private List<ObservationPoint> extractObservationPoints(
      ClassOrInterfaceDeclaration targetClass, String className, String sourceCode) {
    return targetClass.findAll(Statement.class).stream()
        .filter(
            statement ->
                statement
                    .findAncestor(ClassOrInterfaceDeclaration.class)
                    .map(targetClass::equals)
                    .orElse(false))
        .filter(this::isExecutableStatement)
        .filter(statement -> statement.getRange().isPresent())
        .map(
            statement ->
                new ObservationPoint(
                    nodeId(statement),
                    className,
                    statement.getRange().get().begin.line,
                    statement.getRange().get().begin.line,
                    statement.getRange().get().end.line,
                    statement.getRange().get().begin.column,
                    statement.getRange().get().end.column,
                    statement.getClass().getSimpleName(),
                    rangeText(statement, sourceCode)))
        .sorted(Comparator.comparingInt(ObservationPoint::lineNumber))
        .toList();
  }

  private String rangeText(Node node, String sourceCode) {
    return node.getRange()
        .map(
            range -> {
              int startOffset = offsetOfLineColumn(sourceCode, range.begin.line, range.begin.column);
              int endOffset = offsetOfLineColumn(sourceCode, range.end.line, range.end.column);
              if (startOffset < 0 || endOffset < startOffset || startOffset >= sourceCode.length()) {
                return node.toString();
              }
              return sourceCode.substring(startOffset, Math.min(endOffset + 1, sourceCode.length()));
            })
        .orElseGet(node::toString);
  }

  private int offsetOfLineColumn(String sourceCode, int line, int column) {
    int currentLine = 1;
    int offset = 0;
    while (offset < sourceCode.length() && currentLine < line) {
      if (sourceCode.charAt(offset) == '\n') {
        currentLine++;
      }
      offset++;
    }
    return offset + column - 1;
  }

  private boolean isExecutableStatement(Statement statement) {
    return !(statement instanceof BlockStmt
        || statement instanceof EmptyStmt
        || statement instanceof LabeledStmt
        || statement instanceof LocalClassDeclarationStmt);
  }

  @SuppressWarnings("unchecked") // JavaParser exposes findAncestor through a generic varargs API.
  private List<AstVariable> extractVariables(CompilationUnit compilationUnit) {
    List<AstVariable> variables = new ArrayList<>();

    for (VariableDeclarator declarator : compilationUnit.findAll(VariableDeclarator.class)) {
      if (declarator.findAncestor(FieldDeclaration.class).isPresent()
          || findCallable(declarator).isEmpty()) {
        continue;
      }
      variables.add(
          toAstVariable(
              declarator,
              declarator.getNameAsString(),
              declaredType(declarator),
              findLocalScope(declarator)));
    }

    for (Parameter parameter : compilationUnit.findAll(Parameter.class)) {
      Node scope = findParameterScope(parameter);
      if (scope != null) {
        variables.add(
            toAstVariable(
                parameter, parameter.getNameAsString(), parameter.getType().asString(), scope));
      }
    }

    return variables;
  }

  @SuppressWarnings("unchecked") // JavaParser exposes findAncestor through a generic varargs API.
  private Optional<CallableDeclaration<?>> findCallable(Node node) {
    Optional<MethodDeclaration> method = node.findAncestor(MethodDeclaration.class);
    if (method.isPresent()) {
      return Optional.of((CallableDeclaration<?>) method.get());
    }
    return node
        .findAncestor(ConstructorDeclaration.class)
        .map(constructor -> (CallableDeclaration<?>) constructor);
  }

  private AstVariable toAstVariable(Node node, String name, String type, Node scopeNode) {
    int declarationLine = node.getRange().map(range -> range.begin.line).orElse(-1);
    int declarationEndLine = node.getRange().map(range -> range.end.line).orElse(declarationLine);
    int scopeStartLine = scopeNode.getRange().map(range -> range.begin.line).orElse(declarationLine);
    int scopeEndLine = scopeNode.getRange().map(range -> range.end.line).orElse(declarationEndLine);
    return new AstVariable(
        nodeId(node),
        name,
        type,
        declarationLine,
        declarationEndLine,
        scopeStartLine,
        scopeEndLine);
  }

  private Node findLocalScope(VariableDeclarator declarator) {
    Optional<Node> current = declarator.getParentNode();
    while (current.isPresent()) {
      Node node = current.get();
      if (node instanceof ForStmt
          || node instanceof ForEachStmt
          || node instanceof CatchClause
          || node instanceof BlockStmt) {
        return node;
      }
      current = node.getParentNode();
    }
    return declarator;
  }

  private String declaredType(VariableDeclarator declarator) {
    if (!declarator.getType().isVarType()) {
      return declarator.getType().asString();
    }
    return declarator.getInitializer().map(this::inferInitializerType).orElse("var");
  }

  private String inferInitializerType(Expression initializer) {
    if (initializer instanceof EnclosedExpr enclosedExpr) {
      return inferInitializerType(enclosedExpr.getInner());
    }
    if (initializer instanceof BooleanLiteralExpr) {
      return "boolean";
    }
    if (initializer instanceof CharLiteralExpr) {
      return "char";
    }
    if (initializer instanceof StringLiteralExpr) {
      return "java.lang.String";
    }
    if (initializer instanceof LongLiteralExpr) {
      return "long";
    }
    if (initializer instanceof DoubleLiteralExpr doubleLiteral) {
      String literal = doubleLiteral.getValue();
      return literal.endsWith("f") || literal.endsWith("F") ? "float" : "double";
    }
    if (initializer instanceof IntegerLiteralExpr) {
      return "int";
    }
    if (initializer instanceof ArrayCreationExpr arrayCreation) {
      return arrayCreation.getElementType().asString() + "[]".repeat(arrayCreation.getLevels().size());
    }
    if (initializer instanceof ObjectCreationExpr objectCreation) {
      return objectCreation.getType().asString();
    }
    return "var";
  }

  private Node findParameterScope(Parameter parameter) {
    Optional<CatchClause> catchClause = parameter.findAncestor(CatchClause.class);
    if (catchClause.isPresent()) {
      return catchClause.get();
    }
    Optional<CallableDeclaration<?>> callable = findCallable(parameter);
    if (callable.isPresent()) {
      return callableScope(callable.get());
    }
    return null;
  }

  private Node callableScope(CallableDeclaration<?> callable) {
    if (callable instanceof MethodDeclaration method) {
      Optional<BlockStmt> body = method.getBody();
      return body.isPresent() ? body.get() : method;
    }
    if (callable instanceof ConstructorDeclaration constructor) {
      return constructor.getBody();
    }
    return callable;
  }

  private String formatProblem(Problem problem) {
    return problem
        .getLocation()
        .map(
            location ->
                "Line "
                    + location.toRange().map(range -> range.begin.line).orElse(-1)
                    + ": "
                    + problem.getMessage())
        .orElse(problem.getMessage());
  }

  private record TargetType(
      String className, boolean hasMainMethod, ClassOrInterfaceDeclaration declaration) {}

  public record Analysis(
      CompilationUnit compilationUnit,
      String mainClassName,
      boolean hasMainMethod,
      List<ObservationPoint> observationPoints,
      List<AstVariable> variables) {}
}
