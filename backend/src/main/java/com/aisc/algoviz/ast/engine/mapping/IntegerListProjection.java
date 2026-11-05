package com.aisc.algoviz.ast.engine.mapping;

import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Selects only AST-proven List<Integer> inputs for bounded JDI projection. */
public record IntegerListProjection(Set<String> dataDeclarationIds) {
  public IntegerListProjection { dataDeclarationIds = Set.copyOf(dataDeclarationIds); }

  public static IntegerListProjection fromHints(List<AlgorithmHint> hints) {
    Set<String> ids = new HashSet<>();
    for (AlgorithmHint hint : hints) {
      var pattern = hint.twoPointers();
      if (pattern == null || !"INTEGER_LIST".equals(pattern.representation())) continue;
      String id = pattern.declarationIds().get("data");
      if (id != null) ids.add(id);
    }
    return new IntegerListProjection(ids);
  }

  public boolean includes(String declarationId) {
    return dataDeclarationIds.contains(declarationId);
  }
}
