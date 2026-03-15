package com.auth.service.dto.events;

import java.time.LocalDateTime;

public class UserRegisteredEvent {

    private Long userId;
    private String email;
    private String name;
    private LocalDateTime occurredAt;

    public UserRegisteredEvent(Long userId, String email, String name) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.occurredAt = LocalDateTime.now();
    }

    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
