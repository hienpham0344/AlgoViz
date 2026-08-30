package com.aisc.algoviz.problem.mapper;

import com.aisc.algoviz.problem.dto.ProblemDetailDto;
import com.aisc.algoviz.problem.dto.ProblemSummaryDto;
import com.aisc.algoviz.problem.dto.SolutionResponseDto;
import com.aisc.algoviz.problem.dto.request.CreateProblemRequestDto;
import com.aisc.algoviz.problem.dto.request.CreateSolutionRequestDto;
import com.aisc.algoviz.problem.dto.request.UpdateProblemRequestDto;
import com.aisc.algoviz.problem.entity.Problem;
import com.aisc.algoviz.problem.entity.Solution;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Component chuyên trách chuyển đổi dữ liệu hai chiều giữa Entity (Database) và DTOs.
 * Đảm bảo nguyên lý Single Responsibility Principle (SRP).
 *
 * @author AlgoViz Development Team
 */
@Component
public class ProblemMapper {

    /**
     * Chuyển đổi từ Problem Entity sang ProblemSummaryDto (Dành cho trang danh sách).
     */
    public ProblemSummaryDto toSummaryDto(Problem problem) {
        if (problem == null) return null;

        return ProblemSummaryDto.builder()
                .id(problem.getId())
                .leetcodeId(problem.getLeetcodeId())
                .title(problem.getTitle())
                .slug(problem.getSlug())
                .difficulty(problem.getDifficulty())
                .patternTags(problem.getPatternTags() != null ? new ArrayList<>(problem.getPatternTags()) : new ArrayList<>())
                .build();
    }

    /**
     * Chuyển đổi từ Problem Entity sang ProblemDetailDto (Dành cho màn chi tiết bài toán).
     */
    public ProblemDetailDto toDetailDto(Problem problem) {
        if (problem == null) return null;

        List<SolutionResponseDto> solutionDtos = problem.getSolutions() != null
                ? problem.getSolutions().stream().map(this::toSolutionResponseDto).toList()
                : new ArrayList<>();

        return ProblemDetailDto.builder()
                .id(problem.getId())
                .leetcodeId(problem.getLeetcodeId())
                .title(problem.getTitle())
                .slug(problem.getSlug())
                .difficulty(problem.getDifficulty())
                .description(problem.getDescription())
                .patternTags(problem.getPatternTags() != null ? new ArrayList<>(problem.getPatternTags()) : new ArrayList<>())
                .solutions(solutionDtos)
                .build();
    }

    /**
     * Chuyển đổi từ Solution Entity sang SolutionResponseDto.
     */
    public SolutionResponseDto toSolutionResponseDto(Solution solution) {
        if (solution == null) return null;

        return SolutionResponseDto.builder()
                .id(solution.getId())
                .patternId(solution.getPatternId())
                .codeSnippet(solution.getCodeSnippet())
                .explanation(solution.getExplanation())
                .timeComplexity(solution.getTimeComplexity())
                .spaceComplexity(solution.getSpaceComplexity())
                .language(solution.getLanguage())
                .build();
    }

    /**
     * Chuyển đổi từ CreateProblemRequestDto sang Problem Entity để lưu Database.
     */
    public Problem toEntity(CreateProblemRequestDto dto) {
        if (dto == null) return null;

        String slug = (dto.getSlug() != null && !dto.getSlug().isBlank())
                ? dto.getSlug().trim()
                : generateSlug(dto.getTitle());

        Problem problem = Problem.builder()
                .leetcodeId(dto.getLeetcodeId())
                .title(dto.getTitle().trim())
                .slug(slug)
                .difficulty(dto.getDifficulty())
                .description(dto.getDescription())
                .patternTags(dto.getPatternTags() != null ? new ArrayList<>(dto.getPatternTags()) : new ArrayList<>())
                .build();

        if (dto.getSolutions() != null && !dto.getSolutions().isEmpty()) {
            List<Solution> solutions = dto.getSolutions().stream()
                    .map(solDto -> toSolutionEntity(solDto, problem))
                    .toList();
            problem.setSolutions(new ArrayList<>(solutions));
        }

        return problem;
    }

    /**
     * Chuyển đổi từ CreateSolutionRequestDto sang Solution Entity liên kết với Problem.
     */
    public Solution toSolutionEntity(CreateSolutionRequestDto dto, Problem problem) {
        if (dto == null) return null;

        return Solution.builder()
                .problem(problem)
                .patternId(dto.getPatternId())
                .codeSnippet(dto.getCodeSnippet())
                .explanation(dto.getExplanation())
                .timeComplexity(dto.getTimeComplexity())
                .spaceComplexity(dto.getSpaceComplexity())
                .language(dto.getLanguage() != null && !dto.getLanguage().isBlank() ? dto.getLanguage() : "Java")
                .build();
    }

    /**
     * Cập nhật thông tin bài toán Entity có sẵn từ UpdateProblemRequestDto.
     */
    public void updateEntityFromDto(UpdateProblemRequestDto dto, Problem problem) {
        if (dto == null || problem == null) return;

        if (dto.getLeetcodeId() != null) {
            problem.setLeetcodeId(dto.getLeetcodeId());
        }
        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            problem.setTitle(dto.getTitle().trim());
            if (dto.getSlug() == null || dto.getSlug().isBlank()) {
                problem.setSlug(generateSlug(dto.getTitle()));
            }
        }
        if (dto.getSlug() != null && !dto.getSlug().isBlank()) {
            problem.setSlug(dto.getSlug().trim());
        }
        if (dto.getDifficulty() != null) {
            problem.setDifficulty(dto.getDifficulty());
        }
        if (dto.getDescription() != null) {
            problem.setDescription(dto.getDescription());
        }
        if (dto.getPatternTags() != null) {
            problem.setPatternTags(new ArrayList<>(dto.getPatternTags()));
        }
    }

    /**
     * Hàm tiện ích tự động sinh ra chuỗi Slug URL chuẩn SEO từ Tiêu đề bài toán (Title).
     * Ví dụ: "Two Sum" -> "two-sum", "Add Two Numbers!" -> "add-two-numbers"
     *
     * @param title Tiêu đề bài toán
     * @return Chuỗi Slug chuẩn
     */
    public String generateSlug(String title) {
        if (title == null || title.isBlank()) return "";
        return title.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");
    }
}
