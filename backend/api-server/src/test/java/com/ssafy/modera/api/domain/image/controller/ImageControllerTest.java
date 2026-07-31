package com.ssafy.modera.api.domain.image.controller;

import com.ssafy.modera.api.domain.image.service.ImageCommandService;
import com.ssafy.modera.api.domain.image.service.ImageQueryService;
import com.ssafy.modera.api.domain.image.service.ImageSemanticSearchService;
import com.ssafy.modera.api.domain.image.service.ImageSimilarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ImageControllerTest {

    private ImageCommandService commandService;
    private ImageQueryService queryService;
    private ImageSimilarService similarService;
    private ImageSemanticSearchService searchService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        commandService = mock(ImageCommandService.class);
        queryService = mock(ImageQueryService.class);
        similarService = mock(ImageSimilarService.class);
        searchService = mock(ImageSemanticSearchService.class);
        mockMvc = standaloneSetup(new ImageController(
                commandService, queryService, similarService, searchService)).build();
    }

    @Test
    void exposesEveryImageEndpointWithExpectedSuccessCode() throws Exception {
        String registerBody = """
                {"images":[{"clientRequestId":"d95db8b7-897e-412c-8924-eef3c7bca039",
                "fileName":"a.jpg",
                "contentHash":"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                "fileSize":123,"ocr":{"rawText":"text"}}]}
                """;

        mockMvc.perform(post("/api/v1/images")
                        .contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("I201"));
        mockMvc.perform(post("/api/v1/images/1/upload-url"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("I204"));
        mockMvc.perform(get("/api/v1/images"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("I205"));
        mockMvc.perform(get("/api/v1/images/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("I206"));
        mockMvc.perform(delete("/api/v1/images")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"imageIds\":[1,2]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("I207"));
        mockMvc.perform(put("/api/v1/images/1/favorite")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"favorite\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("I208"));
        mockMvc.perform(post("/api/v1/images/search/semantic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"receipt\",\"page\":0,\"size\":20}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("I209"));
        mockMvc.perform(get("/api/v1/images/1/similar"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("I210"));
        mockMvc.perform(post("/api/v1/images/documentize")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"imageIds\":[1,2]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("I211"));
    }

    @Test
    void rejectsMalformedImageBodiesBeforeServiceInvocation() throws Exception {
        mockMvc.perform(post("/api/v1/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"images":[{"clientRequestId":null,"fileName":" ",
                                "contentHash":"bad","fileSize":0,"ocr":null}]}
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/v1/images")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"imageIds\":[]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/v1/images/1/favorite")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/images/search/semantic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\" \",\"page\":-1,\"size\":0}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/images/documentize")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"imageIds\":[]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commandService, queryService, similarService, searchService);
    }

    @Test
    void acceptsBlankOcrTextForImagesWithoutDetectedText() throws Exception {
        mockMvc.perform(post("/api/v1/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"images":[{"clientRequestId":"d95db8b7-897e-412c-8924-eef3c7bca039",
                                "fileName":"no-text.jpg",
                                "contentHash":"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                                "fileSize":123,"ocr":{"rawText":""}}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("I201"));
    }
}
