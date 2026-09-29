package com.aisc.algoviz.service;

import com.aisc.algoviz.dto.ExecutionResultDto;
import com.aisc.algoviz.dto.SubmissionRequest;
import com.aisc.algoviz.dto.SubmissionResponse;
import com.aisc.algoviz.model.Problem;
import com.aisc.algoviz.model.Submission;
import com.aisc.algoviz.model.User;
import com.aisc.algoviz.repository.ProblemRepository;
import com.aisc.algoviz.repository.SubmissionRepository;
import com.aisc.algoviz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final CodeExecutionService codeExecutionService;

    @Transactional
    public SubmissionResponse submitCode(SubmissionRequest request) {
        User user = userService.getCurrentUser();
        Problem problem = problemRepository.findById(request.getProblemId())
                .orElseThrow(() -> new RuntimeException("Problem not found with id: " + request.getProblemId()));

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

        Submission saved = submissionRepository.save(submission);

        if ("Accepted".equals(evalResult.getStatus())) {
            boolean alreadySolved = submissionRepository.existsByUserIdAndProblemIdAndStatus(user.getId(), problem.getId(), "Accepted");
            if (!alreadySolved) {
                user.setSolvedProblemsCount((user.getSolvedProblemsCount() != null ? user.getSolvedProblemsCount() : 0) + 1);
                user.setPoints((user.getPoints() != null ? user.getPoints() : 0) + 10);
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
