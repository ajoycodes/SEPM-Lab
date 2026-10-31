package com.example.ums.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SemesterResultDto(
        @NotBlank @Size(max = 100) String semester,
        @NotNull @DecimalMin("0.00") @DecimalMax("4.00") @Digits(integer = 1, fraction = 2) BigDecimal gpa) {
}
