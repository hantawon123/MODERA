package com.ssafy.modera.api.domain.category.service;

import com.ssafy.modera.api.domain.category.dto.response.CategoryReanalysisResponse;
import com.ssafy.modera.api.domain.category.repository.CategoryCommandRepository;
import com.ssafy.modera.api.domain.category.repository.CategoryReanalysisTarget;
import com.ssafy.modera.api.domain.event.EventPublisher;
import com.ssafy.modera.api.domain.image.exception.ImageErrorCode;
import com.ssafy.modera.api.domain.image.repository.ImageQueryRepository;
import com.ssafy.modera.api.global.exception.BusinessException;
import com.ssafy.modera.contract.EventTypes;
import com.ssafy.modera.contract.Streams;
import com.ssafy.modera.contract.payload.CategoryReanalysisCompletedPayload;
import com.ssafy.modera.contract.payload.CategoryReanalysisFailedPayload;
import com.ssafy.modera.contract.payload.CategoryReanalysisRequestedPayload;
import com.ssafy.modera.contract.payload.InitialCategoryResolvedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryCommandService {
    private final CategoryCommandRepository categoryCommandRepository;
    private final ImageQueryRepository imageQueryRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public CategoryReanalysisResponse request(Integer userId, Integer imageId) {
        UUID requestId = UUID.randomUUID();
        CategoryReanalysisTarget target = categoryCommandRepository
                .prepareRequest(userId, imageId, requestId)
                .orElseThrow(() ->
                        new BusinessException(ImageErrorCode.CATEGORY_REANALYSIS_UNAVAILABLE));
        eventPublisher.publish(
                Streams.IMAGE_ANALYSIS,
                EventTypes.CATEGORY_REANALYSIS_REQUESTED,
                1,
                new CategoryReanalysisRequestedPayload(
                        requestId, userId, imageId, target.excludedCategoryIds())
        );
        return new CategoryReanalysisResponse(
                requestId, imageId, target.excludedCategoryIds(), "QUEUED");
    }

    @Transactional
    public void complete(CategoryReanalysisCompletedPayload payload) {
        if (categoryCommandRepository.applyResult(
                payload.categoryRequestId(), payload.userId(), payload.imageId(),
                payload.categoryId(), payload.categoryName())) {
            imageQueryRepository.synchronizeUserCategories(payload.userId());
        }
    }

    @Transactional
    public void fail(CategoryReanalysisFailedPayload payload) {
        categoryCommandRepository.clearPending(
                payload.categoryRequestId(), payload.userId(), payload.imageId());
    }

    @Transactional
    public void initialize(InitialCategoryResolvedPayload payload) {
        categoryCommandRepository.saveInitialDefault(
                payload.userId(), payload.imageId(),
                payload.categoryId(), payload.categoryName());
        imageQueryRepository.synchronizeUserCategories(payload.userId());
    }
}
