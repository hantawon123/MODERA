package com.ssafy.modera.api.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "firebase")
public record FirebaseProperties(
        boolean enabled,
        String projectId
) {
}
