package com.ssafy.modera.api.domain.user.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.modera.api.domain.user.exception.UserErrorCode;
import com.ssafy.modera.api.global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class KakaoClient {

    private final KakaoProperties properties;
    private final RestClient restClient;

    public KakaoClient(
            KakaoProperties properties,
            @Qualifier("kakaoRestClient") RestClient restClient
    ) {
        this.properties = properties;
        this.restClient = restClient;
    }

    public KakaoUser getVerifiedUser(String kakaoAccessToken) {
        validateAllowedAppConfiguration();
        try {
            KakaoTokenInfo tokenInfo = restClient.get()
                    .uri(properties.getTokenInfoPath())
                    .headers(headers -> headers.setBearerAuth(kakaoAccessToken))
                    .retrieve()
                    .body(KakaoTokenInfo.class);
            if (tokenInfo == null || tokenInfo.appId() == null) {
                throw new BusinessException(UserErrorCode.KAKAO_LOGIN_FAILED);
            }
            if (!properties.getAllowedAppIds().contains(tokenInfo.appId())) {
                log.warn("Kakao login rejected: appId is not allowed, appId={}", tokenInfo.appId());
                throw new BusinessException(UserErrorCode.KAKAO_APP_NOT_ALLOWED);
            }

            KakaoUser user = restClient.get()
                    .uri(properties.getUserInfoPath())
                    .headers(headers -> headers.setBearerAuth(kakaoAccessToken))
                    .retrieve()
                    .body(KakaoUser.class);
            if (user == null || user.id() == null) {
                throw new BusinessException(UserErrorCode.KAKAO_LOGIN_FAILED);
            }
            return user;
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            log.warn("Kakao API rejected login request: status={}", exception.getStatusCode().value());
            throw new BusinessException(UserErrorCode.KAKAO_LOGIN_FAILED);
        } catch (RestClientException exception) {
            log.warn("Kakao API request failed: cause={}", exception.getClass().getSimpleName());
            throw new BusinessException(UserErrorCode.KAKAO_LOGIN_FAILED);
        }
    }

    private void validateAllowedAppConfiguration() {
        if (properties.getAllowedAppIds() == null || properties.getAllowedAppIds().isEmpty()) {
            log.error("Kakao allowed app IDs are not configured; rejecting login");
            throw new BusinessException(UserErrorCode.KAKAO_APP_NOT_ALLOWED);
        }
    }

    private record KakaoTokenInfo(@JsonProperty("app_id") Long appId) {
    }

    public record KakaoUser(Long id, @JsonProperty("kakao_account") KakaoAccount account) {
        public String email() {
            return account == null ? null : account.email();
        }

        public boolean hasValidVerifiedEmail() {
            return account != null
                    && StringUtils.hasText(account.email())
                    && Boolean.TRUE.equals(account.emailValid())
                    && Boolean.TRUE.equals(account.emailVerified());
        }
    }

    public record KakaoAccount(
            String email,
            @JsonProperty("is_email_valid") Boolean emailValid,
            @JsonProperty("is_email_verified") Boolean emailVerified
    ) {
    }
}
