package com.example.ums;

import com.example.ums.entity.AppUser;
import com.example.ums.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Login, logout and password hashing. */
class AuthenticationTests extends ApiTestSupport {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void loginWithCorrectPasswordReturnsTheUser() throws Exception {
        mockMvc.perform(post("/api/auth/login").param("username", "teacher").param("password", "teacher123"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("teacher"))
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void loginWithWrongPasswordIs401() throws Exception {
        mockMvc.perform(post("/api/auth/login").param("username", "admin").param("password", "wrong"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void loginWithUnknownUserIs401() throws Exception {
        mockMvc.perform(post("/api/auth/login").param("username", "nobody").param("password", "whatever"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginSentAsJsonExplainsTheFormat() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(
                        "Send username and password as form fields (x-www-form-urlencoded), not JSON"));
    }

    @Test
    void requestWithoutLoginIs401() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsTheLoggedInUser() throws Exception {
        mockMvc.perform(get("/api/auth/me").session(loginAsStudent()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("student"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void logoutEndsTheSession() throws Exception {
        MockHttpSession session = loginAsAdmin();
        mockMvc.perform(get("/api/admins").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out"));

        assertThat(session.isInvalid()).isTrue();
        mockMvc.perform(get("/api/admins"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutNeedsPost() throws Exception {
        MockHttpSession session = loginAsTeacher();
        mockMvc.perform(get("/api/auth/logout").session(session))
                .andExpect(status().isNotFound());
        assertThat(session.isInvalid()).isFalse();
    }

    @Test
    void passwordsAreStoredAsBcryptHashes() throws Exception {
        int n = unique();
        mockMvc.perform(post("/api/teachers").session(loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(teacherBody(n))))
                .andExpect(status().isCreated());

        AppUser saved = appUserRepository.findByUsername("teacher_" + n).orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo("secret" + n).startsWith("$2a$");
        assertThat(passwordEncoder.matches("secret" + n, saved.getPassword())).isTrue();

        // and the new teacher can log in with the plain password
        login("teacher_" + n, "secret" + n);
    }
}
