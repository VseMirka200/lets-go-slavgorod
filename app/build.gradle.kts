import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.Project

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.detekt)
}

android {
    namespace = "ru.slavgorod.transport"
    compileSdk = 36

    defaultConfig {
        applicationId = "ru.slavgorod.transport"
        minSdk = 24
        targetSdk = 36
        versionCode = 30002
        versionName = "3.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }

    }

    androidResources {
        localeFilters += listOf("ru")
    }

    signingConfigs {
        create("release") {
            val releaseSigningCredentials = project.resolveReleaseSigningCredentials()
            if (releaseSigningCredentials != null) {
                storeFile = releaseSigningCredentials.storeFile
                storePassword = releaseSigningCredentials.storePassword
                keyAlias = releaseSigningCredentials.keyAlias
                keyPassword = releaseSigningCredentials.keyPassword
            } else if (project.isReleaseSigningRequested()) {
                throw GradleException(
                    "Release signing configuration is missing. " +
                        "Set KEYSTORE_PATH/KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD " +
                        "or android.injected.signing.* before assembling a release build."
                )
            } else {
                storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
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

            ndk {
                debugSymbolLevel = "none"
            }

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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
    }

    lint {
        checkReleaseBuilds = true
        abortOnError = true
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

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn"
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.timber)

    implementation(libs.koinAndroid)
    implementation(libs.koinAndroidxCompose)

    implementation(libs.okhttp)
    implementation(libs.gson)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

}

detekt {
    config.setFrom(rootProject.layout.projectDirectory.file("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = false
    autoCorrect = false
}


private data class ReleaseSigningCredentials(
    val storeFile: File,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String
)

private fun Project.isReleaseSigningRequested(): Boolean {
    return gradle.startParameter.taskNames.any { taskName ->
        taskName.contains("release", ignoreCase = true) &&
            (
                taskName.contains("assemble", ignoreCase = true) ||
                    taskName.contains("bundle", ignoreCase = true) ||
                    taskName.contains("install", ignoreCase = true) ||
                    taskName.contains("package", ignoreCase = true) ||
                    taskName.contains("publish", ignoreCase = true)
                )
    }
}

private fun Project.resolveReleaseSigningCredentials(): ReleaseSigningCredentials? {
    val injectedStoreFilePath = providers.gradleProperty("android.injected.signing.store.file").orNull
    val injectedStorePassword = providers.gradleProperty("android.injected.signing.store.password").orNull
    val injectedKeyAlias = providers.gradleProperty("android.injected.signing.key.alias").orNull
    val injectedKeyPassword = providers.gradleProperty("android.injected.signing.key.password").orNull
    val envKeystorePath = System.getenv("KEYSTORE_PATH")
    val envKeystorePassword = System.getenv("KEYSTORE_PASSWORD")
    val envKeyAlias = System.getenv("KEY_ALIAS")
    val envKeyPassword = System.getenv("KEY_PASSWORD")

    val configuredStorePath = injectedStoreFilePath ?: envKeystorePath
    val configuredStorePassword = injectedStorePassword ?: envKeystorePassword
    val configuredKeyAlias = injectedKeyAlias ?: envKeyAlias
    val configuredKeyPassword = injectedKeyPassword ?: envKeyPassword
    val configuredStoreFile = configuredStorePath
        ?.takeUnless { it.isBlank() }
        ?.let { file(it) }

    val hasValidReleaseSigningConfig =
        configuredStoreFile != null &&
            configuredStoreFile.exists() &&
            !configuredStorePassword.isNullOrBlank() &&
            !configuredKeyAlias.isNullOrBlank() &&
            !configuredKeyPassword.isNullOrBlank()

    return if (hasValidReleaseSigningConfig) {
        ReleaseSigningCredentials(
            storeFile = configuredStoreFile!!,
            storePassword = configuredStorePassword!!,
            keyAlias = configuredKeyAlias!!,
            keyPassword = configuredKeyPassword!!
        )
    } else {
        null
    }
}
