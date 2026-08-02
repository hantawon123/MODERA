package com.ssafy.modera.api.domain.category.event;

import com.ssafy.modera.contract.payload.CategoryReanalysisCompletedPayload;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class CategoryReanalysisResultCoordinator {
    private final Map<UUID, CompletableFuture<CategoryReanalysisCompletedPayload>> pending =
            new ConcurrentHashMap<>();

    public void register(UUID requestId) {
        pending.put(requestId, new CompletableFuture<>());
    }

    public CategoryReanalysisCompletedPayload await(UUID requestId, Duration timeout)
            throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<CategoryReanalysisCompletedPayload> future = pending.get(requestId);
        if (future == null) {
            throw new IllegalStateException("등록되지 않은 카테고리 재분류 요청입니다.");
        }
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } finally {
            pending.remove(requestId, future);
        }
    }

    public void complete(CategoryReanalysisCompletedPayload payload) {
        CompletableFuture<CategoryReanalysisCompletedPayload> future = pending.get(payload.categoryRequestId());
        if (future != null) future.complete(payload);
    }

    public void fail(UUID requestId, String reason) {
        CompletableFuture<CategoryReanalysisCompletedPayload> future = pending.get(requestId);
        if (future != null) future.completeExceptionally(new CategoryReanalysisFailedException(reason));
    }

    public void cancel(UUID requestId) {
        CompletableFuture<CategoryReanalysisCompletedPayload> future = pending.remove(requestId);
        if (future != null) future.cancel(false);
    }

    public static final class CategoryReanalysisFailedException extends RuntimeException {
        public CategoryReanalysisFailedException(String reason) {
            super(reason);
        }
    }
}
