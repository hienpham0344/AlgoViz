package com.aisc.algoviz.service;

import com.aisc.algoviz.dto.ExecutionResultDto;
import com.aisc.algoviz.dto.SubmissionRequest;
import com.aisc.algoviz.model.Problem;
import com.aisc.algoviz.model.Submission;
import com.aisc.algoviz.model.User;
import com.aisc.algoviz.repository.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SubmissionServiceTest {
    @Test
    void firstAcceptedSubmissionIncrementsProgressOnlyOnce() {
        var submissions = mock(SubmissionRepository.class);
        var problems = mock(ProblemRepository.class);
        var users = mock(UserRepository.class);
        var execution = mock(CodeExecutionService.class);
        var userService = mock(UserService.class);
        var service = new SubmissionService(submissions, problems, users, userService, execution);
        var user = User.builder().id(1L).solvedProblemsCount(0).points(0).build();
        var problem = Problem.builder().id(2L).title("Valid Anagram").build();
        when(userService.getCurrentUser()).thenReturn(user);
        when(problems.findById(2L)).thenReturn(Optional.of(problem));
        when(execution.evaluateTestCases(anyString(), anyString(), anyList())).thenReturn(
                ExecutionResultDto.builder().status("Accepted").passedTestCases(1).totalTestCases(1).build());
        when(submissions.existsByUserIdAndProblemIdAndStatus(1L, 2L, "Accepted")).thenReturn(false, true);
        when(submissions.save(any(Submission.class))).thenAnswer(call -> call.getArgument(0));
        var request = new SubmissionRequest();
        request.setProblemId(2L); request.setLanguage("java"); request.setCode("class Main {}");
        service.submitCode(request);
        service.submitCode(request);
        assertEquals(1, user.getSolvedProblemsCount());
        assertEquals(10, user.getPoints());
        verify(users, times(1)).save(user);
        var order = inOrder(submissions);
        order.verify(submissions).existsByUserIdAndProblemIdAndStatus(1L, 2L, "Accepted");
        order.verify(submissions).save(any(Submission.class));
    }
}
