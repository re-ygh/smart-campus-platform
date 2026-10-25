package com.smartcampus.exam.repository;

import com.smartcampus.exam.model.ExamAttempt;
import com.smartcampus.exam.model.StudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Long> {
    List<StudentAnswer> findByAttemptAndTenantId(ExamAttempt attempt, String tenantId);
    List<StudentAnswer> findByAttemptIdAndTenantId(Long attemptId, String tenantId);
}

