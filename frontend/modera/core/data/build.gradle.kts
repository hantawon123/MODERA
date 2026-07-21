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

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.serialization.json)
}
