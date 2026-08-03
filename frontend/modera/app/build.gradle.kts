plugins {
    alias(libs.plugins.modera.android.application)
    alias(libs.plugins.modera.android.application.compose)
    alias(libs.plugins.modera.hilt)
    alias(libs.plugins.kotlinx.serialization)
}

android {
    namespace = "com.ssafy.modera"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.ssafy.modera"
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }

        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(libs.androidx.compose.adaptive)
    implementation(projects.core.common)
    implementation(projects.core.designsystem)
    implementation(projects.core.data)
    implementation(projects.core.model)
    implementation(projects.core.navigation)

    implementation(projects.feature.splash)
    implementation(projects.feature.home)
    implementation(projects.feature.category)
    implementation(projects.feature.favorite)
    implementation(projects.feature.calendar)
    implementation(projects.feature.analyzedimagedetail)
    implementation(projects.feature.imageviewer)
    implementation(projects.feature.document)
    implementation(projects.feature.documentdetail)
    implementation(projects.feature.documentcreate)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.androidx.lifecycle.viewModel.navigation3)
    implementation(libs.androidx.hilt.lifecycle.viewModelCompose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)

    // material3
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.compose.material3.adaptive.navigation)
    implementation(libs.androidx.compose.material3.adaptive.navigation3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.material3.windowSizeClass)

    // ML Kit OCR
    implementation(libs.text.recognition)
    implementation(libs.text.recognition.korean)

    ksp(libs.hilt.compiler)

    testImplementation(libs.hilt.android)
    testImplementation(libs.kotlin.test)

    androidTestImplementation(libs.hilt.android.testing)
}