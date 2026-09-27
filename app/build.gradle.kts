plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.mirzadev.onecenter"
    compileSdk {
        version = release(37)
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            storeFile = file(
                project.findProperty("RELEASE_STORE_FILE") as String
            )
            storePassword =
                project.findProperty("RELEASE_STORE_PASSWORD") as String
            keyAlias =
                project.findProperty("RELEASE_KEY_ALIAS") as String
            keyPassword =
                project.findProperty("RELEASE_KEY_PASSWORD") as String
        }
    }

    defaultConfig {
        applicationId = "com.mirzadev.onecenter"
        minSdk = 26
        targetSdk = 37
        versionCode = 5
        versionName = "1.5.8"

        buildConfigField(
            "String",
            "GITHUB_TOKEN",
            "\"${project.findProperty("GITHUB_TOKEN") ?: ""}\""
        )
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    buildTypes {
        debug {
            val digestDebug = (project.findProperty("EXPECTED_SIGNING_DIGEST_DEBUG") as? String)?.trim('"') ?: ""
            buildConfigField(
                "String",
                "EXPECTED_SIGNING_DIGEST",
                "\"$digestDebug\""
            )
            // Optional, defensive-in-depth native library file digests.
            // Format: "name1:hexdigest1,name2:hexdigest2". Empty by default
            // -> NativeLibraryIntegrity reports NOT_CONFIGURED for every
            // library and nothing is gated on it. See AppIntegrity.kt.
            val libraryDigestsDebug = (project.findProperty("EXPECTED_LIBRARY_DIGESTS_DEBUG") as? String)?.trim('"') ?: ""
            buildConfigField(
                "String",
                "EXPECTED_LIBRARY_DIGESTS",
                "\"$libraryDigestsDebug\""
            )
        }
        release {
            signingConfig = signingConfigs.getByName("release")

            val digestRelease = (project.findProperty("EXPECTED_SIGNING_DIGEST_RELEASE") as? String)?.trim('"') ?: ""
            buildConfigField(
                "String",
                "EXPECTED_SIGNING_DIGEST",
                "\"$digestRelease\""
            )
            val libraryDigestsRelease = (project.findProperty("EXPECTED_LIBRARY_DIGESTS_RELEASE") as? String)?.trim('"') ?: ""
            buildConfigField(
                "String",
                "EXPECTED_LIBRARY_DIGESTS",
                "\"$libraryDigestsRelease\""
            )
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation("com.google.firebase:firebase-auth:24.2.0")
    implementation("com.google.firebase:firebase-database:22.0.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    implementation("dev.chrisbanes.haze:haze:1.6.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
}
