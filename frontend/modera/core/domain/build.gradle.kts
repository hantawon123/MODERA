plugins {
    alias(libs.plugins.modera.android.library)
    alias(libs.plugins.modera.hilt)
}

android {
    namespace = "com.ssafy.modera.core.domain"
}

dependencies {
    api(projects.core.data)
    api(projects.core.model)
}
