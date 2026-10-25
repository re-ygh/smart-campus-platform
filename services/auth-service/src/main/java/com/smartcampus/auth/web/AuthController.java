package com.smartcampus.auth.web;

import com.smartcampus.auth.model.Role;
import com.smartcampus.auth.model.UserAccount;
import com.smartcampus.auth.service.JwtService;
import com.smartcampus.auth.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody @Valid LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails user = (UserDetails) authentication.getPrincipal();
        UserAccount account = (UserAccount) user;
        String token = jwtService.generateToken(user.getUsername(), account.getRole().name(), account.getTenantId());
        return ResponseEntity.ok(Map.of("token", token, "role", account.getRole().name(), "tenantId", account.getTenantId()));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        UserAccount account = (UserAccount) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(Map.of(
                "username", account.getUsername(),
                "role", account.getRole(),
                "tenantId", account.getTenantId(),
                "fullName", account.getFullName()
        ));
    }

    @PostMapping("/faculty/users")
    public ResponseEntity<UserAccount> createUser(@RequestBody @Valid CreateUserRequest request) {
        UserAccount account = (UserAccount) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (account.getRole() != Role.FACULTY) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(userService.createUser(request.username(), request.password(), request.role(), request.fullName()));
    }

    @GetMapping("/faculty/users")
    public ResponseEntity<?> listByRole(@RequestParam Role role) {
        UserAccount account = (UserAccount) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (account.getRole() != Role.FACULTY) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(userService.listUsersByRole(role));
    }

    @DeleteMapping("/faculty/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        UserAccount account = (UserAccount) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (account.getRole() != Role.FACULTY) {
            return ResponseEntity.status(403).build();
        }
        userService.deleteUser(userId);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record CreateUserRequest(@NotBlank String username,
                                    @NotBlank String password,
                                    Role role,
                                    String fullName) {}
}

