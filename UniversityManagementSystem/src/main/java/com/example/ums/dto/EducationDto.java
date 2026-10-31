package com.example.ums.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EducationDto(
        @NotBlank @Size(max = 160) String qualification,
        @NotBlank @Size(max = 200) String institution,
        @Min(1900) @Max(2100) Integer startYear,
        @Min(1900) @Max(2100) Integer endYear) {
}
