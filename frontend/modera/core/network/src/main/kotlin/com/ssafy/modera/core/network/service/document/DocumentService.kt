package com.ssafy.modera.core.network.service.document

import com.skydoves.sandwich.ApiResponse
import com.ssafy.modera.core.network.model.BaseResponse
import com.ssafy.modera.core.network.model.document.CreateDocumentRequest
import com.ssafy.modera.core.network.model.document.DocumentDetailResponse
import com.ssafy.modera.core.network.model.document.DocumentsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DocumentService {

    @GET("api/v1/documents/{documentId}")
    suspend fun fetchDocumentDetail(
        @Path("documentId") documentId: Long,
    ): ApiResponse<BaseResponse<DocumentDetailResponse>>

    @GET("api/v1/documents")
    suspend fun fetchDocuments(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("sort") sort: String,
    ): ApiResponse<BaseResponse<DocumentsResponse>>

    @POST("api/v1/documents")
    suspend fun createDocument(
        @Body request: CreateDocumentRequest,
    ): ApiResponse<BaseResponse<DocumentDetailResponse>>
}