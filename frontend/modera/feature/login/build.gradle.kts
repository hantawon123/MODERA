plugins {
    id("modera.android.feature.impl")
    id("modera.android.hilt")
    alias(libs.plugins.kotlinx.serialization)
}

android {
    namespace = "com.ssafy.modera.feature.login"
}

dependencies {
    implementation(libs.androidx.foundation.layout)
    implementation(libs.kakao.user)
}
