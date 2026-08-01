package com.ssafy.modera.core.network.di

import com.skydoves.sandwich.retrofit.adapters.ApiResponseCallAdapterFactory
import com.ssafy.modera.core.network.AccessTokenInterceptor
import com.ssafy.modera.core.network.BuildConfig
import com.ssafy.modera.core.network.service.AnalyzedImageClient
import com.ssafy.modera.core.network.service.AnalyzedImageService
import com.ssafy.modera.core.network.service.CategoryClient
import com.ssafy.modera.core.network.service.CategoryService
import com.ssafy.modera.core.network.service.ImageClient
import com.ssafy.modera.core.network.service.ImageService
import com.ssafy.modera.core.network.service.search.SearchClient
import com.ssafy.modera.core.network.service.search.SearchService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(AccessTokenInterceptor())
            .apply {
                if (BuildConfig.DEBUG) {
                    addNetworkInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        },
                    )
                }
            }
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        json: Json,
        okHttpClient: OkHttpClient,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://i15d207.p.ssafy.io:8443/")
            .client(okHttpClient)
            .addConverterFactory(
                json.asConverterFactory(
                    "application/json".toMediaType(),
                ),
            )
            .addCallAdapterFactory(
                ApiResponseCallAdapterFactory.create(),
            )
            .build()

    @Provides
    @Singleton
    fun provideCategoryService(
        retrofit: Retrofit,
    ): CategoryService =
        retrofit.create(CategoryService::class.java)

    @Provides
    @Singleton
    fun provideCategoryClient(
        categoryService: CategoryService,
    ): CategoryClient =
        CategoryClient(categoryService)

    @Provides
    @Singleton
    fun provideImageService(
        retrofit: Retrofit,
    ): ImageService =
        retrofit.create(ImageService::class.java)

    @Provides
    @Singleton
    fun provideImageClient(
        imageService: ImageService,
    ): ImageClient =
        ImageClient(imageService)

    @Provides
    @Singleton
    fun provideSearchService(
        retrofit: Retrofit,
    ): SearchService =
        retrofit.create(SearchService::class.java)

    @Provides
    @Singleton
    fun provideSearchClient(
        searchService: SearchService,
    ): SearchClient =
        SearchClient(searchService)

    @Provides
    @Singleton
    fun provideAnalyzedImageService(
        retrofit: Retrofit,
    ): AnalyzedImageService =
        retrofit.create(AnalyzedImageService::class.java)

    @Provides
    @Singleton
    fun provideAnalyzedImageClient(
        analyzedImageService: AnalyzedImageService,
    ): AnalyzedImageClient =
        AnalyzedImageClient(analyzedImageService)
}