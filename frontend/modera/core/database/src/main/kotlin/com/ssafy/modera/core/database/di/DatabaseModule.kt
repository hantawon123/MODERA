package com.ssafy.modera.core.database.di

import android.content.Context
import androidx.room.Room
import com.ssafy.modera.core.database.ModeraDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    @Singleton
    fun providesModeraDatabase(
        @ApplicationContext context: Context,
    ): ModeraDatabase =
        Room.databaseBuilder(
            context = context,
            klass = ModeraDatabase::class.java,
            name = "modera-database",
        ).build()
}