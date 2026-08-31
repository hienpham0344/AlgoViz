package com.aisc.algoviz.problem.service.impl;

import com.aisc.algoviz.common.dto.PageResponse;
import com.aisc.algoviz.problem.dto.ProblemDetailDto;
import com.aisc.algoviz.problem.dto.ProblemSummaryDto;
import com.aisc.algoviz.problem.dto.SolutionResponseDto;
import com.aisc.algoviz.problem.dto.request.CreateProblemRequestDto;
import com.aisc.algoviz.problem.dto.request.CreateSolutionRequestDto;
import com.aisc.algoviz.problem.dto.request.ProblemFilterRequest;
import com.aisc.algoviz.problem.dto.request.UpdateProblemRequestDto;
import com.aisc.algoviz.problem.entity.Problem;
import com.aisc.algoviz.problem.entity.Solution;
import com.aisc.algoviz.problem.mapper.ProblemMapper;
import com.aisc.algoviz.problem.repository.ProblemRepository;
import com.aisc.algoviz.problem.repository.SolutionRepository;
import com.aisc.algoviz.problem.repository.specification.ProblemSpecification;
import com.aisc.algoviz.problem.service.ProblemService;
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

        // 1. Xác định thứ tự sắp xếp an toàn từ Whitelist
        Sort.Direction direction = "desc".equalsIgnoreCase(filterRequest.getSortDirection())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        
        String safeSortBy = filterRequest.getSanitizedSortBy();
        Pageable pageable = PageRequest.of(filterRequest.getPage(), filterRequest.getSize(), Sort.by(direction, safeSortBy));

        // 2. Xây dựng Specification qua helper class
        Specification<Problem> spec = ProblemSpecification.buildSpecification(filterRequest);

        // 3. Thực thi truy vấn phân trang
        Page<Problem> problemPage = problemRepository.findAll(spec, pageable);

        // 4. Chuyển đổi Entity -> DTO qua Mapper
        Page<ProblemSummaryDto> dtoPage = problemPage.map(problemMapper::toSummaryDto);

        return PageResponse.from(dtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public ProblemDetailDto getProblemById(Long id) {
        log.debug("Fetching problem detail by ID: {}", id);

        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài toán với ID: " + id));

        return problemMapper.toDetailDto(problem);
    }

    @Override
    @Transactional(readOnly = true)
    public ProblemDetailDto getProblemBySlug(String slug) {
        log.debug("Fetching problem detail by slug: {}", slug);

        Problem problem = problemRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài toán với slug: " + slug));

        return problemMapper.toDetailDto(problem);
    }

    @Override
    @Transactional
    public ProblemDetailDto createProblem(CreateProblemRequestDto createDto) {
        log.info("Creating new problem: {}", createDto.getTitle());

        String slug = (createDto.getSlug() != null && !createDto.getSlug().isBlank())
                ? createDto.getSlug().trim()
                : problemMapper.generateSlug(createDto.getTitle());

        if (problemRepository.findBySlug(slug).isPresent()) {
            throw new BadRequestException("Bài toán với slug '" + slug + "' đã tồn tại trong hệ thống.");
        }

        Problem problem = problemMapper.toEntity(createDto);
        Problem savedProblem = problemRepository.save(problem);

        log.info("Successfully created problem ID: {}", savedProblem.getId());
        return problemMapper.toDetailDto(savedProblem);
    }

    @Override
    @Transactional
    public ProblemDetailDto updateProblem(Long id, UpdateProblemRequestDto updateDto) {
        log.info("Updating problem ID: {}", id);

        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài toán với ID: " + id));

        if (updateDto.getSlug() != null && !updateDto.getSlug().isBlank()
                && !updateDto.getSlug().equalsIgnoreCase(problem.getSlug())) {
            if (problemRepository.findBySlug(updateDto.getSlug()).isPresent()) {
                throw new BadRequestException("Bài toán với slug '" + updateDto.getSlug() + "' đã tồn tại.");
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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài toán với ID: " + id));

        problemRepository.delete(problem);
        log.info("Successfully deleted problem ID: {}", id);
    }

    @Override
    @Transactional
    public SolutionResponseDto addSolutionToProblem(Long problemId, CreateSolutionRequestDto solutionDto) {
        log.info("Adding solution to problem ID: {}", problemId);

        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài toán với ID: " + problemId));

        Solution solution = problemMapper.toSolutionEntity(solutionDto, problem);
        Solution savedSolution = solutionRepository.save(solution);

        log.info("Successfully added solution ID: {} to problem ID: {}", savedSolution.getId(), problemId);
        return problemMapper.toSolutionResponseDto(savedSolution);
    }
}
