package com.example.ums.controller;

import com.example.ums.dto.UserSummary;
import com.example.ums.exception.ResourceNotFoundException;
import com.example.ums.repository.AppUserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST /api/auth/login and POST /api/auth/logout are handled by Spring Security's own filters
 * (see SecurityConfig), so they never reach a controller. This one only answers "who am I?".
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth")
public class AuthController {

    private final AppUserRepository appUserRepository;

    public AuthController(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @GetMapping("/me")
    @Operation(summary = "Who am I")
    public ResponseEntity<UserSummary> me(Authentication authentication) {
        UserSummary user = appUserRepository.findByUsername(authentication.getName())
                .map(UserSummary::from)
                // The account was deleted while this session was still logged in.
                .orElseThrow(() -> new ResourceNotFoundException("Your account no longer exists"));
        return ResponseEntity.ok(user);
    }
}
