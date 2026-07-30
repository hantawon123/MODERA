package com.ssafy.modera.api.domain.image.service;

import com.ssafy.modera.api.domain.image.client.WorkerSearchClient;
import com.ssafy.modera.api.domain.image.client.WorkerSearchClient.WorkerSimilarImage;
import com.ssafy.modera.api.domain.image.dto.response.SimilarImageItemResponse;
import com.ssafy.modera.api.domain.image.dto.response.SimilarImagesResponse;
import com.ssafy.modera.api.domain.image.exception.ImageErrorCode;
import com.ssafy.modera.api.domain.image.repository.ImageQueryRepository;
import com.ssafy.modera.api.domain.image.repository.UserImageViewDetail;
import com.ssafy.modera.api.domain.image.repository.UserImageViewSummary;
import com.ssafy.modera.api.domain.library.repository.UserImageRepository;
import com.ssafy.modera.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageSimilarService {

    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 50;

    private static final int OVER_FETCH_MULTIPLIER = 2;
    private static final int OVER_FETCH_MARGIN = 5;
    private static final int WORKER_MAX_LIMIT = 100;

    private final UserImageRepository userImageRepository;
    private final ImageQueryRepository imageQueryRepository;
    private final WorkerSearchClient workerSearchClient;
    private final ThumbnailUrlFactory thumbnailUrlFactory;

    public SimilarImagesResponse getSimilarImages(
            Integer userId,
            Integer imageId,
            int limit
    ) {
        validateImageOwnership(userId, imageId);

        String baseTitle = imageQueryRepository.findDetail(userId, imageId)
                .map(UserImageViewDetail::title)
                .orElse(null);

        int effectiveLimit = clampLimit(limit);

        List<WorkerSimilarImage> similarImages =
                workerSearchClient.findSimilar(
                        imageId,
                        userId,
                        overFetchLimit(effectiveLimit)
                );

        if (similarImages.isEmpty()) {
            return SimilarImagesResponse.of(
                    imageId,
                    baseTitle,
                    List.of()
            );
        }

        List<Integer> similarImageIds = similarImages.stream()
                .map(WorkerSimilarImage::imageId)
                .toList();

        Map<Integer, UserImageViewSummary> summariesByImageId =
                imageQueryRepository
                        .findAllByUserIdAndImageIdIn(
                                userId,
                                similarImageIds
                        )
                        .stream()
                        .collect(Collectors.toMap(
                                UserImageViewSummary::imageId,
                                Function.identity(),
                                (first, ignored) -> first
                        ));

        List<SimilarImageItemResponse> items =
                createResponseItems(
                        similarImages,
                        summariesByImageId,
                        effectiveLimit
                );

        if (items.size() < effectiveLimit) {
            log.debug(
                    "유사 이미지 개수가 요청 limit보다 적습니다. "
                            + "imageId={}, workerCount={}, responseCount={}, limit={}",
                    imageId,
                    similarImages.size(),
                    items.size(),
                    effectiveLimit
            );
        }

        return SimilarImagesResponse.of(
                imageId,
                baseTitle,
                List.copyOf(items)
        );
    }

    private void validateImageOwnership(
            Integer userId,
            Integer imageId
    ) {
        userImageRepository
                .findByUserIdAndImageIdAndDelYn(
                        userId,
                        imageId,
                        "N"
                )
                .orElseThrow(() ->
                        new BusinessException(
                                ImageErrorCode.IMAGE_NOT_FOUND
                        )
                );
    }

    private List<SimilarImageItemResponse> createResponseItems(
            List<WorkerSimilarImage> similarImages,
            Map<Integer, UserImageViewSummary> summariesByImageId,
            int limit
    ) {
        List<SimilarImageItemResponse> items =
                new ArrayList<>(limit);

        for (WorkerSimilarImage similarImage : similarImages) {
            if (items.size() >= limit) {
                break;
            }

            UserImageViewSummary summary =
                    summariesByImageId.get(similarImage.imageId());

            // 삭제됐거나 해당 사용자가 소유하지 않은 이미지는 조회 결과에서 제외한다.
            if (summary == null) {
                continue;
            }

            items.add(new SimilarImageItemResponse(
                    summary.imageId(),
                    summary.title(),
                    summary.summary(),
                    Boolean.TRUE.equals(summary.favorite()),
                    thumbnailUrlFactory.createViewUrl(
                            summary.thumbnailKey()
                    ),
                    summary.tagNames(),
                    summary.categoryName(),
                    similarImage.score()
            ));
        }

        return items;
    }

    private int clampLimit(int limit) {
        return Math.min(
                Math.max(limit, MIN_LIMIT),
                MAX_LIMIT
        );
    }

    private int overFetchLimit(int effectiveLimit) {
        return Math.min(
                effectiveLimit * OVER_FETCH_MULTIPLIER
                        + OVER_FETCH_MARGIN,
                WORKER_MAX_LIMIT
        );
    }
}