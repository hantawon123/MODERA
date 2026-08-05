package com.ssafy.modera.api.domain.category.controller;

import com.ssafy.modera.api.domain.category.dto.response.CategoryListResponse;
import com.ssafy.modera.api.domain.category.service.CategoryQueryService;
import com.ssafy.modera.api.domain.image.exception.ImageErrorCode;
import com.ssafy.modera.api.global.exception.BusinessException;
import com.ssafy.modera.api.global.exception.GlobalErrorCode;
import com.ssafy.modera.api.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class CategoryControllerTest {

    private CategoryQueryService categoryQueryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        categoryQueryService = mock(CategoryQueryService.class);
        mockMvc = standaloneSetup(new CategoryController(categoryQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void bindsHttpQueryParametersAndReturnsT202() throws Exception {
        when(categoryQueryService.getCategories(null, "IMAGE_COUNT_DESC"))
                .thenReturn(new CategoryListResponse(List.of()));

        mockMvc.perform(get("/api/v1/categories")
                        .queryParam("sort", "IMAGE_COUNT_DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.code").value("T202"))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.page").doesNotExist());

        verify(categoryQueryService)
                .getCategories(null, "IMAGE_COUNT_DESC");
    }

    @Test
    void appliesSpecificationDefaultsThroughHttpBinding() throws Exception {
        when(categoryQueryService.getCategories(null, "NAME_ASC"))
                .thenReturn(new CategoryListResponse(List.of()));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("T202"));

        verify(categoryQueryService).getCategories(null, "NAME_ASC");
    }

    @Test
    void returnsCommonApiEnvelopeForInvalidSort() throws Exception {
        when(categoryQueryService.getCategories(null, "drop table category"))
                .thenThrow(new BusinessException(GlobalErrorCode.INVALID_PARAMETER));

        mockMvc.perform(get("/api/v1/categories")
                        .queryParam("sort", "drop table category"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value("FAIL"))
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    void redirectsThumbnailWithPresignedLocationAndNoStore() throws Exception {
        when(categoryQueryService.getThumbnailRedirectUrl(null, 1744084819))
                .thenReturn("http://localhost:9002/category-thumbnails/1744084819.png?X-Amz-Signature=abc");

        mockMvc.perform(get("/api/v1/categories/1744084819/thumbnail"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        "http://localhost:9002/category-thumbnails/1744084819.png?X-Amz-Signature=abc"))
                // 리다이렉트를 캐시하면 만료된 presigned URL이 재사용되므로 no-store다.
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void hidesUnownedCategoryThumbnailBehindEnvelope404() throws Exception {
        when(categoryQueryService.getThumbnailRedirectUrl(null, 99))
                .thenThrow(new BusinessException(ImageErrorCode.CATEGORY_NOT_FOUND));

        mockMvc.perform(get("/api/v1/categories/99/thumbnail"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.result").value("FAIL"))
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }
}
