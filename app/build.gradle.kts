import java.security.KeyStore

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.auraplayer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.auraplayer"
        minSdk = 26
        targetSdk = 35
        versionCode = 37
        versionName = "2.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("daveConfig") {
            val keystoreFile = file("dave.keystore")
            if (keystoreFile.exists()) {
                val detectedAlias = try {
                    val ks = KeyStore.getInstance("PKCS12")
                    keystoreFile.inputStream().use { ks.load(it, "daveplayer".toCharArray()) }
                    ks.aliases().toList().firstOrNull() ?: "davekey"
                } catch (_: Exception) {
                    "davekey"
                }
                storeFile = keystoreFile
                storePassword = "daveplayer"
                keyAlias = detectedAlias
                keyPassword = "daveplayer"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            val daveSign = signingConfigs.getByName("daveConfig")
            if (daveSign.storeFile != null) {
                signingConfig = daveSign
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            val daveSign = signingConfigs.getByName("daveConfig")
            if (daveSign.storeFile != null) {
                signingConfig = daveSign
            }
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Media3 (ExoPlayer, Session, UI)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)

    // Coil Image Loading
    implementation(libs.coil.compose)

    // NewPipeExtractor (YouTube Stream Extractor) & OkHttp
    implementation("com.github.teamnewpipe:NewPipeExtractor:v0.26.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
