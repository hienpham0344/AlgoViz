package com.aisc.algoviz.controller;

import com.aisc.algoviz.dto.ProblemDetailDto;
import com.aisc.algoviz.dto.ProblemDto;
import com.aisc.algoviz.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;

    @GetMapping
    public ResponseEntity<List<ProblemDto>> getAllProblems(
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(problemService.getAllProblems(difficulty, category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProblemDetailDto> getProblemById(@PathVariable Long id) {
        return ResponseEntity.ok(problemService.getProblemById(id));
    }
}
