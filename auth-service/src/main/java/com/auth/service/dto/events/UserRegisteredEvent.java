package com.auth.service.dto.events;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UserRegisteredEvent(
    Long userId,
    String email,
    String name,
    LocalDateTime occurredAt
){}
