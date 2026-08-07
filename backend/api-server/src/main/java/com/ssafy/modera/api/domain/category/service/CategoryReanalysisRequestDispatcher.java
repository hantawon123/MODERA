package com.ssafy.modera.api.domain.category.service;

import com.ssafy.modera.api.domain.category.repository.CategoryCommandRepository;
import com.ssafy.modera.api.domain.category.repository.CategoryReanalysisTarget;
import com.ssafy.modera.api.domain.event.EventPublisher;
import com.ssafy.modera.api.domain.image.exception.ImageErrorCode;
import com.ssafy.modera.api.global.exception.BusinessException;
import com.ssafy.modera.contract.EventTypes;
import com.ssafy.modera.contract.Streams;
import com.ssafy.modera.contract.payload.CategoryReanalysisRequestedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryReanalysisRequestDispatcher {
    private final CategoryCommandRepository categoryCommandRepository;
    private final EventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    public List<Integer> dispatch(Integer userId, Integer imageId, UUID requestId) {
        CategoryReanalysisTarget target = transactionTemplate.execute(status ->
                categoryCommandRepository.prepareRequest(userId, imageId, requestId)
                        .orElseThrow(() -> new BusinessException(
                                ImageErrorCode.CATEGORY_REANALYSIS_UNAVAILABLE))
        );
        if (target == null) {
            throw new BusinessException(ImageErrorCode.CATEGORY_REANALYSIS_UNAVAILABLE);
        }
        try {
            eventPublisher.publish(
                    Streams.IMAGE_ANALYSIS,
                    EventTypes.CATEGORY_REANALYSIS_REQUESTED,
                    1,
                    new CategoryReanalysisRequestedPayload(
                            requestId, userId, imageId, target.excludedCategoryIds())
            );
        } catch (RuntimeException exception) {
            transactionTemplate.executeWithoutResult(status ->
                    categoryCommandRepository.clearPending(requestId, userId, imageId));
            throw exception;
        }
        return target.excludedCategoryIds();
    }
}
