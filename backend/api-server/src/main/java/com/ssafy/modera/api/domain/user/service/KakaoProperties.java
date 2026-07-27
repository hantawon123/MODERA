package com.ssafy.modera.api.domain.user.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "oauth.kakao")
public class KakaoProperties {

    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String tokenUri = "https://kauth.kakao.com/oauth/token";
    private String userInfoUri = "https://kapi.kakao.com/v2/user/me";
}
