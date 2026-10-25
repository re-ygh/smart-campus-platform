package com.smartcampus.booking.repository;

import com.smartcampus.booking.model.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findByTenantId(String tenantId);
}

