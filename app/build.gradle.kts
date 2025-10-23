import io.gitlab.arturbosch.detekt.Detekt

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    kotlin("kapt") // Используем kapt вместо KSP
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.example.lets_go_slavgorod"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.lets_go_slavgorod"
        minSdk = 24
        targetSdk = 35
        versionCode = 10081
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        multiDexEnabled = true
        
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
        
        // BuildConfig поля для конфигурации
        buildConfigField("String", "GITHUB_REPO_OWNER", "\"VseMirka200\"")
        buildConfigField("String", "GITHUB_REPO_NAME", "\"lets-go-slavgorod\"")
        buildConfigField("String", "GITHUB_API_URL", "\"https://api.github.com/repos/VseMirka200/lets-go-slavgorod/releases/latest\"")
        buildConfigField("long", "UPDATE_CHECK_INTERVAL_HOURS", "1L")
        buildConfigField("long", "UPDATE_CACHE_TTL_HOURS", "24L")
    }

    signingConfigs {
        create("release") {
            // Проверяем environment variables для production
            val keystorePath = System.getenv("KEYSTORE_PATH")
            val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
            val keyAlias = System.getenv("KEY_ALIAS")
            val keyPassword = System.getenv("KEY_PASSWORD")
            
            if (keystorePath != null && keystorePassword != null && keyAlias != null && keyPassword != null) {
                // Production: используем environment variables
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            } else {
                // Development: используем debug keystore без предупреждения
                storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
                storePassword = "android"
                this.keyAlias = "androiddebugkey"
                this.keyPassword = "android"
            }
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            isJniDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // Оптимизации для размера APK
            ndk {
                debugSymbolLevel = "none"
            }
            
            // Оптимизация ресурсов
            packaging {
                resources {
                    excludes += setOf(
                        "META-INF/**",
                        "kotlin/**",
                        "**.properties",
                        "**.bin"
                    )
                }
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    
    // Фильтрация локалей для уменьшения размера APK
    androidResources {
        localeFilters += setOf("ru")
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    
    kotlinOptions {
        jvmTarget = "11"
        
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xcontext-receivers"
        )
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE*",
                "/META-INF/NOTICE*"
            )
        }
    }
}

dependencies {
    // Core
    implementation(libs.androidx.multidex)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    
    // Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    
    // Compose UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    
    // Utilities
    implementation(libs.timber)
    
    // Dependency Injection
    implementation("io.insert-koin:koin-android:3.5.3")
    implementation("io.insert-koin:koin-androidx-compose:3.5.3")
    
    // HTTP Client with Caching
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Security - шифрование данных
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // WorkManager - фоновые задачи
    implementation(libs.androidx.work.runtime)
    testImplementation("androidx.work:work-testing:2.9.0")
    
    // Baseline Profile
    implementation(libs.androidx.profileinstaller)
    
    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    
    // Desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    
    // Detekt
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.7")
}

// Конфигурация Detekt
detekt {
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    buildUponDefaultConfig = true
    allRules = false
    autoCorrect = false
}