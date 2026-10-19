package com.aisc.algoviz.problem.service;

import com.aisc.algoviz.common.exception.AppException;
import com.aisc.algoviz.common.exception.ErrorCode;
import com.aisc.algoviz.problem.dto.ProblemDetailDto;
import com.aisc.algoviz.problem.dto.ProblemDto;
import com.aisc.algoviz.problem.entity.LegacyProblem;
import com.aisc.algoviz.problem.exception.ProblemErrorCode;
import com.aisc.algoviz.problem.mapper.ProblemMapper;
import com.aisc.algoviz.problem.repository.LegacyProblemRepository;
import com.aisc.algoviz.problem.repository.SolutionRepository;
import com.aisc.algoviz.submission.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LegacyProblemServiceTest {

    @Mock
    private LegacyProblemRepository legacyProblemRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private SolutionRepository solutionRepository;

    @Mock
    private ProblemMapper problemMapper;

    @InjectMocks
    private LegacyProblemService legacyProblemService;

    private LegacyProblem sampleProblem;

    @BeforeEach
    void setUp() {
        sampleProblem = LegacyProblem.legacyBuilder()
                .id(1L)
                .title("Two Sum")
                .difficulty("Easy")
                .category("Arrays")
                .description("Two Sum problem description")
                .testCases(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Should return all problems when no filter is provided")
    void testGetAllProblemsNoFilter() {
        when(legacyProblemRepository.findAll()).thenReturn(List.of(sampleProblem));
        when(submissionRepository.countByProblemId(1L)).thenReturn(10L);
        when(submissionRepository.countByProblemIdAndStatus(1L, "Accepted")).thenReturn(6L);

        List<ProblemDto> result = legacyProblemService.getAllProblems(null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Two Sum", result.get(0).getTitle());
        assertEquals(60.0, result.get(0).getAcceptanceRate());
    }

    @Test
    @DisplayName("Should filter problems by difficulty")
    void testGetProblemsByDifficulty() {
        when(legacyProblemRepository.findByDifficulty("Easy")).thenReturn(List.of(sampleProblem));
        when(submissionRepository.countByProblemId(1L)).thenReturn(0L);

        List<ProblemDto> result = legacyProblemService.getAllProblems("Easy", null);

        assertEquals(1, result.size());
        verify(legacyProblemRepository, times(1)).findByDifficulty("Easy");
    }

    @Test
    @DisplayName("Should retrieve problem details by ID")
    void testGetProblemByIdSuccess() {
        when(legacyProblemRepository.findById(1L)).thenReturn(Optional.of(sampleProblem));

        ProblemDetailDto detail = legacyProblemService.getProblemById(1L);

        assertNotNull(detail);
        assertEquals(1L, detail.getId());
        assertEquals("Two Sum", detail.getTitle());
    }

    @Test
    @DisplayName("Should throw PROBLEM_NOT_FOUND when problem does not exist")
    void testGetProblemByIdNotFound() {
        when(legacyProblemRepository.findById(999L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> legacyProblemService.getProblemById(999L));
        assertEquals(ProblemErrorCode.PROBLEM_NOT_FOUND, ex.getErrorCode());
    }
}
