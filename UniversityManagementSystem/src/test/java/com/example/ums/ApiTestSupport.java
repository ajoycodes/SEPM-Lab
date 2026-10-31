package com.example.ums;

import com.example.ums.entity.Account;
import com.example.ums.entity.Role;
import com.example.ums.repository.AccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The whole app on an H2 database, with helpers that log in through the real login endpoint. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class ApiTestSupport {

    static final String PASSWORD = "Password123!";
    private static final AtomicInteger COUNTER = new AtomicInteger();

    @Autowired
    MockMvc mvc;

    @Autowired
    AccountRepository accounts;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    ObjectMapper objectMapper;

    MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", email)
                        .param("password", password))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    MockHttpSession loginAsAdmin() throws Exception {
        return login("admin@university.edu", "Admin123!");
    }

    MockHttpSession loginAs(Account account) throws Exception {
        return login(account.getEmail(), PASSWORD);
    }

    /** A new account saved directly in the database, with a unique email. */
    Account account(Role role) {
        Account account = new Account();
        account.setName("Test " + role);
        account.setEmail(uniqueEmail());
        account.setRole(role);
        account.setDepartment("Computer Science and Engineering");
        account.setPasswordHash(passwordEncoder.encode(PASSWORD));
        return accounts.saveAndFlush(account);
    }

    static String uniqueEmail() {
        return "user" + COUNTER.incrementAndGet() + "@test.edu";
    }

    static String body(String email) {
        return """
                {"name":"Test User","email":"%s","password":"%s","department":"CSE"}""".formatted(email, PASSWORD);
    }

    long idFrom(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
}
