package com.catalog.service.config;

import lombok.Builder;

@Builder
public record AuthenticatedUser(Long userId, String email) {}
