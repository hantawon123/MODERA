plugins {
    id("modera.android.feature.impl")
    id("modera.android.hilt")
    alias(libs.plugins.kotlinx.serialization)
}

android {
    namespace = "com.ssafy.modera.feature.imageviewer"
}

dependencies {
    implementation(libs.coil.kt)
    implementation(libs.coil.kt.compose)
}
