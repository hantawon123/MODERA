package com.ssafy.modera.api.domain.user.service;

import com.ssafy.modera.api.domain.user.exception.UserErrorCode;
import com.ssafy.modera.api.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoClientTest {

    private static final String ACCESS_TOKEN = "test-kakao-access-token";

    private KakaoProperties properties;
    private MockRestServiceServer server;
    private KakaoClient kakaoClient;

    @BeforeEach
    void setUp() {
        properties = new KakaoProperties();
        properties.setApiBaseUrl("https://kakao.test");
        properties.setAllowedAppIds(new LinkedHashSet<>(Set.of(12345L, 67890L)));

        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getApiBaseUrl());
        server = MockRestServiceServer.bindTo(builder).build();
        kakaoClient = new KakaoClient(properties, builder.build());
    }

    @Test
    void verifiesAppAndReturnsKakaoUserForValidAccessToken() {
        expectTokenInfo(12345L);
        server.expect(requestTo("https://kakao.test/v2/user/me"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andRespond(withSuccess("""
                        {
                          "id": 998877,
                          "kakao_account": {
                            "email": "user@example.com",
                            "is_email_valid": true,
                            "is_email_verified": true
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        KakaoClient.KakaoUser user = kakaoClient.getVerifiedUser(ACCESS_TOKEN);

        assertThat(user.id()).isEqualTo(998877L);
        assertThat(user.email()).isEqualTo("user@example.com");
        assertThat(user.hasValidVerifiedEmail()).isTrue();
        server.verify();
    }

    @Test
    void rejectsInvalidAccessToken() {
        server.expect(requestTo("https://kakao.test/v1/user/access_token_info"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertError(UserErrorCode.KAKAO_LOGIN_FAILED);
        server.verify();
    }

    @Test
    void rejectsExpiredAccessToken() {
        server.expect(requestTo("https://kakao.test/v1/user/access_token_info"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .body("{\"code\":-401,\"msg\":\"expired\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertError(UserErrorCode.KAKAO_LOGIN_FAILED);
        server.verify();
    }

    @Test
    void rejectsTokenIssuedForUnallowedApp() {
        expectTokenInfo(99999L);

        assertError(UserErrorCode.KAKAO_APP_NOT_ALLOWED);
        server.verify();
    }

    @Test
    void rejectsLoginWhenAllowedAppIdsAreMissing() {
        properties.setAllowedAppIds(new LinkedHashSet<>());

        assertError(UserErrorCode.KAKAO_APP_NOT_ALLOWED);
        server.verify();
    }

    @Test
    void convertsKakaoApiFailureToProjectAuthenticationError() {
        server.expect(requestTo("https://kakao.test/v1/user/access_token_info"))
                .andRespond(withServerError());

        assertError(UserErrorCode.KAKAO_LOGIN_FAILED);
        server.verify();
    }

    private void expectTokenInfo(long appId) {
        server.expect(requestTo("https://kakao.test/v1/user/access_token_info"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andRespond(withSuccess("{\"app_id\":" + appId + "}", MediaType.APPLICATION_JSON));
    }

    private void assertError(UserErrorCode expected) {
        assertThatThrownBy(() -> kakaoClient.getVerifiedUser(ACCESS_TOKEN))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(expected);
    }
}
