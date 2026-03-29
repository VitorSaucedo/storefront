package com.auth.service.dto;

import lombok.Builder;

@Builder
public record AuthResponse(String token, String email, String name, String role){ }

