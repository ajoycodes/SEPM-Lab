package com.example.ums;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** The error codes: 400, 404 and 409. */
class ValidationTests extends ApiTestSupport {

    @Test
    void invalidFieldsAre400AndListed() throws Exception {
        mvc.perform(post("/api/students").session(loginAsAdmin()).with(csrf()).contentType("application/json")
                        .content("{\"name\":\"\",\"email\":\"not-an-email\",\"password\":\"short\",\"department\":\"CSE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.length()").value(3));
    }

    @Test
    void passwordIsRequiredWhenCreating() throws Exception {
        mvc.perform(post("/api/students").session(loginAsAdmin()).with(csrf()).contentType("application/json")
                        .content("{\"name\":\"A\",\"email\":\"" + uniqueEmail() + "\",\"department\":\"CSE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password is required"));
    }

    @Test
    void passwordOver72BytesIs400() throws Exception {
        String request = """
                {"name":"A","email":"%s","password":"%s","department":"CSE"}"""
                .formatted(uniqueEmail(), "ব".repeat(30));
        mvc.perform(post("/api/students").session(loginAsAdmin()).with(csrf())
                        .contentType("application/json").content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateEmailIs409AcrossRoles() throws Exception {
        mvc.perform(post("/api/students").session(loginAsAdmin()).with(csrf()).contentType("application/json")
                        .content(body("admin@university.edu")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    void unknownIdIs404() throws Exception {
        MockHttpSession admin = loginAsAdmin();
        mvc.perform(get("/api/teachers/999999").session(admin)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/teachers/999999").session(admin).with(csrf())).andExpect(status().isNotFound());
    }
}
