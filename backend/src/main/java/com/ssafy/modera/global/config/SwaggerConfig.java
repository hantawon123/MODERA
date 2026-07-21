package com.ssafy.modera.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {
    private static final String JWT_SCHEME_NAME = "JWT-Auth";

    @Value("${server.url:http://localhost:8080}")
    private String serverUrl;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .addSecurityItem(securityRequirement())
                .components(components())
                .servers(servers());
    }

    @Bean
    public GroupedOpenApi allApi() {
        return createGroup("00. All API", "/**");
    }

    private Info apiInfo() {
        return new Info()
                .title("Loop:ON API Document")
                .version("v1.0")
                .description("""
                        Loop:ON 프로젝트의 REST API 명세서입니다.
                        
                        **[인증 가이드]**
                        - Access Token: 우측 상단 `Authorize` 버튼 -> `Bearer {token}` 입력
                        - Refresh Token: `httpOnly` 쿠키로 자동 관리됨
                        """);
    }

    private Components components() {
        return new Components()
                .addSecuritySchemes(JWT_SCHEME_NAME, new SecurityScheme()
                        .name(JWT_SCHEME_NAME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Access Token 입력")
                );
    }

    private SecurityRequirement securityRequirement() {
        return new SecurityRequirement().addList(JWT_SCHEME_NAME);
    }

    private List<Server> servers() {
        return List.of(
                new Server().url(serverUrl).description("Loop:ON Prod Server (HTTPS)"),
                new Server().url("http://localhost:8080").description("Local Server")
        );
    }

    private GroupedOpenApi createGroup(String groupName, String... paths) {
        return GroupedOpenApi.builder()
                .group(groupName)
                .pathsToMatch(paths)
                .build();
    }
}
