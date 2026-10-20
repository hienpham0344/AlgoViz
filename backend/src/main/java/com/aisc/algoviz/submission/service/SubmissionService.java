package com.aisc.algoviz.submission.service;

import com.aisc.algoviz.submission.dto.ExecutionResultDto;
import com.aisc.algoviz.common.exception.AppException;
import com.aisc.algoviz.problem.exception.ProblemErrorCode;
import com.aisc.algoviz.submission.dto.SubmissionRequest;
import com.aisc.algoviz.submission.dto.SubmissionResponse;
import com.aisc.algoviz.problem.entity.LegacyProblem;
import com.aisc.algoviz.submission.entity.Submission;
import com.aisc.algoviz.user.entity.User;
import com.aisc.algoviz.problem.repository.LegacyProblemRepository;
import com.aisc.algoviz.submission.repository.SubmissionRepository;
import com.aisc.algoviz.user.repository.UserRepository;
import com.aisc.algoviz.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final LegacyProblemRepository legacyProblemRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final CodeExecutionService codeExecutionService;

    @org.springframework.beans.factory.annotation.Value("${algoviz.gamification.points-per-accepted:10}")
    private int pointsPerAccepted = 10;

    @Transactional
    public SubmissionResponse submitCode(SubmissionRequest request) {
        User user = userService.getCurrentUser();
        LegacyProblem problem = legacyProblemRepository.findById(request.getProblemId())
                .orElseThrow(() -> new AppException(ProblemErrorCode.PROBLEM_NOT_FOUND));

        ExecutionResultDto evalResult = codeExecutionService.evaluateTestCases(
                request.getLanguage(),
                request.getCode(),
                problem.getTestCases()
        );

        Submission submission = Submission.builder()
                .user(user)
                .problem(problem)
                .language(request.getLanguage())
                .code(request.getCode())
                .status(evalResult.getStatus())
                .executionTimeMs(evalResult.getExecutionTimeMs())
                .memoryUsageMb(evalResult.getMemoryUsageMb())
                .passedTestCases(evalResult.getPassedTestCases())
                .totalTestCases(evalResult.getTotalTestCases())
                .errorMessage(evalResult.getErrorMessage())
                .build();

        boolean alreadySolved = "Accepted".equals(evalResult.getStatus())
                && submissionRepository.existsByUserIdAndProblemIdAndStatus(user.getId(), problem.getId(), "Accepted");
        Submission saved = submissionRepository.save(submission);

        if ("Accepted".equals(evalResult.getStatus())) {
            if (!alreadySolved) {
                user.setSolvedProblemsCount((user.getSolvedProblemsCount() != null ? user.getSolvedProblemsCount() : 0) + 1);
                user.setPoints((user.getPoints() != null ? user.getPoints() : 0) + pointsPerAccepted);
                userRepository.save(user);
            }
        }

        return mapToResponse(saved);
    }

    public List<SubmissionResponse> getUserSubmissionHistory() {
        User user = userService.getCurrentUser();
        return submissionRepository.findByUserIdOrderBySubmittedAtDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<SubmissionResponse> getUserSubmissionsForProblem(Long problemId) {
        User user = userService.getCurrentUser();
        return submissionRepository.findByUserIdAndProblemIdOrderBySubmittedAtDesc(user.getId(), problemId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private SubmissionResponse mapToResponse(Submission s) {
        return SubmissionResponse.builder()
                .id(s.getId())
                .problemId(s.getProblem().getId())
                .problemTitle(s.getProblem().getTitle())
                .username(s.getUser().getUsername())
                .language(s.getLanguage())
                .code(s.getCode())
                .status(s.getStatus())
                .executionTimeMs(s.getExecutionTimeMs())
                .memoryUsageMb(s.getMemoryUsageMb())
                .passedTestCases(s.getPassedTestCases())
                .totalTestCases(s.getTotalTestCases())
                .errorMessage(s.getErrorMessage())
                .submittedAt(s.getSubmittedAt())
                .build();
    }
}
