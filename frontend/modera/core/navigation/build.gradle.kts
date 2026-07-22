plugins {
    alias(libs.plugins.modera.android.library)
    alias(libs.plugins.modera.android.library.compose)
    alias(libs.plugins.modera.hilt)
    alias(libs.plugins.kotlinx.serialization)
}

android {
    namespace = "com.ssafy.modera.core.navigation"
    buildFeatures {
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.material)
    implementation(projects.core.model)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // Navigation3
    implementation(libs.androidx.lifecycle.viewModel.navigation3)
    api(libs.androidx.navigation3.runtime)
    api(libs.androidx.navigation3.ui)

    // json parsing
    implementation(libs.kotlinx.serialization.json)
}