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
 * user_schema.users. 6단계에서는 이벤트 소비 시 nickname 조회용 최소 매핑이었고,
 * 규약3(인증)에서 회원가입/로그인에 필요한 필드를 채우는 빌더가 추가됐다.
 */
@Entity
@Table(name = "users", schema = "user_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "login_id")
    private String loginId;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "email")
    private String email;

    @Column(name = "nickname", nullable = false)
    private String nickname;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder
    public User(String provider, String providerId, String loginId, String passwordHash, String email, String nickname,
                OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.provider = provider;
        this.providerId = providerId;
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.email = email;
        this.nickname = nickname;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
