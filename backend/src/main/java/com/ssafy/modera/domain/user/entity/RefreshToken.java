package com.ssafy.modera.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * 기기(deviceId) 단위로 하나만 유효한 Refresh Token 레코드.
 * 원문은 절대 저장하지 않고 SHA-256 해시(64자 hex)만 보관한다.
 */
@Entity
@Table(name = "refresh_token", uniqueConstraints = {
        @UniqueConstraint(name = "uq_refresh_token_user_device", columnNames = {"user_id", "device_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long tokenId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private RefreshToken(User user, String deviceId, String tokenHash, Instant expiresAt) {
        this.user = user;
        this.deviceId = deviceId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public static RefreshToken issue(User user, String deviceId, String tokenHash, Instant expiresAt) {
        return new RefreshToken(user, deviceId, tokenHash, expiresAt);
    }

    /**
     * 재발급(3-3) 시 기존 레코드를 새 토큰 해시로 교체한다. (RTR: 1기기 1토큰 유지)
     */
    public void rotate(String tokenHash, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(Instant now) {
        return this.expiresAt.isBefore(now);
    }

    public boolean matches(String tokenHash) {
        return this.tokenHash.equals(tokenHash);
    }
}
