package com.ssafy.modera.worker.domain.search.service;

import com.ssafy.modera.contract.EventTypes;
import com.ssafy.modera.contract.Streams;
import com.ssafy.modera.contract.payload.ImageSearchCompletedPayload;
import com.ssafy.modera.contract.payload.ImageSearchFailedPayload;
import com.ssafy.modera.contract.payload.ImageSemanticSearchRequestedPayload;
import com.ssafy.modera.worker.domain.event.EventPublisher;
import com.ssafy.modera.worker.domain.search.client.SemanticSearchClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticSearchService {

    private static final int MAX_ERROR_MESSAGE = 500;

    private final SemanticSearchClient semanticSearchClient;
    private final EventPublisher eventPublisher;

    public void handle(ImageSemanticSearchRequestedPayload payload) {
        if (payload.correlationId() == null || payload.correlationId().isBlank()) {
            log.error("IMAGE_SEMANTIC_SEARCH_REQUESTED에 correlationId가 없어 처리를 거부한다: userId={}",
                    payload.userId());
            return;
        }

        SemanticSearchClient.SearchResult result;
        try {
            result = semanticSearchClient.search(
                    payload.userId(),
                    payload.query(),
                    payload.page(),
                    payload.size()
            );
            validate(result);
        } catch (Exception exception) {
            publishFailed(payload.correlationId(), exception);
            return;
        }

        List<ImageSearchCompletedPayload.Hit> hits = result.hits().stream()
                .map(hit -> new ImageSearchCompletedPayload.Hit(
                        hit.imageId(),
                        hit.score()
                ))
                .toList();

        eventPublisher.publish(
                Streams.ANALYSIS_RESULT,
                EventTypes.IMAGE_SEARCH_COMPLETED,
                1,
                new ImageSearchCompletedPayload(
                        payload.correlationId(),
                        result.total(),
                        result.page(),
                        result.size(),
                        hits
                )
        );
        log.info("IMAGE_SEARCH_COMPLETED 발행: correlationId={} userId={} hits={} total={}",
                payload.correlationId(), payload.userId(), hits.size(), result.total());
    }

    private void validate(SemanticSearchClient.SearchResult result) {
        if (result == null
                || result.total() < 0
                || result.page() < 0
                || result.size() < 1
                || result.hits() == null
                || result.hits().stream().anyMatch(hit ->
                        hit == null
                                || hit.imageId() <= 0
                                || !Double.isFinite(hit.score()))) {
            throw new IllegalStateException("AI semantic search response is invalid");
        }
    }

    private void publishFailed(String correlationId, Exception exception) {
        String reason = exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
        if (reason.length() > MAX_ERROR_MESSAGE) {
            reason = reason.substring(0, MAX_ERROR_MESSAGE);
        }
        eventPublisher.publish(
                Streams.ANALYSIS_RESULT,
                EventTypes.IMAGE_SEARCH_FAILED,
                1,
                new ImageSearchFailedPayload(correlationId, reason)
        );
        log.warn("IMAGE_SEARCH_FAILED 발행: correlationId={} reason={}", correlationId, reason);
    }
}
