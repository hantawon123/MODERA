plugins {
    alias(libs.plugins.modera.android.library)
    alias(libs.plugins.modera.hilt)
}

android {
    namespace = "com.ssafy.modera.core.datastore"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    api(projects.core.common)
    api(projects.core.model)
    api(projects.core.datastoreProto)

    implementation(libs.androidx.dataStore)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
