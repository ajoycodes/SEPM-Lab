package com.example.ums.config;

import com.example.ums.dto.ApiError;
import com.example.ums.dto.UserSummary;
import com.example.ums.repository.AppUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import java.io.IOException;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    private final AppUserRepository appUserRepository;

    public SecurityConfig(ObjectMapper objectMapper, AppUserRepository appUserRepository) {
        this.objectMapper = objectMapper;
        this.appUserRepository = appUserRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // A JSON API called from Postman or curl, not from HTML forms. Cross-site requests are
                // blocked instead by the SameSite=Strict session cookie (see application.yml).
                .csrf(AbstractHttpConfigurer::disable)

                // Role-based access control. The first matching rule wins.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/error").permitAll()
                        .requestMatchers("/api/admins/**").hasRole("ADMIN")
                        .requestMatchers("/api/teachers/**").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers("/api/students/**").hasAnyRole("ADMIN", "STUDENT")
                        .anyRequest().authenticated())

                // Login: POST /api/auth/login with form fields username and password.
                // On success Spring stores the user in the HTTP session (JSESSIONID cookie).
                .formLogin(form -> form
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler((request, response, authentication) -> writeJson(response, HttpStatus.OK,
                                UserSummary.from(appUserRepository.findByUsername(authentication.getName())
                                        .orElseThrow())))
                        .failureHandler((request, response, exception) -> writeJson(response,
                                HttpStatus.UNAUTHORIZED, ApiError.of(HttpStatus.UNAUTHORIZED,
                                        loginFailureMessage(request), request.getRequestURI()))))

                // Logout: POST /api/auth/logout ends the session.
                .logout(logout -> logout
                        .logoutRequestMatcher(PathPatternRequestMatcher.withDefaults()
                                .matcher(HttpMethod.POST, "/api/auth/logout"))
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) ->
                                writeJson(response, HttpStatus.OK, Map.of("message", "Logged out"))))

                // An API never redirects back after login, so don't keep the request that got a 401.
                .requestCache(RequestCacheConfigurer::disable)

                // JSON instead of a redirect to a login page.
                .exceptionHandling(exceptions -> exceptions
                        // 401: not logged in
                        .authenticationEntryPoint((request, response, exception) -> writeJson(response,
                                HttpStatus.UNAUTHORIZED, ApiError.of(HttpStatus.UNAUTHORIZED,
                                        "Log in first: POST /api/auth/login", request.getRequestURI())))
                        // 403: logged in, but this role may not use this resource
                        .accessDeniedHandler((request, response, exception) -> writeJson(response,
                                HttpStatus.FORBIDDEN, ApiError.of(HttpStatus.FORBIDDEN,
                                        "Your role is not allowed to access this resource",
                                        request.getRequestURI()))));

        return http.build();
    }

    /** BCrypt: a salted, slow hash. Used both when saving a password and when checking a login. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private String loginFailureMessage(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType != null && contentType.startsWith(MediaType.APPLICATION_JSON_VALUE)) {
            return "Send username and password as form fields (x-www-form-urlencoded), not JSON";
        }
        return "Invalid username or password";
    }

    private void writeJson(HttpServletResponse response, HttpStatus status, Object body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE); // JSON is UTF-8, like the controllers' replies
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
