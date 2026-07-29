package com.ssafy.modera.worker.domain.search.conroller;

import com.ssafy.modera.worker.domain.search.repository.SimilarImageRepository;
import com.ssafy.modera.worker.domain.search.repository.SimilarImageRepository.SimilarImageRow;
import com.ssafy.modera.worker.domain.search.service.SimilarImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;


/**
 * 연관 이미지 조회 내부 API (api-server 전용)
 *
 * 인증 : 콜백과 동일한 X-Internal-Token, worker 호스트 포트 노출 X
 * -> 도커 내부 네트워크 + 공유 토큰
 *
 * userId는 api-server가 JWT 검증 끝낸 값임..
 * 재검증 없이 신뢰
 */
@Slf4j
@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
public class SimilarImageController {

    private final SimilarImageService similarImageService;
    @Value("${internal.callback.token}")
    private String expectedToken;

    @GetMapping("/images/{imageId}/similar")
    public SimilarImagesResponse findSimilar(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @PathVariable int imageId,
            @RequestParam int userId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        verifyToken(token);

        List<SimilarImageRow> rows = similarImageService.findSimilar(imageId, userId, limit);
        log.info("연관 이미지 조회: imageId={} userId={} -> {}건", imageId, userId, rows.size());

        return new SimilarImagesResponse(
                imageId,
                rows.stream().map(r -> new SimilarImage(r.imageId(), r.score())).toList()
        );
    }

    private void verifyToken(String token) {
        if (token == null || !token.equals(expectedToken)) {
            log.warn("연관 이미지 조회 토큰 불일치 — 요청 거부");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    /** api-server가 imageIds로 자기 DB(user_image_view)를 조회해 화면을 조립한다. */
    public record SimilarImagesResponse(int baseImageId, List<SimilarImage> results) {}

    public record SimilarImage(int imageId, float score) {}
}
