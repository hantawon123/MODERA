package com.ssafy.modera.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiVersioningConfig implements WebMvcConfigurer {
    private static final String VERSION_HEADER = "X-Api-Version";
    private static final String DEFAULT_VERSION = "1.0";
    private static final String[] API_VERSION = {
            "1.0"
    };

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer
                .useRequestHeader(VERSION_HEADER)
                .setDefaultVersion(DEFAULT_VERSION)
                .addSupportedVersions(API_VERSION)
        ;
    }
}
