plugins {
    id("modera.android.feature")
    id("modera.android.hilt")
    alias(libs.plugins.kotlinx.serialization)
}

android {
    namespace = "com.ssafy.modera.feature.home"
}

dependencies{
    implementation(libs.coil.kt)
    implementation(libs.coil.kt.compose)
}
