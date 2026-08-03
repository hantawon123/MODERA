package com.ssafy.modera.api.domain.image.service;

import com.ssafy.modera.api.domain.image.exception.ImageErrorCode;
import com.ssafy.modera.api.domain.image.repository.DocumentSourceImage;
import com.ssafy.modera.api.domain.image.repository.ImageQueryRepository;
import com.ssafy.modera.api.domain.image.repository.UserImageViewDetail;
import com.ssafy.modera.api.domain.image.repository.UserImageViewSummary;
import com.ssafy.modera.api.domain.library.repository.UserImageRepository;
import com.ssafy.modera.api.global.exception.BusinessException;
import com.ssafy.modera.api.global.exception.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 유사 이미지·문서화 추천의 DB 조회 구간만 담당하는 읽기 전용 트랜잭션 경계.
 *
 * <p>{@link ImageSimilarService}가 worker HTTP 호출(연결 1초·읽기 3초)을 트랜잭션
 * 밖에서 수행할 수 있도록, HTTP 호출 앞뒤의 조회를 짧은 트랜잭션으로 쪼갠 것이다.
 * AuthService의 LoginCredentialReader와 같은 이유·같은 패턴이다 — 외부 대기가
 * 트랜잭션 안에 있으면 그 시간만큼 DB 커넥션을 점유해서, worker 지연이 무관한
 * API 전체의 커넥션 고갈로 번진다(로그인 경로에서 실측된 패턴).
 */
@Component
@RequiredArgsConstructor
public class ImageSimilarReader {

    private static final String ANALYSIS_STATUS_COMPLETED = "COMPLETED";

    private final UserImageRepository userImageRepository;
    private final ImageQueryRepository imageQueryRepository;

    /** 소유권을 검증하고 기준 이미지 제목을 돌려준다(제목이 없으면 null). 미소유·삭제는 404. */
    @Transactional(readOnly = true)
    public String readBaseTitle(Integer userId, Integer imageId) {
        userImageRepository
                .findByUserIdAndImageIdAndDelYn(userId, imageId, "N")
                .orElseThrow(() -> new BusinessException(ImageErrorCode.IMAGE_NOT_FOUND));

        return imageQueryRepository.findDetail(userId, imageId)
                .map(UserImageViewDetail::title)
                .orElse(null);
    }

    /** worker가 돌려준 후보를 본인 소유 활성 이미지로 재검증하기 위한 요약 조회. */
    @Transactional(readOnly = true)
    public Map<Integer, UserImageViewSummary> readSummaries(Integer userId, List<Integer> imageIds) {
        return imageQueryRepository
                .findAllByUserIdAndImageIdIn(userId, imageIds)
                .stream()
                .collect(Collectors.toMap(
                        UserImageViewSummary::imageId,
                        Function.identity(),
                        (first, ignored) -> first
                ));
    }

    /**
     * 문서화 기준 이미지 검증. 중복·비정상 ID는 400, 소유하지 않은 것이 섞이면 404(존재
     * 여부를 숨기는 5-2와 같은 정책), 분석 미완료가 섞이면 409다 — 이 선택 목록은 그대로
     * 문서 생성(8-2)으로 가므로 문서 쪽과 같은 기준으로 미리 끊는 것이기도 하다.
     */
    @Transactional(readOnly = true)
    public void validateDocumentizeBase(Integer userId, List<Integer> imageIds) {
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
}
