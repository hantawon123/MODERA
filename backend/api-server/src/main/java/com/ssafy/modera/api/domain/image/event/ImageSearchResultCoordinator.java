package com.ssafy.modera.api.domain.image.event;

import com.ssafy.modera.contract.payload.ImageSearchCompletedPayload;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class ImageSearchResultCoordinator {

    private final Map<String, CompletableFuture<ImageSearchCompletedPayload>> pending =
            new ConcurrentHashMap<>();

    public void register(String correlationId) {
        pending.put(correlationId, new CompletableFuture<>());
    }

    public ImageSearchCompletedPayload await(String correlationId, Duration timeout)
            throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<ImageSearchCompletedPayload> future = pending.get(correlationId);
        if (future == null) {
            throw new IllegalStateException("등록되지 않은 이미지 검색 요청입니다.");
        }
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } finally {
            pending.remove(correlationId, future);
        }
    }

    public void complete(ImageSearchCompletedPayload payload) {
        CompletableFuture<ImageSearchCompletedPayload> future =
                pending.get(payload.correlationId());
        if (future != null) {
            future.complete(payload);
        }
    }

    public void fail(String correlationId, String reason) {
        CompletableFuture<ImageSearchCompletedPayload> future = pending.get(correlationId);
        if (future != null) {
            future.completeExceptionally(new ImageSearchFailedException(reason));
        }
    }

    public void cancel(String correlationId) {
        CompletableFuture<ImageSearchCompletedPayload> future = pending.remove(correlationId);
        if (future != null) {
            future.cancel(false);
        }
    }

    public static final class ImageSearchFailedException extends RuntimeException {
        public ImageSearchFailedException(String reason) {
            super(reason);
        }
    }
}
