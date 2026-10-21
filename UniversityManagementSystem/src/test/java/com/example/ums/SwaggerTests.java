package com.example.ums;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Swagger UI is open to everyone; the data behind it is not. */
class SwaggerTests extends ApiTestSupport {

    @Test
    void homePageRedirectsToSwaggerUi() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/swagger-ui.html"));
    }

    @Test
    void swaggerUiLoadsWithoutLogin() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void apiDescriptionListsLoginAsFormAndTheRealStatusCodes() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.requestBody.content"
                        + "['application/x-www-form-urlencoded']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/logout'].post").exists())
                .andExpect(jsonPath("$.paths['/api/teachers'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/teachers/{id}'].delete.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/api/students'].get.responses['403']").exists())
                .andExpect(jsonPath("$.components.schemas.ApiError").exists());
    }

    @Test
    void dataStaysProtected() throws Exception {
        mockMvc.perform(get("/api/teachers"))
                .andExpect(status().isUnauthorized());
    }
}
