plugins {
    id("modera.android.feature")
    id("modera.android.hilt")
    alias(libs.plugins.kotlinx.serialization)
}

android {
    namespace = "com.ssafy.modera.feature.home"
}

dependencies {
    implementation(libs.androidx.foundation.layout)
    implementation(projects.feature.categoryimages)
    implementation(libs.coil.kt)
    implementation(libs.coil.kt.compose)
    implementation(libs.androidx.activity.compose)
}
