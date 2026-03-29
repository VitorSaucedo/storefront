package com.auth.service.dto;

import lombok.Builder;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

@Builder
public record UserResponse(
    @JsonSerialize(using = ToStringSerializer.class)
    Long id,
    String name,
    String email,
    String role
){}
