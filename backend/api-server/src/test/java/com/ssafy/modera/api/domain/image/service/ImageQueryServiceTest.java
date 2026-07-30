package com.ssafy.modera.api.domain.image.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.modera.api.domain.image.repository.ImageListPage;
import com.ssafy.modera.api.domain.image.repository.ImageListRow;
import com.ssafy.modera.api.domain.image.repository.ImageQueryRepository;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

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
    void returnsPageInSpecificationShape() {
        OffsetDateTime uploadedAt = OffsetDateTime.now();
        when(imageQueryRepository.findImages(
                1, null, null, "C++", "UPLOADED_DESC", 0, 20))
                .thenReturn(new ImageListPage(
                        List.of(new ImageListRow(
                                10, "C++", "summary", false, null,
                                List.of("C++", "공부"), "공부", uploadedAt)),
                        1
                ));

        var response = imageQueryService.getImages(
                1, null, 0, 20, "UPLOADED_DESC", "  C++  ", null);

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
        assertThat(response.list().getFirst().thumbnailUrl()).isNull();
        assertThat(response.list().getFirst().tags()).containsExactly("C++", "공부");
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
                        List.of(),
                        List.of(),
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
}
