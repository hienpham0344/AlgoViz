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
import com.aisc.algoviz.problem.enums.Difficulty;
import com.aisc.algoviz.problem.exception.ProblemErrorCode;
import com.aisc.algoviz.problem.mapper.ProblemMapper;
import com.aisc.algoviz.problem.repository.ProblemRepository;
import com.aisc.algoviz.problem.repository.SolutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProblemServiceImplTest {

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private SolutionRepository solutionRepository;

    @Mock
    private ProblemMapper problemMapper;

    @InjectMocks
    private ProblemServiceImpl problemService;

    private Problem sampleProblem;
    private ProblemDetailDto sampleDetailDto;
    private ProblemSummaryDto sampleSummaryDto;

    @BeforeEach
    void setUp() {
        sampleProblem = Problem.builder()
                .id(1L)
                .title("Two Sum")
                .slug("two-sum")
                .difficulty(Difficulty.EASY)
                .build();

        sampleDetailDto = ProblemDetailDto.builder()
                .id(1L)
                .title("Two Sum")
                .slug("two-sum")
                .difficulty(Difficulty.EASY)
                .build();

        sampleSummaryDto = ProblemSummaryDto.builder()
                .id(1L)
                .title("Two Sum")
                .slug("two-sum")
                .difficulty(Difficulty.EASY)
                .build();
    }

    @Test
    @DisplayName("getProblems with filter should query repository and map to PageResponse")
    void testGetProblems() {
        ProblemFilterRequest filterRequest = new ProblemFilterRequest();
        filterRequest.setPage(0);
        filterRequest.setSize(10);
        filterRequest.setSortBy("createdAt");
        filterRequest.setSortDirection("desc");

        Page<Problem> problemPage = new PageImpl<>(List.of(sampleProblem));
        when(problemRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(problemPage);
        when(problemMapper.toSummaryDto(sampleProblem)).thenReturn(sampleSummaryDto);

        PageResponse<ProblemSummaryDto> result = problemService.getProblems(filterRequest);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Two Sum", result.getContent().get(0).getTitle());
        assertEquals(0, result.getPageNumber());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("getProblemById returns problem when found")
    void testGetProblemByIdFound() {
        when(problemRepository.findById(1L)).thenReturn(Optional.of(sampleProblem));
        when(problemMapper.toDetailDto(sampleProblem)).thenReturn(sampleDetailDto);

        ProblemDetailDto result = problemService.getProblemById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("two-sum", result.getSlug());
        assertEquals(Difficulty.EASY, result.getDifficulty());
    }

    @Test
    @DisplayName("getProblemById throws PROBLEM_NOT_FOUND when not found")
    void testGetProblemByIdNotFound() {
        when(problemRepository.findById(999L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> problemService.getProblemById(999L));
        assertEquals(ProblemErrorCode.PROBLEM_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("getProblemBySlug returns problem when found")
    void testGetProblemBySlugFound() {
        when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.of(sampleProblem));
        when(problemMapper.toDetailDto(sampleProblem)).thenReturn(sampleDetailDto);

        ProblemDetailDto result = problemService.getProblemBySlug("two-sum");

        assertNotNull(result);
        assertEquals("two-sum", result.getSlug());
    }

    @Test
    @DisplayName("getProblemBySlug throws PROBLEM_NOT_FOUND when not found")
    void testGetProblemBySlugNotFound() {
        when(problemRepository.findBySlug("unknown")).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> problemService.getProblemBySlug("unknown"));
        assertEquals(ProblemErrorCode.PROBLEM_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("createProblem succeeds when slug does not exist")
    void testCreateProblemSuccess() {
        CreateProblemRequestDto createDto = CreateProblemRequestDto.builder()
                .title("Two Sum")
                .slug("two-sum")
                .difficulty(Difficulty.EASY)
                .build();

        when(problemMapper.toEntity(createDto)).thenReturn(sampleProblem);
        when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.empty());
        when(problemRepository.save(sampleProblem)).thenReturn(sampleProblem);
        when(problemMapper.toDetailDto(sampleProblem)).thenReturn(sampleDetailDto);

        ProblemDetailDto result = problemService.createProblem(createDto);

        assertNotNull(result);
        assertEquals("two-sum", result.getSlug());
        verify(problemRepository, times(1)).save(sampleProblem);
    }

    @Test
    @DisplayName("createProblem throws SLUG_ALREADY_EXISTS when slug exists")
    void testCreateProblemDuplicateSlug() {
        CreateProblemRequestDto createDto = CreateProblemRequestDto.builder()
                .title("Two Sum")
                .slug("two-sum")
                .build();

        when(problemMapper.toEntity(createDto)).thenReturn(sampleProblem);
        when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.of(sampleProblem));

        AppException ex = assertThrows(AppException.class, () -> problemService.createProblem(createDto));
        assertEquals(ProblemErrorCode.SLUG_ALREADY_EXISTS, ex.getErrorCode());
        verify(problemRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateProblem succeeds and saves updated entity")
    void testUpdateProblemSuccess() {
        UpdateProblemRequestDto updateDto = UpdateProblemRequestDto.builder()
                .title("Two Sum Updated")
                .slug("two-sum")
                .build();

        when(problemRepository.findById(1L)).thenReturn(Optional.of(sampleProblem));
        when(problemRepository.save(sampleProblem)).thenReturn(sampleProblem);
        when(problemMapper.toDetailDto(sampleProblem)).thenReturn(sampleDetailDto);

        ProblemDetailDto result = problemService.updateProblem(1L, updateDto);

        assertNotNull(result);
        verify(problemMapper, times(1)).updateEntityFromDto(updateDto, sampleProblem);
        verify(problemRepository, times(1)).save(sampleProblem);
    }

    @Test
    @DisplayName("updateProblem throws SLUG_ALREADY_EXISTS if changed to an existing slug")
    void testUpdateProblemDuplicateSlug() {
        UpdateProblemRequestDto updateDto = UpdateProblemRequestDto.builder()
                .title("New Title")
                .slug("another-existing-slug")
                .build();

        Problem existingOther = Problem.builder().id(2L).slug("another-existing-slug").build();

        when(problemRepository.findById(1L)).thenReturn(Optional.of(sampleProblem));
        when(problemRepository.findBySlug("another-existing-slug")).thenReturn(Optional.of(existingOther));

        AppException ex = assertThrows(AppException.class, () -> problemService.updateProblem(1L, updateDto));
        assertEquals(ProblemErrorCode.SLUG_ALREADY_EXISTS, ex.getErrorCode());
        verify(problemRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteProblem deletes existing problem")
    void testDeleteProblemSuccess() {
        when(problemRepository.findById(1L)).thenReturn(Optional.of(sampleProblem));

        problemService.deleteProblem(1L);

        verify(problemRepository, times(1)).delete(sampleProblem);
    }

    @Test
    @DisplayName("deleteProblem throws PROBLEM_NOT_FOUND when problem does not exist")
    void testDeleteProblemNotFound() {
        when(problemRepository.findById(999L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> problemService.deleteProblem(999L));
        assertEquals(ProblemErrorCode.PROBLEM_NOT_FOUND, ex.getErrorCode());
        verify(problemRepository, never()).delete((Problem) any());
    }

    @Test
    @DisplayName("addSolutionToProblem maps, saves and returns SolutionResponseDto")
    void testAddSolutionToProblemSuccess() {
        CreateSolutionRequestDto solutionDto = CreateSolutionRequestDto.builder()
                .codeSnippet("def solve(): pass")
                .explanation("Hash table lookup")
                .language("PYTHON")
                .build();

        Solution solution = Solution.builder()
                .id(10L)
                .codeSnippet("def solve(): pass")
                .problem(sampleProblem)
                .build();

        SolutionResponseDto responseDto = SolutionResponseDto.builder()
                .id(10L)
                .codeSnippet("def solve(): pass")
                .explanation("Hash table lookup")
                .build();

        when(problemRepository.findById(1L)).thenReturn(Optional.of(sampleProblem));
        when(problemMapper.toSolutionEntity(solutionDto, sampleProblem)).thenReturn(solution);
        when(solutionRepository.save(solution)).thenReturn(solution);
        when(problemMapper.toSolutionResponseDto(solution)).thenReturn(responseDto);

        SolutionResponseDto result = problemService.addSolutionToProblem(1L, solutionDto);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("def solve(): pass", result.getCodeSnippet());
    }
}
