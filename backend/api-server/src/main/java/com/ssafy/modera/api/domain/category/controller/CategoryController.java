package com.ssafy.modera.api.domain.category.controller;

import com.ssafy.modera.api.domain.category.dto.response.CategoryListResponse;
import com.ssafy.modera.api.domain.category.service.CategoryQueryService;
import com.ssafy.modera.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryQueryService categoryQueryService;

    @Operation(summary = "카테고리 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<CategoryListResponse>> getCategories(
            @AuthenticationPrincipal Integer userId,
            @RequestParam(name = "sort", defaultValue = "name,asc") String sort
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "T202",
                categoryQueryService.getCategories(userId, sort)
        ));
    }
}
