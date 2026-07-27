package com.ssafy.modera.api.domain.user.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.modera.api.domain.user.exception.UserErrorCode;
import com.ssafy.modera.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoClient {

    private final KakaoProperties properties;
    private final RestClient restClient = RestClient.create();

    public KakaoUser getUser(String authorizationCode) {
        validateConfiguration();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.getClientId());
        form.add("redirect_uri", properties.getRedirectUri());
        form.add("code", authorizationCode);
        if (StringUtils.hasText(properties.getClientSecret())) {
            form.add("client_secret", properties.getClientSecret());
        }

        try {
            KakaoToken token = restClient
                    .post()
                    .uri(properties.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KakaoToken.class);

            if (token == null || !StringUtils.hasText(token.accessToken())) {
                throw new BusinessException(UserErrorCode.KAKAO_LOGIN_FAILED);
            }

            KakaoUser user = restClient
                    .get()
                    .uri(properties.getUserInfoUri())
                    .headers(headers -> headers.setBearerAuth(token.accessToken()))
                    .retrieve()
                    .body(KakaoUser.class);

            if (user == null || user.id() == null) {
                throw new BusinessException(UserErrorCode.KAKAO_LOGIN_FAILED);
            }
            return user;
        } catch (RestClientException exception) {
            log.warn("Kakao API request failed: {}", exception.getMessage());
            throw new BusinessException(UserErrorCode.KAKAO_LOGIN_FAILED);
        }
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(properties.getClientId()) || !StringUtils.hasText(properties.getRedirectUri())) {
            log.error("Kakao OAuth configuration is missing");
            throw new BusinessException(UserErrorCode.KAKAO_LOGIN_FAILED);
        }
    }

    private record KakaoToken(@JsonProperty("access_token") String accessToken) {
    }

    public record KakaoUser(Long id, @JsonProperty("kakao_account") KakaoAccount account) {
        public String email() {
            return account == null ? null : account.email();
        }
    }

    public record KakaoAccount(String email) {
    }
}
