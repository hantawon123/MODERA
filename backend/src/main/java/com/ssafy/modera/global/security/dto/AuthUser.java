package com.ssafy.modera.global.security.dto;

public record AuthUser(
        Long userId,
        String email,
        String role
) {

    public static AuthUser of(Long userId, String email, String role) {
        return new AuthUser(userId, email, role);
    }
}
