package com.ssafy.modera.api.domain.category.controller;

import com.ssafy.modera.api.domain.category.dto.response.CategoryListResponse;
import com.ssafy.modera.api.domain.category.service.CategoryQueryService;
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
        when(categoryQueryService.getCategories(null, "imageCount,desc"))
                .thenReturn(new CategoryListResponse(List.of()));

        mockMvc.perform(get("/api/v1/categories")
                        .queryParam("sort", "imageCount,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.code").value("T202"))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.page").doesNotExist());

        verify(categoryQueryService)
                .getCategories(null, "imageCount,desc");
    }

    @Test
    void appliesSpecificationDefaultsThroughHttpBinding() throws Exception {
        when(categoryQueryService.getCategories(null, "name,asc"))
                .thenReturn(new CategoryListResponse(List.of()));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("T202"));

        verify(categoryQueryService).getCategories(null, "name,asc");
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
}
