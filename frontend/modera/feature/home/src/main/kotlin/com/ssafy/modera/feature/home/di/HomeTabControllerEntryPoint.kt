package com.ssafy.modera.feature.home.di

import com.ssafy.modera.feature.home.HomeTabController
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HomeTabControllerEntryPoint {
    fun homeTabController(): HomeTabController
}
