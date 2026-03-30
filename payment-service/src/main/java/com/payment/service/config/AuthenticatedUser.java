package com.payment.service.config;

import lombok.Builder;

@Builder
public record AuthenticatedUser(Long userId, String email) {}
