package com.ssafy.modera.api.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "storage.s3")
public class StorageProperties {

    /** 서버가 MinIO에 접근·서명할 때 쓰는 엔드포인트 */
    private String internalEndpoint;

    /** presigned URL을 클라이언트에 줄 때 호스트를 치환할 공개 엔드포인트 */
    private String publicEndpoint;

    private String region;
    private Bucket bucket = new Bucket();
    private String accessKey;
    private String secretKey;
    private boolean pathStyleAccess;

    @Getter
    @Setter
    public static class Bucket {
        private String pictures;
        private String thumbnails;
    }
}
