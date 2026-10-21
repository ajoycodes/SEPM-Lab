package com.example.ums.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** JSON body for POST and PUT /api/teachers. */
public record TeacherRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(max = 100) String department,
        @NotBlank @Size(max = 50) String designation,
        @NotBlank @Size(min = 3, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9._-]*$", message = "may contain only letters, digits, '.', '_' and '-'")
        String username,
        @NotBlank(groups = OnCreate.class) @Size(min = 6, max = 72) String password) {
}
