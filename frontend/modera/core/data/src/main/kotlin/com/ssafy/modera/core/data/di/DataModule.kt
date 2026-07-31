package com.ssafy.modera.core.data.di

import com.ssafy.modera.core.data.repository.AnalyzedImageRepository
import com.ssafy.modera.core.data.repository.CategoryRepository
import com.ssafy.modera.core.data.repository.DefaultAnalyzedImageRepository
import com.ssafy.modera.core.data.repository.DefaultCategoryRepository
import com.ssafy.modera.core.data.repository.DefaultImageRepository
import com.ssafy.modera.core.data.repository.ImageRepository
import com.ssafy.modera.core.data.repository.calendar.DefaultDeviceCalendarRepository
import com.ssafy.modera.core.data.repository.calendar.DeviceCalendarRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

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
    abstract fun bindsDeviceCalendarRepository(
        impl: DefaultDeviceCalendarRepository,
    ): DeviceCalendarRepository
}
