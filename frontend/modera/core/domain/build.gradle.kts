plugins {
    alias(libs.plugins.modera.android.library)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.ssafy.modera.core.domain"
}

dependencies {
    api(projects.core.data)
    api(projects.core.model)
}
