package com.ssafy.modera.api.domain.user.service;

import com.ssafy.modera.api.domain.user.dto.request.KakaoLoginRequest;
import com.ssafy.modera.api.domain.user.dto.request.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class AuthTransactionBoundaryTest {

    @Test
    void passwordAndKakaoNetworkCallsStayOutsideDatabaseTransactions() throws Exception {
        Method localLogin = AuthService.class.getMethod("login", LoginRequest.class);
        Method kakaoLogin = AuthService.class.getMethod("kakaoLogin", KakaoLoginRequest.class);

        assertThat(localLogin.getAnnotation(Transactional.class)).isNull();
        assertThat(kakaoLogin.getAnnotation(Transactional.class)).isNull();
    }

    @Test
    void databaseOperationsKeepTheirOwnShortTransactions() throws Exception {
        Transactional credentialTransaction = LoginCredentialReader.class
                .getMethod("findByLoginId", String.class)
                .getAnnotation(Transactional.class);
        Transactional kakaoUserTransaction = KakaoUserTransactionService.class
                .getMethod("login", String.class, String.class, String.class)
                .getAnnotation(Transactional.class);
        Transactional refreshTokenTransaction = RefreshTokenCommandService.class
                .getMethod("upsert", Integer.class, String.class, String.class,
                        java.time.OffsetDateTime.class)
                .getAnnotation(Transactional.class);

        assertThat(credentialTransaction).isNotNull();
        assertThat(credentialTransaction.readOnly()).isTrue();
        assertThat(kakaoUserTransaction).isNotNull();
        assertThat(refreshTokenTransaction).isNotNull();
    }
}
