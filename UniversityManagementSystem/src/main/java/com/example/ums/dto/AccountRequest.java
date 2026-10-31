package com.example.ums.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * JSON body for POST and PUT on /api/admins, /api/teachers and /api/students.
 * There is no role field: the endpoint decides the role.
 */
public record AccountRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 200) String email,
        @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 100) String department,
        @Size(max = 100) String designation,
        @Size(max = 160) String specialization,
        Boolean departmentHead,
        Boolean facultyDean) {
}
