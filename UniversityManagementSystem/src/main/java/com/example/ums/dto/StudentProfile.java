package com.example.ums.dto;

import java.math.BigDecimal;
import java.util.List;

/** A student's profile. Private: only that student can read it. */
public record StudentProfile(Long id, String name, String email, String phone, String roll, String department,
                             String program, BigDecimal cgpa, List<SemesterResultDto> semesterResults) {
}
