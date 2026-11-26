package com.aisc.algoviz.analysis;

import com.aisc.algoviz.analysis.dto.AiAnalysisRequest;
import com.aisc.algoviz.analysis.dto.AiAnalysisResponse;
import com.aisc.algoviz.analysis.service.AiAnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AiAnalysisAndCorsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AiAnalysisService aiAnalysisService;

    @Test
    @DisplayName("CORS allows Manifest V3 Chrome Extension origin on /api/ast/health and preflight OPTIONS")
    void corsAllowsChromeExtensionOrigin() throws Exception {
        String extensionOrigin = "chrome-extension://abcdefghijklmnop";

        mockMvc.perform(get("/api/ast/health")
                        .header("Origin", extensionOrigin))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", extensionOrigin))
                .andExpect(jsonPath("$.engine").value("AST-tool/JDI"));

        mockMvc.perform(options("/api/ast/trace")
                        .header("Origin", extensionOrigin)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", extensionOrigin));
    }

    @Test
    @DisplayName("AiAnalysisService uses JavaParser AST & AlgorithmPatternAnalyzer for Java Binary Search and Bubble Sort")
    void aiAnalysisServiceUsesAstForJavaPatterns() throws Exception {
        String binarySearchJava = """
                class Solution {
                    int binarySearch(int[] data, int target) {
                        int lo = 0, hi = data.length - 1;
                        while (lo <= hi) {
                            int mid = lo + (hi - lo) / 2;
                            if (data[mid] == target) return mid;
                            if (data[mid] < target) lo = mid + 1;
                            else hi = mid - 1;
                        }
                        return -1;
                    }
                }
                """;

        AiAnalysisRequest bsReq = new AiAnalysisRequest();
        bsReq.setCode(binarySearchJava);
        bsReq.setLanguage("java");
        bsReq.setProblemContext("Binary Search");

        AiAnalysisResponse bsRes = aiAnalysisService.analyzeCode(bsReq);
        assertThat(bsRes.getTimeComplexity()).isEqualTo("O(log N)");
        assertThat(bsRes.getSpaceComplexity()).isEqualTo("O(1)");
        assertThat(bsRes.getSummary()).contains("binary-search");
        assertThat(bsRes.getOptimizationSuggestions()).anyMatch(s -> s.contains("binary-search"));

        String bubbleSortJava = """
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

        mockMvc.perform(post("/api/ai/analyze")
                        .header("Origin", "chrome-extension://abcdefghijklmnop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "code", bubbleSortJava,
                                "language", "Java 21",
                                "problemContext", "Sorting"
                        ))))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "chrome-extension://abcdefghijklmnop"))
                .andExpect(jsonPath("$.timeComplexity").value("O(N²)"))
                .andExpect(jsonPath("$.spaceComplexity").value("O(1)"))
                .andExpect(jsonPath("$.summary").value(org.hamcrest.Matchers.containsString("bubble-sort")));
    }

    @Test
    @DisplayName("AiAnalysisService preserves heuristic fallback for Python code")
    void aiAnalysisServicePreservesPythonHeuristics() {
        AiAnalysisRequest pyReq = new AiAnalysisRequest();
        pyReq.setCode("def solve(nums):\n    res = []\n    for x in range(len(nums)):\n        res.append(x)\n    return res\n");
        pyReq.setLanguage("python");

        AiAnalysisResponse pyRes = aiAnalysisService.analyzeCode(pyReq);
        assertThat(pyRes.getTimeComplexity()).isEqualTo("O(N)");
        assertThat(pyRes.getSpaceComplexity()).isEqualTo("O(N)");
    }
}
