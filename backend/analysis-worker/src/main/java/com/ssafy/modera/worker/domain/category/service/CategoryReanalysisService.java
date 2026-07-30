package com.ssafy.modera.worker.domain.category.service;

import com.ssafy.modera.contract.EventTypes;
import com.ssafy.modera.contract.Streams;
import com.ssafy.modera.contract.payload.CategoryReanalysisCompletedPayload;
import com.ssafy.modera.contract.payload.CategoryReanalysisFailedPayload;
import com.ssafy.modera.contract.payload.CategoryReanalysisRequestedPayload;
import com.ssafy.modera.worker.domain.category.client.CategoryReanalysisClient;
import com.ssafy.modera.worker.domain.category.repository.CategoryReanalysisJobRepository;
import com.ssafy.modera.worker.domain.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryReanalysisService {

    private final CategoryReanalysisJobRepository jobRepository;
    private final CategoryReanalysisClient client;
    private final EventPublisher eventPublisher;

    public void handle(CategoryReanalysisRequestedPayload payload) {
        if (!jobRepository.create(
                payload.categoryRequestId(), payload.userId(),
                payload.imageId(), payload.excludedCategoryIds())) {
            return;
        }
        jobRepository.updateStatus(payload.categoryRequestId(), "PROCESSING");
        try {
            CategoryReanalysisClient.CategoryResult result =
                    client.reanalyze(payload.imageId(), payload.excludedCategoryIds());
            if (payload.excludedCategoryIds().contains(result.categoryId())) {
                throw new IllegalStateException("AI returned an excluded category");
            }
            jobRepository.updateStatus(payload.categoryRequestId(), "COMPLETED");
            eventPublisher.publish(
                    Streams.ANALYSIS_RESULT,
                    EventTypes.CATEGORY_REANALYSIS_COMPLETED,
                    1,
                    new CategoryReanalysisCompletedPayload(
                            payload.categoryRequestId(), payload.userId(),
                            payload.imageId(), result.categoryId(), result.categoryName())
            );
        } catch (Exception exception) {
            jobRepository.updateStatus(payload.categoryRequestId(), "FAILED");
            eventPublisher.publish(
                    Streams.ANALYSIS_RESULT,
                    EventTypes.CATEGORY_REANALYSIS_FAILED,
                    1,
                    new CategoryReanalysisFailedPayload(
                            payload.categoryRequestId(), payload.userId(),
                            payload.imageId(), "CATEGORY_REANALYSIS_FAILED",
                            String.valueOf(exception.getMessage()), true)
            );
        }
    }
}
