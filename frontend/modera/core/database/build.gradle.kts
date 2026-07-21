plugins {
    alias(libs.plugins.modera.android.library)
    alias(libs.plugins.modera.android.room)
    alias(libs.plugins.modera.hilt)
}

android {
    namespace = "com.ssafy.modera.core.database"
}

dependencies {
    api(projects.core.model)

    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}