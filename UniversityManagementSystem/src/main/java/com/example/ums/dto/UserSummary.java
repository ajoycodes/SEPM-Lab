package com.example.ums.dto;

import com.example.ums.entity.AppUser;
import com.example.ums.entity.Role;

/** The logged-in user, returned by login and by GET /api/auth/me. */
public record UserSummary(Long id, String name, String username, Role role) {

    public static UserSummary from(AppUser user) {
        return new UserSummary(user.getId(), user.getName(), user.getUsername(), user.getRole());
    }
}
