package com.ssafy.modera.worker.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 이 Spring Boot 버전은 내부적으로 Jackson 3(tools.jackson)을 자동구성하는 것으로 보여
 * com.fasterxml.jackson.databind.ObjectMapper 빈이 자동으로 생기지 않는다(api-server도 동일).
 * event-contract는 Jackson 2(com.fasterxml.jackson) API로 작성되어 있으므로 이 타입의
 * ObjectMapper를 이벤트 payload 직렬화 전용으로 직접 등록한다.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
