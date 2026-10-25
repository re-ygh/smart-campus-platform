package com.smartcampus.exam.web;

import com.smartcampus.exam.model.Course;
import com.smartcampus.exam.service.ExamService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final ExamService examService;

    @PostMapping
    @PreAuthorize("hasAnyRole('FACULTY','TEACHER')")
    public ResponseEntity<Course> create(@RequestBody @Valid CreateCourseRequest request, Authentication authentication) {
        return ResponseEntity.ok(examService.createCourse(request.title(), request.description(), authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<Course>> list() {
        return ResponseEntity.ok(examService.listCourses());
    }

    public record CreateCourseRequest(@NotBlank String title, String description) {}
}

