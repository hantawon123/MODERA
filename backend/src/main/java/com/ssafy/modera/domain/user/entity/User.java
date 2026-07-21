package com.ssafy.modera.domain.user.entity;

import com.ssafy.modera.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uq_users_provider", columnNames = {"provider", "provider_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Provider provider;

    @Column(name = "provider_id", length = 255)
    private String providerId;

    @Column(name = "login_id", unique = true, length = 20)
    private String loginId;

    @Column(name = "password_hash", length = 72)
    private String passwordHash;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 30)
    private String nickname;

    private User(Provider provider, String providerId, String loginId, String passwordHash, String email, String nickname) {
        this.provider = provider;
        this.providerId = providerId;
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.email = email;
        this.nickname = nickname;
    }

    /**
     * 자체 회원가입(3-1) 전용 생성자. passwordHash 는 반드시 인코딩된 값이어야 한다.
     */
    public static User ofLocal(String loginId, String passwordHash, String email, String nickname) {
        return new User(Provider.LOCAL, null, loginId, passwordHash, email, nickname);
    }

    public boolean isLocal() {
        return this.provider == Provider.LOCAL;
    }
}
