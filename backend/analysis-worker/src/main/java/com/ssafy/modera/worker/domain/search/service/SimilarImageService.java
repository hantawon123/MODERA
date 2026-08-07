package com.ssafy.modera.worker.domain.search.service;

import com.ssafy.modera.worker.domain.search.repository.SimilarImageRepository;
import com.ssafy.modera.worker.domain.search.repository.SimilarImageRepository.SimilarImageRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimilarImageService {

    private final SimilarImageRepository similarImageRepository;

    /** 다중 기준(5-7 추천 위젯) 검색에만 적용되는 상한. 단일 검색은 상한이 없다. */
    private static final int MAX_LIMIT = 50;
    private static final int DEFAULT_LIMIT = 10;

    /**
     * 단일 기준 검색. 개수 상한이 없다 — 유사도 하한(MIN_SCORE)을 넘는 것을 전부 돌려준다.
     * 결과 크기는 LIMIT이 아니라 그 하한으로 조절한다.
     */
    public List<SimilarImageRow> findSimilar(int imageId, int userId) {
        List<SimilarImageRow> result = similarImageRepository.findSimilar(imageId, userId);

        log.debug("연관 이미지 조회: imageId={} userId={} -> {}건", imageId, userId, result.size());

        return result;
    }

    /** 다중 기준(centroid) 검색. 기준이 비면 조회 없이 빈 결과다. */
    public List<SimilarImageRow> findSimilarToAll(List<Integer> imageIds, int userId, int limit) {
        if (imageIds == null || imageIds.isEmpty()) {
            return List.of();
        }
        int safeLimit = clamp(limit);
        List<SimilarImageRow> result =
                similarImageRepository.findSimilarToAll(imageIds, userId, safeLimit);

        log.debug("다중 연관 이미지 조회: base={}장 userId={} limit={} -> {}건",
                imageIds.size(), userId, safeLimit, result.size());

        return result;
    }

    private int clamp(int limit) {
        if (limit < 1) {
            return DEFAULT_LIMIT;
        }

        return Math.min(limit, MAX_LIMIT);
    }
}
