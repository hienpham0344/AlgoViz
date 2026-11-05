package com.aisc.algoviz.ast.engine.algorithm;

import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.github.javaparser.ast.body.MethodDeclaration;
import java.util.List;

/** A source-only recognition strategy. Implementations must stay independent of runtime tracing. */
public interface MethodPatternDetector {
  List<AlgorithmHint> detect(MethodDeclaration method);
}
