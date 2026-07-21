plugins {
    alias(libs.plugins.modera.android.library)
    alias(libs.plugins.modera.android.library.compose)
    alias(libs.plugins.modera.hilt)
    alias(libs.plugins.kotlinx.serialization)
}

android {
    namespace = "com.ssafy.modera.core.navigation"
}

dependencies {
    implementation(projects.core.model)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // Navigation3
    api(libs.androidx.navigation3.runtime)
    api(libs.androidx.navigation3.ui)

    // json parsing
    implementation(libs.kotlinx.serialization.json)
}