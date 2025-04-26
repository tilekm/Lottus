plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "kz.tilek.lottus"
    compileSdk = 35

    defaultConfig {
        applicationId = "kz.tilek.lottus"
        minSdk = 31
        targetSdk = 35
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.material.v1110)
    implementation(libs.androidx.constraintlayout.v214)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Retrofit для сетевых запросов
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // OkHttp для настройки клиента и логгирования запросов
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Coroutines для асинхронных операций
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // LiveData и ViewModel
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.glide)


    // RxJava (часто используется со StompProtocolAndroid)
    implementation(libs.rxjava)
    implementation(libs.rxandroid)

    // Lifecycle KTX для lifecycleScope
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // https://mvnrepository.com/artifact/com.github.NaikSoftware/StompProtocolAndroid
    implementation(libs.stompprotocolandroid)

    implementation(libs.androidx.material3)
    implementation(libs.androidx.material3.window.size.class1)
    implementation(libs.androidx.material3.adaptive.navigation.suite)

    implementation(libs.glide) // Проверь последнюю версию
    // ksp("com.github.bumptech.glide:compiler:4.16.0") // Если используешь аннотации Glide

    // Для Activity Result API (выбор изображений)
    implementation(libs.androidx.activity.ktx) // Проверь последнюю версию
    implementation(libs.androidx.fragment.ktx) // Проверь последнюю версию

    // ViewPager2 для галереи в деталях
    implementation(libs.androidx.viewpager2) // Проверь последнюю версию

    // Индикатор для ViewPager2 (точки) - опционально
     implementation(libs.dotsindicator) // Проверь последнюю версию

}