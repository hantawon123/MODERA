// Gradle의 일부 API가 아직 안정화되지 않았다는 경고를 숨김
// 예: enableFeaturePreview, repositoriesMode 같은 설정에서 경고가 뜰 수 있음
@file:Suppress("UnstableApiUsage")

// Gradle의 Type-safe Project Accessors 기능 활성화
// 기존: implementation(project(":core:data"))
// 사용 가능: implementation(projects.core.data)
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Gradle 플러그인을 어디서 가져올지 설정하는 영역
// 예: com.android.application, org.jetbrains.kotlin.android, hilt plugin 등
pluginManagement {

    // build-logic 폴더를 포함 빌드로 등록
    // convention plugin을 직접 만들어서 사용할 수 있음
    // 예: id("offpick.android.application")
    includeBuild("build-logic")

    repositories {
        // Android / Google 계열 플러그인을 가져오는 저장소
        google {
            content {
                // androidx 계열 플러그인/아티팩트 허용
                // 예: androidx.room, androidx.navigation 등
                includeGroupByRegex("androidx\\..*")

                // Android Gradle Plugin 허용
                // 예: com.android.application, com.android.library
                includeGroupByRegex("com\\.android(\\..*|)")

                // Google Android 계열 허용
                // 예: com.google.android.libraries.maps 등
                includeGroupByRegex("com\\.google\\.android\\..*")

                // Google Play Services / Firebase Gradle Plugin 허용
                // 예: com.google.gms.google-services
                includeGroupByRegex("com\\.google\\.gms(\\..*|)")
            }

            // snapshot 버전은 받지 않고 release 버전만 사용
            mavenContent {
                releasesOnly()
            }
        }
        // Maven Central 저장소
        // 여기서는 플러그인 중 com.google.dagger 그룹만 허용
        mavenCentral {
            content {
                // Hilt Gradle Plugin 쪽에서 필요
                // 예: com.google.dagger:hilt-android-gradle-plugin
                includeGroup("com.google.dagger")
            }

            // release 버전만 사용
            mavenContent {
                releasesOnly()
            }
        }

        // Gradle Plugin Portal
        // Kotlin Gradle Plugin, 기타 외부 Gradle 플러그인을 찾는 저장소
        // 예: org.jetbrains.kotlin.android
        gradlePluginPortal()
    }
}

// 일반 라이브러리 의존성을 어디서 가져올지 설정하는 영역
// 예: implementation(libs.androidx.core.ktx)
// 예: implementation(libs.hilt.android)
// 예: implementation(libs.firebase.messaging)
dependencyResolutionManagement {

    // 각 모듈의 build.gradle.kts 안에서 repositories {} 선언을 금지
    // 저장소는 settings.gradle.kts에서만 관리하게 강제
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        // Android / Google 계열 라이브러리를 가져오는 저장소
        google {
            content {
                // AndroidX 라이브러리 허용
                // 예: androidx.core:core-ktx
                // 예: androidx.activity:activity-compose
                // 예: androidx.navigation:navigation-compose
                includeGroupByRegex("androidx\\..*")

                // Android Gradle 관련 아티팩트 허용
                includeGroupByRegex("com\\.android(\\..*|)")

                // Google Android 계열 라이브러리 허용
                // 예: maps, credentials 일부 Google Android 라이브러리
                includeGroupByRegex("com\\.google\\.android\\..*")

                // Firebase 라이브러리 허용
                // 예: com.google.firebase:firebase-messaging
                // 예: com.google.firebase:firebase-auth
                includeGroupByRegex("com\\.google\\.firebase(\\..*|)")

                // Google Play Services 라이브러리 허용
                // 예: com.google.android.gms:play-services-maps
                // 예: com.google.android.gms:play-services-location
                includeGroupByRegex("com\\.google\\.gms(\\..*|)")

                // ML Kit (bundled) 허용
                includeGroup("com.google.mlkit")

                // LiteRT-LM
                includeGroupByRegex("com\\.google\\.ai\\.edge(\\..*|)")
            }

            // release 버전만 사용
            mavenContent {
                releasesOnly()
            }
        }
        // Maven Central 저장소
        // Kotlin, Coroutine, Retrofit, OkHttp, Hilt 등 대부분의 일반 라이브러리가 여기서 내려옴
        mavenCentral {
            // snapshot 버전 제외, release 버전만 사용
            mavenContent {
                releasesOnly()
            }
        }
        maven {
            url = uri("https://devrepo.kakao.com/nexus/content/groups/public/")
            content {
                includeGroup("com.kakao.sdk")
            }
            mavenContent {
                releasesOnly()
            }
        }
    }
}

rootProject.name = "modera"
include(":app")
include(":core:model")
include(":core:network")
include(":core:database")
include(":core:datastore")
include(":core:datastore-proto")
include(":core:data")
include(":core:navigation")
include(":core:designsystem")
include(":core:common")
include(":core:domain")
include(":core:ui")

include(":feature:splash")
include(":feature:home")
include(":feature:analyzedimage:detail")
include(":feature:analyzedimage:related")
include(":feature:imageviewer")
include(":feature:category")
include(":feature:favorite")
include(":feature:calendar")
include(":feature:document")
include(":feature:document:documents")
include(":feature:document:documentedit")
include(":feature:document:documentdetail")
include(":feature:document:documentcreate")
include(":feature:login")
include(":feature:settings")
include(":sync:work")