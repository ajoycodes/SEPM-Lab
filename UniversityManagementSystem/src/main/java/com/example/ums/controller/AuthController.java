package com.example.ums.controller;
//It passess to accountService
import com.example.ums.dto.AccountResponse;
import com.example.ums.security.CurrentUser;
import com.example.ums.service.AccountService;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * POST /api/auth/login and POST /api/auth/logout are handled by Spring Security's own filters
 * (see SecurityConfig), so they never reach a controller.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CurrentUser currentUser;
    private final AccountService accountService;

    public AuthController(CurrentUser currentUser, AccountService accountService) {
        this.currentUser = currentUser;
        this.accountService = accountService;
    }

    /** The token a client must send back on every POST, PUT and DELETE. */
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    @GetMapping("/me")
    public AccountResponse me() {
        return accountService.toResponse(currentUser.require());
    }
}
