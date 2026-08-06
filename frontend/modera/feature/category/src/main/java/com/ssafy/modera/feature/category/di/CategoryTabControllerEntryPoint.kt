package com.ssafy.modera.feature.category.di

import com.ssafy.modera.feature.category.CategoryTabController
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CategoryTabControllerEntryPoint {
    fun categoryTabController(): CategoryTabController
}
