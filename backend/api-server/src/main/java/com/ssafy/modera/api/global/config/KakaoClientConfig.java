package com.ssafy.modera.api.global.config;

import com.ssafy.modera.api.domain.user.service.KakaoProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
public class KakaoClientConfig {

    @Bean
    @Qualifier("kakaoRestClient")
    public RestClient kakaoRestClient(KakaoProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());

        return RestClient.builder()
                .baseUrl(properties.getApiBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
