import java.net.URI

plugins {
    alias(libs.plugins.modera.android.library)
    alias(libs.plugins.modera.hilt)
    alias(libs.plugins.kotlinx.serialization)
}

android {
    namespace = "com.ssafy.modera.core.network"

    // Supply through -P or environment variables; private endpoints stay outside Git.
    defaultConfig {
        fun endpoint(name: String, fallback: String): String {
            val raw = providers.gradleProperty(name)
                .orElse(providers.environmentVariable(name)).orElse(fallback).get().trim()
            val uri = URI(raw)
            require(uri.scheme in listOf("http", "https") && uri.host != null &&
                uri.userInfo == null && uri.query == null && uri.fragment == null) {
                "$name must be an absolute HTTP(S) URL without credentials, query or fragment"
            }
            return "\"" + raw.trimEnd('/') + "/\""
        }
        buildConfigField("String", "API_BASE_URL", endpoint("API_BASE_URL", "https://api.example.com/"))
        buildConfigField("String", "MEDIA_BASE_URL", endpoint("MEDIA_BASE_URL", "https://storage.example.com/"))
    }


    buildFeatures {
        buildConfig = true
    }
}

dependencies {
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
