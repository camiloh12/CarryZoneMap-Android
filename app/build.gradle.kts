import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    id("jacoco")
}

// Read API key from local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { stream ->
        localProperties.load(stream)
    }
}

android {
    namespace = "com.carryzonemap.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.carryzonemap.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Expose the API key as a BuildConfig field
        buildConfigField("String", "MAPTILER_API_KEY", "\"${localProperties.getProperty("MAPTILER_API_KEY") ?: ""}\"")
        buildConfigField("String", "MAPBOX_ACCESS_TOKEN", "\"${localProperties.getProperty("MAPBOX_ACCESS_TOKEN") ?: ""}\"")
        buildConfigField("String", "STADIA_API_KEY", "\"${localProperties.getProperty("STADIA_API_KEY") ?: ""}\"")

        // Supabase configuration
        buildConfigField("String", "SUPABASE_URL", "\"${localProperties.getProperty("SUPABASE_URL") ?: ""}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProperties.getProperty("SUPABASE_ANON_KEY") ?: ""}\"")
    }

    signingConfigs {
        // Release signing config - supports both local.properties and environment variables
        // Environment variables take precedence (for CI/CD)
        val keystoreFile = System.getenv("KEYSTORE_FILE") ?: localProperties.getProperty("KEYSTORE_FILE")
        val keystorePassword = System.getenv("KEYSTORE_PASSWORD") ?: localProperties.getProperty("KEYSTORE_PASSWORD")
        val keyAlias = System.getenv("KEY_ALIAS") ?: localProperties.getProperty("KEY_ALIAS")
        val keyPassword = System.getenv("KEY_PASSWORD") ?: localProperties.getProperty("KEY_PASSWORD")

        if (!keystoreFile.isNullOrEmpty()) {
            create("release") {
                storeFile = file(keystoreFile)
                storePassword = keystorePassword
                keyAlias = keyAlias
                keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            // Only use release signing if it was configured
            val keystoreFile = System.getenv("KEYSTORE_FILE") ?: localProperties.getProperty("KEYSTORE_FILE")
            if (!keystoreFile.isNullOrEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            // Different package name so debug and release can coexist
            applicationIdSuffix = ".debug"
            // Different app name to distinguish in launcher
            versionNameSuffix = "-DEBUG"
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true // Enable BuildConfig generation
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

// JaCoCo configuration for code coverage
jacoco {
    toolVersion = "0.8.12"
}

tasks.withType<Test> {
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

// Custom task for generating code coverage reports
// This task generates reports from the JaCoCo execution data
tasks.register<JacocoReport>("jacocoTestReport") {
    // Note: This task should be run after testDebugUnitTest has already run
    // Run with: ./gradlew testDebugUnitTest jacocoTestReport --continue
    // The --continue flag ensures coverage report is generated even if some tests fail

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    val fileFilter = listOf(
        // Android framework
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",

        // Hilt/Dagger generated code
        "**/*_HiltModules*",
        "**/*_Factory*",
        "**/*_MembersInjector*",
        "hilt_aggregated_deps/**",
        "dagger/hilt/**",
        "**/di/**",

        // Room generated code
        "**/*_Impl*",
        "**/*Database*_Impl*",

        // Compose generated code
        "**/ComposableSingletons*",
        "**/*\$\$*",

        // Data Transfer Objects (DTOs) - simple data classes
        "**/dto/**",

        // Application class
        "**/CarryZoneApplication*",

        // UI Screens and Composables (require UI tests, not unit tests)
        "**/ui/MapScreen*",
        "**/ui/auth/LoginScreen*",
        "**/ui/auth/SignUpScreen*",
        "**/ui/components/PinDialog*",
        "**/MainActivity*",

        // Legacy map code (minimal logic, being phased out)
        "**/map/FeatureLayerManager*"
    )

    val debugTree = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }

    val mainSrc = "${project.projectDir}/src/main/java"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(layout.buildDirectory) {
        include("jacoco/testDebugUnitTest.exec")
    })
}

// Task for coverage verification with minimum thresholds
tasks.register<JacocoCoverageVerification>("jacocoCoverageVerification") {
    dependsOn("jacocoTestReport")

    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal() // 80% minimum coverage
            }
        }

        rule {
            enabled = true
            element = "CLASS"

            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.70".toBigDecimal() // 70% per class
            }

            excludes = listOf(
                // Exclude DTOs and simple data classes
                "com.carryzonemap.app.data.remote.dto.*",

                // Exclude UI composables (hard to unit test)
                "com.carryzonemap.app.ui.MapScreen*",
                "com.carryzonemap.app.ui.auth.*Screen*",
                "com.carryzonemap.app.ui.components.*",

                // Exclude Application class
                "com.carryzonemap.app.CarryZoneApplication",

                // Exclude generated code
                "*.*_Factory",
                "*.*_HiltModules*",
                "*.*_Impl"
            )
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material-icons-extended-android:1.6.8")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // MapLibre Android SDK (no API key required with demo style)
    implementation("org.maplibre.gl:android-sdk:11.8.0")

    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.51")
    ksp("com.google.dagger:hilt-compiler:2.51")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")

    // Supabase
    implementation("io.github.jan-tennert.supabase:postgrest-kt:3.0.1")
    implementation("io.github.jan-tennert.supabase:realtime-kt:3.0.1")
    implementation("io.github.jan-tennert.supabase:auth-kt:3.0.1")
    implementation("io.github.jan-tennert.supabase:storage-kt:3.0.1")

    // Ktor for networking (required by Supabase)
    implementation("io.ktor:ktor-client-android:3.0.1")
    implementation("io.ktor:ktor-client-core:3.0.1")
    implementation("io.ktor:ktor-utils:3.0.1")

    // Kotlinx Serialization (required by Supabase)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    // WorkManager for background sync
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("org.json:json:20240303") // Provides a JVM implementation for org.json
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("app.cash.turbine:turbine:1.1.0") // For Flow testing
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.robolectric:robolectric:4.13") // Android framework for unit tests
    testImplementation("androidx.test:core:1.6.1") // For ApplicationProvider
    testImplementation("io.ktor:ktor-client-mock:3.0.1") // MockEngine for testing HTTP clients
    testImplementation("io.ktor:ktor-client-content-negotiation:3.0.1") // For JSON serialization in tests
    testImplementation("io.ktor:ktor-serialization-kotlinx-json:3.0.1") // JSON serialization

    // Instrumentation Testing
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.51")
    kspAndroidTest("com.google.dagger:hilt-compiler:2.51")
}
