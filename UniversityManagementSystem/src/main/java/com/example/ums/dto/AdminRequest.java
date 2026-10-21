package com.example.ums.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** JSON body for POST and PUT /api/admins. */
public record AdminRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 100) String email,
        @Size(max = 20) String phone,
        @NotBlank @Size(min = 3, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9._-]*$", message = "may contain only letters, digits, '.', '_' and '-'")
        String username,
        @NotBlank(groups = OnCreate.class) @Size(min = 6, max = 72) String password) {
}
