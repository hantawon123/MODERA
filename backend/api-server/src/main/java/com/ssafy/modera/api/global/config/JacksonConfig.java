package com.ssafy.modera.api.global.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 이 Spring Boot 버전은 내부적으로 Jackson 3(tools.jackson)을 자동구성하는 것으로 보여
 * com.fasterxml.jackson.databind.ObjectMapper 빈이 자동으로 생기지 않는다(analysis-worker도 동일).
 * event-contract는 Jackson 2(com.fasterxml.jackson) API로 작성되어 있으므로 이 타입의
 * ObjectMapper를 이벤트 payload 직렬화 전용으로 직접 등록한다.
 * (REST 컨트롤러의 @RequestBody/@ResponseBody는 Spring이 자동구성한 별도 컨버터를 그대로 쓴다.)
 */
@Configuration
public class JacksonConfig {

    /**
     * 모르는 필드는 무시한다.
     *
     * <p>기본값(FAIL_ON_UNKNOWN_PROPERTIES=true)이면 상대 서버가 payload에 필드를 하나
     * 추가하는 것만으로 이쪽 역직렬화가 터진다. 그 예외는 컨슈머의 "영구 오류" 분류에
     * 걸려 XACK 후 스킵되므로 이벤트가 재시도 없이 조용히 사라진다 — 두 서버를 동시에
     * 배포하지 못하는 순간(롤링 배포, 한쪽만 먼저 머지된 브랜치)마다 데이터가 유실된다.
     *
     * <p>필드를 지우거나 이름을 바꾸는 변경은 여전히 양쪽을 함께 배포해야 한다. 이 설정이
     * 지켜 주는 건 "추가"뿐이고, 그것만으로도 배포 순서 제약이 크게 줄어든다.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
