package com.ssafy.modera.core.network.di

import com.skydoves.sandwich.retrofit.adapters.ApiResponseCallAdapterFactory
import com.ssafy.modera.core.network.AccessTokenInterceptor
import com.ssafy.modera.core.network.AccessTokenAuthenticator
import com.ssafy.modera.core.network.AuthHttpClient
import com.ssafy.modera.core.network.AuthRetrofit
import com.ssafy.modera.core.network.BuildConfig
import com.ssafy.modera.core.network.service.AnalyzedImageClient
import com.ssafy.modera.core.network.service.AnalyzedImageService
import com.ssafy.modera.core.network.service.CalendarClient
import com.ssafy.modera.core.network.service.CalendarService
import com.ssafy.modera.core.network.service.CategoryClient
import com.ssafy.modera.core.network.service.CategoryService
import com.ssafy.modera.core.network.service.ImageClient
import com.ssafy.modera.core.network.service.ImageService
import com.ssafy.modera.core.network.service.document.DocumentClient
import com.ssafy.modera.core.network.service.document.DocumentService
import com.ssafy.modera.core.network.service.search.SearchClient
import com.ssafy.modera.core.network.service.search.SearchService
import com.ssafy.modera.core.network.service.auth.AuthClient
import com.ssafy.modera.core.network.service.auth.AuthService
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
import java.util.concurrent.TimeUnit
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
    fun provideOkHttpClient(
        accessTokenInterceptor: AccessTokenInterceptor,
        accessTokenAuthenticator: AccessTokenAuthenticator,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(accessTokenInterceptor)
            .authenticator(accessTokenAuthenticator)
            .readTimeout(
                180,
                TimeUnit.SECONDS,
            )
            .apply {
                if (BuildConfig.DEBUG) {
                    addNetworkInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                            redactHeader("Authorization")
                        },
                    )
                }
            }
            .build()

    @Provides
    @Singleton
    @AuthHttpClient
    fun provideAuthOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .readTimeout(180, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    addNetworkInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                        },
                    )
                }
            }
            .build()

    @Provides
    @Singleton
    fun provideAuthService(@AuthRetrofit retrofit: Retrofit): AuthService =
        retrofit.create(AuthService::class.java)

    @Provides
    @Singleton
    fun provideAuthClient(authService: AuthService): AuthClient =
        AuthClient(authService)

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
    @AuthRetrofit
    fun provideAuthRetrofit(
        json: Json,
        @AuthHttpClient okHttpClient: OkHttpClient,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://i15d207.p.ssafy.io:8443/")
            .client(okHttpClient)
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType()),
            )
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
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

    @Provides
    @Singleton
    fun provideCalendarService(
        retrofit: Retrofit,
    ): CalendarService =
        retrofit.create(CalendarService::class.java)

    @Provides
    @Singleton
    fun provideCalendarClient(
        calendarService: CalendarService,
    ): CalendarClient =
        CalendarClient(calendarService)

    @Provides
    @Singleton
    fun provideDocumentService(
        retrofit: Retrofit,
    ): DocumentService =
        retrofit.create(DocumentService::class.java)

    @Provides
    @Singleton
    fun provideDocumentClient(
        documentService: DocumentService,
    ): DocumentClient =
        DocumentClient(
            documentService = documentService,
        )
}
