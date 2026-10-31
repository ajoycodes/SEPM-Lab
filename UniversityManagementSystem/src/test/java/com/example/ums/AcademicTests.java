package com.example.ums;

import com.example.ums.entity.Account;
import com.example.ums.entity.Role;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Faculties, departments, and who may appoint a head or dean. */
class AcademicTests extends ApiTestSupport {

    private static String teacherBody(String department, boolean head, boolean dean) {
        return """
                {"name":"Academic Test","email":"%s","password":"%s","department":"%s","designation":"Professor",
                 "specialization":"Testing","departmentHead":%s,"facultyDean":%s}"""
                .formatted(uniqueEmail(), PASSWORD, department, head, dean);
    }

    @Test
    void catalogIsPublicWithFourFacultiesAndTwentyDepartments() throws Exception {
        String reply = mvc.perform(get("/api/academics")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode faculties = objectMapper.readTree(reply).get("faculties");
        assertThat(faculties.size()).isEqualTo(4);
        int departments = 0;
        for (JsonNode faculty : faculties) {
            departments += faculty.get("departments").size();
        }
        assertThat(departments).isEqualTo(20);
    }

    @Test
    void departmentCodeBecomesTheFullNameAndFacultyIsWorkedOut() throws Exception {
        mvc.perform(post("/api/students").session(loginAsAdmin()).with(csrf())
                        .contentType("application/json").content(body(uniqueEmail())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.department").value("Computer Science and Engineering"))
                .andExpect(jsonPath("$.faculty").value("Faculty of Electrical and Electronic Engineering"));
    }

    @Test
    void eachPostHasOneHolderAndStaysATeacher() throws Exception {
        MockHttpSession admin = loginAsAdmin();
        mvc.perform(post("/api/teachers").session(admin).with(csrf()).contentType("application/json")
                        .content(teacherBody("Physics", true, true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("TEACHER"));
        mvc.perform(post("/api/teachers").session(admin).with(csrf()).contentType("application/json")
                .content(teacherBody("Physics", true, false))).andExpect(status().isConflict());
        mvc.perform(post("/api/teachers").session(admin).with(csrf()).contentType("application/json")
                .content(teacherBody("Chemistry", false, true))).andExpect(status().isConflict());
        mvc.perform(post("/api/teachers").session(admin).with(csrf()).contentType("application/json")
                .content(teacherBody("Civil Engineering", false, true))).andExpect(status().isCreated());
    }

    @Test
    void onlyAdminsAppointLeaders() throws Exception {
        Account teacher = account(Role.TEACHER);
        MockHttpSession session = loginAs(teacher);
        mvc.perform(post("/api/teachers").session(session).with(csrf()).contentType("application/json")
                .content(teacherBody("Architecture", true, false))).andExpect(status().isForbidden());
        mvc.perform(post("/api/teachers").session(session).with(csrf()).contentType("application/json")
                .content(teacherBody("Architecture", false, false))).andExpect(status().isCreated());
    }

    @Test
    void aStudentCannotBeALeader() throws Exception {
        mvc.perform(post("/api/students").session(loginAsAdmin()).with(csrf()).contentType("application/json")
                .content(teacherBody("Mathematics", true, false))).andExpect(status().isBadRequest());
    }
}
