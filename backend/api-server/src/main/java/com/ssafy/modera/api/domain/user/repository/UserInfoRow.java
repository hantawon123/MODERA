package com.ssafy.modera.api.domain.user.repository;

public record UserInfoRow(
        Integer userId,
        String loginId,
        String email,
        boolean notification,
        boolean backgroundAnalysis
) {
}
