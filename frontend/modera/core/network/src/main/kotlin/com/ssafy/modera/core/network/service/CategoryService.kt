package com.ssafy.modera.core.network.service

import com.skydoves.sandwich.ApiResponse
import com.ssafy.modera.core.network.model.BaseResponse
import com.ssafy.modera.core.network.model.category.CategoriesResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface CategoryService {

    @GET("api/v1/categories")
    suspend fun fetchCategories(
        @Query("sort") sort: String,
    ): ApiResponse<BaseResponse<CategoriesResponse>>
}