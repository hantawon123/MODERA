plugins {
    alias(libs.plugins.modera.android.library)
    alias(libs.plugins.modera.hilt)
}

android {
    namespace = "com.ssafy.modera.core.data"
}

dependencies {
    api(projects.core.common)
    api(projects.core.database)
    api(projects.core.datastore)
    api(projects.core.network)

    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
}
