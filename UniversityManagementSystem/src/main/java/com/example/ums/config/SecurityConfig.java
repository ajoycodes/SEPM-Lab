package com.example.ums.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.util.Map;

@Configuration
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** BCrypt: a salted, slow hash. Used both when saving a password and when checking a login. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF protection stays on (Spring's default), so POST, PUT and DELETE need the token
                // from GET /api/auth/csrf. The website's JavaScript sends it automatically.

                // Role-based access control. The first matching rule wins.
                .authorizeHttpRequests(auth -> auth
                        // Web pages, and data that is public on purpose
                        .requestMatchers("/", "/index.html", "/teachers.html", "/profile.html",
                                "/favicon.svg", "/css/**", "/js/**", "/error").permitAll()
                        .requestMatchers("/api/auth/csrf", "/api/auth/login", "/api/academics").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/teachers", "/api/public/teachers/*",
                                "/api/events", "/api/events/*").permitAll()
                        // Only teachers publish events
                        .requestMatchers("/api/events", "/api/events/*").hasRole("TEACHER")
                        // Admins manage all three resources, teachers only teachers, students only students
                        .requestMatchers("/api/admins", "/api/admins/**").hasRole("ADMIN")
                        .requestMatchers("/api/teachers", "/api/teachers/**").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers("/api/students", "/api/students/**").hasAnyRole("ADMIN", "STUDENT")
                        .anyRequest().authenticated())

                // Login: POST /api/auth/login with form fields username (the email) and password.
                // On success Spring stores the user in the HTTP session (JSESSIONID cookie).
                .formLogin(form -> form
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler((request, response, authentication) ->
                                writeJson(response, HttpStatus.OK, "Logged in"))
                        .failureHandler((request, response, exception) ->
                                writeJson(response, HttpStatus.UNAUTHORIZED, "Invalid email or password")))

                // Logout: POST /api/auth/logout ends the session.
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpStatus.NO_CONTENT.value())))

                // JSON instead of a redirect to a login page.
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                writeJson(response, HttpStatus.UNAUTHORIZED, "Login required"))
                        .accessDeniedHandler((request, response, exception) ->
                                writeJson(response, HttpStatus.FORBIDDEN, "Access denied or invalid CSRF token")));

        return http.build();
    }

    private void writeJson(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of("message", message));
    }
}
