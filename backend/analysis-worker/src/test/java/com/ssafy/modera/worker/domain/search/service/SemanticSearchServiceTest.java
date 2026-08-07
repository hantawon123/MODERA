package com.ssafy.modera.worker.domain.search.service;

import com.ssafy.modera.contract.EventTypes;
import com.ssafy.modera.contract.Streams;
import com.ssafy.modera.contract.payload.ImageSearchCompletedPayload;
import com.ssafy.modera.contract.payload.ImageSearchFailedPayload;
import com.ssafy.modera.contract.payload.ImageSemanticSearchRequestedPayload;
import com.ssafy.modera.worker.domain.event.EventPublisher;
import com.ssafy.modera.worker.domain.search.client.SemanticSearchClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemanticSearchServiceTest {

    @Mock SemanticSearchClient semanticSearchClient;
    @Mock EventPublisher eventPublisher;
    @InjectMocks SemanticSearchService semanticSearchService;

    @Test
    void callsAiWithoutCorrelationIdAndPublishesCompletedWithOriginalCorrelationId() {
        ImageSemanticSearchRequestedPayload request =
                new ImageSemanticSearchRequestedPayload(
                        "correlation-1", 7, "프로그래밍 책", 0, 20);
        when(semanticSearchClient.search(7, "프로그래밍 책", 0, 20))
                .thenReturn(new SemanticSearchClient.SearchResult(
                        2, 0, 20,
                        List.of(
                                new SemanticSearchClient.SearchHit(18, 3.9),
                                new SemanticSearchClient.SearchHit(21, 1.8)
                        )));

        semanticSearchService.handle(request);

        ArgumentCaptor<ImageSearchCompletedPayload> result =
                ArgumentCaptor.forClass(ImageSearchCompletedPayload.class);
        verify(eventPublisher).publish(
                eq(Streams.ANALYSIS_RESULT),
                eq(EventTypes.IMAGE_SEARCH_COMPLETED),
                eq(1),
                result.capture());
        assertThat(result.getValue().correlationId()).isEqualTo("correlation-1");
        assertThat(result.getValue().hits())
                .extracting(ImageSearchCompletedPayload.Hit::imageId)
                .containsExactly(18, 21);
    }

    @Test
    void publishesFailedWhenAiCallFails() {
        ImageSemanticSearchRequestedPayload request =
                new ImageSemanticSearchRequestedPayload(
                        "correlation-2", 7, "영수증", 0, 20);
        when(semanticSearchClient.search(any(), any(), anyInt(), anyInt()))
                .thenThrow(new IllegalStateException("AI unavailable"));

        semanticSearchService.handle(request);

        ArgumentCaptor<ImageSearchFailedPayload> result =
                ArgumentCaptor.forClass(ImageSearchFailedPayload.class);
        verify(eventPublisher).publish(
                eq(Streams.ANALYSIS_RESULT),
                eq(EventTypes.IMAGE_SEARCH_FAILED),
                eq(1),
                result.capture());
        assertThat(result.getValue().correlationId()).isEqualTo("correlation-2");
        assertThat(result.getValue().reason()).contains("AI unavailable");
    }

    @Test
    void rejectsInvalidAiHitsAndMissingCorrelationId() {
        ImageSemanticSearchRequestedPayload invalidResultRequest =
                new ImageSemanticSearchRequestedPayload(
                        "correlation-3", 7, "검색", 0, 20);
        when(semanticSearchClient.search(7, "검색", 0, 20))
                .thenReturn(new SemanticSearchClient.SearchResult(
                        1, 0, 20,
                        List.of(new SemanticSearchClient.SearchHit(-1, Double.NaN))));

        semanticSearchService.handle(invalidResultRequest);
        verify(eventPublisher).publish(
                eq(Streams.ANALYSIS_RESULT),
                eq(EventTypes.IMAGE_SEARCH_FAILED),
                eq(1),
                any(ImageSearchFailedPayload.class));

        ImageSemanticSearchRequestedPayload missingCorrelation =
                new ImageSemanticSearchRequestedPayload(
                        " ", 7, "검색", 0, 20);
        semanticSearchService.handle(missingCorrelation);
        verify(semanticSearchClient, times(1)).search(7, "검색", 0, 20);
    }
}
