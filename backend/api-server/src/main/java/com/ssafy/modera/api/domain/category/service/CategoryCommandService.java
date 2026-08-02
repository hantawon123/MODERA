package com.ssafy.modera.api.domain.category.service;

import com.ssafy.modera.api.domain.category.dto.response.CategoryReanalysisResponse;
import com.ssafy.modera.api.domain.category.event.CategoryReanalysisResultCoordinator;
import com.ssafy.modera.api.domain.category.repository.CategoryCommandRepository;
import com.ssafy.modera.api.domain.image.exception.ImageErrorCode;
import com.ssafy.modera.api.domain.image.repository.ImageQueryRepository;
import com.ssafy.modera.api.domain.notification.outbox.UserDataChangeOutboxService;
import com.ssafy.modera.api.global.exception.BusinessException;
import com.ssafy.modera.contract.payload.CategoryReanalysisCompletedPayload;
import com.ssafy.modera.contract.payload.CategoryReanalysisFailedPayload;
import com.ssafy.modera.contract.payload.InitialCategoryResolvedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
public class CategoryCommandService {
    private final CategoryCommandRepository categoryCommandRepository;
    private final ImageQueryRepository imageQueryRepository;
    private final CategoryReanalysisRequestDispatcher requestDispatcher;
    private final CategoryReanalysisResultCoordinator resultCoordinator;
    private final UserDataChangeOutboxService userDataChangeOutboxService;

    @Value("${category.reanalysis.timeout:30s}")
    private Duration reanalysisTimeout;

    public CategoryReanalysisResponse request(Integer userId, Integer imageId) {
        UUID requestId = UUID.randomUUID();
        resultCoordinator.register(requestId);
        try {
            List<Integer> excludedCategoryIds =
                    requestDispatcher.dispatch(userId, imageId, requestId);
            resultCoordinator.await(requestId, reanalysisTimeout);
            return new CategoryReanalysisResponse(
                    requestId, imageId, excludedCategoryIds, "COMPLETED");
        } catch (TimeoutException exception) {
            throw new BusinessException(ImageErrorCode.CATEGORY_REANALYSIS_TIMEOUT);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ImageErrorCode.CATEGORY_REANALYSIS_FAILED);
        } catch (ExecutionException exception) {
            throw new BusinessException(ImageErrorCode.CATEGORY_REANALYSIS_FAILED);
        } catch (BusinessException exception) {
            resultCoordinator.cancel(requestId);
            throw exception;
        } catch (RuntimeException exception) {
            resultCoordinator.cancel(requestId);
            throw new BusinessException(ImageErrorCode.CATEGORY_REANALYSIS_FAILED);
        }
    }

    @Transactional
    public void complete(CategoryReanalysisCompletedPayload payload) {
        if (categoryCommandRepository.applyResult(
                payload.categoryRequestId(), payload.userId(), payload.imageId(),
                payload.categoryId(), payload.categoryName())) {
            imageQueryRepository.synchronizeUserCategories(payload.userId());
            userDataChangeOutboxService.record(
                    payload.userId(), "IMAGE_CATEGORY", String.valueOf(payload.imageId()));
            afterCommit(() -> resultCoordinator.complete(payload));
        }
    }

    @Transactional
    public void fail(CategoryReanalysisFailedPayload payload) {
        categoryCommandRepository.clearPending(
                payload.categoryRequestId(), payload.userId(), payload.imageId());
        afterCommit(() -> resultCoordinator.fail(
                payload.categoryRequestId(), payload.message()));
    }

    @Transactional
    public void initialize(InitialCategoryResolvedPayload payload) {
        categoryCommandRepository.saveInitialDefault(
                payload.userId(), payload.imageId(),
                payload.categoryId(), payload.categoryName());
        imageQueryRepository.synchronizeUserCategories(payload.userId());
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        action.run();
                    }
                }
        );
    }
}
