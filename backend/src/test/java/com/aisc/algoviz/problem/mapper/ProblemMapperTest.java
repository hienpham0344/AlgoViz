package com.aisc.algoviz.problem.mapper;

import com.aisc.algoviz.problem.entity.Problem;
import com.aisc.algoviz.problem.entity.Solution;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProblemMapperTest {
    private final ProblemMapper mapper = Mappers.getMapper(ProblemMapper.class);

    @Test
    void preservesExecutableSolutionSourceAndMetadata() {
        String source = "public class TwoSumSolution {\n  public static void main(String[] args) {\n    System.out.println(2 + 7 == 9);\n  }\n}\n";
        var solution = Solution.builder().codeSnippet(source).language("Java")
                .explanation("Compare nums[i] + nums[j] == target.")
                .timeComplexity("O(n²)").spaceComplexity("O(1)").build();
        var response = mapper.toSolutionResponseDto(solution);
        assertEquals(source, response.getCodeSnippet());
        assertEquals("Java", response.getLanguage());
        assertEquals(solution.getExplanation(), response.getExplanation());
        assertEquals("O(n²)", response.getTimeComplexity());
    }

    @Test
    void keepsProblemTextSeparateFromSlugGeneration() {
        var problem = Problem.builder().title("Binary Search").slug("binary-search")
                .description("Return -1 if nums[mid] != target.").build();
        var response = mapper.toDetailDto(problem);
        assertEquals("Binary Search", response.getTitle());
        assertEquals("binary-search", response.getSlug());
        assertEquals(problem.getDescription(), response.getDescription());
        assertEquals("binary-search", mapper.mapSlug(null, "Binary Search"));
    }
}
