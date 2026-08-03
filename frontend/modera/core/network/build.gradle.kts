import java.util.Properties

plugins {
    alias(libs.plugins.modera.android.library)
    alias(libs.plugins.modera.hilt)
    alias(libs.plugins.kotlinx.serialization)
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { load(it) }
    }
}
val accessToken: String = localProperties.getProperty("accessToken", "")

android {
    namespace = "com.ssafy.modera.core.network"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "ACCESS_TOKEN", "\"$accessToken\"")
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.datastore)

    implementation(libs.sandwich)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlin.serialization)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.arch.core.testing)
}
