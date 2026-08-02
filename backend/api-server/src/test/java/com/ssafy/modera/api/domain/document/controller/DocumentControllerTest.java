package com.ssafy.modera.api.domain.document.controller;

import com.ssafy.modera.api.domain.document.dto.response.DocumentDetailResponse;
import com.ssafy.modera.api.domain.document.service.DocumentCommandService;
import com.ssafy.modera.api.domain.document.service.DocumentDeleteService;
import com.ssafy.modera.api.domain.document.service.DocumentQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class DocumentControllerTest {

    private DocumentCommandService commandService;
    private DocumentQueryService queryService;
    private DocumentDeleteService deleteService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        commandService = mock(DocumentCommandService.class);
        queryService = mock(DocumentQueryService.class);
        deleteService = mock(DocumentDeleteService.class);
        mockMvc = standaloneSetup(new DocumentController(
                commandService, queryService, deleteService)).build();
    }

    @Test
    void exposesEveryDocumentEndpoint() throws Exception {
        String requestId = "d95db8b7-897e-412c-8924-eef3c7bca039";

        mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientRequestId\":\"" + requestId + "\",\"imageIds\":[1]}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/documents"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("D201"));
        mockMvc.perform(get("/api/v1/documents/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("D203"));
        mockMvc.perform(get("/api/v1/documents/1/images"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("D204"));
        mockMvc.perform(post("/api/v1/documents/1/regenerate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientRequestId\":\"" + requestId + "\",\"imageIds\":null}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/documents/1/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientRequestId\":\"" + requestId + "\",\"imageIds\":[2]}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/documents/1/images/exclude")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientRequestId\":\"" + requestId + "\",\"imageIds\":[1]}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/documents/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("D205"));
    }

    @Test
    void rejectsEmptyDocumentMutationsBeforeServiceInvocation() throws Exception {
        mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientRequestId\":null,\"imageIds\":[]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/documents/1/regenerate")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/documents/1/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientRequestId\":null,\"imageIds\":[]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/documents/1/images/exclude")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientRequestId\":null,\"imageIds\":[]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commandService, queryService, deleteService);
    }

    @Test
    void returnsMarkdownContentWithActualNewlinesAfterSingleJsonDeserialization() throws Exception {
        String markdown = "# 문서 제목\n\n첫 번째 문단입니다.\n- 항목 1\n- 항목 2";
        when(queryService.getDocument(null, 101)).thenReturn(new DocumentDetailResponse(
                101,
                "테스트 문서",
                "문서 요약",
                markdown,
                1,
                0,
                List.of(1),
                false,
                OffsetDateTime.parse("2026-08-02T12:00:00+09:00")
        ));

        mockMvc.perform(get("/api/v1/documents/101"))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentType())
                        .isEqualTo("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.data.content").value(markdown))
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
                    assertThat(body).contains("# 문서 제목\\n\\n첫 번째 문단입니다.");
                    assertThat(body).doesNotContain("# 문서 제목\\\\n");
                });
    }
}
