package com.aisc.algoviz.submission.controller;

import com.aisc.algoviz.submission.dto.ExecutionResultDto;
import com.aisc.algoviz.submission.dto.RunCodeRequest;
import com.aisc.algoviz.submission.dto.SubmissionRequest;
import com.aisc.algoviz.submission.dto.SubmissionResponse;
import com.aisc.algoviz.submission.service.CodeExecutionService;
import com.aisc.algoviz.submission.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;
    private final CodeExecutionService codeExecutionService;

    @PostMapping("/run")
    public ResponseEntity<ExecutionResultDto> runCode(@Valid @RequestBody RunCodeRequest request) {
        return ResponseEntity.ok(codeExecutionService.runCodeCustomInput(
                request.getLanguage(),
                request.getCode(),
                request.getCustomInput()
        ));
    }

    @PostMapping("/submit")
    public ResponseEntity<SubmissionResponse> submitCode(@Valid @RequestBody SubmissionRequest request) {
        return ResponseEntity.ok(submissionService.submitCode(request));
    }

    @GetMapping("/history")
    public ResponseEntity<List<SubmissionResponse>> getUserHistory() {
        return ResponseEntity.ok(submissionService.getUserSubmissionHistory());
    }

    @GetMapping("/problem/{problemId}")
    public ResponseEntity<List<SubmissionResponse>> getUserSubmissionsForProblem(@PathVariable Long problemId) {
        return ResponseEntity.ok(submissionService.getUserSubmissionsForProblem(problemId));
    }
}
