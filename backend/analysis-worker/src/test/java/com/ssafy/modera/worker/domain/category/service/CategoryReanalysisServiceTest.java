package com.ssafy.modera.worker.domain.category.service;

import com.ssafy.modera.contract.EventTypes;
import com.ssafy.modera.contract.Streams;
import com.ssafy.modera.contract.payload.CategoryReanalysisCompletedPayload;
import com.ssafy.modera.contract.payload.CategoryReanalysisFailedPayload;
import com.ssafy.modera.contract.payload.CategoryReanalysisRequestedPayload;
import com.ssafy.modera.worker.domain.category.client.CategoryReanalysisClient;
import com.ssafy.modera.worker.domain.category.repository.CategoryReanalysisJobRepository;
import com.ssafy.modera.worker.domain.event.EventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryReanalysisServiceTest {

    @Mock CategoryReanalysisJobRepository jobRepository;
    @Mock CategoryReanalysisClient client;
    @Mock EventPublisher eventPublisher;
    @InjectMocks CategoryReanalysisService categoryReanalysisService;

    @Test
    void sendsImageAndExcludedCategoriesToAiAndPublishesItsIdAndName() {
        var request = request();
        when(jobRepository.create(
                request.categoryRequestId(), request.userId(),
                request.imageId(), request.excludedCategoryIds())).thenReturn(true);
        when(client.reanalyze(7, 18, request.excludedCategoryIds()))
                .thenReturn(new CategoryReanalysisClient.CategoryResult(13, "새 카테고리"));

        categoryReanalysisService.handle(request);

        verify(jobRepository).updateStatus(request.categoryRequestId(), "PROCESSING");
        verify(jobRepository).updateStatus(request.categoryRequestId(), "COMPLETED");
        ArgumentCaptor<CategoryReanalysisCompletedPayload> payload =
                ArgumentCaptor.forClass(CategoryReanalysisCompletedPayload.class);
        verify(eventPublisher).publish(
                eq(Streams.ANALYSIS_RESULT),
                eq(EventTypes.CATEGORY_REANALYSIS_COMPLETED),
                eq(1),
                payload.capture());
        assertThat(payload.getValue().categoryId()).isEqualTo(13);
        assertThat(payload.getValue().categoryName()).isEqualTo("새 카테고리");
    }

    @Test
    void rejectsAnExcludedAiCategoryAndPublishesFailure() {
        var request = request();
        when(jobRepository.create(any(), any(), any(), any())).thenReturn(true);
        when(client.reanalyze(7, 18, request.excludedCategoryIds()))
                .thenReturn(new CategoryReanalysisClient.CategoryResult(8, "중복"));

        categoryReanalysisService.handle(request);

        verify(jobRepository).updateStatus(request.categoryRequestId(), "FAILED");
        verify(eventPublisher).publish(
                eq(Streams.ANALYSIS_RESULT),
                eq(EventTypes.CATEGORY_REANALYSIS_FAILED),
                eq(1),
                any(CategoryReanalysisFailedPayload.class));
    }

    @Test
    void ignoresARepeatedRequestId() {
        var request = request();
        when(jobRepository.create(any(), any(), any(), any())).thenReturn(false);

        categoryReanalysisService.handle(request);

        verify(client, never()).reanalyze(any(), any(), any());
        verify(eventPublisher, never()).publish(any(), any(), any(Integer.class), any());
    }

    private CategoryReanalysisRequestedPayload request() {
        return new CategoryReanalysisRequestedPayload(
                UUID.randomUUID(), 7, 18, List.of(3, 5, 8));
    }
}
