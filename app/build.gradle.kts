plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "com.ledger.personal"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ledger.personal"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        resourceConfigurations += listOf("en", "hi")
        buildConfigField("String", "PIN_SHA256", "\"\"")
        buildConfigField("String", "PIN_SHA256_BACKUP", "\"\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    flavorDimensions += "environment"
    productFlavors {
        create("mock") {
            dimension = "environment"
            isDefault = true
            applicationIdSuffix = ".mock"
            versionNameSuffix = "-mock"
            manifestPlaceholders["appName"] = "Personal Ledger Mock"
            manifestPlaceholders["appLinkHost"] = "mock.app.personal-ledger.com"
            buildConfigField("String", "APP_LINK_HOST", "\"mock.app.personal-ledger.com\"")
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/retail/v1/\"")
            buildConfigField("String", "TICKER_URL", "\"ws://10.0.2.2:8080/retail/v1/ticks\"")
            buildConfigField("String", "PAYMENTS_BASE_URL", "\"http://10.0.2.2:8091/v1/\"")
            buildConfigField("String", "PROMOTIONS_BASE_URL", "\"http://10.0.2.2:8094/v1/\"")
        }
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            manifestPlaceholders["appName"] = "Personal Ledger Dev"
            manifestPlaceholders["appLinkHost"] = "dev.app.personal-ledger.com"
            buildConfigField("String", "APP_LINK_HOST", "\"dev.app.personal-ledger.com\"")
            buildConfigField("String", "API_BASE_URL", "\"https://dev-api.personal-ledger.com/retail/v1/\"")
            buildConfigField("String", "TICKER_URL", "\"wss://dev-api.personal-ledger.com/retail/v1/ticks\"")
            buildConfigField("String", "PAYMENTS_BASE_URL", "\"https://dev-payments.personal-ledger.com/v1/\"")
            buildConfigField("String", "PROMOTIONS_BASE_URL", "\"https://dev-promotions.personal-ledger.com/v1/\"")
        }
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            manifestPlaceholders["appName"] = "Personal Ledger Staging"
            manifestPlaceholders["appLinkHost"] = "staging.app.personal-ledger.com"
            buildConfigField("String", "APP_LINK_HOST", "\"staging.app.personal-ledger.com\"")
            buildConfigField("String", "API_BASE_URL", "\"https://staging-api.personal-ledger.com/retail/v1/\"")
            buildConfigField("String", "TICKER_URL", "\"wss://staging-api.personal-ledger.com/retail/v1/ticks\"")
            buildConfigField("String", "PAYMENTS_BASE_URL", "\"https://staging-payments.personal-ledger.com/v1/\"")
            buildConfigField("String", "PROMOTIONS_BASE_URL", "\"https://staging-promotions.personal-ledger.com/v1/\"")
        }
        create("prod") {
            dimension = "environment"
            manifestPlaceholders["appName"] = "Personal Ledger"
            manifestPlaceholders["appLinkHost"] = "app.personal-ledger.com"
            buildConfigField("String", "APP_LINK_HOST", "\"app.personal-ledger.com\"")
            buildConfigField("String", "API_BASE_URL", "\"https://api.personal-ledger.com/retail/v1/\"")
            buildConfigField("String", "TICKER_URL", "\"wss://api.personal-ledger.com/retail/v1/ticks\"")
            buildConfigField("String", "PAYMENTS_BASE_URL", "\"https://payments.personal-ledger.com/v1/\"")
            buildConfigField("String", "PROMOTIONS_BASE_URL", "\"https://promotions.personal-ledger.com/v1/\"")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            buildConfigField("Boolean", "ENABLE_HTTP_LOGGING", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("Boolean", "ENABLE_HTTP_LOGGING", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
        checkDependencies = true
        disable += listOf("GradleDependency", "AndroidGradlePluginVersion", "OldTargetApi")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    sourceSets {
        getByName("androidTest").assets.srcDir("$rootDir/core/database/schemas")
    }
}

kotlin {
    jvmToolchain(17)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/detekt.yml"))
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:mock"))
    implementation(project(":feature:ledger"))
    implementation(project(":feature:wallet"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:scan"))
    implementation(project(":feature:update"))

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // AndroidX & Architecture
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode)
    implementation(libs.androidx.biometric)

    // Networking: Retrofit with the OkHttp engine
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.kotlinx.serialization)

    // Kotlinx Serialization & Coroutines
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Logging & Debug Tooling
    implementation(libs.timber)
    debugImplementation(libs.leakcanary)

    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(platform(libs.okhttp.bom))
    testImplementation(libs.okhttp.core)
    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.paging.testing)

    // Instrumented Testing
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.work.testing)
}
