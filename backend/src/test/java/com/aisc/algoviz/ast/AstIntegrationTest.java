package com.aisc.algoviz.ast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.aisc.algoviz.ast.engine.algorithm.AlgorithmPatternAnalyzer;
import com.aisc.algoviz.ast.engine.debug.*;
import com.aisc.algoviz.ast.engine.mapping.VariableMapper;
import com.aisc.algoviz.ast.engine.visualization.VisualTraceBuilder;
import com.aisc.algoviz.ast.engine.service.*;
import com.aisc.algoviz.submission.sandbox.DockerSandboxProperties;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AstIntegrationTest {
    MockMvc mvc;
    ObjectMapper json = new ObjectMapper();
    @BeforeEach void setup() {
        var analyzer = new AstAnalyzer();
        var recognizer = new AlgorithmPatternAnalyzer();
        var service = new AstTraceService(analyzer, recognizer, new TraceSourcePreparer(analyzer, json),
                new TargetCompiler(), new JdiSession(new BreakpointMapper(), new VariableMapper()),
                new VisualTraceBuilder(), new DockerSandboxProperties());
        mvc = MockMvcBuilders.standaloneSetup(new AstController(service, new AstService(),
                new UploadedJavaSourceReader(), analyzer, recognizer)).build();
    }

    @Test void exposesEmbeddedEngineAndParsesJavaWithoutRunningIt() throws Exception {
        mvc.perform(get("/api/ast/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.engine").value("AST-tool/JDI"));
        mvc.perform(multipart("/api/ast/parse").file(new MockMultipartFile("file", "Sample.java",
                "text/plain", "class Sample { int x = 1; }".getBytes())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
    }

    @Test void capturesActualMutationsAndStandardInputFromMain() throws Exception {
        String source = """
                public class Main {
                    public static void main(String[] args) {
                        int[] nums = {new java.util.Scanner(System.in).nextInt(), 2};
                        nums[0] = nums[0] + 3;
                        System.out.println(java.util.Arrays.toString(nums));
                    }
                }
                """;
        JsonNode body = trace(source, "java", "7");
        assertThat(body.path("status").asText()).isEqualTo("Success");
        assertThat(body.path("output").asText()).contains("[10, 2]");
        assertThat(body.path("steps").toString()).contains("[7,2]", "[10,2]");
        assertThat(body.path("partial").asBoolean()).isFalse();
    }

    @Test void tracesSolutionMethodsWithOriginalEditorLineNumbers() throws Exception {
        String source = """
                class Solution {
                    public int[] twoSum(int[] nums, int target) {
                        for (int i = 0; i < nums.length; i++) {
                            for (int j = i + 1; j < nums.length; j++) {
                                if (nums[i] + nums[j] == target) return new int[]{i, j};
                            }
                        }
                        return new int[0];
                    }
                }
                """;
        JsonNode body = trace(source, "Java", "6\n3 2 4");
        assertThat(body.path("output").asText()).isEqualTo("[1, 2]");
        assertThat(body.path("source").asText()).isEqualTo(source);
        assertThat(body.path("steps").size()).isPositive();
        for (JsonNode step : body.path("steps")) {
            assertThat(step.path("lineNumber").asInt()).isBetween(1, source.lines().toList().size());
            assertThat(step.path("code").asText()).doesNotContain("__algoviz");
        }
    }

    @Test void rejectsUnsupportedLanguagesAndInvalidJavaInsteadOfSimulating() throws Exception {
        mvc.perform(post("/api/ast/trace").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("code", "print(1)", "language", "python"))))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/ast/trace").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("code", "class {", "language", "java"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test void executesTheWorkspaceSortMethodAndBuildsAlgorithmVisualStates() throws Exception {
        String source = """
                public class Main {
                    public static void bubbleSort(int[] arr) {
                        for (int i = 0; i < arr.length - 1; i++) {
                            for (int j = 0; j < arr.length - i - 1; j++) {
                                if (arr[j] > arr[j + 1]) {
                                    int temp = arr[j];
                                    arr[j] = arr[j + 1];
                                    arr[j + 1] = temp;
                                }
                            }
                        }
                    }
                }
                """;
        JsonNode body = trace(source, "java", "[5, 2, 3]");
        assertThat(body.path("output").asText()).isEqualTo("[2, 3, 5]");
        assertThat(body.path("algorithmHints").size()).isPositive();
        assertThat(body.path("steps").toString()).contains("sortFrame");
    }

    @Test void debuggeeHasAnIsolatedClasspathAndDoesNotInheritServerEnvironment() throws Exception {
        String source = """
                class Main {
                    public static void main(String[] args) throws Exception {
                        System.out.println(System.getenv().keySet());
                        try { Class.forName("org.springframework.boot.SpringApplication"); }
                        catch (ClassNotFoundException expected) { System.out.println("isolated"); }
                    }
                }
                """;
        JsonNode body = trace(source, "java", "");
        assertThat(body.path("output").asText()).contains("LANG", "isolated").doesNotContain("HOME", "PATH");
    }

    @Test void traceLimitsProduceAnExplicitPartialResult() {
        var analyzer = new AstAnalyzer();
        var service = new AstTraceService(analyzer, new AlgorithmPatternAnalyzer(), new TraceSourcePreparer(analyzer, json),
                new TargetCompiler(), new JdiSession(new BreakpointMapper(), new VariableMapper(), 1, 5),
                new VisualTraceBuilder(), new DockerSandboxProperties());
        var result = service.trace("class Main { public static void main(String[] args) { int count=0; while(true) { count++; } } }", "java", "");
        assertThat(result.partial()).isTrue();
        assertThat(result.status()).isEqualTo("Trace Limit Reached");
        assertThat(result.warnings()).isNotEmpty();
    }

    @Test void coalescedLineBreakpointsAreWarningsRatherThanExecutionFailure() throws Exception {
        String code = "class Main { public static void main(String[] args) { int a=1; a++; System.out.println(a); } }";
        var result = trace(code, "java", "");
        assertThat(result.path("output").asText()).isEqualTo("2");
        assertThat(result.path("status").asText()).isEqualTo("Success");
        assertThat(result.path("partial").asBoolean()).isFalse();
    }

    @Test void dockerExecutionModeNeverFallsBackToAnUnisolatedJdiTarget() {
        var analyzer = new AstAnalyzer();
        var docker = new DockerSandboxProperties();
        docker.setEnabled(true);
        var service = new AstTraceService(analyzer, new AlgorithmPatternAnalyzer(), new TraceSourcePreparer(analyzer, json),
                new TargetCompiler(), new JdiSession(new BreakpointMapper(), new VariableMapper()), new VisualTraceBuilder(), docker);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.trace("class Main {}", "java", ""))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    private JsonNode trace(String code, String language, String input) throws Exception {
        String body = mvc.perform(post("/api/ast/trace").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("code", code, "language", language, "customInput", input))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }
}
