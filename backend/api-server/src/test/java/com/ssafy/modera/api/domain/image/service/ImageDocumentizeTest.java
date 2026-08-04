package com.ssafy.modera.api.domain.image.service;

import com.ssafy.modera.api.domain.image.client.WorkerSearchClient;
import com.ssafy.modera.api.domain.image.client.WorkerSearchClient.WorkerSimilarImage;
import com.ssafy.modera.api.domain.image.dto.response.ImageSummaryResponse;
import com.ssafy.modera.api.domain.image.exception.ImageErrorCode;
import com.ssafy.modera.api.domain.image.repository.DocumentSourceImage;
import com.ssafy.modera.api.domain.image.repository.ImageQueryRepository;
import com.ssafy.modera.api.domain.library.repository.UserImageRepository;
import com.ssafy.modera.api.global.exception.BusinessException;
import com.ssafy.modera.api.global.exception.GlobalErrorCode;
import com.ssafy.modera.api.global.response.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** 5-7 문서화 관련 자료 검색의 검증·필터링 검증. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImageDocumentizeTest {

    private static final Integer USER_ID = 1;

    @Mock
    private UserImageRepository userImageRepository;
    @Mock
    private ImageQueryRepository imageQueryRepository;
    @Mock
    private WorkerSearchClient workerSearchClient;
    @Mock
    private ThumbnailUrlFactory thumbnailUrlFactory;
    @Mock
    private ImageQueryService imageQueryService;

    private ImageSimilarService imageSimilarService;

    @BeforeEach
    void setUp() {
        // 검증 로직은 ImageSimilarReader로 옮겨졌지만 검증 규칙 자체는 그대로다 —
        // mock 리포지토리를 실제 reader로 감싸 기존 시나리오를 동일하게 태운다.
        imageSimilarService = new ImageSimilarService(
                new ImageSimilarReader(userImageRepository, imageQueryRepository),
                workerSearchClient,
                thumbnailUrlFactory,
                imageQueryService
        );
    }

    @Test
    @DisplayName("worker 결과를 5-1 DTO로, 기준 이미지는 결과에서 제거한다")
    void mapsHitsAndFiltersBaseImages() {
        givenOwnedCompleted(11, 12);
        // worker 계약이 어긋나 기준(11)이 섞여 와도 걸러야 한다
        given(workerSearchClient.findSimilarToAll(anyList(), eq(USER_ID), anyInt()))
                .willReturn(List.of(hit(13), hit(11), hit(14)));
        given(imageQueryService.getImagesInOrder(eq(USER_ID), eq(List.of(13, 14)), anyInt(), anyInt(), anyLong()))
                .willReturn(pageOf(summary(13), summary(14)));

        PageResponse<ImageSummaryResponse> result =
                imageSimilarService.findDocumentizeCandidates(USER_ID, List.of(11, 12));

        assertThat(result.list()).extracting(ImageSummaryResponse::imageId).containsExactly(13, 14);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(10);
    }

    @Test
    @DisplayName("worker가 비어 있으면(장애 degrade 포함) 빈 페이지를 돌려준다")
    void returnsEmptyPageWhenWorkerHasNothing() {
        givenOwnedCompleted(11);
        given(workerSearchClient.findSimilarToAll(anyList(), eq(USER_ID), anyInt()))
                .willReturn(List.of());

        PageResponse<ImageSummaryResponse> result =
                imageSimilarService.findDocumentizeCandidates(USER_ID, List.of(11));

        assertThat(result.list()).isEmpty();
        assertThat(result.totalElements()).isZero();
        verify(imageQueryService, never()).getImagesInOrder(any(), anyList(), anyInt(), anyInt(), anyLong());
    }

    @Test
    @DisplayName("imageIds에 중복이 있으면 400이다")
    void rejectsDuplicateBaseIds() {
        assertThatThrownBy(() ->
                imageSimilarService.findDocumentizeCandidates(USER_ID, List.of(11, 11)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.INVALID_PARAMETER);
    }

    @Test
    @DisplayName("소유하지 않은 이미지가 섞이면 404다(존재 여부를 숨긴다)")
    void rejectsUnownedBaseImages() {
        given(imageQueryRepository.findDocumentSources(eq(USER_ID), anyList()))
                .willReturn(List.of(source(11, "COMPLETED")));   // 12는 안 돌아옴

        assertThatThrownBy(() ->
                imageSimilarService.findDocumentizeCandidates(USER_ID, List.of(11, 12)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ImageErrorCode.IMAGE_NOT_FOUND);
    }

    @Test
    @DisplayName("분석 미완료 이미지가 섞이면 409다")
    void rejectsUnanalyzedBaseImages() {
        given(imageQueryRepository.findDocumentSources(eq(USER_ID), anyList()))
                .willReturn(List.of(source(11, "COMPLETED"), source(12, "PROCESSING")));

        assertThatThrownBy(() ->
                imageSimilarService.findDocumentizeCandidates(USER_ID, List.of(11, 12)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ImageErrorCode.IMAGE_ANALYSIS_NOT_COMPLETED);
    }

    private void givenOwnedCompleted(Integer... imageIds) {
        List<DocumentSourceImage> sources = List.of(imageIds).stream()
                .map(id -> source(id, "COMPLETED"))
                .toList();
        given(imageQueryRepository.findDocumentSources(eq(USER_ID), anyList())).willReturn(sources);
    }

    private DocumentSourceImage source(Integer imageId, String analysisStatus) {
        return new DocumentSourceImage(imageId, "제목", "요약", "IT",
                List.of("태그"), List.of(), OffsetDateTime.now(), analysisStatus);
    }

    private WorkerSimilarImage hit(Integer imageId) {
        return new WorkerSimilarImage(imageId, 0.9f);
    }

    private ImageSummaryResponse summary(Integer imageId) {
        return new ImageSummaryResponse(imageId, "제목", "요약", false, null,
                List.of("태그"), 1, "IT", OffsetDateTime.now(), false, false);
    }

    private PageResponse<ImageSummaryResponse> pageOf(ImageSummaryResponse... items) {
        List<ImageSummaryResponse> list = List.of(items);
        return new PageResponse<>(list, 0, 10, list.size(), 1, false, false);
    }
}
