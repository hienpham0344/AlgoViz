package com.aisc.algoviz.ast.engine.model;

/** The runtime value of one source variable at one observation point. */
public record ObservationResult(
    String astNodeId,
    String variableName,
    String declaredType,
    String runtimeValue,
    String visualType,
    Object visualValue,
    int lineNumber,
    @com.fasterxml.jackson.annotation.JsonIgnore Long arrayObjectId,
    @com.fasterxml.jackson.annotation.JsonIgnore Long objectId) {
  public ObservationResult(String astNodeId, String variableName, String declaredType,
      String runtimeValue, String visualType, Object visualValue, int lineNumber, Long arrayObjectId) {
    this(astNodeId, variableName, declaredType, runtimeValue, visualType, visualValue,
        lineNumber, arrayObjectId, arrayObjectId);
  }

  public ObservationResult(String astNodeId, String variableName, String declaredType,
      String runtimeValue, String visualType, Object visualValue, int lineNumber) {
    this(astNodeId, variableName, declaredType, runtimeValue, visualType, visualValue,
        lineNumber, null);
  }
}
