package com.ssafy.modera.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import com.ssafy.modera.core.datastore.recentSearchesDataStore
import com.ssafy.modera.core.datastore.proto.RecentSearches
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun providesRecentSearchesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<RecentSearches> = context.recentSearchesDataStore
}
