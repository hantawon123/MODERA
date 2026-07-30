package com.ssafy.modera.api.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * AI 서버 내부 API 호출용 RestClient.
 *
 * <p>worker 내부 API(WorkerClientConfig)와 같은 신뢰 모델이다 — 같은 X-Internal-Token을
 * 쓰고, api-server가 JWT·소유권을 이미 검증한 값을 넘긴다.
 *
 * <p>read 타임아웃이 worker 호출(3초)보다 훨씬 긴 이유는 이 호출이 LLM 생성이기
 * 때문이다. 문서 하나에 수 초~수십 초가 걸리고, 그동안 요청 스레드가 묶인다. 무한
 * 대기는 절대 안 되지만(스레드가 영영 안 돌아온다) 3초로는 정상 응답도 못 받는다.
 *
 * <p>HTTP/1.1로 고정하는 것도 worker와 같은 이유다 — uvicorn이 HTTP/2 upgrade를
 * 거절하면서 본문이 유실되는 문제가 있다.
 */
@Configuration
public class AiClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(90);

    @Bean
    public RestClient aiRestClient(
            @Value("${ai.base-url:http://ai-server:8000}") String baseUrl,
            @Value("${internal.token}") String internalToken) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader("X-Internal-Token", internalToken)
                .build();
    }
}
