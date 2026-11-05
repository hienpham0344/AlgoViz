package com.aisc.algoviz.ast.engine.model;

/** Identifies the suspended frame and bytecode location within one debug run. */
public record ExecutionContext(
    long threadId,
    String threadName,
    String className,
    String methodName,
    String methodSignature,
    int stackDepth,
    long codeIndex,
    @com.fasterxml.jackson.annotation.JsonInclude(
            com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        Long invocationId) {
  public ExecutionContext(long threadId, String threadName, String className,
      String methodName, String methodSignature, int stackDepth, long codeIndex) {
    this(threadId, threadName, className, methodName, methodSignature, stackDepth, codeIndex, null);
  }
}
