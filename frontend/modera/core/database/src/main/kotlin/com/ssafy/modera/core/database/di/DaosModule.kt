package com.ssafy.modera.core.database.di

import com.ssafy.modera.core.database.ModeraDatabase
import com.ssafy.modera.core.database.dao.DocumentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object DaosModule {

    @Provides
    fun providesDocumentDao(
        database: ModeraDatabase,
    ): DocumentDao =
        database.documentDao()
}