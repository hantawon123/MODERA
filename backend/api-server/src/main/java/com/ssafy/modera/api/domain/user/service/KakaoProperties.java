package com.ssafy.modera.api.domain.user.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "oauth.kakao")
public class KakaoProperties {

    private String apiBaseUrl = "https://kapi.kakao.com";
    private String userInfoPath = "/v2/user/me";
    private String tokenInfoPath = "/v1/user/access_token_info";
    private Set<Long> allowedAppIds = new LinkedHashSet<>();
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(5);
}
