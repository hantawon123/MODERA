plugins {
    id("modera.android.feature")
    id("modera.android.hilt")
}

android {
    namespace = "com.ssafy.modera.feature.categoryimages"
}

dependencies {
    implementation(libs.coil.kt)
    implementation(libs.coil.kt.compose)
}
