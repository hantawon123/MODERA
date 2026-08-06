plugins {
    id("modera.android.feature.impl")
    alias(libs.plugins.kotlinx.serialization)
}

android {
    namespace = "com.ssafy.modera.feature.analyzedimage.related"
}

dependencies {
    implementation(projects.feature.analyzedimage.api)

    implementation(libs.coil.kt)
    implementation(libs.coil.kt.compose)
}
