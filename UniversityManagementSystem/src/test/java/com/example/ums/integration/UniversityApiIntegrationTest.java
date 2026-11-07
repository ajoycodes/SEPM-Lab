package com.example.ums.integration;

import com.example.ums.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests: the whole application starts (controllers, Spring Security, services, JPA) on an
 * in-memory H2 database, and every request goes through the real login, CSRF and role checks.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UniversityApiIntegrationTest {

    // The first admin, created at startup by DataSeeder from application-test.properties.
    private static final String ADMIN_EMAIL = "admin@university.edu";
    private static final String ADMIN_PASSWORD = "Admin123!";

    @Autowired
    MockMvc mvc;

    @Autowired
    AccountRepository accountRepository;

    /** Logs in through POST /api/auth/login, as the website does, and returns the session. */
    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", email)
                        .param("password", password))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private static String json(String name, String email, String password) {
        return """
                {"name":"%s","email":"%s","password":"%s","department":"CSE"}""".formatted(name, email, password);
    }

    @Test
    void adminCanCreateReadUpdateAndDeleteAStudent() throws Exception {
        MockHttpSession admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        MvcResult created = mvc.perform(post("/api/students").session(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Rahim Uddin", "rahim@university.edu", "Student123!")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();
        String location = created.getResponse().getHeader("Location");

        mvc.perform(get(location).session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rahim Uddin"));

        mvc.perform(put(location).session(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rahim Ahmed\",\"email\":\"rahim@university.edu\",\"department\":\"CSE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rahim Ahmed"));

        mvc.perform(delete(location).session(admin).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get(location).session(admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void eachRoleReachesOnlyItsOwnResource() throws Exception {
        mvc.perform(get("/api/students")).andExpect(status().isUnauthorized());

        MockHttpSession admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        mvc.perform(post("/api/teachers").session(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Karim Hossain", "karim@university.edu", "Teacher123!")))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/students").session(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Sadia Islam", "sadia@university.edu", "Student123!")))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/admins").session(admin)).andExpect(status().isOk());

        MockHttpSession teacher = login("karim@university.edu", "Teacher123!");
        mvc.perform(get("/api/teachers").session(teacher)).andExpect(status().isOk());
        mvc.perform(get("/api/students").session(teacher)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admins").session(teacher)).andExpect(status().isForbidden());

        MockHttpSession student = login("sadia@university.edu", "Student123!");
        mvc.perform(get("/api/students").session(student)).andExpect(status().isOk());
        mvc.perform(get("/api/teachers").session(student)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admins").session(student)).andExpect(status().isForbidden());
    }

    @Test
    void passwordIsStoredAsABcryptHashAndWorksForLogin() throws Exception {
        MockHttpSession admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        mvc.perform(post("/api/teachers").session(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Farhana Akter", "farhana@university.edu", "Teacher123!")))
                .andExpect(status().isCreated());

        String storedHash = accountRepository.findByEmail("farhana@university.edu").orElseThrow().getPasswordHash();
        assertThat(storedHash).isNotEqualTo("Teacher123!").startsWith("$2a$");

        mvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", "farhana@university.edu")
                        .param("password", "Wrong123!"))
                .andExpect(status().isUnauthorized());

        MockHttpSession teacher = login("farhana@university.edu", "Teacher123!");
        mvc.perform(get("/api/auth/me").session(teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mvc.perform(post("/api/auth/logout").session(teacher).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(teacher.isInvalid()).isTrue();
    }
}
