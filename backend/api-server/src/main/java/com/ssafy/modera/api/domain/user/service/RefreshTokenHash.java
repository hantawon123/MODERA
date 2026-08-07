package com.ssafy.modera.api.domain.user.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Refresh Token 원문을 DB에 남기지 않기 위한 공통 SHA-256 해시 변환기. */
final class RefreshTokenHash {

    private static final String ALGORITHM = "SHA-256";

    private RefreshTokenHash() {
    }

    static String sha256(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
