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
    ":core:testing",
    ":core:mock",
    ":feature:ledger",
    ":feature:budgets",
    ":feature:fx",
    ":feature:kyc",
    ":feature:wallet",
    ":feature:ticker",
    ":feature:assistant",
    ":feature:pos",
    ":feature:banking",
)
