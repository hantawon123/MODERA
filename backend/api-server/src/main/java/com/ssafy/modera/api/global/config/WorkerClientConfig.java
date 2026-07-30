package com.ssafy.modera.api.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * worker 내부 API 호출용 RestClient
 * worker의 FastAPiAnalysisClient와 같은 신뢰 모델 -X-Internal-Token과 도커 세팅
 * <p>
 * 타임아웃을 반드시 명시한다 — worker가 죽거나 느려졌을 때 연관 이미지 조회는
 * 빈 목록으로 degrade하는 게 정책인데(WorkerSearchClient 참고), 타임아웃이 없으면
 * degrade하기 전에 api 요청 스레드가 먼저 매달린다.
 */
@Configuration
public class WorkerClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(1);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(3);

    @Bean
    public RestClient workerRestClient(
            @Value("${worker.base-url:http://modera-worker:8081}") String baseUrl,
            @Value("${internal.token}") String internalToken) {
        HttpClient httpClient = HttpClient.newBuilder()
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
