package com.ssafy.modera.worker.domain.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.modera.contract.EventEnvelope;
import com.ssafy.modera.contract.EventTypes;
import com.ssafy.modera.contract.Streams;
import com.ssafy.modera.contract.payload.ImageSemanticSearchRequestedPayload;
import com.ssafy.modera.contract.payload.ImageUploadedPayload;
import com.ssafy.modera.worker.domain.analysis.client.AnalysisClient;
import com.ssafy.modera.worker.domain.analysis.entity.AnalysisJob;
import com.ssafy.modera.worker.domain.analysis.repository.AnalysisJobRepository;
import com.ssafy.modera.worker.domain.category.service.CategoryReanalysisService;
import com.ssafy.modera.worker.domain.document.DocumentGenerationService;
import com.ssafy.modera.worker.domain.search.service.SemanticSearchService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageAnalysisConsumerTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final AnalysisJobRepository jobRepository = mock(AnalysisJobRepository.class);
    private final AnalysisClient analysisClient = mock(AnalysisClient.class);
    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private final SemanticSearchService semanticSearchService = mock(SemanticSearchService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ImageAnalysisConsumer consumer = new ImageAnalysisConsumer(
            redisTemplate,
            objectMapper,
            jobRepository,
            analysisClient,
            eventPublisher,
            mock(DocumentGenerationService.class),
            mock(CategoryReanalysisService.class),
            semanticSearchService,
            mock(EventPerformanceMetrics.class)
    );

    @AfterEach
    void tearDown() {
        // 시맨틱 검색 전용 executor 스레드를 정리한다(start()를 안 불러도 안전하다).
        consumer.stop();
    }

    @Test
    void routesSemanticSearchEventToItsService() {
        ImageSemanticSearchRequestedPayload payload =
                new ImageSemanticSearchRequestedPayload(
                        "correlation-1", 7, "프로그래밍 책", 0, 20);
        MapRecord<String, String, String> record = semanticRecord(payload);
        StreamOperations<String, String, String> streamOperations = mock(StreamOperations.class);
        when(redisTemplate.<String, String>opsForStream()).thenReturn(streamOperations);

        consumer.processRecord(record);

        // 검색은 전용 스레드에서 비동기로 처리되므로 timeout으로 완료를 기다린다.
        verify(semanticSearchService, timeout(2000)).handle(payload);
        verify(streamOperations, timeout(2000)).acknowledge(
                Streams.IMAGE_ANALYSIS,
                Streams.GROUP_ANALYSIS_WORKERS,
                record.getId().getValue()
        );
    }

    @Test
    void semanticSearchRunsOnDedicatedThreadSoAnalysisIsNotBlocked() {
        ImageSemanticSearchRequestedPayload payload =
                new ImageSemanticSearchRequestedPayload(
                        "correlation-2", 7, "영수증", 0, 10);
        MapRecord<String, String, String> record = semanticRecord(payload);
        StreamOperations<String, String, String> streamOperations = mock(StreamOperations.class);
        when(redisTemplate.<String, String>opsForStream()).thenReturn(streamOperations);
        AtomicReference<String> handlerThread = new AtomicReference<>();
        doAnswer(invocation -> {
            handlerThread.set(Thread.currentThread().getName());
            return null;
        }).when(semanticSearchService).handle(any());

        consumer.processRecord(record);

        verify(streamOperations, timeout(2000)).acknowledge(
                eq(Streams.IMAGE_ANALYSIS), eq(Streams.GROUP_ANALYSIS_WORKERS), anyString());
        assertThat(handlerThread.get()).isEqualTo("semantic-search-consumer");
    }

    @Test
    void semanticSearchFailureLeavesRecordUnackedForRedelivery() {
        ImageSemanticSearchRequestedPayload payload =
                new ImageSemanticSearchRequestedPayload(
                        "correlation-3", 7, "책", 0, 10);
        MapRecord<String, String, String> record = semanticRecord(payload);
        StreamOperations<String, String, String> streamOperations = mock(StreamOperations.class);
        when(redisTemplate.<String, String>opsForStream()).thenReturn(streamOperations);
        doThrow(new IllegalStateException("redis publish down"))
                .when(semanticSearchService).handle(any());

        consumer.processRecord(record);

        verify(semanticSearchService, timeout(2000)).handle(any());
        // 일시 오류는 XACK하지 않고 PEL에 남긴다(메인 경로와 같은 정책).
        verify(streamOperations, after(300).never())
                .acknowledge(anyString(), anyString(), anyString());
    }

    private MapRecord<String, String, String> semanticRecord(
            ImageSemanticSearchRequestedPayload payload) {
        EventEnvelope envelope = EventEnvelope.of(
                EventTypes.IMAGE_SEMANTIC_SEARCH_REQUESTED,
                1,
                Instant.now().toString(),
                payload,
                objectMapper
        );
        return StreamRecords
                .newRecord()
                .ofMap(envelope.toFieldMap())
                .withStreamKey(Streams.IMAGE_ANALYSIS);
    }

    @Test
    void reuploadCreatesAFullJobEvenWhenAnOldCompletedJobExists() {
        ImageUploadedPayload payload =
                new ImageUploadedPayload(7, 6, "6/7-60.jpg", null);
        when(jobRepository.existsByImageIdAndStatusIn(eq(7), any(Set.class)))
                .thenReturn(false);
        when(jobRepository.save(any(AnalysisJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        consumer.handleImageUploaded(payload, true);

        ArgumentCaptor<AnalysisJob> jobCaptor =
                ArgumentCaptor.forClass(AnalysisJob.class);
        verify(analysisClient).requestAnalysis(
                jobCaptor.capture(), eq("6/7-60.jpg"), eq(null));
        assertThat(jobCaptor.getValue().getTriggerType()).isEqualTo("REUPLOAD");
        assertThat(jobCaptor.getValue().getStage()).isEqualTo("FULL");
        assertThat(jobCaptor.getValue().getStatus()).isEqualTo("PROCESSING");
    }

    @Test
    void ordinaryUploadStillSkipsWhenCompletedJobExists() {
        ImageUploadedPayload payload =
                new ImageUploadedPayload(7, 6, "6/7-60.jpg", null);
        when(jobRepository.existsByImageIdAndStatusIn(
                7, Set.of("PENDING", "PROCESSING", "COMPLETED")))
                .thenReturn(true);

        consumer.handleImageUploaded(payload, false);

        verify(jobRepository, never()).save(any());
        verify(analysisClient, never()).requestAnalysis(any(), any(), any());
    }

    @Test
    void reuploadSkipsWhileAnotherAnalysisIsInFlight() {
        ImageUploadedPayload payload =
                new ImageUploadedPayload(7, 6, "6/7-60.jpg", null);
        when(jobRepository.existsByImageIdAndStatusIn(
                7, Set.of("PENDING", "PROCESSING")))
                .thenReturn(true);

        consumer.handleImageUploaded(payload, true);

        verify(jobRepository, never()).save(any());
        verify(analysisClient, never()).requestAnalysis(any(), any(), any());
    }

    @Test
    void newImageCreatesInitialFullAnalysisJob() {
        ImageUploadedPayload payload =
                new ImageUploadedPayload(8, 6, "6/8-new.jpg", null);
        when(jobRepository.existsByImageIdAndStatusIn(
                8, Set.of("PENDING", "PROCESSING", "COMPLETED")))
                .thenReturn(false);
        when(jobRepository.save(any(AnalysisJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        consumer.handleImageUploaded(payload, false);

        ArgumentCaptor<AnalysisJob> jobCaptor =
                ArgumentCaptor.forClass(AnalysisJob.class);
        verify(analysisClient).requestAnalysis(
                jobCaptor.capture(), eq("6/8-new.jpg"), eq(null));
        assertThat(jobCaptor.getValue().getTriggerType()).isEqualTo("INITIAL");
        assertThat(jobCaptor.getValue().getStage()).isEqualTo("FULL");
        assertThat(jobCaptor.getValue().getStatus()).isEqualTo("PROCESSING");
    }
}
