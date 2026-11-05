package com.aisc.algoviz.ast.engine.mapping;

import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** AST-proven field projection for the input list of an Activity Selection hint. */
public record IntervalValueProjection(Map<String, Fields> byInputDeclarationId) {
  public IntervalValueProjection {
    byInputDeclarationId = Map.copyOf(byInputDeclarationId);
  }

  public static IntervalValueProjection fromHints(List<AlgorithmHint> hints) {
    Map<String, Fields> projections = new LinkedHashMap<>();
    Set<String> conflicts = new HashSet<>();
    for (AlgorithmHint hint : hints) {
      var pattern = hint.activitySelection();
      if (pattern == null || !"INTERVAL_LIST".equals(pattern.representation())) continue;
      String inputId = pattern.declarationIds().get("input");
      String start = pattern.roleValues().get("start");
      String end = pattern.roleValues().get("end");
      String runtimeType = pattern.roleValues().get("runtimeType");
      if (inputId != null && start != null && end != null && runtimeType != null
          && !start.equals(end)
          && !conflicts.contains(inputId)) {
        Fields fields = new Fields(start, end, runtimeType);
        Fields existing = projections.putIfAbsent(inputId, fields);
        if (existing != null && !existing.equals(fields)) {
          projections.remove(inputId);
          conflicts.add(inputId);
        }
      }
    }
    return new IntervalValueProjection(projections);
  }

  public Fields fieldsFor(String inputDeclarationId) {
    return byInputDeclarationId.get(inputDeclarationId);
  }

  public record Fields(String start, String end, String runtimeType) {}
}
