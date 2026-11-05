package com.aisc.algoviz.ast;

import com.aisc.algoviz.ast.engine.algorithm.AlgorithmCatalog;
import com.aisc.algoviz.ast.engine.algorithm.AlgorithmPatternAnalyzer;
import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.aisc.algoviz.ast.engine.debug.DebugException;
import com.aisc.algoviz.ast.engine.dto.AstParseResponse;
import com.aisc.algoviz.ast.engine.exception.CodeParseException;
import com.aisc.algoviz.ast.engine.service.AstService;
import com.aisc.algoviz.ast.engine.service.UploadedJavaSourceReader;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AstController {
    private final AstTraceService traces;
    private final AstService parser;
    private final UploadedJavaSourceReader uploads;
    private final AstAnalyzer analyzer;
    private final AlgorithmPatternAnalyzer recognizer;

    public AstController(AstTraceService traces, AstService parser, UploadedJavaSourceReader uploads,
            AstAnalyzer analyzer, AlgorithmPatternAnalyzer recognizer) {
        this.traces = traces; this.parser = parser; this.uploads = uploads;
        this.analyzer = analyzer; this.recognizer = recognizer;
    }

    public record TraceRequest(@NotBlank @Size(max = 50000) String code,
            @NotBlank String language, @Size(max = 20000) String customInput) {}

    @GetMapping("/ast/health")
    public Map<String, Object> health() {
        return Map.of("status", traces.available() ? "UP" : "DISABLED", "engine", "AST-tool/JDI", "languages", List.of("java"));
    }

    @PostMapping(value = "/ast/trace", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AstTraceService.TraceResponse trace(@Valid @RequestBody TraceRequest request) {
        return traces.trace(request.code(), request.language(), request.customInput());
    }

    @PostMapping(value = "/ast/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AstParseResponse parse(@RequestParam("file") MultipartFile file) {
        return AstParseResponse.success(parser.parse(uploads.read(file)));
    }

    @PostMapping(value = "/debug/steps", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AstTraceService.TraceResponse debugSteps(@RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "") @Size(max = 20000) String customInput) {
        if (customInput.length() > 20000) throw new IllegalArgumentException("Testcase input is too large.");
        return traces.trace(uploads.read(file), "java", customInput);
    }

    @PostMapping(value = "/debug", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object debug(@RequestParam("file") MultipartFile file, @RequestParam(defaultValue = "") String customInput) {
        return debugSteps(file, customInput).steps().stream().flatMap(s -> s.variables().stream()).toList();
    }

    @GetMapping("/algorithms/catalog")
    public Map<String, Object> catalog() {
        return Map.of("version", AlgorithmCatalog.VERSION, "algorithms", AlgorithmCatalog.entries());
    }

    @PostMapping(value = "/algorithms/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> recognize(@RequestParam("file") MultipartFile file) {
        return Map.of("catalogVersion", AlgorithmCatalog.VERSION,
                "algorithmHints", recognizer.analyze(analyzer.parseCompilationUnit(uploads.read(file))));
    }

    @ExceptionHandler({CodeParseException.class, DebugException.class})
    public ResponseEntity<?> invalidSource(RuntimeException ex) {
        return ResponseEntity.unprocessableEntity().body(Map.of("message", ex.getMessage(), "code", "INVALID_TRACE_SOURCE",
                "problems", ex instanceof CodeParseException parse ? parse.getProblems() : List.of()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> invalidUpload(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> unavailable(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", ex.getReason() == null ? "Trace unavailable" : ex.getReason()));
    }
}
