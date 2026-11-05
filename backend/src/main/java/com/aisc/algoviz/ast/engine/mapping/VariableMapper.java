package com.aisc.algoviz.ast.engine.mapping;

import com.aisc.algoviz.ast.engine.model.AstVariable;
import com.aisc.algoviz.ast.engine.model.ObservationPoint;
import com.aisc.algoviz.ast.engine.model.ObservationResult;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.CharValue;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveValue;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.Value;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Converts JDI-visible locals into result records that point back to their AST declarations. */
@Component
public class VariableMapper {

  private static final String UNAVAILABLE_METADATA = "<unavailable>";
  private static final int MAX_ARRAY_ELEMENTS = 100;
  private static final int MAX_NESTING_DEPTH = 3;
  private static final int MAX_STRING_LENGTH = 2_000;
  private static final int MAX_TOTAL_ARRAY_VALUES = 1_000;
  private static final int MAX_LINKED_NODES = 100;
  private static final int MAX_TREE_NODES = 100;
  private static final int MAX_MAP_ENTRIES = 100;

  public List<ObservationResult> map(
      StackFrame frame, ObservationPoint observationPoint, List<AstVariable> astVariables)
    throws AbsentInformationException {
    return map(frame, observationPoint, astVariables, observationPoint.lineNumber(),
        new IntervalValueProjection(Map.of()));
  }

  public List<ObservationResult> map(
      StackFrame frame, ObservationPoint observationPoint, List<AstVariable> astVariables,
      int eventLine) throws AbsentInformationException {
    return map(frame, observationPoint, astVariables, eventLine,
        new IntervalValueProjection(Map.of()));
  }

  public List<ObservationResult> map(
      StackFrame frame,
      ObservationPoint observationPoint,
      List<AstVariable> astVariables,
      int eventLine,
      IntervalValueProjection intervalProjection) throws AbsentInformationException {
    return map(frame, observationPoint, astVariables, eventLine, intervalProjection,
        new IntegerListProjection(Set.of()));
  }

  public List<ObservationResult> map(
      StackFrame frame,
      ObservationPoint observationPoint,
      List<AstVariable> astVariables,
      int eventLine,
      IntervalValueProjection intervalProjection,
      IntegerListProjection integerListProjection) throws AbsentInformationException {
    List<LocalVariable> visibleVariables = frame.visibleVariables();
    List<ObservationResult> results = new ArrayList<>();
    for (LocalVariable variable : visibleVariables) {
      String variableName = readVariableName(variable);
      String jdiTypeName = readTypeName(variable);
      Optional<AstVariable> astVariable =
          findAstVariable(
              variableName, jdiTypeName, eventLine, astVariables);
      FormattedValue formattedValue;
      Long arrayObjectId = null;
      Long objectId = null;
      try {
        Value value = frame.getValue(variable);
        formattedValue = format(value);
        if (value instanceof ArrayReference arrayReference) {
          arrayObjectId = arrayReference.uniqueID();
          objectId = arrayObjectId;
        } else if (value instanceof ObjectReference objectReference) {
          objectId = objectReference.uniqueID();
          IntervalValueProjection.Fields fields = astVariable
              .map(ast -> intervalProjection.fieldsFor(ast.astNodeId())).orElse(null);
          if (fields != null)
            formattedValue = projectIntervalList(objectReference, fields,
                astVariable.map(AstVariable::declaredType).orElse(""));
          else if (astVariable.map(ast -> integerListProjection.includes(ast.astNodeId()))
              .orElse(false)) formattedValue = projectIntegerList(objectReference);
        }
      } catch (RuntimeException ex) {
        formattedValue = new FormattedValue("[unavailable]", "unavailable", null);
      }
      results.add(
          new ObservationResult(
              astVariable.map(AstVariable::astNodeId).orElse(observationPoint.astNodeId()),
              variableName,
              astVariable.map(AstVariable::declaredType).orElse(jdiTypeName),
              formattedValue.text(),
              formattedValue.visualType(),
              formattedValue.visualValue(),
              eventLine,
              arrayObjectId,
              objectId));
    }
    return List.copyOf(results);
  }

  private String readVariableName(LocalVariable variable) {
    try {
      return variable.name();
    } catch (RuntimeException ex) {
      return UNAVAILABLE_METADATA;
    }
  }

  private String readTypeName(LocalVariable variable) {
    try {
      return variable.typeName();
    } catch (RuntimeException ex) {
      return UNAVAILABLE_METADATA;
    }
  }

  /**
   * JDI does not expose a LocalVariable start/end line range. Its visibleVariables() result is
   * already filtered by the runtime local-variable table for the current frame. AST ranges then
   * select the innermost matching source declaration, which resolves shadowed names safely.
   */
  public Optional<AstVariable> findAstVariable(
      String variableName, String jdiTypeName, int eventLine, List<AstVariable> astVariables) {
    return astVariables.stream()
        .filter(variable -> variable.name().equals(variableName))
        .filter(variable -> typesMatch(variable.declaredType(), jdiTypeName))
        .filter(variable -> variable.isInScopeAt(eventLine))
        .min(
            Comparator.comparingInt(AstVariable::scopeWidth)
                .thenComparing(Comparator.comparingInt(AstVariable::declarationLine).reversed()));
  }

  private boolean typesMatch(String sourceType, String jdiTypeName) {
    String normalizedSource = eraseGenerics(sourceType);
    String normalizedJdi = eraseGenerics(jdiTypeName);
    if (normalizedSource.equals("var")) {
      return true;
    }
    return normalizedSource.equals(normalizedJdi)
        || simpleName(normalizedSource).equals(simpleName(normalizedJdi));
  }

  private String eraseGenerics(String typeName) {
    int genericStart = typeName.indexOf('<');
    return (genericStart >= 0 ? typeName.substring(0, genericStart) : typeName).trim();
  }

  private String simpleName(String typeName) {
    int lastDot = typeName.lastIndexOf('.');
    return lastDot < 0 ? typeName : typeName.substring(lastDot + 1);
  }

  private String serialize(Value value) {
    return serialize(value, new FormatContext(), 0);
  }

  private FormattedValue format(Value value) {
    String text = serialize(value);
    String visualType = visualType(value);
    Object visualValue;
    try {
      visualValue = structuredValue(value, new FormatContext(), 0);
    } catch (RuntimeException ex) {
      visualType = "unavailable";
      visualValue = null;
    }
    return new FormattedValue(text, visualType, visualValue);
  }

  private String serialize(Value value, FormatContext context, int depth) {
    if (value == null) {
      return "null";
    }
    if (value instanceof StringReference stringReference) {
      try {
        return serializeString(stringReference.value());
      } catch (RuntimeException ex) {
        return referenceIdentity(stringReference) + "[unavailable]";
      }
    }
    if (value instanceof CharValue charValue) {
      try {
        return "'" + escape(String.valueOf(charValue.value()), '\'') + "'";
      } catch (RuntimeException ex) {
        return "<unavailable>";
      }
    }
    if (value instanceof PrimitiveValue) {
      return value.toString();
    }
    if (value instanceof ArrayReference arrayReference) {
      return serializeArray(arrayReference, context, depth);
    }
    if (value instanceof ObjectReference objectReference) {
      return serializeObject(objectReference, context, depth);
    }
    return value.toString();
  }

  private String serializeString(String value) {
    boolean truncated = value.length() > MAX_STRING_LENGTH;
    String visible = truncated ? value.substring(0, MAX_STRING_LENGTH) : value;
    if (truncated && !visible.isEmpty() && Character.isHighSurrogate(visible.charAt(visible.length() - 1))) {
      visible = visible.substring(0, visible.length() - 1);
    }
    String result = "\"" + escape(visible, '"');
    if (truncated) {
      result += "...\" (length=" + value.length() + ")";
    } else {
      result += "\"";
    }
    return result;
  }

  private String serializeArray(
      ArrayReference arrayReference, FormatContext context, int depth) {
    if (depth >= MAX_NESTING_DEPTH) {
      return "<max-depth>";
    }

    long identity;
    try {
      identity = arrayReference.uniqueID();
    } catch (RuntimeException ex) {
      return referenceIdentity(arrayReference) + "[unavailable]";
    }

    if (!context.activeArrayIds.add(identity)) {
      return "<cycle>";
    }

    try {
      int length = arrayReference.length();
      int count =
          Math.min(
              Math.min(length, MAX_ARRAY_ELEMENTS),
              context.remainingArrayValues);
      context.remainingArrayValues -= count;

      List<Value> values = count == 0 ? List.of() : arrayReference.getValues(0, count);
      StringBuilder result = new StringBuilder("[");
      for (int index = 0; index < values.size(); index++) {
        if (index > 0) {
          result.append(", ");
        }
        result.append(serialize(values.get(index), context, depth + 1));
      }

      boolean truncated = values.size() < length;
      if (truncated) {
        if (!values.isEmpty()) {
          result.append(", ");
        }
        result.append("...");
      }
      result.append("]");
      if (truncated) {
        result.append(" (length=").append(length).append(")");
      }
      return result.toString();
    } catch (RuntimeException ex) {
      return referenceIdentity(arrayReference) + "[unavailable]";
    } finally {
      context.activeArrayIds.remove(identity);
    }
  }

  private String serializeObject(
      ObjectReference objectReference, FormatContext context, int depth) {
    if (depth >= MAX_NESTING_DEPTH) {
      return referenceIdentity(objectReference);
    }

    long identity;
    try {
      identity = objectReference.uniqueID();
    } catch (RuntimeException ex) {
      return referenceIdentity(objectReference) + "[unavailable]";
    }

    if (!context.activeObjectIds.add(identity)) {
      return "<cycle>";
    }

    try {
      String typeName = objectReference.referenceType().name();
      if (isBoxedPrimitive(typeName)) {
        Value boxedValue = fieldValue(objectReference, "value");
        return boxedValue == null ? referenceIdentity(objectReference) : serialize(boxedValue, context, depth + 1);
      }
      if (isListNode(typeName, objectReference)) {
        return serializeListNode(objectReference, context, depth);
      }
      if (isTreeNode(typeName, objectReference)) {
        TreeBudget budget = new TreeBudget();
        context.activeObjectIds.remove(identity);
        return serializeTreeNode(objectReference, context, depth, budget);
      }
      if (isArrayList(typeName)) {
        return serializeArrayList(objectReference, context, depth);
      }
      if (isLinkedList(typeName)) {
        return serializeLinkedList(objectReference, context, depth);
      }
      if (isHashSet(typeName)) {
        return serializeHashSet(objectReference, context, depth);
      }
      if (isArrayDeque(typeName)) {
        return serializeArrayDeque(objectReference, context, depth);
      }
      if (isHashMap(typeName)) {
        return serializeHashMap(objectReference, context, depth);
      }
      // Invoking Object.toString() can run arbitrary target code while all threads are suspended.
      // A reference identity is deterministic and avoids causing side effects in the debuggee.
      return referenceIdentity(objectReference);
    } catch (RuntimeException ex) {
      return referenceIdentity(objectReference) + "[unavailable]";
    } finally {
      context.activeObjectIds.remove(identity);
    }
  }

  private boolean isArrayList(String typeName) {
    return typeName.equals("java.util.ArrayList");
  }

  private boolean isLinkedList(String typeName) {
    return typeName.equals("java.util.LinkedList");
  }

  private boolean isHashSet(String typeName) {
    return typeName.equals("java.util.HashSet") || typeName.equals("java.util.LinkedHashSet");
  }

  private boolean isArrayDeque(String typeName) {
    return typeName.equals("java.util.ArrayDeque");
  }

  private boolean isHashMap(String typeName) {
    return typeName.equals("java.util.HashMap") || typeName.equals("java.util.LinkedHashMap");
  }

  private boolean isBoxedPrimitive(String typeName) {
    return typeName.equals("java.lang.Boolean")
        || typeName.equals("java.lang.Byte")
        || typeName.equals("java.lang.Character")
        || typeName.equals("java.lang.Short")
        || typeName.equals("java.lang.Integer")
        || typeName.equals("java.lang.Long")
        || typeName.equals("java.lang.Float")
        || typeName.equals("java.lang.Double");
  }

  private String visualType(Value value) {
    if (value == null) {
      return "null";
    }
    if (value instanceof StringReference) {
      return "string";
    }
    if (value instanceof CharValue) {
      return "char";
    }
    if (value instanceof PrimitiveValue) {
      return "primitive";
    }
    if (value instanceof ArrayReference) {
      return "array";
    }
    if (value instanceof ObjectReference objectReference) {
      try {
        String typeName = objectReference.referenceType().name();
        if (isBoxedPrimitive(typeName)) {
          return "primitive";
        }
        if (isListNode(typeName, objectReference)) {
          return "linked-list";
        }
        if (isTreeNode(typeName, objectReference)) {
          return "tree";
        }
        if (isArrayList(typeName) || isLinkedList(typeName) || isHashSet(typeName) || isArrayDeque(typeName)) {
          return "collection";
        }
        if (isHashMap(typeName)) {
          return "map";
        }
        return "reference";
      } catch (RuntimeException ex) {
        return "reference";
      }
    }
    return "value";
  }

  private Object structuredValue(Value value, FormatContext context, int depth) {
    if (value == null) {
      return null;
    }
    if (value instanceof StringReference stringReference) {
      return stringReference.value();
    }
    if (value instanceof CharValue charValue) {
      return String.valueOf(charValue.value());
    }
    if (value instanceof PrimitiveValue primitiveValue) {
      return primitiveStructuredValue(primitiveValue.toString());
    }
    if (value instanceof ArrayReference arrayReference) {
      return structuredArray(arrayReference, context, depth);
    }
    if (value instanceof ObjectReference objectReference) {
      return structuredObject(objectReference, context, depth);
    }
    return value.toString();
  }

  private Object primitiveStructuredValue(String text) {
    if ("true".equals(text) || "false".equals(text)) {
      return Boolean.valueOf(text);
    }
    try {
      return Integer.valueOf(text);
    } catch (NumberFormatException ignored) {
      // Try wider and floating-point forms below.
    }
    try {
      return Long.valueOf(text);
    } catch (NumberFormatException ignored) {
      // Try floating-point below.
    }
    try {
      return Double.valueOf(text);
    } catch (NumberFormatException ignored) {
      return text;
    }
  }

  private Object structuredArray(ArrayReference arrayReference, FormatContext context, int depth) {
    if (depth >= MAX_NESTING_DEPTH) {
      return referenceMap(arrayReference);
    }
    long identity = arrayReference.uniqueID();
    if (!context.activeArrayIds.add(identity)) {
      return cycleMap();
    }
    try {
      int length = arrayReference.length();
      int count = Math.min(Math.min(length, MAX_ARRAY_ELEMENTS), context.remainingArrayValues);
      context.remainingArrayValues -= count;
      List<Value> values = count == 0 ? List.of() : arrayReference.getValues(0, count);
      List<Object> result = new ArrayList<>();
      for (Value element : values) {
        result.add(structuredValue(element, context, depth + 1));
      }
      if (count < length) {
        Map<String, Object> truncated = new LinkedHashMap<>();
        truncated.put("truncated", true);
        truncated.put("length", length);
        result.add(truncated);
      }
      return result;
    } finally {
      context.activeArrayIds.remove(identity);
    }
  }

  private Object structuredObject(ObjectReference objectReference, FormatContext context, int depth) {
    if (depth >= MAX_NESTING_DEPTH) {
      return referenceMap(objectReference);
    }
    long identity = objectReference.uniqueID();
    if (!context.activeObjectIds.add(identity)) {
      return cycleMap();
    }
    try {
      String typeName = objectReference.referenceType().name();
      if (isBoxedPrimitive(typeName)) {
        return structuredValue(fieldValue(objectReference, "value"), context, depth + 1);
      }
      if (isListNode(typeName, objectReference)) {
        return structuredListNode(objectReference, context, depth);
      }
      if (isTreeNode(typeName, objectReference)) {
        context.activeObjectIds.remove(identity);
        TreeBudget budget = new TreeBudget();
        return structuredTreeNode(objectReference, context, depth, budget);
      }
      if (isArrayList(typeName)) {
        return structuredArrayList(objectReference, context, depth);
      }
      if (isLinkedList(typeName)) {
        return structuredLinkedList(objectReference, context, depth);
      }
      if (isHashSet(typeName)) {
        Value mapValue = fieldValue(objectReference, "map");
        return mapValue instanceof ObjectReference map
            ? structuredHashMapEntries(map, context, depth, false)
            : List.of();
      }
      if (isArrayDeque(typeName)) {
        return structuredArrayDeque(objectReference, context, depth);
      }
      if (isHashMap(typeName)) {
        return structuredHashMapEntries(objectReference, context, depth, true);
      }
      return referenceMap(objectReference);
    } finally {
      context.activeObjectIds.remove(identity);
    }
  }

  private boolean isListNode(String typeName, ObjectReference objectReference) {
    return simpleName(typeName).equals("ListNode")
        || (findField(objectReference, "next") != null
            && (findField(objectReference, "val") != null || findField(objectReference, "value") != null));
  }

  private boolean isTreeNode(String typeName, ObjectReference objectReference) {
    return simpleName(typeName).equals("TreeNode")
        || (findField(objectReference, "left") != null
            && findField(objectReference, "right") != null
            && (findField(objectReference, "val") != null || findField(objectReference, "value") != null));
  }

  private String serializeArrayList(
      ObjectReference list, FormatContext context, int depth) {
    int size = intField(list, "size", MAX_ARRAY_ELEMENTS);
    Value elementData = fieldValue(list, "elementData");
    if (!(elementData instanceof ArrayReference elements)) {
      return referenceIdentity(list) + "[unavailable]";
    }
    int count = Math.min(Math.min(size, MAX_ARRAY_ELEMENTS), context.remainingArrayValues);
    context.remainingArrayValues -= count;
    return serializeIndexedValues(elements.getValues(0, count), size, context, depth + 1);
  }

  private String serializeArrayDeque(
      ObjectReference deque, FormatContext context, int depth) {
    Value elementsValue = fieldValue(deque, "elements");
    if (!(elementsValue instanceof ArrayReference elements)) {
      return referenceIdentity(deque) + "[unavailable]";
    }
    int head = intField(deque, "head", 0);
    int tail = intField(deque, "tail", 0);
    int capacity = elements.length();
    int size = tail >= head ? tail - head : capacity - head + tail;
    int count = Math.min(Math.min(size, MAX_ARRAY_ELEMENTS), context.remainingArrayValues);
    context.remainingArrayValues -= count;

    List<Value> values = new ArrayList<>();
    for (int index = 0; index < count; index++) {
      values.add(elements.getValue((head + index) % capacity));
    }
    return serializeIndexedValues(values, size, context, depth + 1);
  }

  private String serializeLinkedList(
      ObjectReference list, FormatContext context, int depth) {
    int size = intField(list, "size", MAX_LINKED_NODES);
    Value first = fieldValue(list, "first");
    List<Value> values = new ArrayList<>();
    ObjectReference node = first instanceof ObjectReference object ? object : null;
    Set<Long> seenNodes = new HashSet<>();
    while (node != null && values.size() < Math.min(size, MAX_LINKED_NODES)) {
      long id = node.uniqueID();
      if (!seenNodes.add(id)) {
        break;
      }
      values.add(fieldValue(node, "item"));
      Value next = fieldValue(node, "next");
      node = next instanceof ObjectReference nextNode ? nextNode : null;
    }
    return serializeIndexedValues(values, size, context, depth + 1);
  }

  private String serializeHashSet(
      ObjectReference set, FormatContext context, int depth) {
    Value mapValue = fieldValue(set, "map");
    if (!(mapValue instanceof ObjectReference map)) {
      return referenceIdentity(set) + "[unavailable]";
    }
    return serializeHashMapEntries(map, context, depth, false);
  }

  private String serializeHashMap(
      ObjectReference map, FormatContext context, int depth) {
    return serializeHashMapEntries(map, context, depth, true);
  }

  private String serializeHashMapEntries(
      ObjectReference map, FormatContext context, int depth, boolean includeValues) {
    int size = intField(map, "size", MAX_MAP_ENTRIES);
    Value tableValue = fieldValue(map, "table");
    if (!(tableValue instanceof ArrayReference table)) {
      return includeValues ? "{}" : "[]";
    }

    List<String> entries = new ArrayList<>();
    Set<Long> seenNodes = new HashSet<>();
    int tableLength = table.length();
    for (int bucket = 0; bucket < tableLength && entries.size() < MAX_MAP_ENTRIES; bucket++) {
      Value bucketValue = table.getValue(bucket);
      ObjectReference node = bucketValue instanceof ObjectReference object ? object : null;
      while (node != null && entries.size() < MAX_MAP_ENTRIES) {
        long id = node.uniqueID();
        if (!seenNodes.add(id)) {
          break;
        }
        String key = serialize(fieldValue(node, "key"), context, depth + 1);
        if (includeValues) {
          String value = serialize(fieldValue(node, "value"), context, depth + 1);
          entries.add(key + ": " + value);
        } else {
          entries.add(key);
        }
        Value next = fieldValue(node, "next");
        node = next instanceof ObjectReference nextNode ? nextNode : null;
      }
    }

    String open = includeValues ? "{" : "[";
    String close = includeValues ? "}" : "]";
    String result = open + String.join(", ", entries);
    if (entries.size() < size) {
      result += entries.isEmpty() ? "..." : ", ...";
    }
    return result + close;
  }

  private String serializeListNode(
      ObjectReference head, FormatContext context, int depth) {
    StringBuilder result = new StringBuilder("[");
    ObjectReference node = head;
    Set<Long> seenNodes = new HashSet<>();
    int count = 0;
    while (node != null && count < MAX_LINKED_NODES) {
      long id = node.uniqueID();
      if (!seenNodes.add(id)) {
        if (count > 0) {
          result.append(" -> ");
        }
        result.append("<cycle>");
        break;
      }
      if (count > 0) {
        result.append(" -> ");
      }
      result.append(serialize(listOrTreeValue(node), context, depth + 1));
      Value next = fieldValue(node, "next");
      node = next instanceof ObjectReference nextNode ? nextNode : null;
      count++;
    }
    if (node != null && count >= MAX_LINKED_NODES) {
      result.append(" -> ...");
    }
    return result.append("]").toString();
  }

  private String serializeTreeNode(
      ObjectReference node, FormatContext context, int depth, TreeBudget budget) {
    if (node == null) {
      return "null";
    }
    if (depth >= MAX_NESTING_DEPTH || budget.remaining-- <= 0) {
      return referenceIdentity(node);
    }
    long identity = node.uniqueID();
    if (!context.activeObjectIds.add(identity)) {
      return "<cycle>";
    }
    try {
      Value left = fieldValue(node, "left");
      Value right = fieldValue(node, "right");
      String value = serialize(listOrTreeValue(node), context, depth + 1);
      return "{val: "
          + value
          + ", left: "
          + serializeTreeNode(left instanceof ObjectReference object ? object : null, context, depth + 1, budget)
          + ", right: "
          + serializeTreeNode(right instanceof ObjectReference object ? object : null, context, depth + 1, budget)
          + "}";
    } finally {
      context.activeObjectIds.remove(identity);
    }
  }

  private Value listOrTreeValue(ObjectReference node) {
    Value value = fieldValue(node, "val");
    return value != null ? value : fieldValue(node, "value");
  }

  private String serializeIndexedValues(
      List<Value> values, int totalSize, FormatContext context, int depth) {
    StringBuilder result = new StringBuilder("[");
    for (int index = 0; index < values.size(); index++) {
      if (index > 0) {
        result.append(", ");
      }
      result.append(serialize(values.get(index), context, depth));
    }
    if (values.size() < totalSize) {
      if (!values.isEmpty()) {
        result.append(", ");
      }
      result.append("...");
    }
    return result.append("]").toString();
  }

  private Object structuredArrayList(ObjectReference list, FormatContext context, int depth) {
    int size = intField(list, "size", MAX_ARRAY_ELEMENTS);
    Value elementData = fieldValue(list, "elementData");
    if (!(elementData instanceof ArrayReference elements)) {
      return referenceMap(list);
    }
    int count = Math.min(Math.min(size, MAX_ARRAY_ELEMENTS), context.remainingArrayValues);
    context.remainingArrayValues -= count;
    return structuredValues(elements.getValues(0, count), size, context, depth + 1);
  }

  private Object structuredArrayDeque(ObjectReference deque, FormatContext context, int depth) {
    Value elementsValue = fieldValue(deque, "elements");
    if (!(elementsValue instanceof ArrayReference elements)) {
      return referenceMap(deque);
    }
    int head = intField(deque, "head", 0);
    int tail = intField(deque, "tail", 0);
    int capacity = elements.length();
    int size = tail >= head ? tail - head : capacity - head + tail;
    int count = Math.min(Math.min(size, MAX_ARRAY_ELEMENTS), context.remainingArrayValues);
    context.remainingArrayValues -= count;

    List<Value> values = new ArrayList<>();
    for (int index = 0; index < count; index++) {
      values.add(elements.getValue((head + index) % capacity));
    }
    return structuredValues(values, size, context, depth + 1);
  }

  private Object structuredLinkedList(ObjectReference list, FormatContext context, int depth) {
    int size = intField(list, "size", MAX_LINKED_NODES);
    Value first = fieldValue(list, "first");
    List<Value> values = new ArrayList<>();
    ObjectReference node = first instanceof ObjectReference object ? object : null;
    Set<Long> seenNodes = new HashSet<>();
    while (node != null && values.size() < Math.min(size, MAX_LINKED_NODES)) {
      long id = node.uniqueID();
      if (!seenNodes.add(id)) {
        break;
      }
      values.add(fieldValue(node, "item"));
      Value next = fieldValue(node, "next");
      node = next instanceof ObjectReference nextNode ? nextNode : null;
    }
    return structuredValues(values, size, context, depth + 1);
  }

  private Object structuredHashMapEntries(
      ObjectReference map, FormatContext context, int depth, boolean includeValues) {
    int size = intField(map, "size", MAX_MAP_ENTRIES);
    Value tableValue = fieldValue(map, "table");
    if (!(tableValue instanceof ArrayReference table)) {
      return List.of();
    }

    List<Object> entries = new ArrayList<>();
    Set<Long> seenNodes = new HashSet<>();
    int tableLength = table.length();
    for (int bucket = 0; bucket < tableLength && entries.size() < MAX_MAP_ENTRIES; bucket++) {
      Value bucketValue = table.getValue(bucket);
      ObjectReference node = bucketValue instanceof ObjectReference object ? object : null;
      while (node != null && entries.size() < MAX_MAP_ENTRIES) {
        long id = node.uniqueID();
        if (!seenNodes.add(id)) {
          break;
        }
        if (includeValues) {
          Map<String, Object> entry = new LinkedHashMap<>();
          entry.put("key", structuredValue(fieldValue(node, "key"), context, depth + 1));
          entry.put("value", structuredValue(fieldValue(node, "value"), context, depth + 1));
          entries.add(entry);
        } else {
          entries.add(structuredValue(fieldValue(node, "key"), context, depth + 1));
        }
        Value next = fieldValue(node, "next");
        node = next instanceof ObjectReference nextNode ? nextNode : null;
      }
    }
    if (entries.size() < size) {
      Map<String, Object> truncated = new LinkedHashMap<>();
      truncated.put("truncated", true);
      truncated.put("size", size);
      entries.add(truncated);
    }
    return entries;
  }

  private Object structuredListNode(ObjectReference head, FormatContext context, int depth) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("kind", "linked-list");
    List<Object> nodes = new ArrayList<>();
    ObjectReference node = head;
    Set<Long> seenNodes = new HashSet<>();
    int count = 0;
    boolean cycle = false;
    boolean truncated = false;
    while (node != null && count < MAX_LINKED_NODES) {
      long id = node.uniqueID();
      if (!seenNodes.add(id)) {
        cycle = true;
        break;
      }
      nodes.add(structuredValue(listOrTreeValue(node), context, depth + 1));
      Value next = fieldValue(node, "next");
      node = next instanceof ObjectReference nextNode ? nextNode : null;
      count++;
    }
    if (node != null && count >= MAX_LINKED_NODES) {
      truncated = true;
    }
    result.put("nodes", nodes);
    result.put("cycle", cycle);
    result.put("truncated", truncated);
    return result;
  }

  private Object structuredTreeNode(
      ObjectReference node, FormatContext context, int depth, TreeBudget budget) {
    if (node == null) {
      return null;
    }
    if (depth >= MAX_NESTING_DEPTH || budget.remaining-- <= 0) {
      return referenceMap(node);
    }
    long identity = node.uniqueID();
    if (!context.activeObjectIds.add(identity)) {
      return cycleMap();
    }
    try {
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("kind", "tree-node");
      result.put("value", structuredValue(listOrTreeValue(node), context, depth + 1));
      Value left = fieldValue(node, "left");
      Value right = fieldValue(node, "right");
      result.put(
          "left",
          structuredTreeNode(left instanceof ObjectReference object ? object : null, context, depth + 1, budget));
      result.put(
          "right",
          structuredTreeNode(right instanceof ObjectReference object ? object : null, context, depth + 1, budget));
      return result;
    } finally {
      context.activeObjectIds.remove(identity);
    }
  }

  private Object structuredValues(
      List<Value> values, int totalSize, FormatContext context, int depth) {
    List<Object> result = new ArrayList<>();
    for (Value value : values) {
      result.add(structuredValue(value, context, depth));
    }
    if (values.size() < totalSize) {
      Map<String, Object> truncated = new LinkedHashMap<>();
      truncated.put("truncated", true);
      truncated.put("size", totalSize);
      result.add(truncated);
    }
    return result;
  }

  private Map<String, Object> referenceMap(ObjectReference reference) {
    Map<String, Object> result = new LinkedHashMap<>();
    try {
      result.put("type", reference.referenceType().name());
      result.put("referenceId", reference.uniqueID());
    } catch (RuntimeException ex) {
      result.put("type", "<unavailable-reference>");
    }
    return result;
  }

  private Map<String, Object> cycleMap() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("cycle", true);
    return result;
  }

  private int intField(ObjectReference objectReference, String fieldName, int fallback) {
    Value value = fieldValue(objectReference, fieldName);
    if (value instanceof PrimitiveValue primitiveValue) {
      try {
        return Integer.parseInt(primitiveValue.toString());
      } catch (NumberFormatException ignored) {
        return fallback;
      }
    }
    return fallback;
  }

  private FormattedValue projectIntervalList(
      ObjectReference list, IntervalValueProjection.Fields fields, String declaredType) {
    String implementation = list.referenceType().name();
    if (!isArrayList(implementation) && !isLinkedList(implementation))
      return intervalListUnavailable("UNKNOWN", -1);
    int size = intField(list, "size", -1);
    if (size < 0) return intervalListUnavailable("UNKNOWN", -1);
    int limit = Math.min(size, MAX_ARRAY_ELEMENTS);
    List<Value> elements = new ArrayList<>();
    if (isArrayList(implementation)) {
      Value storage = fieldValue(list, "elementData");
      if (!(storage instanceof ArrayReference array) || array.length() < limit)
        return intervalListUnavailable("UNKNOWN", size);
      if (limit > 0) elements.addAll(array.getValues(0, limit));
    } else {
      Value first = fieldValue(list, "first");
      ObjectReference node = first instanceof ObjectReference reference ? reference : null;
      Set<Long> seen = new HashSet<>();
      while (node != null && elements.size() < limit && seen.add(node.uniqueID())) {
        elements.add(fieldValue(node, "item"));
        Value next = fieldValue(node, "next");
        node = next instanceof ObjectReference reference ? reference : null;
      }
      if (elements.size() < limit) return intervalListUnavailable("UNKNOWN", size);
    }

    String expectedType = genericElementType(declaredType);
    List<Map<String, Object>> projected = new ArrayList<>();
    boolean unknown = expectedType == null;
    for (int index = 0; index < elements.size(); index++) {
      Map<String, Object> pair = new LinkedHashMap<>();
      pair.put("index", index);
      Value element = elements.get(index);
      if (!(element instanceof ObjectReference interval)
          || expectedType == null
          || !simpleTypeName(interval.referenceType().name()).equals(expectedType)
          || !interval.referenceType().name().equals(fields.runtimeType())) {
        pair.put("status", "UNKNOWN");
        unknown = true;
      } else {
        Field startField = findField(interval, fields.start());
        Field endField = findField(interval, fields.end());
        Value start = startField == null ? null : interval.getValue(startField);
        Value end = endField == null ? null : interval.getValue(endField);
        if (start instanceof IntegerValue startInt && end instanceof IntegerValue endInt) {
          pair.put("start", startInt.value());
          pair.put("end", endInt.value());
          pair.put("status", "AVAILABLE");
          pair.put("elementObjectId", interval.uniqueID());
        } else {
          pair.put("status", "UNKNOWN");
          unknown = true;
        }
      }
      projected.add(pair);
    }
    boolean truncated = limit < size;
    Map<String, Object> visual = new LinkedHashMap<>();
    visual.put("values", projected);
    visual.put("length", size);
    visual.put("truncated", truncated);
    visual.put("status", truncated ? "TRUNCATED" : unknown ? "UNKNOWN" : "AVAILABLE");
    return new FormattedValue(projected.toString(), "interval-list", visual);
  }

  private FormattedValue intervalListUnavailable(String status, int size) {
    Map<String, Object> visual = new LinkedHashMap<>();
    visual.put("values", List.of());
    visual.put("length", size < 0 ? null : size);
    visual.put("truncated", false);
    visual.put("status", status);
    return new FormattedValue("[unavailable]", "interval-list", visual);
  }

  private FormattedValue projectIntegerList(ObjectReference list) {
    String implementation = list.referenceType().name();
    if (!isArrayList(implementation) && !isLinkedList(implementation))
      return integerListUnavailable("UNKNOWN", null);
    int size = intField(list, "size", -1);
    if (size < 0) return integerListUnavailable("UNKNOWN", null);
    int limit = Math.min(size, MAX_ARRAY_ELEMENTS);
    List<Value> elements = new ArrayList<>();
    if (isArrayList(implementation)) {
      Value storage = fieldValue(list, "elementData");
      if (!(storage instanceof ArrayReference array) || array.length() < limit)
        return integerListUnavailable("UNKNOWN", size);
      if (limit > 0) elements.addAll(array.getValues(0, limit));
    } else {
      Value first = fieldValue(list, "first");
      ObjectReference node = first instanceof ObjectReference reference ? reference : null;
      Set<Long> seen = new HashSet<>();
      while (node != null && elements.size() < limit && seen.add(node.uniqueID())) {
        elements.add(fieldValue(node, "item"));
        Value next = fieldValue(node, "next");
        node = next instanceof ObjectReference reference ? reference : null;
      }
      if (elements.size() < limit) return integerListUnavailable("UNKNOWN", size);
    }
    List<Object> values = new ArrayList<>();
    boolean unknown = false;
    for (Value element : elements) {
      if (element == null) {
        values.add(null);
        unknown = true;
      } else if (element instanceof ObjectReference integer
          && integer.referenceType().name().equals("java.lang.Integer")
          && fieldValue(integer, "value") instanceof IntegerValue number) {
        values.add(number.value());
      } else {
        values.add(null);
        unknown = true;
      }
    }
    boolean truncated = limit < size;
    Map<String, Object> visual = new LinkedHashMap<>();
    visual.put("values", values);
    visual.put("length", size);
    visual.put("truncated", truncated);
    visual.put("status", truncated ? "TRUNCATED" : unknown ? "UNKNOWN" : "AVAILABLE");
    return new FormattedValue(values.toString(), "integer-list", visual);
  }

  private FormattedValue integerListUnavailable(String status, Integer length) {
    Map<String, Object> visual = new LinkedHashMap<>();
    visual.put("values", List.of());
    visual.put("length", length);
    visual.put("truncated", false);
    visual.put("status", status);
    return new FormattedValue("[unavailable]", "integer-list", visual);
  }

  private String genericElementType(String declaredType) {
    int open = declaredType.indexOf('<');
    int close = declaredType.lastIndexOf('>');
    return open < 0 || close <= open ? null
        : simpleTypeName(declaredType.substring(open + 1, close).trim());
  }

  private String simpleTypeName(String name) {
    int separator = Math.max(name.lastIndexOf('.'), name.lastIndexOf('$'));
    return name.substring(separator + 1);
  }

  private Value fieldValue(ObjectReference objectReference, String fieldName) {
    Field field = findField(objectReference, fieldName);
    return field == null ? null : objectReference.getValue(field);
  }

  private Field findField(ObjectReference objectReference, String fieldName) {
    try {
      ReferenceType type = objectReference.referenceType();
      while (type != null) {
        Field field = type.fieldByName(fieldName);
        if (field != null) {
          return field;
        }
        type = type instanceof ClassType classType ? classType.superclass() : null;
      }
    } catch (RuntimeException ignored) {
      return null;
    }
    return null;
  }

  private String referenceIdentity(ObjectReference reference) {
    try {
      return reference.referenceType().name() + "#" + reference.uniqueID();
    } catch (RuntimeException ex) {
      return "<unavailable-reference>";
    }
  }

  private String escape(String value, char quote) {
    StringBuilder escaped = new StringBuilder(value.length());
    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
      switch (character) {
        case '\\' -> escaped.append("\\\\");
        case '\n' -> escaped.append("\\n");
        case '\r' -> escaped.append("\\r");
        case '\t' -> escaped.append("\\t");
        case '\b' -> escaped.append("\\b");
        case '\f' -> escaped.append("\\f");
        default -> {
          if (character < 0x20) {
            escaped.append(String.format("\\u%04x", (int) character));
          } else {
            if (character == quote) {
              escaped.append('\\');
            }
            escaped.append(character);
          }
        }
      }
    }
    return escaped.toString();
  }

  private static final class FormatContext {

    private int remainingArrayValues = MAX_TOTAL_ARRAY_VALUES;
    private final Set<Long> activeArrayIds = new HashSet<>();
    private final Set<Long> activeObjectIds = new HashSet<>();
  }

  private static final class TreeBudget {

    private int remaining = MAX_TREE_NODES;
  }

  private record FormattedValue(String text, String visualType, Object visualValue) {}
}
