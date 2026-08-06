package com.ssafy.modera.api.domain.image.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.modera.api.domain.image.repository.ImageListPage;
import com.ssafy.modera.api.domain.image.repository.ImageListRow;
import com.ssafy.modera.api.domain.image.repository.ImageQueryRepository;
import com.ssafy.modera.api.domain.image.repository.ImageSyncRow;
import com.ssafy.modera.api.domain.image.repository.UserImageViewDetail;
import com.ssafy.modera.api.global.config.StorageProperties;
import com.ssafy.modera.api.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ImageQueryServiceTest {

    @Mock ImageQueryRepository imageQueryRepository;
    @Mock StorageProperties storageProperties;
    @Mock S3Client s3Client;
    @Mock S3Presigner s3Presigner;
    @Mock ObjectMapper objectMapper;
    @InjectMocks ImageQueryService imageQueryService;

    @Test
    void rejectsUnsupportedSort() {
        assertThatThrownBy(() -> imageQueryService.getImages(
                1, null, 0, 20, "UNKNOWN", null, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void syncsDetailLevelFieldsWithoutPresignedUrls() {
        OffsetDateTime uploadedAt = OffsetDateTime.now();
        when(imageQueryRepository.findSyncPage(1, 0, 100))
                .thenReturn(new ImageQueryRepository.ImageSyncPage(
                        List.of(new ImageSyncRow(
                                10, "C++", true, "summary", 3, "공부",
                                List.of("C++", "공부"), List.of("가격: 32,000원"),
                                null, "정제 텍스트", uploadedAt, true, false)),
                        250
                ));

        var response = imageQueryService.getSyncPage(1, 0, 100);

        var item = response.list().getFirst();
        assertThat(item.imageId()).isEqualTo(10);
        assertThat(item.categoryId()).isEqualTo(3);
        assertThat(item.ocrRefinedText()).isEqualTo("정제 텍스트");
        assertThat(item.isDocumented()).isTrue();
        // presigned URL 필드 자체가 없다 — 만료값을 로컬 DB에 저장하지 못하게 하는 계약.
        assertThat(Arrays.stream(item.getClass().getRecordComponents())
                .map(component -> component.getName()))
                .doesNotContain("imageUrl", "thumbnailUrl");
        assertThat(response.totalElements()).isEqualTo(250);
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    void rejectsSyncPageBeyondSizeLimit() {
        assertThatThrownBy(() -> imageQueryService.getSyncPage(1, 0, 201))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> imageQueryService.getSyncPage(1, -1, 100))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void returnsPageInSpecificationShape() {
        OffsetDateTime uploadedAt = OffsetDateTime.now();
        when(imageQueryRepository.findImages(
                1, null, null, "C++", "UPLOADED_DESC", 0, 20))
                .thenReturn(new ImageListPage(
                        List.of(new ImageListRow(
                                10, "C++", "summary", false, null,
                                List.of("C++", "공부"), 3, "공부", uploadedAt,
                                true, false)),
                        1
                ));

        var response = imageQueryService.getImages(
                1, null, 0, 20, "UPLOADED_DESC", "  C++  ", null);

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
        assertThat(response.list().getFirst().thumbnailUrl()).isNull();
        assertThat(response.list().getFirst().tags()).containsExactly("C++", "공부");
        assertThat(response.list().getFirst().categoryId()).isEqualTo(3);
        assertThat(response.list().getFirst().isDocumented()).isTrue();
        assertThat(response.list().getFirst().isCalendared()).isFalse();
        assertThat(Arrays.stream(response.getClass().getRecordComponents())
                .map(component -> component.getName()))
                .containsExactly(
                        "list", "page", "size", "totalElements", "hasNext", "hasPrevious");
    }

    @Test
    void listPresignsKnownThumbnailWithoutRemoteExistenceCheck() throws Exception {
        OffsetDateTime uploadedAt = OffsetDateTime.now();
        when(imageQueryRepository.findImages(
                1, null, 3, null, "UPLOADED_DESC", 0, 20))
                .thenReturn(new ImageListPage(List.of(new ImageListRow(
                        10, "title", "summary", false, "thumb/10.jpg",
                        List.of(), 3, "category", uploadedAt, false, false)), 1));
        StorageProperties.Bucket bucket = new StorageProperties.Bucket();
        bucket.setThumbnails("thumbnails");
        when(storageProperties.getBucket()).thenReturn(bucket);
        PresignedGetObjectRequest presigned =
                org.mockito.Mockito.mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(URI.create("https://storage.example/thumb").toURL());
        when(s3Presigner.presignGetObject(any(
                software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.class
        ))).thenReturn(presigned);

        var response = imageQueryService.getImages(
                1, null, 0, 20, "UPLOADED_DESC", null, 3);

        assertThat(response.list().getFirst().thumbnailUrl())
                .isEqualTo("https://storage.example/thumb");
        verify(s3Client, never()).headObject(any(
                software.amazon.awssdk.services.s3.model.HeadObjectRequest.class));
    }

    @Test
    void rejectsDetailWhenAnalysisIsNotCompleted() {
        when(imageQueryRepository.findDetail(1, 10))
                .thenReturn(Optional.of(new UserImageViewDetail(
                        "1/10-image.jpg",
                        null,
                        "UPLOADED",
                        "FAILED",
                        "image.jpg",
                        false,
                        null,
                        null,
                        null,
                        List.of(),
                        List.of(),
                        null,
                        null,
                        null,
                        false,
                        false
                )));

        assertThatThrownBy(() -> imageQueryService.getImage(1, 10))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(
                        ((BusinessException) exception).getErrorCode().getCode()
                ).isEqualTo("IMAGE_ANALYSIS_NOT_COMPLETED"));
    }

    @Test
    void returnsCompletedImageDetail() throws Exception {
        when(imageQueryRepository.findDetail(1, 10))
                .thenReturn(Optional.of(new UserImageViewDetail(
                        "1/10-image.jpg",
                        null,
                        "UPLOADED",
                        "COMPLETED",
                        "image.jpg",
                        true,
                        "summary",
                        3,
                        "공부",
                        List.of("C++"),
                        List.of("가격: 32000원"),
                        null,
                        "삼성전자 제품 안내(정제본)",
                        OffsetDateTime.parse("2026-08-01T10:30:00+09:00"),
                        false,
                        false
                )));
        StorageProperties.Bucket bucket = new StorageProperties.Bucket();
        bucket.setPictures("pictures");
        when(storageProperties.getBucket()).thenReturn(bucket);
        PresignedGetObjectRequest presigned =
                org.mockito.Mockito.mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(
                URI.create("https://storage.example/image").toURL()
        );
        when(s3Presigner.presignGetObject(any(
                software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.class
        ))).thenReturn(presigned);

        var response = imageQueryService.getImage(1, 10);

        assertThat(response.imageId()).isEqualTo(10);
        assertThat(response.imageUrl()).isEqualTo("https://storage.example/image");
        assertThat(response.favorite()).isTrue();
        assertThat(response.categoryId()).isEqualTo(3);
        assertThat(response.tags()).containsExactly("C++");
        assertThat(response.ocrRefinedText()).isEqualTo("삼성전자 제품 안내(정제본)");
        assertThat(response.uploadedAt())
                .isEqualTo(OffsetDateTime.parse("2026-08-01T10:30:00+09:00"));
    }
}
