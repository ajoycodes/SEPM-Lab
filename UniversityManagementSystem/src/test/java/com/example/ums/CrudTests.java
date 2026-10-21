package com.example.ums;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import java.util.Map;
import java.util.function.IntFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Create, read, update and delete for all three resources, with the HTTP status codes. */
class CrudTests extends ApiTestSupport {

    private static Map<String, Object> bodyFor(String resource, int n) {
        IntFunction<Map<String, Object>> maker = switch (resource) {
            case "admins" -> ApiTestSupport::adminBody;
            case "teachers" -> ApiTestSupport::teacherBody;
            default -> ApiTestSupport::studentBody;
        };
        return maker.apply(n);
    }

    @ParameterizedTest(name = "full lifecycle of /api/{0}")
    @ValueSource(strings = {"admins", "teachers", "students"})
    void createReadUpdateDelete(String resource) throws Exception {
        MockHttpSession admin = loginAsAdmin();
        String base = "/api/" + resource;
        int n = unique();

        // Create -> 201 Created + Location header, password not echoed
        var created = mockMvc.perform(post(base).session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json(bodyFor(resource, n))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode createdJson = read(created);
        long id = createdJson.get("id").asLong();
        String location = created.getResponse().getHeader("Location");
        assertThat(location).endsWith(base + "/" + id);
        assertThat(createdJson.has("password")).isFalse();

        // Read one -> 200
        mockMvc.perform(get(base + "/" + id).session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(bodyFor(resource, n).get("name")));

        // Read all -> 200 and contains it
        mockMvc.perform(get(base).session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").exists());

        // Update -> 200, no password in the body keeps the old password
        Map<String, Object> changed = bodyFor(resource, n);
        changed.put("name", "Renamed " + n);
        changed.remove("password");
        mockMvc.perform(put(base + "/" + id).session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json(changed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed " + n));
        login((String) changed.get("username"), "secret" + n);

        // Delete -> 204, then it is gone -> 404
        mockMvc.perform(delete(base + "/" + id).session(admin))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(base + "/" + id).session(admin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(endsWith("with id " + id + " was not found")));
    }

    @Test
    void updateWithNewPasswordChangesTheLogin() throws Exception {
        MockHttpSession admin = loginAsAdmin();
        int n = unique();
        long id = read(mockMvc.perform(post("/api/students").session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json(studentBody(n))))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();

        Map<String, Object> body = studentBody(n);
        body.put("password", "brand-new-pass");
        mockMvc.perform(put("/api/students/" + id).session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isOk());

        login("student_" + n, "brand-new-pass");
        mockMvc.perform(post("/api/auth/login").param("username", "student_" + n).param("password", "secret" + n))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidBodyIs400WithFieldErrors() throws Exception {
        Map<String, Object> body = teacherBody(unique());
        body.put("email", "not-an-email");
        body.put("name", "");
        body.remove("password");
        mockMvc.perform(post("/api/teachers").session(loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void duplicateUsernameIs409() throws Exception {
        MockHttpSession admin = loginAsAdmin();
        Map<String, Object> body = studentBody(unique());
        body.put("username", "teacher"); // already used by the seeded teacher, in another role
        mockMvc.perform(post("/api/students").session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username 'teacher' is already taken"));
    }

    @Test
    void duplicateRollIs409() throws Exception {
        Map<String, Object> body = studentBody(unique());
        body.put("roll", "2107001"); // the seeded student's roll
        mockMvc.perform(post("/api/students").session(loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isConflict());
    }

    @Test
    void unknownIdIs404AndNonNumericIdIs400() throws Exception {
        MockHttpSession admin = loginAsAdmin();
        mockMvc.perform(get("/api/teachers/999999").session(admin))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/teachers/999999").session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json(teacherBody(unique()))))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/teachers/999999").session(admin))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/teachers/abc").session(admin))
                .andExpect(status().isBadRequest());
    }

    @Test
    void brokenJsonIs400AndWrongContentTypeIs415() throws Exception {
        MockHttpSession admin = loginAsAdmin();
        mockMvc.perform(post("/api/students").session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{ not json"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/students").session(admin)
                        .contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void wrongMethodIs405() throws Exception {
        mockMvc.perform(delete("/api/students").session(loginAsAdmin()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void lastAdminCannotBeDeleted() throws Exception {
        MockHttpSession admin = loginAsAdmin();
        // Remove any admins other tests left behind, so only the starting admin remains.
        JsonNode admins = read(mockMvc.perform(get("/api/admins").session(admin)).andReturn());
        long seededId = 0;
        for (JsonNode a : admins) {
            if (a.get("username").asText().equals("admin")) {
                seededId = a.get("id").asLong();
            } else {
                mockMvc.perform(delete("/api/admins/" + a.get("id").asLong()).session(admin))
                        .andExpect(status().isNoContent());
            }
        }
        mockMvc.perform(delete("/api/admins/" + seededId).session(admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot delete the last admin"));
    }
}
