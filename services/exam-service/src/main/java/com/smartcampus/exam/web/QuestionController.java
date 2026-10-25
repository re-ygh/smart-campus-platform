package com.smartcampus.exam.web;

import com.smartcampus.exam.model.Question;
import com.smartcampus.exam.service.ExamService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final ExamService examService;

    @PostMapping
    @PreAuthorize("hasAnyRole('FACULTY','TEACHER')")
    public ResponseEntity<Question> addQuestion(@RequestBody @Valid AddQuestionRequest request) {
        Question question = examService.addQuestion(
                request.examId(),
                request.questionText(),
                request.options(),
                request.correctOptionIndex(),
                request.points()
        );
        return ResponseEntity.ok(question);
    }

    @GetMapping("/exam/{examId}")
    @PreAuthorize("hasAnyRole('FACULTY','TEACHER')")
    public ResponseEntity<List<Question>> listQuestions(@PathVariable Long examId) {
        return ResponseEntity.ok(examService.listQuestions(examId));
    }

    public record AddQuestionRequest(
            @NotNull Long examId,
            @NotBlank String questionText,
            @NotNull List<String> options,
            @NotNull @Min(0) Integer correctOptionIndex,
            @NotNull @Min(1) Integer points
    ) {}
}

