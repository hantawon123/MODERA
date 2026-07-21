plugins {
    alias(libs.plugins.modera.jvm.library)
    alias(libs.plugins.modera.hilt)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}