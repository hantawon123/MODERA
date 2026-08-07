package com.ssafy.modera.worker.domain.search.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.List;

@Component
public class FastApiSemanticSearchClient implements SemanticSearchClient {

    private static final String HEADER_INTERNAL_TOKEN = "X-Internal-Token";

    private final RestClient restClient;
    private final String internalToken;

    public FastApiSemanticSearchClient(
            @Value("${analysis-worker.ai-server-url}") String aiServerUrl,
            @Value("${internal.callback.token}") String internalToken
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.restClient = RestClient.builder()
                .baseUrl(aiServerUrl)
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
        this.internalToken = internalToken;
    }

    @Override
    public SearchResult search(Integer userId, String query, int page, int size) {
        SearchEnvelope response = restClient.post()
                .uri("/internal/v1/images/search/semantic")
                .header(HEADER_INTERNAL_TOKEN, internalToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SearchRequest(userId, query, page, size))
                .retrieve()
                .body(SearchEnvelope.class);

        if (response == null || response.payload() == null) {
            throw new IllegalStateException("AI semantic search response is empty");
        }

        SearchPayload payload = response.payload();
        List<SearchHit> hits = payload.hits() == null
                ? List.of()
                : payload.hits().stream()
                        .map(hit -> new SearchHit(hit.imageId(), hit.score()))
                        .toList();

        return new SearchResult(
                payload.total(),
                payload.page(),
                payload.size(),
                hits
        );
    }

    /** Worker -> AI 요청. correlationId는 의도적으로 포함하지 않는다. */
    private record SearchRequest(
            Integer userId,
            String query,
            int page,
            int size
    ) {
    }

    private record SearchEnvelope(
            String eventType,
            int version,
            SearchPayload payload
    ) {
    }

    private record SearchPayload(
            String correlationId,
            long total,
            int page,
            int size,
            List<SearchHitResponse> hits
    ) {
    }

    private record SearchHitResponse(
            int imageId,
            double score
    ) {
    }
}
