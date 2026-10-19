package com.aisc.algoviz.submission.service;

import com.aisc.algoviz.common.exception.AppException;
import com.aisc.algoviz.problem.entity.LegacyProblem;
import com.aisc.algoviz.problem.exception.ProblemErrorCode;
import com.aisc.algoviz.problem.repository.LegacyProblemRepository;
import com.aisc.algoviz.submission.dto.ExecutionResultDto;
import com.aisc.algoviz.submission.dto.SubmissionRequest;
import com.aisc.algoviz.submission.dto.SubmissionResponse;
import com.aisc.algoviz.submission.entity.Submission;
import com.aisc.algoviz.submission.repository.SubmissionRepository;
import com.aisc.algoviz.user.entity.User;
import com.aisc.algoviz.user.repository.UserRepository;
import com.aisc.algoviz.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private LegacyProblemRepository legacyProblemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private CodeExecutionService codeExecutionService;

    @InjectMocks
    private SubmissionService submissionService;

    private User sampleUser;
    private LegacyProblem sampleProblem;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(submissionService, "pointsPerAccepted", 10);

        sampleUser = User.builder()
                .id(1L)
                .username("coder1")
                .points(50)
                .solvedProblemsCount(5)
                .build();

        sampleProblem = LegacyProblem.legacyBuilder()
                .id(100L)
                .title("Two Sum")
                .testCases(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Accepted submission for the first time increments points and solved count")
    void testFirstAcceptedSubmission() {
        when(userService.getCurrentUser()).thenReturn(sampleUser);
        when(legacyProblemRepository.findById(100L)).thenReturn(Optional.of(sampleProblem));

        ExecutionResultDto evalResult = ExecutionResultDto.builder()
                .status("Accepted")
                .executionTimeMs(15.5)
                .memoryUsageMb(24.0)
                .passedTestCases(3)
                .totalTestCases(3)
                .build();

        when(codeExecutionService.evaluateTestCases("python", "print('hello')", sampleProblem.getTestCases()))
                .thenReturn(evalResult);
        when(submissionRepository.existsByUserIdAndProblemIdAndStatus(1L, 100L, "Accepted")).thenReturn(false);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            return Submission.builder()
                    .id(500L)
                    .user(s.getUser())
                    .problem(s.getProblem())
                    .language(s.getLanguage())
                    .code(s.getCode())
                    .status(s.getStatus())
                    .executionTimeMs(s.getExecutionTimeMs())
                    .memoryUsageMb(s.getMemoryUsageMb())
                    .passedTestCases(s.getPassedTestCases())
                    .totalTestCases(s.getTotalTestCases())
                    .build();
        });

        SubmissionRequest request = new SubmissionRequest();
        request.setProblemId(100L);
        request.setLanguage("python");
        request.setCode("print('hello')");

        SubmissionResponse response = submissionService.submitCode(request);

        assertNotNull(response);
        assertEquals(500L, response.getId());
        assertEquals("Accepted", response.getStatus());
        assertEquals(100L, response.getProblemId());
        assertEquals("Two Sum", response.getProblemTitle());
        assertEquals("coder1", response.getUsername());

        // Assert User state mutation
        assertEquals(6, sampleUser.getSolvedProblemsCount());
        assertEquals(60, sampleUser.getPoints());
        verify(userRepository, times(1)).save(sampleUser);

        // Assert Submission entity capture
        ArgumentCaptor<Submission> subCaptor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionRepository).save(subCaptor.capture());
        Submission savedSub = subCaptor.getValue();
        assertEquals("python", savedSub.getLanguage());
        assertEquals(3, savedSub.getPassedTestCases());
    }

    @Test
    @DisplayName("Accepted submission when already solved does NOT increase points or count again")
    void testRepeatedAcceptedSubmission() {
        when(userService.getCurrentUser()).thenReturn(sampleUser);
        when(legacyProblemRepository.findById(100L)).thenReturn(Optional.of(sampleProblem));

        ExecutionResultDto evalResult = ExecutionResultDto.builder()
                .status("Accepted")
                .executionTimeMs(10.0)
                .memoryUsageMb(20.0)
                .passedTestCases(3)
                .totalTestCases(3)
                .build();

        when(codeExecutionService.evaluateTestCases(anyString(), anyString(), anyList()))
                .thenReturn(evalResult);
        when(submissionRepository.existsByUserIdAndProblemIdAndStatus(1L, 100L, "Accepted")).thenReturn(true);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            s.setId(501L);
            return s;
        });

        SubmissionRequest request = new SubmissionRequest();
        request.setProblemId(100L);
        request.setLanguage("python");
        request.setCode("print('solution')");

        SubmissionResponse response = submissionService.submitCode(request);

        assertNotNull(response);
        // Points and count MUST remain unchanged
        assertEquals(5, sampleUser.getSolvedProblemsCount());
        assertEquals(50, sampleUser.getPoints());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Failed submission (Wrong Answer) saves submission without updating user points")
    void testFailedSubmission() {
        when(userService.getCurrentUser()).thenReturn(sampleUser);
        when(legacyProblemRepository.findById(100L)).thenReturn(Optional.of(sampleProblem));

        ExecutionResultDto evalResult = ExecutionResultDto.builder()
                .status("Wrong Answer")
                .executionTimeMs(12.0)
                .memoryUsageMb(22.0)
                .passedTestCases(1)
                .totalTestCases(3)
                .errorMessage("Expected 4, got 5")
                .build();

        when(codeExecutionService.evaluateTestCases(anyString(), anyString(), anyList())).thenReturn(evalResult);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            s.setId(502L);
            return s;
        });

        SubmissionRequest request = new SubmissionRequest();
        request.setProblemId(100L);
        request.setLanguage("python");
        request.setCode("print('wrong')");

        SubmissionResponse response = submissionService.submitCode(request);

        assertNotNull(response);
        assertEquals("Wrong Answer", response.getStatus());
        assertEquals(5, sampleUser.getSolvedProblemsCount());
        assertEquals(50, sampleUser.getPoints());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("submitCode throws PROBLEM_NOT_FOUND when problem ID does not exist")
    void testSubmitCodeProblemNotFound() {
        when(userService.getCurrentUser()).thenReturn(sampleUser);
        when(legacyProblemRepository.findById(999L)).thenReturn(Optional.empty());

        SubmissionRequest request = new SubmissionRequest();
        request.setProblemId(999L);
        request.setLanguage("python");
        request.setCode("pass");

        AppException ex = assertThrows(AppException.class, () -> submissionService.submitCode(request));
        assertEquals(ProblemErrorCode.PROBLEM_NOT_FOUND, ex.getErrorCode());
        verify(submissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("getUserSubmissionHistory maps and returns list in descending order")
    void testGetUserSubmissionHistory() {
        when(userService.getCurrentUser()).thenReturn(sampleUser);

        Submission sub = Submission.builder()
                .id(1L)
                .problem(sampleProblem)
                .user(sampleUser)
                .language("python")
                .code("test")
                .status("Accepted")
                .build();

        when(submissionRepository.findByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(List.of(sub));

        List<SubmissionResponse> history = submissionService.getUserSubmissionHistory();

        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals("Accepted", history.get(0).getStatus());
        assertEquals(100L, history.get(0).getProblemId());
    }

    @Test
    @DisplayName("getUserSubmissionsForProblem maps and returns list filtered by problem ID")
    void testGetUserSubmissionsForProblem() {
        when(userService.getCurrentUser()).thenReturn(sampleUser);

        Submission sub = Submission.builder()
                .id(2L)
                .problem(sampleProblem)
                .user(sampleUser)
                .language("java")
                .code("class Main{}")
                .status("Wrong Answer")
                .build();

        when(submissionRepository.findByUserIdAndProblemIdOrderBySubmittedAtDesc(1L, 100L)).thenReturn(List.of(sub));

        List<SubmissionResponse> results = submissionService.getUserSubmissionsForProblem(100L);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Wrong Answer", results.get(0).getStatus());
    }
}
