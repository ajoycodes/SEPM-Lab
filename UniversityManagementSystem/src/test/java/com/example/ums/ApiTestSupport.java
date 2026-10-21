package com.example.ums;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared setup: the whole app with an H2 database, and helpers that log in through the real
 * POST /api/auth/login endpoint and keep the session, just like Postman keeps the cookie.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiTestSupport {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected MockHttpSession login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    protected MockHttpSession loginAsAdmin() throws Exception {
        return login("admin", "admin123");
    }

    protected MockHttpSession loginAsTeacher() throws Exception {
        return login("teacher", "teacher123");
    }

    protected MockHttpSession loginAsStudent() throws Exception {
        return login("student", "student123");
    }

    protected String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    protected JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    /** A number no other test has used, so usernames, e-mails and rolls never clash. */
    protected static int unique() {
        return COUNTER.incrementAndGet();
    }

    protected static Map<String, Object> adminBody(int n) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Admin " + n);
        body.put("email", "admin" + n + "@test.edu");
        body.put("phone", "+88017" + n);
        body.put("username", "admin_" + n);
        body.put("password", "secret" + n);
        return body;
    }

    protected static Map<String, Object> teacherBody(int n) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Teacher " + n);
        body.put("email", "teacher" + n + "@test.edu");
        body.put("department", "CSE");
        body.put("designation", "Lecturer");
        body.put("username", "teacher_" + n);
        body.put("password", "secret" + n);
        return body;
    }

    protected static Map<String, Object> studentBody(int n) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Student " + n);
        body.put("email", "student" + n + "@test.edu");
        body.put("roll", "R" + n);
        body.put("department", "CSE");
        body.put("batch", 2022);
        body.put("username", "student_" + n);
        body.put("password", "secret" + n);
        return body;
    }
}
