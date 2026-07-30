plugins {
    alias(libs.plugins.modera.android.library)
    alias(libs.plugins.modera.android.library.compose)
}

android {
    namespace = "com.ssafy.modera.core.ui"
}

dependencies {
    api(projects.core.designsystem)
    api(projects.core.model)

    implementation(libs.androidx.core.ktx)
    implementation(libs.coil.kt)
    implementation(libs.coil.kt.compose)
}
