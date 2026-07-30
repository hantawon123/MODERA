package com.ssafy.modera.api.domain.image.service;

import com.ssafy.modera.api.domain.image.client.WorkerSearchClient;
import com.ssafy.modera.api.domain.image.client.WorkerSearchClient.WorkerSimilarImage;
import com.ssafy.modera.api.domain.image.dto.SimilarImageItemResponse;
import com.ssafy.modera.api.domain.image.dto.SimilarImagesResponse;
import com.ssafy.modera.api.domain.image.exception.ImageErrorCode;
import com.ssafy.modera.api.domain.library.repository.UserImageRepository;
import com.ssafy.modera.api.domain.query.repository.UserImageViewDetail;
import com.ssafy.modera.api.domain.query.repository.UserImageViewRepository;
import com.ssafy.modera.api.domain.query.repository.UserImageViewSummary;
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

/**
 * 연관 이미지 조회. 유사도 계산은 worker(analysis_result.embedding)가, 화면에 뿌릴
 * 메타데이터는 api-server(query_schema.user_image_view)가 갖고 있어서 둘을 합친다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageSimilarService {

    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 50;

    // worker에는 limit보다 넉넉히 요청한다. worker는 modera_analysis만 보므로 삭제 여부
    // (user_image_view.del_yn)를 알 수 없고, 걸러내는 건 api-server뿐이다. 딱 limit만
    // 받아오면 그중 일부가 걸러진 만큼 응답이 비어 보인다 — limit=2인데 1건만 나가는 식.
    // 여유분을 받아 필터링한 뒤 limit으로 자른다.
    private static final int OVER_FETCH_MULTIPLIER = 2;
    private static final int OVER_FETCH_MARGIN = 5;
    private static final int WORKER_MAX_LIMIT = 100;

    private final UserImageRepository userImageRepository;
    private final UserImageViewRepository userImageViewRepository;
    private final WorkerSearchClient workerSearchClient;
    private final ThumbnailUrlFactory thumbnailUrlFactory;

    public SimilarImagesResponse getSimilarImages(Integer userId, Integer imageId, int limit) {
        // ① 소유권 검증은 반드시 여기서 한다. worker는 modera_analysis만 붙으므로
        // library_schema.user_image를 볼 수 없고, 넘겨받은 userId를 그대로 신뢰한다
        // (worker SimilarImageController 주석). 그 신뢰의 근거를 만드는 게 이 한 줄이다.
        // 남의 이미지는 존재 자체를 숨기려고 403이 아니라 404로 응답한다(5-2와 동일).
        userImageRepository.findByUserIdAndImageId(userId, imageId)
                .orElseThrow(() -> new BusinessException(ImageErrorCode.IMAGE_NOT_FOUND));

        String baseTitle = userImageViewRepository.findDetail(userId, imageId)
                .map(UserImageViewDetail::title)
                .orElse(null);

        int effectiveLimit = clampLimit(limit);

        // ② worker 호출. 실패하면 예외가 아니라 빈 목록이 온다(WorkerSearchClient 실패 정책).
        List<WorkerSimilarImage> similarImages =
                workerSearchClient.findSimilar(imageId, userId, overFetchLimit(effectiveLimit));
        if (similarImages.isEmpty()) {
            return SimilarImagesResponse.of(imageId, baseTitle, List.of());
        }

        // ③ 받은 imageId들을 한 번에 조회한다(건당 조회하면 N+1).
        List<Integer> imageIds = similarImages.stream()
                .map(WorkerSimilarImage::imageId)
                .toList();
        Map<Integer, UserImageViewSummary> summariesByImageId = userImageViewRepository
                .findAllByUserIdAndImageIdIn(userId, imageIds)
                .stream()
                .collect(Collectors.toMap(UserImageViewSummary::imageId, Function.identity()));

        // ④ worker가 준 유사도 순서를 유지한다 — IN 조회 결과 순서는 보장되지 않으므로
        // worker 응답을 기준으로 훑는다. view에 없는 id(삭제됐거나 read model이 아직
        // 안 만들어진 경우)는 화면에 뿌릴 게 없어서 건너뛴다.
        List<SimilarImageItemResponse> items = new ArrayList<>(effectiveLimit);
        for (WorkerSimilarImage similarImage : similarImages) {
            if (items.size() == effectiveLimit) {
                break;
            }
            UserImageViewSummary summary = summariesByImageId.get(similarImage.imageId());
            if (summary == null) {
                continue;
            }
            items.add(new SimilarImageItemResponse(
                    summary.imageId(),
                    summary.title(),
                    summary.summary(),
                    Boolean.TRUE.equals(summary.favorite()),
                    // ⑤ 썸네일은 key만 저장돼 있으므로 조회 시점에 presigned GET URL로 바꾼다.
                    thumbnailUrlFactory.createViewUrl(summary.thumbnailKey()),
                    summary.tagNames(),
                    summary.categoryName(),
                    similarImage.score()
            ));
        }

        if (items.size() < effectiveLimit) {
            // 여유분까지 훑고도 limit을 못 채웠다는 뜻 — 후보 자체가 부족하거나
            // 삭제·미생성으로 걸러진 게 많다.
            log.debug("연관 이미지가 limit 미달: imageId={} worker={}건 -> 응답={}건 (limit={})",
                    imageId, similarImages.size(), items.size(), effectiveLimit);
        }
        return SimilarImagesResponse.of(imageId, baseTitle, items);
    }

    /**
     * 잘못된 limit은 400으로 막지 않고 범위 안으로 당긴다 — 부가 기능이라 요청을
     * 실패시키는 쪽이 손해가 크고, 상한은 worker/DB를 보호하려고 둔다.
     */
    private int clampLimit(int limit) {
        return Math.min(Math.max(limit, MIN_LIMIT), MAX_LIMIT);
    }

    /** 필터링으로 빠질 몫을 감안해 worker에 더 넉넉히 요청할 개수 */
    private int overFetchLimit(int effectiveLimit) {
        return Math.min(effectiveLimit * OVER_FETCH_MULTIPLIER + OVER_FETCH_MARGIN, WORKER_MAX_LIMIT);
    }
}
