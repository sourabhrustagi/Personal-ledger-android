# Library choices

The version catalog at `gradle/libs.versions.toml` is the single source of dependency versions. This document explains why each dependency family exists. A library is added only when the Android or Kotlin standard library does not already solve the problem.

## Language and asynchronous work

### Kotlin

Kotlin is the primary language because Android tooling, Compose, coroutines and Ktor are Kotlin-first. It gives the project null safety, sealed types, extension functions and concise immutable models.

**Instead of Java:** Java remains interoperable, but it needs more boilerplate for the same state and asynchronous flows. Kotlin is the supported first-class language for modern Android.

### Kotlin coroutines and Flow

Coroutines run database, network and cryptographic work without blocking the main thread. `Flow` carries Room rows, preferences, sync state and UI state as cancellable streams.

**Instead of RxJava:** coroutines are integrated into Kotlin, Android lifecycle APIs, Room, WorkManager and Ktor. RxJava is still capable, but would add a second asynchronous type system and a larger API surface.

### kotlinx.serialization

The same compile-time serializers encode and decode Ktor request and response DTOs and type-safe navigation routes. It avoids reflection and keeps release builds friendly to R8.

**Instead of Gson:** Gson is reflection-based and less strict about nullability. **Instead of Moshi:** Moshi is a strong Android option, but kotlinx.serialization is native to Kotlin and is already required by navigation and Ktor.

## Networking

### Ktor Client

Ktor owns REST calls, static configuration calls, device binding and the market WebSocket. The project uses typed request bodies, `ContentNegotiation`, kotlinx.serialization, structured cancellation and one client configuration per trust boundary:

- the authenticated client may attach the session and supports WebSockets;
- the public static client sends no session token and uses a disk cache;
- both clients share timeouts, certificate pinning, redacted diagnostics and flavor-specific base URLs.

**Instead of Retrofit:** Retrofit is excellent when an annotation-only REST interface is the whole networking requirement. LedgerOne also needs WebSockets, separate public and authenticated clients, custom static caching and Kotlin-native request pipelines. Ktor handles those in one coroutine-based client instead of combining Retrofit for REST with a separate WebSocket abstraction. The trade-off is that Ktor request code is more explicit than Retrofit annotations.

Ktor `3.3.3` is used because it is the latest stable patch compatible with this project's Kotlin 2.2 toolchain. Newer Ktor lines require a newer Kotlin compiler and should be adopted together with that compiler, not independently.

### OkHttp engine

Ktor runs on the OkHttp engine on Android. OkHttp provides mature connection pooling, TLS, certificate pinning, HTTP caching and reliable Android networking. Its BOM keeps the core and logging artifacts on one version. Version 5.4.0 is the newest release this project can use: 5.5.0 requires compile SDK 37, and Android Gradle Plugin 9.0.1 supports compile SDK 36.

**Instead of Ktor CIO:** CIO is useful for multiplatform consistency. OkHttp is preferred for this Android-only app because certificate pinning, Chucker integration, persistent HTTP cache behavior and Android TLS support are already proven here. Ktor remains the public networking API, so changing the engine later does not affect repositories.

### Chucker

Debug builds use Chucker to inspect requests on the device. Authorization and cookie headers are redacted. Release builds receive the no-op artifact, so the inspector cannot collect production traffic.

**Instead of Flipper or Stetho:** Chucker needs no desktop process and is focused on OkHttp traffic. Flipper is broader but adds desktop tooling and more integration surface; Stetho is no longer the preferred modern Android inspector.

### OkHttp logging interceptor

Debug builds log only request and response summary lines. Headers and bodies are excluded, and sensitive headers are redacted. Release logging is disabled.

**Instead of body logging:** financial payloads can contain personal data. Summary logging gives useful timing and status information without copying transaction bodies into Logcat.

## Storage and background work

### Room

Room is the local source of truth for transactions, sync state, POS queues, KYC drafts and assistant history. It provides compile-time SQL checks, migrations, transactions and `Flow`.

**Instead of raw SQLite:** Room removes cursor and schema boilerplate while retaining SQLite reliability. **Instead of SQLDelight:** SQLDelight is strong for multiplatform projects; Room has tighter Android, Paging and WorkManager integration for this Android-only app.

### DataStore Preferences

DataStore stores small user settings such as theme, privacy options and category names using transactional, asynchronous updates.

**Instead of SharedPreferences:** DataStore avoids synchronous disk access and lost-update problems. Structured relational data still belongs in Room.

### WorkManager

WorkManager schedules durable transaction sync and alert work under network constraints, with retries surviving process death and device restart.

**Instead of a foreground service or AlarmManager:** those APIs are appropriate for user-visible continuous work or exact alarms. Ledger sync is deferrable guaranteed work, which is WorkManager's intended use case.

### Paging 3

Paging loads large transaction histories incrementally from Room and integrates directly with Compose.

**Instead of manual page state:** Paging already handles invalidation, cancellation, loading states and backpressure, reducing custom state and edge cases.

## UI and lifecycle

### Jetpack Compose and Material 3

Compose builds screens from immutable state and reusable functions. Material 3 supplies accessible controls, typography and interaction behavior.

**Instead of XML views:** XML views remain supported, but Compose gives this state-driven app less adapter and view-binding boilerplate and integrates directly with `StateFlow`.

### Navigation Compose

Navigation Compose provides the back stack, deep links and serializable type-safe routes while the app module remains the only module that joins feature screens.

**Instead of string routes or a third-party router:** typed routes remove manual argument parsing and use the same lifecycle model as Compose.

### Lifecycle ViewModel

ViewModels own screen state across configuration changes, and lifecycle-aware collection stops unnecessary UI observation in the background.

**Instead of storing state in activities:** ViewModels separate Android recreation from business state and are directly supported by Hilt and Compose.

## Dependency injection

### Hilt

Hilt creates repositories, databases, workers, Ktor clients and ViewModels with compile-time generated graphs. Qualifiers distinguish authenticated and public clients.

**Instead of manual service locators:** Hilt validates missing bindings at build time and scopes process-level resources correctly. **Instead of Koin:** Koin is concise and useful, but runtime graph failures are less desirable for a modular financial app than Hilt's compile-time validation.

## Device capabilities

### CameraX

CameraX supplies lifecycle-aware camera preview and image analysis for KYC and QR capture.

**Instead of Camera2:** Camera2 gives lower-level control but requires substantially more device and lifecycle handling that these screens do not need.

### ML Kit barcode scanning

ML Kit decodes QR formats from CameraX frames on-device.

**Instead of maintaining a decoder:** it handles camera rotation, image formats and device optimization. ZXing is a valid lighter alternative, but ML Kit integrates directly with Android image objects and supports the existing analyzer pipeline.

### AndroidX Biometric

BiometricPrompt provides a consistent system UI and capability checks while private keys remain in Android Keystore.

**Instead of custom fingerprint UI:** applications cannot safely reproduce system biometric authentication or its security guarantees.

## Diagnostics

### Timber

Timber centralizes application logging and plants a debug tree only in debug builds.

**Instead of direct `Log` calls:** tags and release behavior remain consistent without repeating build checks at every call site.

### LeakCanary

LeakCanary automatically detects retained activities, fragments and Compose hosts in debug builds.

**Instead of manual heap checks:** automatic detection shortens feedback during development. Android Studio profiling remains useful for deeper investigations.

## Testing and quality

### JUnit 4, AndroidX Test, Espresso and Compose UI Test

JUnit runs local business-rule tests. AndroidX Test supplies instrumented runners, Espresso covers Android interactions, and Compose UI Test understands semantics rather than screen coordinates. Ktor's `MockEngine` checks HTTP paths and serialization without a server.

### Detekt and ktlint

Detekt enforces complexity and correctness rules; ktlint provides deterministic Kotlin formatting.

**Instead of review-only style enforcement:** automated checks are repeatable in CI and prevent style discussions from hiding behavioral review.

## Build tooling

### Android Gradle Plugin and Kotlin Gradle plugins

AGP owns Android variants, resources, manifests, packaging, lint and R8. Kotlin's Android, serialization and Compose compiler plugins compile the Kotlin-specific language features used by the app.

### KSP

KSP runs Hilt and Room code generation using Kotlin symbols.

**Instead of kapt:** KSP avoids Java stub generation and is generally faster and more accurate for Kotlin source. Its version must remain compatible with the Kotlin compiler.

## Dependency policy

Use stable releases that are compatible with the repository's Kotlin, AGP and Gradle versions. “Latest” is not sufficient when a library was compiled with newer Kotlin metadata. Upgrade the compiler and its dependent plugins as one tested change, then move libraries that require that compiler. Avoid adding overlapping libraries—for example, a second JSON parser, database, dependency-injection container or HTTP client—unless the existing choice has a measured gap.
