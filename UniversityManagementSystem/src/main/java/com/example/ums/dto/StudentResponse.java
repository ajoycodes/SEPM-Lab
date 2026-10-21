package com.example.ums.dto;

import com.example.ums.entity.Role;
import com.example.ums.entity.Student;

/** What the API returns for a student. The password hash is never sent back. */
public record StudentResponse(Long id, String name, String email, String roll, String department, Integer batch,
                              String username, Role role) {

    public static StudentResponse from(Student student) {
        return new StudentResponse(student.getId(), student.getName(), student.getEmail(), student.getRoll(),
                student.getDepartment(), student.getBatch(), student.getUsername(), student.getRole());
    }
}
