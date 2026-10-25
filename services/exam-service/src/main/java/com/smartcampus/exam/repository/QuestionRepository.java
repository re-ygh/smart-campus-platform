package com.smartcampus.exam.repository;

import com.smartcampus.exam.model.Exam;
import com.smartcampus.exam.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByExamAndTenantId(Exam exam, String tenantId);
    List<Question> findByExamIdAndTenantId(Long examId, String tenantId);
}

