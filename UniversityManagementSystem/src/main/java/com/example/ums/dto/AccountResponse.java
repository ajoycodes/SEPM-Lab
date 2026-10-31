package com.example.ums.dto;

import com.example.ums.entity.Role;

/** What the API returns for an account. No password, and no private student details. */
public record AccountResponse(Long id, String name, String email, String department, Role role, String faculty,
                              String designation, String specialization, boolean departmentHead,
                              boolean facultyDean) {
}
