# Adding a core or feature module

`core/` and `feature/` are Gradle module trees. Each folder under them is one module, registered in `settings.gradle.kts`. `:app` is the only module that depends on every feature.

## What goes where

**`core/`** is shared infrastructure. A core module is reused by many screens.

| Module | Kind | Owns |
|--------|------|------|
| `:core:model` | Kotlin JVM | Domain types, repository interfaces, business rules |
| `:core:network` | Android library | Ktor APIs, DTOs |
| `:core:database` | Android library | Room, sync, DataStore |
| `:core:designsystem` | Android library | Theme, Inter, `LedgerOneText` and the other shared components |
| `:core:mock` | Kotlin JVM | Demo catalog and mock backends |
| `:core:testing` | Kotlin JVM | Shared test samples |

**`feature/`** is one product area per folder. A feature owns its screens and view models. It may depend on core modules. It never depends on another feature. That rule is checked by `ModuleBoundaryTest`.

```
:app
  → every :feature:*
  → every :core:*

:feature:*
  → :core:model
  → :core:designsystem
  → :core:database / :core:network / :core:mock only when that feature needs them
  → never another :feature:*

:core:network, :core:database, :core:mock, :core:testing
  → :core:model
```

`:core:model` stays a JVM module. Keep Android, Compose, Room, and Ktor out of it.

## 1. Make a core module

### Kotlin-only (same shape as `:core:model`)

```bash
mkdir -p core/rules/src/main/kotlin/com/ledger/personal
```

`core/rules/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:model"))
}
```

### Android library (same shape as `:core:network`)

```bash
mkdir -p core/analytics/src/main/kotlin/com/ledger/personal
```

`core/analytics/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.ledger.personal.analytics"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(project(":core:model"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
```

Register it in `settings.gradle.kts`:

```kotlin
include(":core:rules")
```

Wire it from the modules that need it. `:app` is the usual first consumer:

```kotlin
implementation(project(":core:rules"))
```

Put types and interfaces in `:core:model` first when more than one module will share them. Network and Room implementations stay in `:core:network` and `:core:database`.

## 2. Make a feature module

```bash
mkdir -p feature/cards/src/main/kotlin/com/ledger/personal/ui/feature
```

`feature/cards/build.gradle.kts` (copy `:feature:wallet`, then add Hilt only if the screen has a `@HiltViewModel`):

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.ledger.personal.feature.cards"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(libs.kotlinx.coroutines.android)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation("androidx.compose.foundation:foundation")
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
```

Add Hilt the same way `:feature:wallet` does when the feature has a view model that Hilt constructs:

```kotlin
alias(libs.plugins.hilt)
alias(libs.plugins.ksp)
// ...
implementation(libs.hilt.android)
implementation(libs.androidx.hilt.navigation.compose)
ksp(libs.hilt.compiler)
```

Register it in `settings.gradle.kts`:

```kotlin
include(":feature:cards")
```

Add it only in `app/build.gradle.kts`:

```kotlin
implementation(project(":feature:cards"))
```

Leave other feature `build.gradle.kts` files unchanged. `ModuleBoundaryTest` fails if one feature lists another.

## 3. Put the code in the right files

Inside the feature:

- `Cards.kt` — screen state (`CardsUiState`)
- `CardsViewModel.kt` — the view model
- `CardsScreen.kt` — Compose UI

Use `LedgerOneText`, `LedgerOneButton`, `FeatureScaffold`, and theme colours. Hex colours (`Color(0x…`) in `feature/**` fail `ModuleBoundaryTest`.

In `:app`:

- add a `Screen.Cards` route
- compose it in `LedgerNavHost`
- keep flavor switches and Hilt bindings in `:app` (`RepositoryModule`, `TeamModule`, `DemoModule`)

## 4. Sync and check

1. Sync Gradle, or compile `:app:compileMockDebugKotlin`.
2. `./gradlew :app:testMockDebugUnitTest --tests com.ledger.personal.ModuleBoundaryTest`
3. `:app:detekt` if you touched `:app`.
