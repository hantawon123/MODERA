package com.ssafy.modera.worker.domain.search.client;

import java.util.List;

/**
 * 자연어 이미지 검색을 AI 서버에 위임한다.
 *
 * <p>correlationId는 API 서버와 Worker 사이의 이벤트 응답 매칭용 값이므로
 * AI 서버에는 전달하지 않는다. Worker가 응답 이벤트를 만들 때 원래 요청의
 * correlationId를 다시 붙인다.
 */
public interface SemanticSearchClient {

    SearchResult search(Integer userId, String query, int page, int size);

    record SearchResult(
            long total,
            int page,
            int size,
            List<SearchHit> hits
    ) {
    }

    record SearchHit(
            int imageId,
            double score
    ) {
    }
}
