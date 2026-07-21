package com.ssafy.modera.core.common.network

import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME

@Qualifier
@Retention(RUNTIME)
annotation class Dispatcher(val moderaDispatcher: ModeraDispatcher)

enum class ModeraDispatcher {
    Default,
    IO,
}