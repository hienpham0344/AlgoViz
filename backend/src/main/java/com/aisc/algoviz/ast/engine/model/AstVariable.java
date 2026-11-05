package com.aisc.algoviz.ast.engine.model;

/** Metadata from the source AST used to disambiguate local variables with the same name. */
public record AstVariable(
    String astNodeId,
    String name,
    String declaredType,
    int declarationLine,
    int declarationEndLine,
    int scopeStartLine,
    int scopeEndLine) {

  public boolean isInScopeAt(int lineNumber) {
    return declarationLine <= lineNumber
        && scopeStartLine <= lineNumber
        && lineNumber <= scopeEndLine;
  }

  public int scopeWidth() {
    return scopeEndLine - scopeStartLine;
  }
}
