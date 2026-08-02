package com.ssafy.modera.api.domain.category.controller;

import com.ssafy.modera.api.domain.category.dto.response.CategoryReanalysisResponse;
import com.ssafy.modera.api.domain.category.service.CategoryCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ImageCategoryCommandControllerTest {

    private CategoryCommandService categoryCommandService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        categoryCommandService = mock(CategoryCommandService.class);
        mockMvc = standaloneSetup(
                new ImageCategoryCommandController(categoryCommandService)
        ).build();
    }

    @Test
    void acceptsCategoryReanalysisAndReturnsI212() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(categoryCommandService.request(null, 18))
                .thenReturn(new CategoryReanalysisResponse(
                        requestId,
                        18,
                        List.of(3, 5, 7),
                        "COMPLETED"
                ));

        mockMvc.perform(post("/api/v1/images/18/category/reanalysis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.code").value("I212"))
                .andExpect(jsonPath("$.data.categoryRequestId")
                        .value(requestId.toString()))
                .andExpect(jsonPath("$.data.imageId").value(18))
                .andExpect(jsonPath("$.data.excludedCategoryIds[0]").value(3))
                .andExpect(jsonPath("$.data.excludedCategoryIds[1]").value(5))
                .andExpect(jsonPath("$.data.excludedCategoryIds[2]").value(7))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        verify(categoryCommandService).request(null, 18);
    }
}
