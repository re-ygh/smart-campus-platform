package com.smartcampus.exam.repository;

import com.smartcampus.exam.model.Exam;
import com.smartcampus.exam.model.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {
    List<ExamAttempt> findByTenantIdAndStudentUsername(String tenantId, String studentUsername);
    List<ExamAttempt> findByExamAndTenantId(Exam exam, String tenantId);
}

