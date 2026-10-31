package com.example.ums;

import com.example.ums.entity.Account;
import com.example.ums.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Anyone can read events, only teachers publish, and only the author edits or deletes. */
class EventTests extends ApiTestSupport {

    private static final String EVENT = """
            {"title":"Test event","description":"A test","venue":"Room 1",
             "startsAt":"2027-01-01T10:00:00Z","endsAt":"2027-01-01T11:00:00Z"}""";

    private long publish(MockHttpSession teacher) throws Exception {
        return idFrom(mvc.perform(post("/api/events").session(teacher).with(csrf())
                        .contentType("application/json").content(EVENT))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn());
    }

    @Test
    void authorHasFullControlAndAnyoneCanRead() throws Exception {
        MockHttpSession author = loginAs(account(Role.TEACHER));
        long id = publish(author);

        mvc.perform(get("/api/events")).andExpect(status().isOk());
        mvc.perform(get("/api/events/" + id)).andExpect(status().isOk());
        mvc.perform(put("/api/events/" + id).session(author).with(csrf()).contentType("application/json")
                        .content(EVENT.replace("Test event", "Updated event")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated event"));
        mvc.perform(delete("/api/events/" + id).session(author).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/events/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void onlyTeachersPublish() throws Exception {
        for (MockHttpSession session : new MockHttpSession[]{loginAs(account(Role.STUDENT)), loginAsAdmin()}) {
            mvc.perform(post("/api/events").session(session).with(csrf())
                            .contentType("application/json").content(EVENT))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void anotherTeacherCannotChangeIt() throws Exception {
        long id = publish(loginAs(account(Role.TEACHER)));
        MockHttpSession other = loginAs(account(Role.TEACHER));
        mvc.perform(put("/api/events/" + id).session(other).with(csrf())
                .contentType("application/json").content(EVENT)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/events/" + id).session(other).with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    void anEventMustEndAfterItStarts() throws Exception {
        String backwards = EVENT.replace("2027-01-01T11:00:00Z", "2026-01-01T11:00:00Z");
        mvc.perform(post("/api/events").session(loginAs(account(Role.TEACHER))).with(csrf())
                .contentType("application/json").content(backwards)).andExpect(status().isBadRequest());
    }

    @Test
    void deletingATeacherDeletesTheirEvents() throws Exception {
        Account teacher = account(Role.TEACHER);
        long id = publish(loginAs(teacher));
        mvc.perform(delete("/api/teachers/" + teacher.getId()).session(loginAsAdmin()).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/events/" + id)).andExpect(status().isNotFound());
    }
}
