package com.ssafy.modera.worker.domain.search.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class FastApiSemanticSearchClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sendsWorkerAiContractWithoutCorrelationId() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> token = new AtomicReference<>();
        AtomicReference<String> protocol = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/v1/images/search/semantic", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            token.set(exchange.getRequestHeaders().getFirst("X-Internal-Token"));
            protocol.set(exchange.getProtocol());
            byte[] response = """
                    {"eventType":"IMAGE_SEARCH_COMPLETED","version":1,
                     "payload":{"correlationId":"ai-generated-id",
                                "total":1,"page":0,"size":20,
                                "hits":[{"imageId":18,"score":3.9}]}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            FastApiSemanticSearchClient client =
                    new FastApiSemanticSearchClient(
                            "http://localhost:" + server.getAddress().getPort(),
                            "internal-secret");

            SemanticSearchClient.SearchResult result =
                    client.search(7, "프로그래밍 책", 0, 20);

            JsonNode json = objectMapper.readTree(requestBody.get());
            assertThat(protocol.get()).isEqualTo("HTTP/1.1");
            assertThat(json.path("userId").asInt()).isEqualTo(7);
            assertThat(json.path("query").asText()).isEqualTo("프로그래밍 책");
            assertThat(json.path("page").asInt()).isZero();
            assertThat(json.path("size").asInt()).isEqualTo(20);
            assertThat(json.has("correlationId")).isFalse();
            assertThat(token.get()).isEqualTo("internal-secret");
            assertThat(result.hits()).hasSize(1);
            assertThat(result.hits().getFirst().imageId()).isEqualTo(18);
        } finally {
            server.stop(0);
        }
    }
}
