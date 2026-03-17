package com.notecurve.auth.dto;

public record LoginResponse(
    String message,
    String token,
    String loginId,
    String name,
    Long id,
    String profileImage,
    String role
) {}
