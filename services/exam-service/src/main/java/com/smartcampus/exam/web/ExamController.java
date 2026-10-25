package com.smartcampus.exam.web;

import com.smartcampus.exam.model.*;
import com.smartcampus.exam.service.ExamService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @PostMapping
    @PreAuthorize("hasAnyRole('FACULTY','TEACHER')")
    public ResponseEntity<Exam> create(@RequestBody @Valid CreateExamRequest request) {
        return ResponseEntity.ok(examService.createExam(request.courseId(), request.title(), request.startTime(), request.durationMinutes()));
    }

    @GetMapping
    public ResponseEntity<List<Exam>> list(Authentication authentication) {
        // Check if user is a teacher - if so, only show their exams
        if (authentication != null && 
            authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"))) {
            return ResponseEntity.ok(examService.listExamsForTeacher(authentication.getName()));
        }
        // For students and faculty, show all exams
        return ResponseEntity.ok(examService.listExams());
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> start(@PathVariable Long id,
                                    Authentication authentication,
                                    @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            ExamAttempt attempt = examService.startExam(id, authentication.getName(), authHeader);
            return ResponseEntity.ok(attempt);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/questions")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<Question>> getQuestionsForStudent(@PathVariable Long id) {
        return ResponseEntity.ok(examService.listQuestionsForStudent(id));
    }

    @PostMapping("/attempts/{attemptId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ExamAttempt> submitAnswers(@PathVariable Long attemptId,
                                                      @RequestBody @Valid SubmitAnswersRequest request,
                                                      Authentication authentication) {
        try {
            ExamAttempt result = examService.submitAnswers(attemptId, request.answers(), authentication.getName());
            return ResponseEntity.ok(result);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/attempts/{attemptId}/result")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getResult(@PathVariable Long attemptId, Authentication authentication) {
        try {
            ExamAttempt attempt = examService.getAttemptResult(attemptId, authentication.getName());
            List<StudentAnswer> answers = examService.getStudentAnswers(attemptId);
            return ResponseEntity.ok(Map.of("attempt", attempt, "answers", answers));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{examId}")
    @PreAuthorize("hasAnyRole('FACULTY','TEACHER')")
    public ResponseEntity<?> delete(@PathVariable Long examId) {
        examService.deleteExam(examId);
        return ResponseEntity.ok().build();
    }

    public record CreateExamRequest(@NotNull Long courseId,
                                    @NotBlank String title,
                                    @NotNull LocalDateTime startTime,
                                    @Min(1) Integer durationMinutes) {}

    public record SubmitAnswersRequest(@NotNull Map<Long, Integer> answers) {}
}

