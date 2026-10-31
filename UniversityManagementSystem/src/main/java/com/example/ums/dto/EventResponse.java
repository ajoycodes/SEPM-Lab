package com.example.ums.dto;

import java.time.Instant;

public record EventResponse(Long id, String title, String description, String venue, Instant startsAt, Instant endsAt,
                            Instant createdAt, Long authorId, String authorName, String department) {
}
