package com.ssafy.modera.domain.user;

import com.ssafy.modera.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/*
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '유저 PK',
    email VARCHAR(50) NOT NULL COMMENT '이메일',
    password VARCHAR(255) NOT NULL COMMENT '비밀번호(BCrypt 해시, 60자)',
    role VARCHAR(20) NOT NULL COMMENT '권한(ROLE_USER)',
    created_at DATETIME NOT NULL,
    updated_at DATETIME
 */
@Entity
@Table(name = "users")
@Getter
public class User extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 20)
    private String role;
}
