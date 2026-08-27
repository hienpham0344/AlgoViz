package com.aisc.algoviz.problem.service.impl;

import com.aisc.algoviz.common.dto.PageResponse;
import com.aisc.algoviz.common.exception.ResourceNotFoundException;
import com.aisc.algoviz.problem.dto.ProblemDetailDto;
import com.aisc.algoviz.problem.dto.ProblemSummaryDto;
import com.aisc.algoviz.problem.dto.SolutionResponseDto;
import com.aisc.algoviz.problem.entity.Difficulty;
import com.aisc.algoviz.problem.entity.Problem;
import com.aisc.algoviz.problem.entity.Solution;
import com.aisc.algoviz.problem.repository.ProblemRepository;
import com.aisc.algoviz.problem.repository.SolutionRepository;
import com.aisc.algoviz.problem.service.ProblemService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Tầng triển khai thực thi chi tiết logic nghiệp vụ bài toán.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemServiceImpl implements ProblemService {

    private final ProblemRepository problemRepository;
    private final SolutionRepository solutionRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProblemSummaryDto> getProblems(
            int page,
            int size,
            Difficulty difficulty,
            String tag,
            String search,
            String sortBy,
            String sortDirection
    ) {
        log.debug("Fetching problems: page={}, size={}, difficulty={}, tag={}, search={}", page, size, difficulty, tag, search);

        // 1. Xác định thứ tự sắp xếp (mặc định theo ID tăng dần)
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String property = (sortBy != null && !sortBy.isBlank()) ? sortBy : "id";
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, property));

        // 2. Xây dựng Specification truy vấn động theo các tham số truyền vào
        Specification<Problem> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Lọc theo độ khó nếu có
            if (difficulty != null) {
                predicates.add(criteriaBuilder.equal(root.get("difficulty"), difficulty));
            }

            // Tìm kiếm theo từ khóa trong tiêu đề (không phân biệt hoa/thường)
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), searchPattern));
            }

            // Lọc theo Tag thuật toán (Pattern Tag)
            if (tag != null && !tag.trim().isEmpty()) {
                predicates.add(criteriaBuilder.isMember(tag.trim(), root.get("patternTags")));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        // 3. Thực thi truy vấn phân trang qua Database
        Page<Problem> problemPage = problemRepository.findAll(spec, pageable);

        // 4. Chuyển đổi từ Entity sang DTO tóm tắt
        Page<ProblemSummaryDto> dtoPage = problemPage.map(this::mapToSummaryDto);

        return PageResponse.from(dtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public ProblemDetailDto getProblemById(Long id) {
        log.debug("Fetching problem detail by ID: {}", id);

        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài toán với ID: " + id));

        return buildProblemDetailDto(problem);
    }

    @Override
    @Transactional(readOnly = true)
    public ProblemDetailDto getProblemBySlug(String slug) {
        log.debug("Fetching problem detail by slug: {}", slug);

        Problem problem = problemRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài toán với slug: " + slug));

        return buildProblemDetailDto(problem);
    }

    private ProblemSummaryDto mapToSummaryDto(Problem problem) {
        return ProblemSummaryDto.builder()
                .id(problem.getId())
                .leetcodeId(problem.getLeetcodeId())
                .title(problem.getTitle())
                .slug(problem.getSlug())
                .difficulty(problem.getDifficulty())
                .patternTags(problem.getPatternTags())
                .build();
    }

    private ProblemDetailDto buildProblemDetailDto(Problem problem) {
        // Lấy danh sách Reference Solutions kèm theo bài toán
        List<Solution> solutions = solutionRepository.findByProblemId(problem.getId());
        List<SolutionResponseDto> solutionDtos = solutions.stream()
                .map(this::mapToSolutionDto)
                .toList();

        return ProblemDetailDto.builder()
                .id(problem.getId())
                .leetcodeId(problem.getLeetcodeId())
                .title(problem.getTitle())
                .slug(problem.getSlug())
                .difficulty(problem.getDifficulty())
                .description(problem.getDescription())
                .patternTags(problem.getPatternTags())
                .solutions(solutionDtos)
                .build();
    }

    private SolutionResponseDto mapToSolutionDto(Solution solution) {
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
}
