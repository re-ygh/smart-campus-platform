package com.smartcampus.exam.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "student_answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ExamAttempt attempt;

    @Column(nullable = false)
    private Long questionId;

    @Column(nullable = false)
    private Integer selectedOptionIndex;

    @Column(nullable = false)
    private Boolean isCorrect;

    private String tenantId;
}

