plugins {
    `kotlin-dsl`
}

group = "com.ssafy.modera.buildlogic"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplicationCompose") {
            id = libs.plugins.modera.android.application.compose.get().pluginId
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }

        register("androidApplication") {
            id = libs.plugins.modera.android.application.asProvider().get().pluginId
            implementationClass = "AndroidApplicationConventionPlugin"
        }

        register("androidLibraryCompose") {
            id = libs.plugins.modera.android.library.compose.get().pluginId
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }

        register("androidLibrary") {
            id = libs.plugins.modera.android.library.asProvider().get().pluginId
            implementationClass = "AndroidLibraryConventionPlugin"
        }

        register("androidFeatureApi") {
            id = libs.plugins.modera.android.feature.api.get().pluginId
            implementationClass = "AndroidFeatureApiConventionPlugin"
        }

        register("androidFeatureImpl") {
            id = libs.plugins.modera.android.feature.impl.get().pluginId
            implementationClass = "AndroidFeatureImplConventionPlugin"
        }

        register("androidRoom") {
            id = libs.plugins.modera.android.room.get().pluginId
            implementationClass = "AndroidRoomConventionPlugin"
        }

        register("androidHilt") {
            id = libs.plugins.modera.hilt.get().pluginId
            implementationClass = "HiltConventionPlugin"
        }

        register("jvmLibrary") {
            id = "modera.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}