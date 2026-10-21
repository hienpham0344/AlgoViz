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
import java.util.Optional;
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
        Long rawId = request.getProblemId();
        LegacyProblem problem = legacyProblemRepository.findById(rawId)
                .or(() -> rawId != null ? legacyProblemRepository.findByLeetcodeId(rawId.intValue()) : Optional.empty())
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

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getUserSubmissionHistory() {
        User user = userService.getCurrentUser();
        return submissionRepository.findByUserIdOrderBySubmittedAtDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public com.aisc.algoviz.common.dto.PageResponse<SubmissionResponse> getUserSubmissionHistoryPaged(org.springframework.data.domain.Pageable pageable) {
        User user = userService.getCurrentUser();
        org.springframework.data.domain.Page<Submission> page = submissionRepository.findByUserIdOrderBySubmittedAtDesc(user.getId(), pageable);
        return com.aisc.algoviz.common.dto.PageResponse.from(page.map(this::mapToResponse));
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getUserSubmissionsForProblem(Long problemId) {
        User user = userService.getCurrentUser();
        LegacyProblem problem = legacyProblemRepository.findById(problemId)
                .or(() -> problemId != null ? legacyProblemRepository.findByLeetcodeId(problemId.intValue()) : Optional.empty())
                .orElse(null);
        Long targetId = (problem != null) ? problem.getId() : problemId;
        return submissionRepository.findByUserIdAndProblemIdOrderBySubmittedAtDesc(user.getId(), targetId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private SubmissionResponse mapToResponse(Submission s) {
        Long pId = s.getProblem().getLeetcodeId() != null 
                ? Long.valueOf(s.getProblem().getLeetcodeId()) 
                : s.getProblem().getId();
        return SubmissionResponse.builder()
                .id(s.getId())
                .problemId(pId)
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
