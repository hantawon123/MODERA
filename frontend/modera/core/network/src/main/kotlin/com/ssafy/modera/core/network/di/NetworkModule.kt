package com.ssafy.modera.core.network.di

import com.skydoves.sandwich.retrofit.adapters.ApiResponseCallAdapterFactory
import com.ssafy.modera.core.network.BuildConfig
import com.ssafy.modera.core.network.service.CategoryClient
import com.ssafy.modera.core.network.service.CategoryService
import com.ssafy.modera.core.network.service.ImageClient
import com.ssafy.modera.core.network.service.ImageService
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
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().apply {
            if (BuildConfig.DEBUG) {
                this.addNetworkInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    },
                )
            }
        }.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(json: Json, okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder().client(okHttpClient).baseUrl("https//i15d207.p.ssafy.io:8000")
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create()).build()
    }

    @Provides
    @Singleton
    fun provideCategoryService(retrofit: Retrofit): CategoryService {
        return retrofit.create(CategoryService::class.java)
    }

    @Provides
    @Singleton
    fun provideCategoryClient(categoryService: CategoryService): CategoryClient {
        return CategoryClient(categoryService)
    }

    @Provides
    @Singleton
    fun provideImageService(retrofit: Retrofit): ImageService {
        return retrofit.create(ImageService::class.java)
    }

    @Provides
    @Singleton
    fun provideImageClient(imageService: ImageService): ImageClient {
        return ImageClient(imageService)
    }
}
