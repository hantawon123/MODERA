package com.ssafy.modera.worker.domain.category.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class FastApiCategoryReanalysisClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sendsJsonBodyAndInternalTokenToAi() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> token = new AtomicReference<>();
        AtomicReference<String> protocol = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/v1/categories/reanalyze", exchange -> {
            requestBody.set(new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8));
            token.set(exchange.getRequestHeaders().getFirst("X-Internal-Token"));
            protocol.set(exchange.getProtocol());
            byte[] response = """
                    {"imageId":38,"categoryId":2089056292,"categoryName":"자동차"}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            FastApiCategoryReanalysisClient client =
                    new FastApiCategoryReanalysisClient(
                            "http://localhost:" + server.getAddress().getPort(),
                            "internal-secret");

            CategoryReanalysisClient.CategoryResult result =
                    client.reanalyze(3, 38, List.of(97969915));

            JsonNode json = objectMapper.readTree(requestBody.get());
            assertThat(protocol.get()).isEqualTo("HTTP/1.1");
            assertThat(token.get()).isEqualTo("internal-secret");
            assertThat(json.path("userId").asInt()).isEqualTo(3);
            assertThat(json.path("imageId").asInt()).isEqualTo(38);
            assertThat(json.path("excludedCategoryIds"))
                    .containsExactly(objectMapper.getNodeFactory().numberNode(97969915));
            assertThat(result.categoryId()).isEqualTo(2089056292);
            assertThat(result.categoryName()).isEqualTo("자동차");
        } finally {
            server.stop(0);
        }
    }
}
