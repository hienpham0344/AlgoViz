package com.aisc.algoviz.ast.engine.model;

/** A source-level statement that can be observed through a JDI breakpoint. */
public record ObservationPoint(
    String astNodeId,
    String className,
    int lineNumber,
    int startLine,
    int endLine,
    int startColumn,
    int endColumn,
    String statementKind,
    String code) {}
