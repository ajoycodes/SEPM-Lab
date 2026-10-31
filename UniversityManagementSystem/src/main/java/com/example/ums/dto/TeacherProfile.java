package com.example.ums.dto;

import java.util.List;

/** A teacher's profile. Public: anyone can read it without logging in. */
public record TeacherProfile(Long id, String name, String email, String phone, String department, String faculty,
                             String designation, boolean departmentHead, boolean facultyDean, String bio,
                             List<EducationDto> education, String researchField, String researchInterests) {
}
