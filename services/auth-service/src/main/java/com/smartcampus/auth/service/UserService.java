package com.smartcampus.auth.service;

import com.smartcampus.auth.model.Role;
import com.smartcampus.auth.model.UserAccount;
import com.smartcampus.auth.repository.UserRepository;
import com.smartcampus.auth.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public @NonNull UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return account;
    }

    public List<UserAccount> listUsersByRole(Role role) {
        return userRepository.findByTenantIdAndRole(TenantContext.getTenantId(), role);
    }

    @Transactional
    public UserAccount createUser(String username, String password, Role role, String fullName) {
        if (role == Role.FACULTY) {
            throw new IllegalArgumentException("Cannot self-create faculty via API");
        }
        UserAccount account = UserAccount.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .role(role)
                .tenantId(TenantContext.getTenantId())
                .fullName(fullName)
                .build();
        return userRepository.save(account);
    }

    @Transactional
    public void deleteUser(Long userId) {
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Check tenant isolation
        if (!user.getTenantId().equals(TenantContext.getTenantId())) {
            throw new IllegalArgumentException("Cannot delete user from different tenant");
        }
        
        // Cannot delete faculty users
        if (user.getRole() == Role.FACULTY) {
            throw new IllegalArgumentException("Cannot delete faculty users");
        }
        
        userRepository.delete(user);
    }
}

