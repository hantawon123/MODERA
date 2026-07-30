package com.ssafy.modera.api.domain.image.service;

import com.ssafy.modera.api.domain.event.EventPublisher;
import com.ssafy.modera.api.domain.image.dto.request.ImageSemanticSearchRequest;
import com.ssafy.modera.api.domain.image.event.ImageSearchResultCoordinator;
import com.ssafy.modera.api.domain.image.exception.ImageErrorCode;
import com.ssafy.modera.api.global.exception.BusinessException;
import com.ssafy.modera.api.global.response.PageResponse;
import com.ssafy.modera.contract.EventTypes;
import com.ssafy.modera.contract.Streams;
import com.ssafy.modera.contract.payload.ImageSearchCompletedPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageSemanticSearchServiceTest {

    @Mock EventPublisher eventPublisher;
    @Mock ImageSearchResultCoordinator resultCoordinator;
    @Mock ImageQueryService imageQueryService;
    @InjectMocks ImageSemanticSearchService imageSemanticSearchService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                imageSemanticSearchService,
                "searchTimeout",
                Duration.ofSeconds(10)
        );
    }

    @Test
    void publishesSearchAndPreservesWorkerHitOrder() throws Exception {
        ImageSearchCompletedPayload result = new ImageSearchCompletedPayload(
                "ignored-by-mock",
                2,
                0,
                20,
                List.of(
                        new ImageSearchCompletedPayload.Hit(102, 3.9),
                        new ImageSearchCompletedPayload.Hit(101, 1.8)
                )
        );
        when(resultCoordinator.await(any(), eq(Duration.ofSeconds(10))))
                .thenReturn(result);
        PageResponse<com.ssafy.modera.api.domain.image.dto.response.ImageSummaryResponse> page =
                new PageResponse<>(List.of(), 0, 20, 2, 1, false, false);
        when(imageQueryService.getImagesInOrder(1, List.of(102, 101), 0, 20, 2))
                .thenReturn(page);

        var response = imageSemanticSearchService.search(
                1,
                new ImageSemanticSearchRequest("  프로그래밍 책  ", null, null)
        );

        assertThat(response).isSameAs(page);
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(
                eq(Streams.IMAGE_ANALYSIS),
                eq(EventTypes.IMAGE_SEMANTIC_SEARCH_REQUESTED),
                eq(1),
                payload.capture()
        );
        assertThat(payload.getValue().toString()).contains("프로그래밍 책", "page=0", "size=20");
    }

    @Test
    void mapsWorkerTimeoutToAiSearchTimeout() throws Exception {
        when(resultCoordinator.await(any(), any()))
                .thenThrow(new TimeoutException());

        assertThatThrownBy(() -> imageSemanticSearchService.search(
                1,
                new ImageSemanticSearchRequest("검색", 0, 20)
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ImageErrorCode.AI_SEARCH_TIMEOUT));
    }
}
