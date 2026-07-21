package com.ssafy.modera.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    // TODO RestClientConfig 작성
    @Bean("restClient")
    public RestClient restClient() {
        return RestClient.builder()
                .baseUrl("") // TODO API URI 등록
                .build();
    }
}
