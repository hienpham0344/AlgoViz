package com.aisc.algoviz.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisResponse {
    private String timeComplexity; // e.g. "O(N log N)"
    private String spaceComplexity; // e.g. "O(N)"
    private String summary;
    private List<String> optimizationSuggestions;
    private List<String> potentialBugs;
    private String refactoredCode;
}
