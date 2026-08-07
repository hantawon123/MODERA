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
 * S3Client는 S3_INTERNAL_ENDPOINT를 향한다(서버가 MinIO에 직접 붙는 주소).
 * S3Presigner는 S3_PUBLIC_ENDPOINT로 서명한다 — SigV4는 Host를 서명 대상 헤더에
 * 포함하므로, 클라이언트가 실제로 접속할 호스트로 서명해야 MinIO의 검증을 통과한다.
 * internal로 서명한 뒤 URL의 호스트만 치환하면 SignatureDoesNotMatch(403)가 난다.
 * (2026-07-28 운영에서 실제 발생. 로컬은 internal==public이라 드러나지 않았다)
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
                .endpointOverride(URI.create(storageProperties.getPublicEndpoint()))
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
