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
 * user_schema.users 회원 엔티티.
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

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder
    public User(String provider, String providerId, String loginId, String passwordHash, String email,
                OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.provider = provider;
        this.providerId = providerId;
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.email = email;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

}
