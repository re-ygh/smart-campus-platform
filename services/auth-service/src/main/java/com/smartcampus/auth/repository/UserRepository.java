package com.smartcampus.auth.repository;

import com.smartcampus.auth.model.Role;
import com.smartcampus.auth.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
    List<UserAccount> findByTenantIdAndRole(String tenantId, Role role);
}

