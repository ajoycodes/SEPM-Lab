package com.example.ums;

import com.example.ums.entity.Account;
import com.example.ums.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Login, logout, CSRF and password hashing. */
class AuthenticationTests extends ApiTestSupport {

    @Test
    void requestWithoutLoginIs401() throws Exception {
        mvc.perform(get("/api/students")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordIs401() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", "admin@university.edu").param("password", "wrong"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void loginAndMeNeverShowThePassword() throws Exception {
        mvc.perform(get("/api/auth/me").session(loginAsAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void loginIgnoresEmailCapitals() throws Exception {
        login("ADMIN@University.edu", "Admin123!");
    }

    @Test
    void logoutEndsTheSession() throws Exception {
        MockHttpSession session = loginAsAdmin();
        mvc.perform(post("/api/auth/logout").session(session).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void writesWithoutCsrfTokenAre403() throws Exception {
        MockHttpSession admin = loginAsAdmin();
        mvc.perform(post("/api/students").session(admin)
                        .contentType("application/json").content(body(uniqueEmail())))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/login")
                        .param("username", "admin@university.edu").param("password", "Admin123!"))
                .andExpect(status().isForbidden());
    }

    @Test
    void csrfEndpointGivesATokenAndHeaderName() throws Exception {
        mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"));
    }

    @Test
    void passwordsAreStoredAsBcryptHashes() throws Exception {
        String email = uniqueEmail();
        mvc.perform(post("/api/teachers").session(loginAsAdmin()).with(csrf())
                        .contentType("application/json").content(body(email)))
                .andExpect(status().isCreated());

        Account saved = accounts.findByEmail(email).orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo(PASSWORD).startsWith("$2a$");
        assertThat(passwordEncoder.matches(PASSWORD, saved.getPasswordHash())).isTrue();
        login(email, PASSWORD);
    }

    @Test
    void sessionSurvivesAnEmailChange() throws Exception {
        Account student = account(Role.STUDENT);
        MockHttpSession session = loginAs(student);
        String newEmail = uniqueEmail();
        mvc.perform(put("/api/profile/me").session(session).with(csrf()).contentType("application/json")
                        .content("{\"name\":\"New Name\",\"email\":\"" + newEmail + "\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(newEmail));
    }
}
