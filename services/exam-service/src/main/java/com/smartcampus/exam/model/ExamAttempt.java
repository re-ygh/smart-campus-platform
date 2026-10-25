package com.smartcampus.exam.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "exam_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exam_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Exam exam;

    private String studentUsername;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer score;
    private Integer maxScore;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;
    
    private String tenantId;
}

