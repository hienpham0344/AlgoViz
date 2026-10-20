package com.aisc.algoviz.problem.controller;

import com.aisc.algoviz.problem.dto.ProblemDetailDto;
import com.aisc.algoviz.problem.dto.ProblemDto;
import com.aisc.algoviz.problem.service.LegacyProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class LegacyProblemController {

    private final LegacyProblemService legacyProblemService;

    @GetMapping
    public ResponseEntity<List<ProblemDto>> getAllProblems(
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(legacyProblemService.getAllProblems(difficulty, category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProblemDetailDto> getProblemById(@PathVariable Long id) {
        return ResponseEntity.ok(legacyProblemService.getProblemById(id));
    }
}
