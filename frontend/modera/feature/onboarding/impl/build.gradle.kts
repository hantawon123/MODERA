plugins {
    id("modera.android.feature.impl")
    id("modera.android.hilt")
}

android {
    namespace = "com.ssafy.modera.feature.onboarding.impl"
}

dependencies {
    implementation(projects.feature.onboarding.api)

    implementation(libs.lottie.compose)
    implementation(libs.coil.kt)
    implementation(libs.coil.kt.compose)
}
