plugins {
    alias(libs.plugins.android.application)
    // 🌟 Firebase와 통신하기 위한 통행증(플러그인) 추가!
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.gymlog"
    compileSdk = 36 // KTS 문법 오류 방지를 위해 정수형으로 수정 권장 (기존 코드에 맞춰 유지 시 에러가 없다면 두셔도 됩니다)

    defaultConfig {
        applicationId = "com.example.gymlog"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)

    // 예쁜 커스텀 점을 찍기 위한 달력 라이브러리 추가
    implementation("com.github.prolificinteractive:material-calendarview:1.4.3")

    // 🌟 Firebase BoM 및 Firestore 라이브러리 추가 (KTS 문법으로 수정: 큰따옴표 및 괄호 사용)
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-firestore")

    // 🌟 Firebase 인증 및 구글 로그인 라이브러리 추가
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
}