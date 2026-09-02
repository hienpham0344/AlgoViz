package com.aisc.algoviz.problem.service;

import com.aisc.algoviz.common.dto.PageResponse;
import com.aisc.algoviz.common.exception.AppException;
import com.aisc.algoviz.problem.dto.request.CreateProblemRequestDto;
import com.aisc.algoviz.problem.dto.request.CreateSolutionRequestDto;
import com.aisc.algoviz.problem.dto.request.ProblemFilterRequest;
import com.aisc.algoviz.problem.dto.request.UpdateProblemRequestDto;
import com.aisc.algoviz.problem.dto.response.ProblemDetailDto;
import com.aisc.algoviz.problem.dto.response.ProblemSummaryDto;
import com.aisc.algoviz.problem.dto.response.SolutionResponseDto;
import com.aisc.algoviz.problem.entity.Problem;
import com.aisc.algoviz.problem.entity.Solution;
import com.aisc.algoviz.problem.exception.ProblemErrorCode;
import com.aisc.algoviz.problem.mapper.ProblemMapper;
import com.aisc.algoviz.problem.repository.ProblemRepository;
import com.aisc.algoviz.problem.repository.ProblemSpecification;
import com.aisc.algoviz.problem.repository.SolutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tầng triển khai thực thi chi tiết logic nghiệp vụ bài toán.
 * Đặt trực tiếp trong package 'service' theo chuẩn Package-by-Feature gọn nhẹ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemServiceImpl implements ProblemService {

    private final ProblemRepository problemRepository;
    private final SolutionRepository solutionRepository;
    private final ProblemMapper problemMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProblemSummaryDto> getProblems(ProblemFilterRequest filterRequest) {
        log.debug("Fetching problems: {}", filterRequest);

        Sort.Direction direction = "desc".equalsIgnoreCase(filterRequest.getSortDirection())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        String safeSortBy = filterRequest.getSanitizedSortBy();
        Pageable pageable = PageRequest.of(filterRequest.getPage(), filterRequest.getSize(), Sort.by(direction, safeSortBy));

        Specification<Problem> spec = ProblemSpecification.buildSpecification(filterRequest);

        Page<Problem> problemPage = problemRepository.findAll(spec, pageable);

        Page<ProblemSummaryDto> dtoPage = problemPage.map(problemMapper::toSummaryDto);

        return PageResponse.from(dtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public ProblemDetailDto getProblemById(Long id) {
        log.debug("Fetching problem detail by ID: {}", id);

        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new AppException(ProblemErrorCode.PROBLEM_NOT_FOUND));

        return problemMapper.toDetailDto(problem);
    }

    @Override
    @Transactional(readOnly = true)
    public ProblemDetailDto getProblemBySlug(String slug) {
        log.debug("Fetching problem detail by slug: {}", slug);

        Problem problem = problemRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ProblemErrorCode.PROBLEM_NOT_FOUND));

        return problemMapper.toDetailDto(problem);
    }

    @Override
    @Transactional
    public ProblemDetailDto createProblem(CreateProblemRequestDto createDto) {
        log.info("Creating new problem: {}", createDto.getTitle());

        Problem problem = problemMapper.toEntity(createDto);

        if (problemRepository.findBySlug(problem.getSlug()).isPresent()) {
            throw new AppException(ProblemErrorCode.SLUG_ALREADY_EXISTS);
        }

        Problem savedProblem = problemRepository.save(problem);

        log.info("Successfully created problem ID: {}", savedProblem.getId());
        return problemMapper.toDetailDto(savedProblem);
    }

    @Override
    @Transactional
    public ProblemDetailDto updateProblem(Long id, UpdateProblemRequestDto updateDto) {
        log.info("Updating problem ID: {}", id);

        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new AppException(ProblemErrorCode.PROBLEM_NOT_FOUND));

        if (updateDto.getSlug() != null && !updateDto.getSlug().isBlank()
                && !updateDto.getSlug().equalsIgnoreCase(problem.getSlug())) {
            if (problemRepository.findBySlug(updateDto.getSlug()).isPresent()) {
                throw new AppException(ProblemErrorCode.SLUG_ALREADY_EXISTS);
            }
        }

        problemMapper.updateEntityFromDto(updateDto, problem);
        Problem updatedProblem = problemRepository.save(problem);

        log.info("Successfully updated problem ID: {}", updatedProblem.getId());
        return problemMapper.toDetailDto(updatedProblem);
    }

    @Override
    @Transactional
    public void deleteProblem(Long id) {
        log.info("Deleting problem ID: {}", id);

        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new AppException(ProblemErrorCode.PROBLEM_NOT_FOUND));

        problemRepository.delete(problem);
        log.info("Successfully deleted problem ID: {}", id);
    }

    @Override
    @Transactional
    public SolutionResponseDto addSolutionToProblem(Long problemId, CreateSolutionRequestDto solutionDto) {
        log.info("Adding solution to problem ID: {}", problemId);

        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new AppException(ProblemErrorCode.PROBLEM_NOT_FOUND));

        Solution solution = problemMapper.toSolutionEntity(solutionDto, problem);
        Solution savedSolution = solutionRepository.save(solution);

        log.info("Successfully added solution ID: {} to problem ID: {}", savedSolution.getId(), problemId);
        return problemMapper.toSolutionResponseDto(savedSolution);
    }
}
