import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// local.properties는 커밋하지 않는다 (CLAUDE.md §16).
// 없어도 빌드는 되어야 하므로 빈 문자열로 폴백한다.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun localProp(key: String): String = localProperties.getProperty(key) ?: ""

android {
    namespace = "com.ai_builder_hackathon.gttgtt"
    // androidx 1.19.0 / lifecycle 2.11.0 이 compileSdk 37 이상을 요구한다.
    // compileSdk(컴파일 시 쓸 수 있는 API)와 targetSdk(런타임 동작 옵트인)는 별개로 올린다.
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.ai_builder_hackathon.gttgtt"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // anon 키는 앱에 포함되어도 안전하다. RLS가 경계이기 때문 (CLAUDE.md §5.1).
        // Upstage 키는 절대 여기 두지 않는다 (§5.4).
        buildConfigField("String", "SUPABASE_URL", "\"${localProp("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProp("SUPABASE_ANON_KEY")}\"")
        // Google 네이티브 로그인의 serverClientId (Web 클라이언트 ID)
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${localProp("GOOGLE_WEB_CLIENT_ID")}\"")
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
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // ViewModel / 상태 수집
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // 직렬화
    implementation(libs.kotlinx.serialization.json)

    // 사진 EXIF (촬영일 추출)
    implementation(libs.androidx.exifinterface)

    // 이미지
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // 글래스모피즘(배경 블러) — 모든 Haze 사용은 ui/component/HazeGlass.kt 에 격리
    implementation(libs.haze)

    // Supabase
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.storage)
    implementation(libs.supabase.functions)
    implementation(libs.supabase.compose.auth)
    implementation(libs.ktor.client.okhttp)

    // Google 네이티브 로그인 (Credential Manager) — compose-auth 가 내부에서 호출한다.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
