package com.ssafy.modera.worker.domain.analysis.callback.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.modera.contract.EventTypes;
import com.ssafy.modera.contract.Streams;
import com.ssafy.modera.contract.payload.AnalysisCompletedPayload;
import com.ssafy.modera.contract.payload.AnalysisFailedPayload;
import com.ssafy.modera.contract.payload.InitialCategoryResolvedPayload;
import com.ssafy.modera.worker.domain.analysis.callback.dto.request.AnalysisCallbackRequest;
import com.ssafy.modera.worker.domain.analysis.entity.AnalysisJob;
import com.ssafy.modera.worker.domain.analysis.repository.AnalysisJobRepository;
import com.ssafy.modera.worker.domain.analysis.repository.AnalysisResultRepository;
import com.ssafy.modera.worker.domain.event.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class AnalysisCallbackServiceTest {

    @Mock AnalysisJobRepository analysisJobRepository;
    @Mock AnalysisResultRepository analysisResultRepository;
    @Mock EventPublisher eventPublisher;
    @Mock ObjectMapper objectMapper;
    @Mock TransactionTemplate transactionTemplate;
    @Mock TransactionStatus transactionStatus;
    @InjectMocks AnalysisCallbackService analysisCallbackService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void executeTransactionsImmediately() {
        doAnswer(invocation -> {
            Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(transactionStatus);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    void preservesTheExistingCompletedEventAndPublishesCategorySeparately() {
        AnalysisJob job = job();
        when(analysisJobRepository.findById(91)).thenReturn(Optional.of(job));
        when(analysisResultRepository.insert(any())).thenReturn(true);

        analysisCallbackService.handle(completed(Map.of(
                "title", "영수증",
                "summary", "결제 내역",
                "categoryId", 3,
                "category", "영수증",
                "tags", List.of("결제")
        )));

        ArgumentCaptor<String> eventType = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, org.mockito.Mockito.times(2)).publish(
                eq(Streams.ANALYSIS_RESULT), eventType.capture(), eq(1), payload.capture());

        assertThat(eventType.getAllValues()).containsExactly(
                EventTypes.ANALYSIS_COMPLETED,
                EventTypes.INITIAL_CATEGORY_RESOLVED);
        assertThat(payload.getAllValues().get(0)).isInstanceOf(AnalysisCompletedPayload.class);
        assertThat(payload.getAllValues().get(1))
                .isEqualTo(new InitialCategoryResolvedPayload(18, 7, 3, "영수증"));
        assertThat(job.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void oldAiResponseWithoutCategoryIdStillCompletesNormalAnalysis() {
        AnalysisJob job = job();
        when(analysisJobRepository.findById(91)).thenReturn(Optional.of(job));
        when(analysisResultRepository.insert(any())).thenReturn(true);

        analysisCallbackService.handle(completed(Map.of(
                "title", "기존 응답",
                "category", "문서"
        )));

        verify(eventPublisher).publish(
                eq(Streams.ANALYSIS_RESULT),
                eq(EventTypes.ANALYSIS_COMPLETED),
                eq(1),
                any(AnalysisCompletedPayload.class));
        verify(eventPublisher, never()).publish(
                eq(Streams.ANALYSIS_RESULT),
                eq(EventTypes.INITIAL_CATEGORY_RESOLVED),
                eq(1),
                any());
        assertThat(job.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void failedCallbackStillPublishesOnlyTheExistingFailureContract() {
        AnalysisJob job = job();
        when(analysisJobRepository.findById(91)).thenReturn(Optional.of(job));
        var request = new AnalysisCallbackRequest(
                91, 18, "FULL", "FAILED", null,
                new AnalysisCallbackRequest.CallbackError("AI_ERROR", "failed", true),
                "model-v1", null);

        analysisCallbackService.handle(request);

        verify(eventPublisher).publish(
                eq(Streams.ANALYSIS_RESULT),
                eq(EventTypes.ANALYSIS_FAILED),
                eq(1),
                any(AnalysisFailedPayload.class));
        verify(eventPublisher, never()).publish(
                eq(Streams.ANALYSIS_RESULT),
                eq(EventTypes.INITIAL_CATEGORY_RESOLVED),
                eq(1),
                any());
        assertThat(job.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void categoryEventFailureDoesNotTurnCompletedAnalysisIntoFailure() {
        AnalysisJob job = job();
        when(analysisJobRepository.findById(91)).thenReturn(Optional.of(job));
        when(analysisResultRepository.insert(any())).thenReturn(true);
        doThrow(new IllegalStateException("redis unavailable"))
                .when(eventPublisher).publish(
                        eq(Streams.ANALYSIS_RESULT),
                        eq(EventTypes.INITIAL_CATEGORY_RESOLVED),
                        eq(1),
                        any(InitialCategoryResolvedPayload.class));

        analysisCallbackService.handle(completed(Map.of(
                "categoryId", 3,
                "category", "영수증"
        )));

        verify(eventPublisher).publish(
                eq(Streams.ANALYSIS_RESULT),
                eq(EventTypes.ANALYSIS_COMPLETED),
                eq(1),
                any(AnalysisCompletedPayload.class));
        verify(eventPublisher, never()).publish(
                eq(Streams.ANALYSIS_RESULT),
                eq(EventTypes.ANALYSIS_FAILED),
                eq(1),
                any());
        assertThat(job.getStatus()).isEqualTo("COMPLETED");
    }

    private AnalysisJob job() {
        return AnalysisJob.builder()
                .userId(7)
                .imageId(18)
                .stage("FULL")
                .status("PROCESSING")
                .attempt(1)
                .triggerType("INITIAL")
                .queuedAt(OffsetDateTime.now())
                .s3Key("7/18.jpg")
                .build();
    }

    private AnalysisCallbackRequest completed(Map<String, Object> result) {
        return new AnalysisCallbackRequest(
                91, 18, "FULL", "COMPLETED", result,
                null, "model-v1", null);
    }
}
