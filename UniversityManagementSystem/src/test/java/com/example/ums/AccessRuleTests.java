package com.example.ums;

import com.example.ums.entity.Account;
import com.example.ums.entity.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** The brief's rule: admins manage all three resources, teachers only teachers, students only students. */
class AccessRuleTests extends ApiTestSupport {

    private MockHttpSession sessionFor(String role) throws Exception {
        return role.equals("ADMIN") ? loginAsAdmin() : loginAs(account(Role.valueOf(role)));
    }

    @ParameterizedTest(name = "{0} has full CRUD on /api/{1}")
    @CsvSource({"ADMIN,admins", "ADMIN,teachers", "ADMIN,students", "TEACHER,teachers", "STUDENT,students"})
    void allowedRolesHaveFullCrud(String role, String resource) throws Exception {
        MockHttpSession session = sessionFor(role);
        String root = "/api/" + resource;
        String email = uniqueEmail();

        MvcResult created = mvc.perform(post(root).session(session).with(csrf())
                        .contentType("application/json").content(body(email)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();
        long id = idFrom(created);

        mvc.perform(get(root).session(session)).andExpect(status().isOk());
        mvc.perform(get(root + "/" + id).session(session)).andExpect(status().isOk());

        // No password in the body keeps the current one.
        mvc.perform(put(root + "/" + id).session(session).with(csrf()).contentType("application/json")
                        .content("{\"name\":\"Updated\",\"email\":\"" + email + "\",\"department\":\"CSE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
        login(email, PASSWORD);

        mvc.perform(delete(root + "/" + id).session(session).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get(root + "/" + id).session(session)).andExpect(status().isNotFound());
    }

    @ParameterizedTest(name = "{0} gets 403 on /api/{1}")
    @CsvSource({"TEACHER,admins", "TEACHER,students", "STUDENT,admins", "STUDENT,teachers"})
    void otherResourcesAreForbidden(String role, String resource) throws Exception {
        MockHttpSession session = sessionFor(role);
        String root = "/api/" + resource;

        mvc.perform(get(root).session(session)).andExpect(status().isForbidden());
        mvc.perform(get(root + "/1").session(session)).andExpect(status().isForbidden());
        mvc.perform(post(root).session(session).with(csrf()).contentType("application/json")
                .content(body(uniqueEmail()))).andExpect(status().isForbidden());
        mvc.perform(put(root + "/1").session(session).with(csrf()).contentType("application/json")
                .content(body(uniqueEmail()))).andExpect(status().isForbidden());
        mvc.perform(delete(root + "/1").session(session).with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    void aTeacherCanManageAnotherTeacher() throws Exception {
        MockHttpSession teacher = loginAs(account(Role.TEACHER));
        Account other = account(Role.TEACHER);
        mvc.perform(put("/api/teachers/" + other.getId()).session(teacher).with(csrf()).contentType("application/json")
                        .content("{\"name\":\"Changed\",\"email\":\"" + other.getEmail() + "\",\"department\":\"CSE\"}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/teachers/" + other.getId()).session(teacher).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void anIdFromAnotherRoleIsNotFound() throws Exception {
        Account student = account(Role.STUDENT);
        mvc.perform(get("/api/teachers/" + student.getId()).session(loginAsAdmin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void roleComesFromTheEndpointNotTheRequest() throws Exception {
        String request = """
                {"name":"Sneaky","email":"%s","password":"%s","department":"CSE","role":"ADMIN"}"""
                .formatted(uniqueEmail(), PASSWORD);
        mvc.perform(post("/api/students").session(loginAs(account(Role.STUDENT))).with(csrf())
                        .contentType("application/json").content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }
}
