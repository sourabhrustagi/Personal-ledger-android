plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.androidx.paging.common)
    implementation(libs.kotlinx.coroutines.android)
    implementation("javax.inject:javax.inject:1")
    testImplementation(libs.junit)
}
