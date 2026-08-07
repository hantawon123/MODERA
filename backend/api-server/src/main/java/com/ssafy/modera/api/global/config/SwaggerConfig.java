package com.ssafy.modera.api.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    private static final String BEARER_SCHEME_NAME = "Bearer Authentication";

    @Value("${server.url:http://localhost:8080}")
    private String prodServerUrl;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME_NAME, bearerScheme()))
                .servers(servers());
    }

    private Info apiInfo() {
        return new Info()
                .title("MODERA api-server API")
                .version("v1")
                .description("""
                        MODERA `api-server`의 외부 공개 API(`/api/v1/**`) 명세입니다.
                        `analysis-worker`는 API가 없는 백그라운드 프로세스라 이 문서에 없습니다.

                        ### 응답 형태
                        `/api/v1/**`의 모든 응답은 아래 envelope로 감싸져 옵니다(성공/실패 공통).
                        ```json
                        {
                          "result": "SUCCESS 또는 FAIL",
                          "code": "성공은 SUCCESS 고정, 실패는 에러코드 문자열",
                          "message": "사람이 읽는 메시지",
                          "data": { "...": "성공 시 실제 데이터, 실패 시 보통 null" },
                          "timestamp": "ISO-8601 UTC"
                        }
                        ```
                        검증 실패(400)일 때는 `data`에 `[{"field": "...", "message": "..."}]` 형태의
                        필드별 오류 배열이 담깁니다.

                        ### 인증 방법
                        1. `POST /api/v1/auth/register`로 가입
                        2. `POST /api/v1/auth/login`으로 `accessToken`/`refreshToken` 발급
                        3. 우측 상단 **Authorize** 버튼 → `accessToken` 값만 붙여넣기(`Bearer ` 접두어는
                           Swagger UI가 자동으로 붙입니다)
                        4. 인증이 필요한 API 호출

                        `accessToken`은 30분 후 만료됩니다. 만료되면 `POST /api/v1/auth/refresh`로
                        재발급받으세요 — 이때 `refreshToken`도 함께 회전(rotation)되어 이전
                        `refreshToken`은 즉시 무효화됩니다(재사용 시 401 `INVALID_REFRESH_TOKEN`).

                        `/internal/**`(MinIO webhook 수신)은 JWT가 아니라 `X-Webhook-Token` 헤더로
                        별도 인증하는 서버 간 내부 전용 API라 이 문서에 포함하지 않았습니다.
                        """);
    }

    private SecurityScheme bearerScheme() {
        return new SecurityScheme()
                .name(BEARER_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("POST /api/v1/auth/login (또는 /refresh)로 받은 accessToken");
    }

    private List<Server> servers() {
        return List.of(
                new Server().url("http://localhost:8080").description("로컬 직접 실행 (local 프로필, ./gradlew :api-server:bootRun)"),
                new Server().url("http://localhost:8090").description("로컬 컨테이너 (docker 프로필, docker compose --profile app up)"),
                new Server().url(prodServerUrl).description("운영")
        );
    }
}
