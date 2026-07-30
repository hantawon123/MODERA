package com.ssafy.modera.api.domain.image.service;

import com.ssafy.modera.api.domain.image.client.WorkerSearchClient;
import com.ssafy.modera.api.domain.image.client.WorkerSearchClient.WorkerSimilarImage;
import com.ssafy.modera.api.domain.image.dto.response.ImageSummaryResponse;
import com.ssafy.modera.api.domain.image.dto.response.SimilarImageItemResponse;
import com.ssafy.modera.api.domain.image.dto.response.SimilarImagesResponse;
import com.ssafy.modera.api.domain.image.exception.ImageErrorCode;
import com.ssafy.modera.api.domain.image.repository.DocumentSourceImage;
import com.ssafy.modera.api.domain.image.repository.ImageQueryRepository;
import com.ssafy.modera.api.domain.image.repository.UserImageViewDetail;
import com.ssafy.modera.api.domain.image.repository.UserImageViewSummary;
import com.ssafy.modera.api.domain.library.repository.UserImageRepository;
import com.ssafy.modera.api.global.exception.BusinessException;
import com.ssafy.modera.api.global.exception.GlobalErrorCode;
import com.ssafy.modera.api.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    /** 5-7 추천 개수. 페이지 요청을 받지 않고 고정한다(둘러보는 목록이 아니라 추천 위젯). */
    private static final int DOCUMENTIZE_LIMIT = 10;

    private static final String ANALYSIS_STATUS_COMPLETED = "COMPLETED";

    private final UserImageRepository userImageRepository;
    private final ImageQueryRepository imageQueryRepository;
    private final WorkerSearchClient workerSearchClient;
    private final ThumbnailUrlFactory thumbnailUrlFactory;
    private final ImageQueryService imageQueryService;

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

    /**
     * 5-7 문서화 관련 자료 검색. 기준 이미지 여러 장과 내용이 비슷한 다른 이미지를
     * 5-1 목록 DTO로 돌려준다 — 문서 만들기 전 "이것도 관련 있어요" 추천을 채우는 용도.
     *
     * <p>worker가 기준 이미지들의 임베딩 평균(centroid)으로 검색하고, 여기서는 결과를
     * 본인 활성 이미지로 재검증해 조립만 한다. worker 장애 시 빈 목록으로 degrade한다
     * (추천이 안 떠도 이미지 선택 화면은 살아야 한다).
     */
    public PageResponse<ImageSummaryResponse> findDocumentizeCandidates(
            Integer userId, List<Integer> imageIds) {
        validateBaseImages(userId, imageIds);

        List<WorkerSimilarImage> hits = workerSearchClient.findSimilarToAll(
                imageIds, userId, overFetchLimit(DOCUMENTIZE_LIMIT));

        // worker가 기준 이미지를 제외해 주지만, 계약이 어긋나도 기준이 추천에 뜨면
        // 화면이 이상해지므로 방어적으로 한 번 더 거른다.
        Set<Integer> baseIds = new HashSet<>(imageIds);
        List<Integer> candidateIds = hits.stream()
                .map(WorkerSimilarImage::imageId)
                .filter(id -> !baseIds.contains(id))
                .toList();

        if (candidateIds.isEmpty()) {
            return emptyPage();
        }

        // getImagesInOrder가 활성 이미지 재검증(삭제분 제외) + 순서 유지 + 5-1 DTO 변환을
        // 전부 한다. over-fetch로 넉넉히 조회했으니 잘라서 상한을 맞춘다.
        List<ImageSummaryResponse> items = imageQueryService
                .getImagesInOrder(userId, candidateIds, 0, DOCUMENTIZE_LIMIT, candidateIds.size())
                .list()
                .stream()
                .limit(DOCUMENTIZE_LIMIT)
                .toList();

        // totalElements는 명세대로 "필터링을 마친 최종 list 개수"다. 페이지도 0/10 고정.
        return new PageResponse<>(
                items,
                0,
                DOCUMENTIZE_LIMIT,
                items.size(),
                items.isEmpty() ? 0 : 1,
                false,
                false
        );
    }

    /**
     * 기준 이미지 검증. 중복·비정상 ID는 400, 소유하지 않은 것이 섞이면 404(존재 여부를
     * 숨기는 5-2와 같은 정책), 분석 미완료가 섞이면 409다 — 이 선택 목록은 그대로 문서
     * 생성(8-2)으로 가므로 문서 쪽과 같은 기준으로 미리 끊는 것이기도 하다.
     */
    private void validateBaseImages(Integer userId, List<Integer> imageIds) {
        Set<Integer> unique = new HashSet<>(imageIds);
        if (unique.size() != imageIds.size()
                || imageIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BusinessException(GlobalErrorCode.INVALID_PARAMETER);
        }

        List<DocumentSourceImage> found = imageQueryRepository.findDocumentSources(userId, imageIds);
        if (found.size() != imageIds.size()) {
            throw new BusinessException(ImageErrorCode.IMAGE_NOT_FOUND);
        }
        boolean hasUnanalyzed = found.stream()
                .anyMatch(source -> !ANALYSIS_STATUS_COMPLETED.equals(source.analysisStatus()));
        if (hasUnanalyzed) {
            throw new BusinessException(ImageErrorCode.IMAGE_ANALYSIS_NOT_COMPLETED);
        }
    }

    private PageResponse<ImageSummaryResponse> emptyPage() {
        return new PageResponse<>(List.of(), 0, DOCUMENTIZE_LIMIT, 0, 0, false, false);
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