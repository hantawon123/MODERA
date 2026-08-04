plugins {
    alias(libs.plugins.modera.android.library)
    alias(libs.plugins.modera.hilt)
}

android {
    namespace = "com.ssafy.modera.sync.work"
}

dependencies {
    implementation(projects.core.data)

    implementation(libs.androidx.work.ktx)
    implementation(libs.hilt.ext.work)
    ksp(libs.hilt.ext.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.work.testing)
}