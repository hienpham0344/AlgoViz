package com.aisc.algoviz.service;

import com.aisc.algoviz.dto.ProblemDetailDto;
import com.aisc.algoviz.dto.ProblemDto;
import com.aisc.algoviz.model.Problem;
import com.aisc.algoviz.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemRepository problemRepository;

    public List<ProblemDto> getAllProblems(String difficulty, String category) {
        List<Problem> problems;
        if (difficulty != null && category != null) {
            problems = problemRepository.findByDifficultyAndCategory(difficulty, category);
        } else if (difficulty != null) {
            problems = problemRepository.findByDifficulty(difficulty);
        } else if (category != null) {
            problems = problemRepository.findByCategory(category);
        } else {
            problems = problemRepository.findAll();
        }

        return problems.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public ProblemDetailDto getProblemById(Long id) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Problem not found with id: " + id));
        return mapToDetailDto(problem);
    }

    private ProblemDto mapToDto(Problem problem) {
        String shortDesc = problem.getDescription() != null && problem.getDescription().length() > 100
                ? problem.getDescription().substring(0, 100) + "..."
                : problem.getDescription();

        return ProblemDto.builder()
                .id(problem.getId())
                .title(problem.getTitle())
                .category(problem.getCategory())
                .difficulty(problem.getDifficulty())
                .shortDescription(shortDesc)
                .build();
    }

    private ProblemDetailDto mapToDetailDto(Problem problem) {
        List<ProblemDetailDto.SampleTestCaseDto> sampleTestCases = problem.getTestCases().stream()
                .filter(tc -> Boolean.TRUE.equals(tc.getIsSample()))
                .map(tc -> new ProblemDetailDto.SampleTestCaseDto(tc.getInput(), tc.getExpectedOutput()))
                .collect(Collectors.toList());

        return ProblemDetailDto.builder()
                .id(problem.getId())
                .title(problem.getTitle())
                .category(problem.getCategory())
                .difficulty(problem.getDifficulty())
                .description(problem.getDescription())
                .inputFormat(problem.getInputFormat())
                .outputFormat(problem.getOutputFormat())
                .constraints(problem.getConstraints())
                .sampleInput(problem.getSampleInput())
                .sampleOutput(problem.getSampleOutput())
                .starterCodePython(problem.getStarterCodePython())
                .starterCodeCpp(problem.getStarterCodeCpp())
                .starterCodeJava(problem.getStarterCodeJava())
                .starterCodeJs(problem.getStarterCodeJs())
                .sampleTestCases(sampleTestCases)
                .build();
    }
}
