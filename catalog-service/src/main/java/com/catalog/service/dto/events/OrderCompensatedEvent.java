package com.catalog.service.dto.events;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record OrderCompensatedEvent(
        Long orderId,
        LocalDateTime occurredAt
) {}
