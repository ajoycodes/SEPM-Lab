package com.example.ums.dto;

import com.example.ums.entity.Admin;
import com.example.ums.entity.Role;

/** What the API returns for an admin. The password hash is never sent back. */
public record AdminResponse(Long id, String name, String email, String phone, String username, Role role) {

    public static AdminResponse from(Admin admin) {
        return new AdminResponse(admin.getId(), admin.getName(), admin.getEmail(), admin.getPhone(),
                admin.getUsername(), admin.getRole());
    }
}
