package com.example.ums.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** JSON body for PUT /api/profile/me. Only the fields that apply to the caller's role are used. */
public record ProfileRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 200) String email,
        @Size(max = 40) String phone,
        // Teachers
        @Size(max = 2000) String bio,
        @Size(max = 160) String researchField,
        @Size(max = 1500) String researchInterests,
        @Size(max = 20) List<@NotNull @Valid EducationDto> education,
        // Students
        @Size(max = 50) String roll,
        @Size(max = 100) String department,
        @Size(max = 150) String program,
        @DecimalMin("0.00") @DecimalMax("4.00") @Digits(integer = 1, fraction = 2) BigDecimal cgpa,
        @Size(max = 30) List<@NotNull @Valid SemesterResultDto> semesterResults) {
}
