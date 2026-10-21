pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
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
    ":feature:wallet",
    ":feature:ticker",
    ":feature:banking",
)
