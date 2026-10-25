package com.smartcampus.exam.repository;

import com.smartcampus.exam.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByTenantId(String tenantId);
}

