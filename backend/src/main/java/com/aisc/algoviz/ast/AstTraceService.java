package com.aisc.algoviz.ast;

import com.aisc.algoviz.ast.engine.algorithm.AlgorithmPatternAnalyzer;
import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.aisc.algoviz.ast.engine.debug.JdiSession;
import com.aisc.algoviz.ast.engine.debug.DebugException;
import com.aisc.algoviz.ast.engine.debug.TargetCompiler;
import com.aisc.algoviz.ast.engine.mapping.IntegerListProjection;
import com.aisc.algoviz.ast.engine.mapping.IntervalValueProjection;
import com.aisc.algoviz.ast.engine.model.*;
import com.aisc.algoviz.ast.engine.visualization.VisualTraceBuilder;
import com.aisc.algoviz.submission.sandbox.DockerSandboxProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;

@Service
public class AstTraceService {
    private final AstAnalyzer analyzer;
    private final AlgorithmPatternAnalyzer recognizer;
    private final TraceSourcePreparer preparer;
    private final TargetCompiler compiler;
    private final JdiSession debugger;
    private final VisualTraceBuilder visualizer;
    private final DockerSandboxProperties docker;
    private final Semaphore slots = new Semaphore(2);
    @Value("${algoviz.ast.enabled:true}") private boolean enabled = true;

    public AstTraceService(AstAnalyzer analyzer, AlgorithmPatternAnalyzer recognizer, TraceSourcePreparer preparer,
            TargetCompiler compiler, JdiSession debugger, VisualTraceBuilder visualizer, DockerSandboxProperties docker) {
        this.analyzer = analyzer; this.recognizer = recognizer; this.preparer = preparer;
        this.compiler = compiler; this.debugger = debugger; this.visualizer = visualizer; this.docker = docker;
    }

    public boolean available() { return enabled && !docker.isEnabled(); }

    public record TraceResponse(String engine, String language, String source, String customInput,
            List<ExecutionObservation> steps, List<String> warnings, List<AlgorithmHint> algorithmHints,
            String status, String output, String errorMessage, boolean partial, double executionTimeMs) {}

    public TraceResponse trace(String code, String language, String input) {
        if (language == null || !(language.equalsIgnoreCase("java") || language.equalsIgnoreCase("Java 21")))
            throw new DebugException("AST-tool runtime tracing currently supports Java only.");
        if (!available()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Java AST/JDI tracing is disabled in this execution environment.");
        if (!slots.tryAcquire()) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "The trace engine is busy. Please try again shortly.");
        long start = System.nanoTime();
        try {
            var prepared = preparer.prepare(code, input);
            var analysis = analyzer.analyze(prepared.executable());
            var hints = recognizer.analyze(analysis.compilationUnit());
            try (var target = compiler.compile(prepared.executable(), analysis)) {
                var run = debugger.observe(target, analysis, IntervalValueProjection.fromHints(hints),
                        IntegerListProjection.fromHints(hints), input);
                var steps = visualizer.build(analysis, hints, run.executions()).stream()
                        .filter(s -> !prepared.syntheticMain() || (s.context() != null && !s.context().methodName().equals("main")))
                        .map(s -> originalLines(s, prepared.lineOffset())).toList();
                boolean partial = run.partial();
                String status = partial ? "Trace Limit Reached" : run.exitCode() == 0 ? "Success" : "Runtime Error";
                return new TraceResponse("AST-tool/JDI", "java", code, input == null ? "" : input, steps,
                        run.warnings(), hints, status, run.output(), run.error(), partial,
                        (System.nanoTime() - start) / 1_000_000d);
            }
        } finally { slots.release(); }
    }

    private static ExecutionObservation originalLines(ExecutionObservation s, int offset) {
        if (offset == 0) return s;
        var variables = s.variables().stream().map(v -> new ObservationResult(originalId(v.astNodeId(), offset),
                v.variableName(), v.declaredType(), v.runtimeValue(), v.visualType(), v.visualValue(),
                Math.max(1, v.lineNumber() - offset), v.arrayObjectId(), v.objectId())).toList();
        return new ExecutionObservation(s.sequence(), originalId(s.statementAstNodeId(), offset),
                s.lineNumber() - offset, s.startLine() - offset, s.endLine() - offset,
                s.startColumn(), s.endColumn(), s.statementKind(), s.code(), variables,
                s.context(), s.visualStates(), s.visualEvents());
    }

    private static final Pattern NODE_ID = Pattern.compile("^(ast-.+)-(\\d+)-(\\d+)-(\\d+)-(\\d+)$");
    private static String originalId(String id, int offset) {
        if (id == null) return null;
        var match = NODE_ID.matcher(id);
        if (!match.matches()) return id;
        return match.group(1) + "-" + Math.max(1, Integer.parseInt(match.group(2)) - offset) + "-" + match.group(3)
                + "-" + Math.max(1, Integer.parseInt(match.group(4)) - offset) + "-" + match.group(5);
    }
}
