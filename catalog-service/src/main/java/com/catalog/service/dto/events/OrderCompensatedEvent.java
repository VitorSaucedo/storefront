package com.catalog.service.dto.events;

import java.time.LocalDateTime;

public record OrderCompensatedEvent(
        Long orderId,
        LocalDateTime occurredAt
) {}
