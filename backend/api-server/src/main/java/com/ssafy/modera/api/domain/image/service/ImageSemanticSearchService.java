package com.ssafy.modera.api.domain.image.service;

import com.ssafy.modera.api.domain.event.EventPublisher;
import com.ssafy.modera.api.domain.image.dto.request.ImageSemanticSearchRequest;
import com.ssafy.modera.api.domain.image.dto.response.ImageSummaryResponse;
import com.ssafy.modera.api.domain.image.event.ImageSearchResultCoordinator;
import com.ssafy.modera.api.domain.image.exception.ImageErrorCode;
import com.ssafy.modera.api.global.exception.BusinessException;
import com.ssafy.modera.api.global.exception.GlobalErrorCode;
import com.ssafy.modera.api.global.response.PageResponse;
import com.ssafy.modera.contract.EventTypes;
import com.ssafy.modera.contract.Streams;
import com.ssafy.modera.contract.payload.ImageSearchCompletedPayload;
import com.ssafy.modera.contract.payload.ImageSemanticSearchRequestedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
public class ImageSemanticSearchService {

    private final EventPublisher eventPublisher;
    private final ImageSearchResultCoordinator resultCoordinator;
    private final ImageQueryService imageQueryService;

    @Value("${image.semantic-search.timeout:10s}")
    private Duration searchTimeout;

    public PageResponse<ImageSummaryResponse> search(
            Integer userId,
            ImageSemanticSearchRequest request
    ) {
        String query = request.query().trim();
        int page = request.page() == null ? 0 : request.page();
        int size = request.size() == null ? 20 : request.size();
        if (query.isEmpty() || page < 0 || size < 1 || size > 100) {
            throw new BusinessException(GlobalErrorCode.INVALID_PARAMETER);
        }

        String correlationId = UUID.randomUUID().toString();
        resultCoordinator.register(correlationId);
        try {
            eventPublisher.publish(
                    Streams.IMAGE_ANALYSIS,
                    EventTypes.IMAGE_SEMANTIC_SEARCH_REQUESTED,
                    1,
                    new ImageSemanticSearchRequestedPayload(
                            correlationId,
                            userId,
                            query,
                            page,
                            size
                    )
            );

            ImageSearchCompletedPayload result =
                    resultCoordinator.await(correlationId, searchTimeout);
            List<Integer> orderedImageIds = result.hits() == null
                    ? List.of()
                    : result.hits().stream()
                            .map(ImageSearchCompletedPayload.Hit::imageId)
                            .toList();

            return imageQueryService.getImagesInOrder(
                    userId,
                    orderedImageIds,
                    result.page(),
                    result.size(),
                    result.total()
            );
        } catch (TimeoutException exception) {
            throw new BusinessException(ImageErrorCode.AI_SEARCH_TIMEOUT);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ImageErrorCode.AI_SEARCH_FAILED);
        } catch (ExecutionException exception) {
            throw new BusinessException(ImageErrorCode.AI_SEARCH_FAILED);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            resultCoordinator.cancel(correlationId);
            throw new BusinessException(ImageErrorCode.AI_SEARCH_FAILED);
        }
    }
}
