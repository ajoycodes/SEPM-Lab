package com.example.ums;

import com.example.ums.entity.Account;
import com.example.ums.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Teacher profiles are public; student details are private to that student. */
class ProfileTests extends ApiTestSupport {

    private String ownBody(Account account, String extraFields) {
        return "{\"name\":\"Updated Owner\",\"email\":\"" + account.getEmail() + "\"" + extraFields + "}";
    }

    @Test
    void publicPagesAndTeacherProfilesNeedNoLogin() throws Exception {
        Account teacher = account(Role.TEACHER);
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/teachers.html")).andExpect(status().isOk());
        mvc.perform(get("/profile.html")).andExpect(status().isOk());
        mvc.perform(get("/api/public/teachers")).andExpect(status().isOk());
        mvc.perform(get("/api/public/teachers/" + teacher.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(teacher.getEmail()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void aStudentIsNotAPublicProfile() throws Exception {
        Account student = account(Role.STUDENT);
        mvc.perform(get("/api/public/teachers/" + student.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/api/profile/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void teacherEditsOwnProfile() throws Exception {
        Account teacher = account(Role.TEACHER);
        String extras = ",\"phone\":\"+8801000000000\",\"bio\":\"My biography\",\"researchField\":\"AI\","
                + "\"researchInterests\":\"Learning systems\",\"education\":[{\"qualification\":\"MSc\","
                + "\"institution\":\"Demo University\",\"startYear\":2015,\"endYear\":2017}]";
        mvc.perform(put("/api/profile/me").session(loginAs(teacher)).with(csrf()).contentType("application/json")
                        .content(ownBody(teacher, extras)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("My biography"))
                .andExpect(jsonPath("$.education[0].qualification").value("MSc"));
        mvc.perform(get("/api/public/teachers/" + teacher.getId()))
                .andExpect(jsonPath("$.phone").value("+8801000000000"));
    }

    @Test
    void studentAcademicDetailsStayPrivate() throws Exception {
        Account student = account(Role.STUDENT);
        Account other = account(Role.STUDENT);
        MockHttpSession session = loginAs(student);
        String extras = ",\"roll\":\"R-" + student.getId() + "\",\"department\":\"CSE\",\"program\":\"BSc Engineering\","
                + "\"cgpa\":3.75,\"semesterResults\":[{\"semester\":\"Year 1 Term 1\",\"gpa\":3.60}]";
        mvc.perform(put("/api/profile/me").session(session).with(csrf()).contentType("application/json")
                        .content(ownBody(student, extras)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cgpa").value(3.75))
                .andExpect(jsonPath("$.semesterResults[0].gpa").value(3.6));

        // Another student's own profile shows their data, and the shared student list has no grades.
        mvc.perform(get("/api/profile/me").session(loginAs(other))).andExpect(jsonPath("$.cgpa").isEmpty());
        mvc.perform(get("/api/students/" + student.getId()).session(loginAs(other)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cgpa").doesNotExist())
                .andExpect(jsonPath("$.roll").doesNotExist());
    }

    @Test
    void badGradesRollsAndYearsAreRejected() throws Exception {
        Account student = account(Role.STUDENT);
        Account other = account(Role.STUDENT);
        other.setRoll("USED-" + other.getId());
        accounts.saveAndFlush(other);
        MockHttpSession session = loginAs(student);

        mvc.perform(put("/api/profile/me").session(session).with(csrf()).contentType("application/json")
                .content(ownBody(student, ",\"cgpa\":4.01"))).andExpect(status().isBadRequest());
        mvc.perform(put("/api/profile/me").session(session).with(csrf()).contentType("application/json")
                .content(ownBody(student, ",\"semesterResults\":[{\"semester\":\"S1\",\"gpa\":-1}]")))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/profile/me").session(session).with(csrf()).contentType("application/json")
                .content(ownBody(student, ",\"semesterResults\":[{\"semester\":\"S1\",\"gpa\":3},{\"semester\":\"s1\",\"gpa\":3}]")))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/profile/me").session(session).with(csrf()).contentType("application/json")
                .content(ownBody(student, ",\"roll\":\"" + other.getRoll() + "\""))).andExpect(status().isConflict());

        mvc.perform(put("/api/profile/me").session(session).with(csrf()).contentType("application/json")
                .content(ownBody(student, ",\"department\":\"  \""))).andExpect(status().isBadRequest());

        Account teacher = account(Role.TEACHER);
        mvc.perform(put("/api/profile/me").session(loginAs(teacher)).with(csrf()).contentType("application/json")
                .content(ownBody(teacher, ",\"education\":[{\"qualification\":\"MSc\",\"institution\":\"U\","
                        + "\"startYear\":2020,\"endYear\":2019}]"))).andExpect(status().isBadRequest());
    }
}
