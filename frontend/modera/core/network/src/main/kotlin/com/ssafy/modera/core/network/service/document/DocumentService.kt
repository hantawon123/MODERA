package com.ssafy.modera.core.network.service.document

import com.skydoves.sandwich.ApiResponse
import com.ssafy.modera.core.network.model.BaseResponse
import com.ssafy.modera.core.network.model.document.CreateDocumentRequest
import com.ssafy.modera.core.network.model.document.DocumentDetailResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface DocumentService {

    @POST("api/v1/documents")
    suspend fun createDocument(
        @Body request: CreateDocumentRequest,
    ): ApiResponse<BaseResponse<DocumentDetailResponse>>
}