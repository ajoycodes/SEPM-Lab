package com.example.ums.config;

import com.example.ums.dto.ApiError;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Map;

/**
 * Swagger UI at /swagger-ui.html. springdoc builds the page from the controllers; this class adds
 * what it cannot see on its own: the login and logout endpoints (Spring Security filters, not
 * controllers) and the real status codes of each endpoint.
 */
@Configuration
public class OpenApiConfig implements WebMvcConfigurer {

    /** Opening http://localhost:8080 goes straight to Swagger UI. */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/swagger-ui.html");
    }

    @Bean
    public OpenAPI universityOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("University Management System")
                        .version("1.0")
                        .description("""
                                1. Open **Auth → POST /api/auth/login**, enter **admin** / **admin123** and press **Execute**.
                                2. The browser keeps the session cookie, so every other request now runs as that user.
                                3. Log in as **teacher** / **teacher123** or **student** / **student123** to see the \
                                role rules (403 Forbidden).
                                4. **POST /api/auth/logout** ends the session; after that every request gets 401.
                                """))
                .tags(List.of(
                        new Tag().name("Auth").description("Log in, log out, who am I"),
                        new Tag().name("Admins").description("Admin only"),
                        new Tag().name("Teachers").description("Admin and teacher"),
                        new Tag().name("Students").description("Admin and student")));
    }

    @Bean
    public OpenApiCustomizer statusCodesAndAuthEndpoints() {
        return openApi -> {
            // Login first, logout last, the controllers' endpoints in between sorted by path,
            // so each group reads: list, create, get one, update, delete.
            Paths paths = new Paths();
            paths.addPathItem("/api/auth/login", new PathItem().post(loginOperation()));
            openApi.getPaths().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> paths.addPathItem(entry.getKey(), entry.getValue()));
            paths.addPathItem("/api/auth/logout", new PathItem().post(logoutOperation()));
            openApi.setPaths(paths);

            // The error body every 4xx reply uses.
            ModelConverters.getInstance().read(ApiError.class).forEach(openApi.getComponents()::addSchemas);

            openApi.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
                if (path.startsWith("/api/auth/")) {
                    if (path.equals("/api/auth/me")) {
                        operation.getResponses().addApiResponse("401", error("Not logged in"));
                    }
                    return;
                }
                describeCrudResponses(path, method, operation.getResponses());
            }));
        };
    }

    /** The success code each CRUD method really returns, plus the errors it can give. */
    private void describeCrudResponses(String path, PathItem.HttpMethod method, ApiResponses responses) {
        boolean byId = path.endsWith("/{id}");
        switch (method) {
            case POST -> {
                success(responses, "201", "Created");
                responses.addApiResponse("400", error("Invalid fields, listed in fieldErrors"));
                responses.addApiResponse("409", error("Username, e-mail or roll already used"));
            }
            case PUT -> {
                success(responses, "200", "Updated");
                responses.addApiResponse("400", error("Invalid fields, listed in fieldErrors"));
                responses.addApiResponse("409", error("Username, e-mail or roll already used"));
            }
            case DELETE -> {
                success(responses, "204", "Deleted");
                if (path.startsWith("/api/admins")) {
                    responses.addApiResponse("409", error("Cannot delete the last admin"));
                }
            }
            default -> success(responses, "200", "OK");
        }
        if (byId) {
            responses.addApiResponse("404", error("No record with this id"));
        }
        responses.addApiResponse("401", error("Not logged in"));
        responses.addApiResponse("403", error("Your role may not use this resource"));
    }

    /** springdoc lists every success as 200; put it under the code the endpoint really returns. */
    private static void success(ApiResponses responses, String code, String description) {
        ApiResponse generated = responses.remove("200");
        responses.addApiResponse(code, (generated == null ? new ApiResponse() : generated).description(description));
    }

    private Operation loginOperation() {
        Schema<?> form = new ObjectSchema()
                .addProperty("username", new StringSchema().example("admin"))
                .addProperty("password", new StringSchema().example("admin123"))
                .required(List.of("username", "password"));
        return new Operation()
                .addTagsItem("Auth")
                .summary("Log in")
                .description("Form fields, not JSON. On success the browser keeps the JSESSIONID cookie.")
                .requestBody(new RequestBody().required(true).content(new Content()
                        .addMediaType("application/x-www-form-urlencoded", new MediaType().schema(form))))
                .responses(new ApiResponses()
                        .addApiResponse("200", json("Logged in; the user", ref("UserSummary")))
                        .addApiResponse("401", error("Wrong username or password")));
    }

    private Operation logoutOperation() {
        Schema<?> message = new ObjectSchema().addProperty("message", new StringSchema().example("Logged out"));
        return new Operation()
                .addTagsItem("Auth")
                .summary("Log out")
                .description("Ends the session; the cookie stops working.")
                .responses(new ApiResponses().addApiResponse("200", json("Logged out", message)));
    }

    private static ApiResponse error(String description) {
        return json(description, ref("ApiError"));
    }

    private static ApiResponse json(String description, Schema<?> schema) {
        return new ApiResponse().description(description)
                .content(new Content().addMediaType("application/json", new MediaType().schema(schema)));
    }

    private static Schema<?> ref(String schemaName) {
        return new Schema<>().$ref("#/components/schemas/" + schemaName);
    }
}
