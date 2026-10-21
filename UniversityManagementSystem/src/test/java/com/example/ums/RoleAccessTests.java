package com.example.ums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Role-based access control: admins manage all three resources,
 * teachers manage only teachers, students manage only students.
 */
class RoleAccessTests extends ApiTestSupport {

    @ParameterizedTest(name = "{0} GET {1} -> {2}")
    @CsvSource({
            "admin,   /api/admins,   200",
            "admin,   /api/teachers, 200",
            "admin,   /api/students, 200",
            "teacher, /api/admins,   403",
            "teacher, /api/teachers, 200",
            "teacher, /api/students, 403",
            "student, /api/admins,   403",
            "student, /api/teachers, 403",
            "student, /api/students, 200",
    })
    void readAccess(String username, String path, int expectedStatus) throws Exception {
        MockHttpSession session = login(username, username + "123");
        mockMvc.perform(get(path).session(session))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void teacherCanCreateUpdateAndDeleteTeachers() throws Exception {
        MockHttpSession teacher = loginAsTeacher();
        int n = unique();
        String location = mockMvc.perform(post("/api/teachers").session(teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(json(teacherBody(n))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(put(location).session(teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(json(teacherBody(n))))
                .andExpect(status().isOk());
        mockMvc.perform(delete(location).session(teacher))
                .andExpect(status().isNoContent());
    }

    @Test
    void studentCanCreateUpdateAndDeleteStudents() throws Exception {
        MockHttpSession student = loginAsStudent();
        int n = unique();
        String location = mockMvc.perform(post("/api/students").session(student)
                        .contentType(MediaType.APPLICATION_JSON).content(json(studentBody(n))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(put(location).session(student)
                        .contentType(MediaType.APPLICATION_JSON).content(json(studentBody(n))))
                .andExpect(status().isOk());
        mockMvc.perform(delete(location).session(student))
                .andExpect(status().isNoContent());
    }

    @Test
    void teacherCannotChangeStudentsOrAdmins() throws Exception {
        MockHttpSession teacher = loginAsTeacher();
        mockMvc.perform(post("/api/students").session(teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(json(studentBody(unique()))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admins").session(teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(json(adminBody(unique()))))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/students/1").session(teacher))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotChangeTeachersOrAdmins() throws Exception {
        MockHttpSession student = loginAsStudent();
        mockMvc.perform(post("/api/teachers").session(student)
                        .contentType(MediaType.APPLICATION_JSON).content(json(teacherBody(unique()))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admins").session(student)
                        .contentType(MediaType.APPLICATION_JSON).content(json(adminBody(unique()))))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/teachers/1").session(student))
                .andExpect(status().isForbidden());
    }

    @Test
    void roleComesFromTheResourceNotTheRequest() throws Exception {
        // A student who sends "role": "ADMIN" still creates a student.
        var body = studentBody(unique());
        body.put("role", "ADMIN");
        mockMvc.perform(post("/api/students").session(loginAsStudent())
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }
}
