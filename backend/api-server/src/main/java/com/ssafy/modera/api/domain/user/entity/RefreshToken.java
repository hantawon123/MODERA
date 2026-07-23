package com.ssafy.modera.api.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * user_schema.refresh_token. 기기(deviceId)당 하나만 유효하다(UNIQUE(user_id, device_id)).
 * 원문은 절대 저장하지 않고 SHA-256 해시(64자 hex)만 보관한다.
 */
@Entity
@Table(name = "refresh_token", schema = "user_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long tokenId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Builder
    public RefreshToken(Long userId, String deviceId, String tokenHash, OffsetDateTime expiresAt, OffsetDateTime createdAt) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    /** 토큰 재발급(RTR) 시 기존 레코드를 새 토큰 해시로 교체한다. 1기기 1토큰 유지. */
    public void rotate(String tokenHash, OffsetDateTime expiresAt) {
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(OffsetDateTime now) {
        return this.expiresAt.isBefore(now);
    }

    public boolean matches(String tokenHash) {
        return this.tokenHash.equals(tokenHash);
    }
}
