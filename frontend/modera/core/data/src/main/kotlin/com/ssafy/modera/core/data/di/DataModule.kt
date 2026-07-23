package com.ssafy.modera.core.data.di

import com.ssafy.modera.core.data.repository.CategoryRepository
import com.ssafy.modera.core.data.repository.DefaultCategoryRepository
import com.ssafy.modera.core.data.repository.DefaultImageRepository
import com.ssafy.modera.core.data.repository.ImageRepository
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
}
