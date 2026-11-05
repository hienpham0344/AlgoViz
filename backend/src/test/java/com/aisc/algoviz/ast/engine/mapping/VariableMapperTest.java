package com.aisc.algoviz.ast.engine.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.aisc.algoviz.ast.engine.model.AstVariable;
import com.aisc.algoviz.ast.engine.model.ObservationPoint;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.PrimitiveValue;
import com.sun.jdi.StackFrame;
import java.util.List;
import org.junit.jupiter.api.Test;

class VariableMapperTest {

  private final VariableMapper mapper = new VariableMapper();

  @Test
  void mapsVariablesUsingRuntimeLineWhenDifferentFromAstStart() throws Exception {
    StackFrame frame = mock(StackFrame.class);
    LocalVariable local = mock(LocalVariable.class);
    when(local.name()).thenReturn("value");
    when(local.typeName()).thenReturn("int");
    when(frame.visibleVariables()).thenReturn(List.of(local));
    PrimitiveValue runtimeValue = primitiveValue("7");
    when(frame.getValue(local)).thenReturn(runtimeValue);
    var point = new ObservationPoint("statement", "Sample", 3, 3, 8, 1, 10, "TryStmt", "try {}");
    var declaration = new AstVariable("local-value", "value", "int", 5, 5, 4, 8);
    assertThat(mapper.map(frame, point, List.of(declaration), 6)).singleElement().satisfies(v -> {
      assertThat(v.lineNumber()).isEqualTo(6);
      assertThat(v.astNodeId()).isEqualTo("local-value");
      assertThat(v.runtimeValue()).isEqualTo("7");
    });
  }

  @Test
  void selectsTheInnermostMatchingAstVariableForSiblingScopes() {
    AstVariable firstScope = new AstVariable("first-value", "value", "int", 5, 5, 4, 7);
    AstVariable secondScope = new AstVariable("second-value", "value", "int", 8, 8, 7, 10);

    assertThat(mapper.findAstVariable("value", "int", 9, List.of(firstScope, secondScope)))
        .contains(secondScope);
  }

  @Test
  void preservesArrayDimensionsWhenMatchingTypes() {
    AstVariable oneDimension = new AstVariable("one-dimension", "values", "int[]", 5, 5, 4, 10);
    AstVariable twoDimensions =
        new AstVariable("two-dimensions", "values", "int[][]", 5, 5, 4, 10);

    assertThat(mapper.findAstVariable("values", "int[][]", 6, List.of(oneDimension, twoDimensions)))
        .contains(twoDimensions);
    assertThat(mapper.findAstVariable("values", "int[]", 6, List.of(twoDimensions)))
        .isEmpty();
  }

  @Test
  void selectsTheMatchingTypeForSameNameVariablesInSiblingScopes() {
    AstVariable integer = new AstVariable("integer", "value", "int", 5, 5, 4, 7);
    AstVariable string = new AstVariable("string", "value", "String", 9, 9, 8, 12);

    assertThat(mapper.findAstVariable("value", "java.lang.String", 10, List.of(integer, string)))
        .contains(string);
    assertThat(mapper.findAstVariable("value", "int", 6, List.of(integer, string)))
        .contains(integer);
  }

  @Test
  void doesNotMatchDifferentReferenceArrayDimensions() {
    AstVariable string = new AstVariable("string", "value", "String", 5, 5, 4, 10);
    AstVariable stringArray = new AstVariable("string-array", "value", "String[]", 5, 5, 4, 10);

    assertThat(mapper.findAstVariable("value", "java.lang.String[]", 6, List.of(string)))
        .isEmpty();
    assertThat(mapper.findAstVariable("value", "java.lang.String", 6, List.of(stringArray)))
        .isEmpty();
  }

  @Test
  void matchesAnInferredVarDeclarationToItsJdiType() {
    AstVariable inferred = new AstVariable("inferred", "value", "var", 5, 5, 4, 10);

    assertThat(mapper.findAstVariable("value", "int", 6, List.of(inferred))).contains(inferred);
  }

  @Test
  void keepsOtherSnapshotRowsWhenOneJdiValueCannotBeRead() throws Exception {
    StackFrame frame = mock(StackFrame.class);
    LocalVariable broken = mock(LocalVariable.class);
    LocalVariable healthy = mock(LocalVariable.class);
    when(broken.name()).thenReturn("broken");
    when(broken.typeName()).thenReturn("int");
    when(healthy.name()).thenReturn("healthy");
    when(healthy.typeName()).thenReturn("int");
    when(frame.visibleVariables()).thenReturn(List.of(broken, healthy));
    doThrow(new RuntimeException("collected")).when(frame).getValue(broken);
    PrimitiveValue healthyValue = primitiveValue("7");
    when(frame.getValue(healthy)).thenReturn(healthyValue);

    assertThat(
            mapper.map(
                frame,
                new ObservationPoint("statement", "demo.Sample", 6, 6, 6, 1, 20, "ExpressionStmt", "call();"),
                List.of()))
        .extracting(result -> result.variableName(), result -> result.runtimeValue())
        .containsExactly(tuple("broken", "[unavailable]"), tuple("healthy", "7"));
  }

  @Test
  void keepsOtherSnapshotRowsWhenOneLocalTypeCannotBeRead() throws Exception {
    StackFrame frame = mock(StackFrame.class);
    LocalVariable broken = mock(LocalVariable.class);
    LocalVariable healthy = mock(LocalVariable.class);
    when(broken.name()).thenReturn("broken");
    when(broken.typeName()).thenThrow(new RuntimeException("type unavailable"));
    when(healthy.name()).thenReturn("healthy");
    when(healthy.typeName()).thenReturn("int");
    when(frame.visibleVariables()).thenReturn(List.of(broken, healthy));
    PrimitiveValue brokenValue = primitiveValue("5");
    PrimitiveValue healthyValue = primitiveValue("7");
    when(frame.getValue(broken)).thenReturn(brokenValue);
    when(frame.getValue(healthy)).thenReturn(healthyValue);

    assertThat(
            mapper.map(
                frame,
                new ObservationPoint("statement", "demo.Sample", 6, 6, 6, 1, 20, "ExpressionStmt", "call();"),
                List.of()))
        .extracting(result -> result.variableName(), result -> result.runtimeValue())
        .containsExactly(tuple("broken", "5"), tuple("healthy", "7"));
  }

  @Test
  void keepsRuntimeValueWhenOneLocalNameCannotBeRead() throws Exception {
    StackFrame frame = mock(StackFrame.class);
    LocalVariable broken = mock(LocalVariable.class);
    LocalVariable healthy = mock(LocalVariable.class);
    when(broken.name()).thenThrow(new RuntimeException("name unavailable"));
    when(broken.typeName()).thenReturn("int");
    when(healthy.name()).thenReturn("healthy");
    when(healthy.typeName()).thenReturn("int");
    when(frame.visibleVariables()).thenReturn(List.of(broken, healthy));
    PrimitiveValue brokenValue = primitiveValue("5");
    PrimitiveValue healthyValue = primitiveValue("7");
    when(frame.getValue(broken)).thenReturn(brokenValue);
    when(frame.getValue(healthy)).thenReturn(healthyValue);

    assertThat(
            mapper.map(
                frame,
                new ObservationPoint("statement", "demo.Sample", 6, 6, 6, 1, 20, "ExpressionStmt", "call();"),
                List.of()))
        .extracting(result -> result.variableName(), result -> result.runtimeValue())
        .containsExactly(tuple("<unavailable>", "5"), tuple("healthy", "7"));
  }

  private PrimitiveValue primitiveValue(String text) {
    PrimitiveValue value = mock(PrimitiveValue.class);
    when(value.toString()).thenReturn(text);
    return value;
  }
}
