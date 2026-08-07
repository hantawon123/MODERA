package com.ssafy.modera.api.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@EnableConfigurationProperties(FirebaseProperties.class)
public class FirebaseConfig {

    @Bean
    @ConditionalOnProperty(prefix = "firebase", name = "enabled", havingValue = "true")
    FirebaseApp firebaseApp(FirebaseProperties properties) throws IOException {
        FirebaseOptions.Builder options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.getApplicationDefault());
        if (properties.projectId() != null && !properties.projectId().isBlank()) {
            options.setProjectId(properties.projectId().trim());
        }
        return FirebaseApp.getApps().stream()
                .findFirst()
                .orElseGet(() -> FirebaseApp.initializeApp(options.build()));
    }

    @Bean
    @ConditionalOnProperty(prefix = "firebase", name = "enabled", havingValue = "true")
    FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
