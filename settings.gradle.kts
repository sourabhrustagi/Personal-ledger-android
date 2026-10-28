pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PersonalLedger"
include(
    ":app",
    ":core:model",
    ":core:network",
    ":core:database",
    ":core:designsystem",
    ":core:ui",
    ":core:testing",
    ":core:mock",
    ":feature:ledger",
    ":feature:ledger:demo",
    ":feature:wallet",
    ":feature:wallet:demo",
    ":feature:auth",
    ":feature:auth:demo",
    ":feature:scan",
    ":feature:scan:demo",
    ":feature:update",
    ":feature:update:demo",
)
