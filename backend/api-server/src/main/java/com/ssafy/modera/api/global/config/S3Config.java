package com.ssafy.modera.api.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * S3Client/S3Presigner는 항상 S3_INTERNAL_ENDPOINT를 향한다(서버가 MinIO에 실제로 붙는 주소).
 * 클라이언트에 돌려줄 URL의 호스트 치환은 사용처(ImageRegistrationService)에서 처리한다.
 */
@Configuration
@RequiredArgsConstructor
public class S3Config {

    private final StorageProperties storageProperties;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(storageProperties.getInternalEndpoint()))
                .region(Region.of(storageProperties.getRegion()))
                .credentialsProvider(credentialsProvider())
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(storageProperties.isPathStyleAccess())
                        .build())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .endpointOverride(URI.create(storageProperties.getInternalEndpoint()))
                .region(Region.of(storageProperties.getRegion()))
                .credentialsProvider(credentialsProvider())
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(storageProperties.isPathStyleAccess())
                        .build())
                .build();
    }

    private StaticCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(storageProperties.getAccessKey(), storageProperties.getSecretKey()));
    }
}
