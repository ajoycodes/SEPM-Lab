package com.example.ums.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record EventRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 4000) String description,
        @NotBlank @Size(max = 200) String venue,
        @NotNull Instant startsAt,
        Instant endsAt) {
}
