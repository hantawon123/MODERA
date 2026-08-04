package com.ssafy.modera.core.data.di

import com.ssafy.modera.core.data.repository.AnalyzedImageRepository
import com.ssafy.modera.core.data.repository.CategoryRepository
import com.ssafy.modera.core.data.repository.DefaultAnalyzedImageRepository
import com.ssafy.modera.core.data.repository.DefaultCategoryRepository
import com.ssafy.modera.core.data.repository.DefaultImageRepository
import com.ssafy.modera.core.data.repository.ImageRepository
import com.ssafy.modera.core.data.repository.calendar.CalendarRepository
import com.ssafy.modera.core.data.repository.calendar.DefaultCalendarRepository
import com.ssafy.modera.core.data.repository.calendar.DefaultDeviceCalendarRepository
import com.ssafy.modera.core.data.repository.calendar.DeviceCalendarRepository
import com.ssafy.modera.core.data.repository.document.DefaultDocumentRepository
import com.ssafy.modera.core.data.repository.document.DocumentRepository
import com.ssafy.modera.core.data.repository.notification.DefaultPushTokenRepository
import com.ssafy.modera.core.data.repository.notification.PushTokenRepository
import com.ssafy.modera.core.data.repository.search.DefaultRecentSearchRepository
import com.ssafy.modera.core.data.repository.search.DefaultSearchRepository
import com.ssafy.modera.core.data.repository.search.RecentSearchRepository
import com.ssafy.modera.core.data.repository.search.SearchRepository
import com.ssafy.modera.core.data.repository.auth.AuthRepository
import com.ssafy.modera.core.data.repository.auth.DefaultAuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindsAuthRepository(
        impl: DefaultAuthRepository,
    ): AuthRepository

    @Binds
    abstract fun bindsCategoryRepository(
        impl: DefaultCategoryRepository,
    ): CategoryRepository

    @Binds
    abstract fun bindsImageRepository(
        impl: DefaultImageRepository,
    ): ImageRepository

    @Binds
    abstract fun bindsAnalyzedImageRepository(
        impl: DefaultAnalyzedImageRepository,
    ): AnalyzedImageRepository

    @Binds
    abstract fun bindsSearchRepository(
        impl: DefaultSearchRepository,
    ): SearchRepository

    @Binds
    abstract fun bindsRecentSearchRepository(
        impl: DefaultRecentSearchRepository,
    ): RecentSearchRepository

    @Binds
    abstract fun bindsDeviceCalendarRepository(
        impl: DefaultDeviceCalendarRepository,
    ): DeviceCalendarRepository

    @Binds
    abstract fun bindsDocumentRepository(
        impl: DefaultDocumentRepository,
    ): DocumentRepository

    @Binds
    abstract fun bindsCalendarRepository(
        impl: DefaultCalendarRepository,
    ): CalendarRepository

    @Binds
    abstract fun bindsPushTokenRepository(
        impl: DefaultPushTokenRepository,
    ): PushTokenRepository
}
