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

    /**
     * 한 번에 돌려주는 최대 개수.
     * 화면은 10개면 충분할듯.....
     */
    private static final int MAX_LIMIT = 50;
    private static final int DEFAULT_LIMIT = 10;

    public List<SimilarImageRow> findSimilar(int imageId, int userId, int limit) {
        int safeLimit = clamp(limit);
        List<SimilarImageRow> result = similarImageRepository.findSimilar(imageId, userId, safeLimit);

        log.debug("연관 이미지 조회: imageId={} userId={} limit={} -> {}건",
                imageId, userId, safeLimit, result.size());

        return result;
    }

    private int clamp(int limit) {
        if (limit < 1) {
            return DEFAULT_LIMIT;
        }

        return Math.min(limit, MAX_LIMIT);
    }
}
