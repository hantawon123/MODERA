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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
            semanticSearchService
    );

    @Test
    void routesSemanticSearchEventToItsService() {
        ImageSemanticSearchRequestedPayload payload =
                new ImageSemanticSearchRequestedPayload(
                        "correlation-1", 7, "프로그래밍 책", 0, 20);
        EventEnvelope envelope = EventEnvelope.of(
                EventTypes.IMAGE_SEMANTIC_SEARCH_REQUESTED,
                1,
                Instant.now().toString(),
                payload,
                objectMapper
        );
        MapRecord<String, String, String> record = StreamRecords
                .newRecord()
                .ofMap(envelope.toFieldMap())
                .withStreamKey(Streams.IMAGE_ANALYSIS);
        StreamOperations<String, String, String> streamOperations = mock(StreamOperations.class);
        when(redisTemplate.<String, String>opsForStream()).thenReturn(streamOperations);

        consumer.processRecord(record);

        verify(semanticSearchService).handle(payload);
        verify(streamOperations).acknowledge(
                Streams.IMAGE_ANALYSIS,
                Streams.GROUP_ANALYSIS_WORKERS,
                record.getId().getValue()
        );
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
