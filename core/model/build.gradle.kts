plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(libs.javax.inject)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.paging.common)
    testImplementation(libs.junit)
}
