package com.smartcampus.exam.repository;

import com.smartcampus.exam.model.Exam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByTenantId(String tenantId);
    List<Exam> findByCourse_TeacherUsernameAndTenantId(String teacherUsername, String tenantId);
}

