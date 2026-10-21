package com.aisc.algoviz.problem.service;

import com.aisc.algoviz.common.dto.PageResponse;
import com.aisc.algoviz.common.exception.AppException;
import com.aisc.algoviz.problem.dto.ProblemDetailDto;
import com.aisc.algoviz.problem.dto.ProblemDto;
import com.aisc.algoviz.problem.entity.LegacyProblem;
import com.aisc.algoviz.problem.exception.ProblemErrorCode;
import com.aisc.algoviz.problem.mapper.ProblemMapper;
import com.aisc.algoviz.problem.repository.LegacyProblemRepository;
import com.aisc.algoviz.problem.repository.SolutionRepository;
import com.aisc.algoviz.submission.repository.ProblemSubmissionStats;
import com.aisc.algoviz.submission.repository.SubmissionRepository;
import com.aisc.algoviz.submission.repository.TestCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Collections;
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
    private TestCaseRepository testCaseRepository;

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
    @DisplayName("Should return all problems when no filter is provided and compute acceptance in O(1)")
    void testGetAllProblemsNoFilter() {
        when(legacyProblemRepository.findAll()).thenReturn(List.of(sampleProblem));
        ProblemSubmissionStats stats = new ProblemSubmissionStats() {
            @Override public Long getProblemId() { return 1L; }
            @Override public Long getTotalCount() { return 10L; }
            @Override public Long getAcceptedCount() { return 6L; }
        };
        when(submissionRepository.findSubmissionStatsGroupedByProblem()).thenReturn(List.of(stats));

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
        when(submissionRepository.findSubmissionStatsGroupedByProblem()).thenReturn(Collections.emptyList());

        List<ProblemDto> result = legacyProblemService.getAllProblems("Easy", null);

        assertEquals(1, result.size());
        verify(legacyProblemRepository, times(1)).findByDifficulty("Easy");
    }

    @Test
    @DisplayName("Should return paginated problems via getProblemsPaged")
    void testGetProblemsPaged() {
        Pageable pageable = PageRequest.of(0, 10);
        when(legacyProblemRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(sampleProblem), pageable, 1));
        when(submissionRepository.findSubmissionStatsGroupedByProblem()).thenReturn(Collections.emptyList());

        PageResponse<ProblemDto> result = legacyProblemService.getProblemsPaged(null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("Two Sum", result.getContent().get(0).getTitle());
    }

    @Test
    @DisplayName("Should retrieve problem details by ID")
    void testGetProblemByIdSuccess() {
        when(legacyProblemRepository.findById(1L)).thenReturn(Optional.of(sampleProblem));
        when(testCaseRepository.findByProblemIdAndIsSampleTrue(1L)).thenReturn(Collections.emptyList());
        when(solutionRepository.findByProblemId(1L)).thenReturn(Collections.emptyList());

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
