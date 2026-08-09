plugins {
    id("modera.android.feature.impl")
    id("modera.android.hilt")
    alias(libs.plugins.kotlinx.serialization)
}

android {
    namespace = "com.ssafy.modera.feature.category"
}

dependencies {
    implementation(libs.androidx.foundation.layout)
    implementation(libs.coil.kt)
    implementation(libs.coil.kt.compose)
    implementation(libs.androidx.activity.compose)

    implementation(projects.core.model)
}
