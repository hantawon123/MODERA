package com.ssafy.modera.api.domain.image.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * worker의 연관 이미지 검색 내부 API 호출
 * userId는 api-server가 JWT 검증, 소유권 확인 끝낸 값 전달 -> worker는 신뢰
 * <p>
 * 실패 정책: 예외를 밖으로 던지지 않고 빈 목록으로 degrade한다. 연관 이미지는
 * 상세 화면의 부가 정보라, worker가 죽었다고 이미 성공적으로 조회된 본체까지
 * 500으로 되돌리는 건 손해가 크다. 대신 로그 레벨로 원인을 구분한다 —
 * 4xx는 토큰 불일치/경로 오타 같은 설정·계약 문제라 재시도해도 낫지 않으므로 error,
 * 그 외(5xx·연결 실패·타임아웃)는 일시 장애로 보고 warn.
 * (CLAUDE.md의 컨슈머 영구/일시 오류 분류와 같은 관점)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkerSearchClient {

    private final RestClient workerRestClient;

    public List<WorkerSimilarImage> findSimilar(Integer imageId, Integer userId, int limit) {
        try {
            WorkerSimilarResponse response = workerRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/v1/images/{imageId}/similar")
                            .queryParam("userId", userId)
                            .queryParam("limit", limit)
                            .build(imageId))
                    .retrieve()
                    .body(WorkerSimilarResponse.class);

            if (response == null || response.results() == null) {
                log.warn("worker 연관 이미지 응답이 비어 있음: imageId={}", imageId);
                return List.of();
            }
            return response.results();

        } catch (HttpClientErrorException exception) {
            // 401이면 internal.token이 worker의 internal.callback.token과 다르다는 뜻이다.
            log.error("worker 연관 이미지 호출 거부됨(설정·계약 확인 필요): imageId={} status={}",
                    imageId, exception.getStatusCode());
            return List.of();

        } catch (RestClientException exception) {
            log.warn("worker 연관 이미지 호출 실패 - 빈 목록으로 응답: imageId={} cause={}",
                    imageId, exception.getMessage());
            return List.of();
        }
    }

    /**
     * 다중 기준 연관 이미지 검색(5-7 문서화 관련 자료). 실패 정책은 단일 검색과 같다 —
     * 추천이 안 떠도 이미지 선택 화면은 살아야 하므로 빈 목록으로 degrade한다.
     */
    public List<WorkerSimilarImage> findSimilarToAll(List<Integer> imageIds, Integer userId, int limit) {
        try {
            WorkerMultiSimilarResponse response = workerRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/v1/images/similar")
                            .queryParam("imageIds", imageIds)
                            .queryParam("userId", userId)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .body(WorkerMultiSimilarResponse.class);

            if (response == null || response.results() == null) {
                log.warn("worker 다중 연관 이미지 응답이 비어 있음: base={}장", imageIds.size());
                return List.of();
            }
            return response.results();

        } catch (HttpClientErrorException exception) {
            // 401이면 internal.token이 worker의 internal.callback.token과 다르다는 뜻이다.
            log.error("worker 다중 연관 이미지 호출 거부됨(설정·계약 확인 필요): base={}장 status={}",
                    imageIds.size(), exception.getStatusCode());
            return List.of();

        } catch (RestClientException exception) {
            log.warn("worker 다중 연관 이미지 호출 실패 - 빈 목록으로 응답: base={}장 cause={}",
                    imageIds.size(), exception.getMessage());
            return List.of();
        }
    }

    // ── worker 응답 스키마 (analysis-worker SimilarImageController 기준) ──
    // score는 worker가 float으로 계산한다. double로 넓히면 0.93f가 0.9300000071525574로
    // 보이므로 응답까지 float으로 들고 간다.
    public record WorkerSimilarResponse(Integer baseImageId, List<WorkerSimilarImage> results) {
    }

    public record WorkerMultiSimilarResponse(List<Integer> baseImageIds, List<WorkerSimilarImage> results) {
    }

    public record WorkerSimilarImage(Integer imageId, Float score) {
    }
}
