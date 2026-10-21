package com.example.ums.dto;

import com.example.ums.entity.Role;
import com.example.ums.entity.Teacher;

/** What the API returns for a teacher. The password hash is never sent back. */
public record TeacherResponse(Long id, String name, String email, String department, String designation,
                              String username, Role role) {

    public static TeacherResponse from(Teacher teacher) {
        return new TeacherResponse(teacher.getId(), teacher.getName(), teacher.getEmail(),
                teacher.getDepartment(), teacher.getDesignation(), teacher.getUsername(), teacher.getRole());
    }
}
