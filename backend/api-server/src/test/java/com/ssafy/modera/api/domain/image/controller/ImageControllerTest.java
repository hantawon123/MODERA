package com.ssafy.modera.api.domain.image.controller;

import com.ssafy.modera.api.domain.image.exception.ImageErrorCode;
import com.ssafy.modera.api.domain.image.service.ImageCommandService;
import com.ssafy.modera.api.domain.image.service.ImageFileRedirectService;
import com.ssafy.modera.api.domain.image.service.ImageQueryService;
import com.ssafy.modera.api.domain.image.service.ImageSemanticSearchService;
import com.ssafy.modera.api.domain.image.service.ImageSimilarService;
import com.ssafy.modera.api.global.exception.BusinessException;
import com.ssafy.modera.api.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ImageControllerTest {

    private ImageCommandService commandService;
    private ImageQueryService queryService;
    private ImageSimilarService similarService;
    private ImageSemanticSearchService searchService;
    private ImageFileRedirectService fileRedirectService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        commandService = mock(ImageCommandService.class);
        queryService = mock(ImageQueryService.class);
        similarService = mock(ImageSimilarService.class);
        searchService = mock(ImageSemanticSearchService.class);
        fileRedirectService = mock(ImageFileRedirectService.class);
        mockMvc = standaloneSetup(new ImageController(
                commandService, queryService, similarService, searchService, fileRedirectService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("원본: 302 + Location(서명 포함) + Cache-Control no-store")
    void redirectsOriginalWithPresignedLocationAndNoStore() throws Exception {
        String presigned = "http://localhost:9002/pictures/4/1-a.jpg"
                + "?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Signature=abc123";
        when(fileRedirectService.getOriginalRedirectUrl(null, 1)).thenReturn(presigned);

        mockMvc.perform(get("/api/v1/images/1/file"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", presigned))
                // 만료되는 presigned URL이 중간 캐시에 남으면 안 된다.
                .andExpect(header().string("Cache-Control", "no-store"));

        assertThat(presigned).contains("X-Amz-Signature=");
    }

    @Test
    @DisplayName("썸네일: 302 + Location(서명 포함) + Cache-Control no-store")
    void redirectsThumbnailWithPresignedLocationAndNoStore() throws Exception {
        String presigned = "http://localhost:9002/thumbnails/4/1-thumb.jpg"
                + "?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Signature=def456";
        when(fileRedirectService.getThumbnailRedirectUrl(null, 1)).thenReturn(presigned);

        mockMvc.perform(get("/api/v1/images/1/thumbnail"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", presigned))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    @DisplayName("같은 imageId를 두 번 호출하면 Location이 서로 다르다(매번 신규 발급)")
    void issuesFreshPresignedUrlOnEveryCall() throws Exception {
        when(fileRedirectService.getOriginalRedirectUrl(null, 1))
                .thenReturn("http://localhost:9002/pictures/4/1-a.jpg?X-Amz-Signature=first")
                .thenReturn("http://localhost:9002/pictures/4/1-a.jpg?X-Amz-Signature=second");

        String first = mockMvc.perform(get("/api/v1/images/1/file"))
                .andExpect(status().isFound())
                .andReturn().getResponse().getHeader("Location");
        String second = mockMvc.perform(get("/api/v1/images/1/file"))
                .andExpect(status().isFound())
                .andReturn().getResponse().getHeader("Location");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("타인 소유·미존재·삭제·업로드 미완료는 모두 404 envelope다(403으로 구분하지 않는다)")
    void hidesInaccessibleImageBehind404() throws Exception {
        when(fileRedirectService.getOriginalRedirectUrl(null, 99))
                .thenThrow(new BusinessException(ImageErrorCode.IMAGE_NOT_FOUND));
        when(fileRedirectService.getThumbnailRedirectUrl(null, 99))
                .thenThrow(new BusinessException(ImageErrorCode.IMAGE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/images/99/file"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.result").value("FAIL"))
                .andExpect(jsonPath("$.code").value("IMAGE_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/images/99/thumbnail"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("IMAGE_NOT_FOUND"));
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
