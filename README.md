# LedgerOne (Personal Ledger)

LedgerOne is an Android banking and personal-finance app. It tracks your transactions, sends and requests money by UPI, manages cards, loans and deposits, pays bills, and keeps working offline.

It is written in Kotlin with Jetpack Compose, Hilt, Room, WorkManager and Ktor.

By default the app runs fully offline on built-in demo data, so you can clone it and run it without a backend.

## Quick start

Requirements: Android Studio (recent), JDK 17, an emulator or phone on Android 8.0 (API 26) or later.

```bash
git clone <this repo>
cd "Personal ledger"
./gradlew :app:installMockDebug
```

Or open the folder in Android Studio, pick the `mockDebug` build variant, and press Run.

In the mock build:

- the sign-up code is `123456`;
- you choose your own 4-digit MPIN during onboarding, and `1111` also unlocks this build;
- Profile → **Mock controls** lets you simulate a slow network, server errors, going offline, empty data, or a sync conflict.

## Build variants

One flavor dimension, `environment`. Debug and release stay build types: debug logs HTTP and adds `.debug` to the application id; release is minified and does not.

| Variant | Application id | API | Data |
|---------|----------------|-----|------|
| `mockDebug` (default) | `com.ledger.personal.mock.debug` | `http://10.0.2.2:8080/retail/v1/` | In-app mock backend |
| `devDebug` / `devRelease` | `com.ledger.personal.dev` (plus `.debug` on debug) | `https://dev-api.personal-ledger.com/retail/v1/` | Real API |
| `stagingDebug` / `stagingRelease` | `com.ledger.personal.staging` (plus `.debug` on debug) | `https://staging-api.personal-ledger.com/retail/v1/` | Real API |
| `prodDebug` / `prodRelease` | `com.ledger.personal` (plus `.debug` on debug) | `https://api.personal-ledger.com/retail/v1/` | Real API |

`mock` is the only flavor on the mock backend. Dev, staging, and prod call their own host. The four apps install side by side, each with its own launcher name and app-link host.

Certificate pinning is wired up, but `PIN_SHA256` and `PIN_SHA256_BACKUP` in `app/build.gradle.kts` are empty on purpose. Fill them with your server's real SPKI hashes before shipping a `prod` release.

## Force update

Force update applies only when all of these are true:

- the launch call to `GET static/version` succeeds;
- that response includes a minimum `versionCode`;
- this install's `versionCode` is **strictly less** than that minimum (`current < required`).

It does **not** apply when the version call fails or omits a minimum (a downed static API must not lock people out), or when this install is equal to or newer than the published minimum.

When it applies, `LedgerOneApp` shows **Update required** and nothing else: onboarding, login, and the tabs stay blocked. **Update now** opens the Play listing for this application id (the `.debug` suffix is stripped so a debug build still points at the store app).

| Light | Dark |
| --- | --- |
| <img src="docs/images/force-update-light.png" width="240" alt="Force update, light"> | <img src="docs/images/force-update-dark.png" width="240" alt="Force update, dark"> |

Raise the published `versionCode` only when a build must be retired — broken crypto, a payment bug, or an unsupported Play services floor. Regular store updates do not need a force update.

## What's in the app

The app has five tabs:

- **Home:** balance card (hidden until you tap the eye), money in and out this month, quick actions, recent transactions, and banners for offline mode and sync conflicts.
- **Money:** accounts, wallet, cards (freeze, limits), loans with an EMI calculator, fixed deposits, and monthly statements you can download as a PDF.
- **Pay:** scan a UPI QR code, send money, split a bill, request money, manage beneficiaries, and pay bills.
- **Insights:** budgets, charts, FX rate alerts, and a live market ticker.
- **Assistant:** answers questions about your money from the ledger on your phone; your questions never leave it.

**Profile** has KYC, the security centre (MPIN, biometrics, device binding), notifications, settings (theme, privacy, language), transaction categories, your disputes, help, and the mock controls.

From any transaction you can edit it, delete it, split it with friends, or report a problem (raise a dispute).

## Project layout

```
app/                 App shell: navigation, flavors, DI wiring, and ui/app (auth, hubs, account, scan, tools)
core/model/          Plain Kotlin: domain types, repository interfaces, business rules
core/network/        Ktor API, DTOs, auth interceptor, certificate pinning, ticker socket
core/database/       Room database, offline-first repository, sync worker, DataStore settings
core/designsystem/   Theme, colours, money formatting, shared Compose components
core/mock/           Mock backend, seeded demo catalog, mock switches
core/testing/        Shared test samples
feature/<name>/      One module per feature: ledger, budgets, fx, kyc, wallet,
                     ticker, assistant, pos, banking
docs/                Architecture document and decision records
```

Feature modules never depend on each other; `ModuleBoundaryTest` fails the build if one does. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for how the pieces fit together.
See [docs/MODULES.md](docs/MODULES.md) to add a `core/` or `feature/` module.
See [docs/LIBRARIES.md](docs/LIBRARIES.md) for the dependency choices and comparisons with alternatives.
See [docs/POS-CRYPTO.md](docs/POS-CRYPTO.md) for why electricity bill lookup and payment use ECDH with AES-GCM, and when other algorithms are the better tool.

## Tests and checks

```bash
./gradlew testMockDebugUnitTest            # app unit tests (mock flavor)
./gradlew :core:model:test :core:mock:test :core:designsystem:testDebugUnitTest
./gradlew detekt ktlintCheck lintMockDebug # static analysis
./gradlew connectedMockDebugAndroidTest    # Room, migration and sync tests (needs an emulator)
```

CI (`.github/workflows/android.yml`) runs on every push to `main` and every pull request into `main`. It runs static analysis, the app mock unit tests, the core module unit tests, and assembles the `mockDebug` APK. A failing test fails the job.

## Contributing

- Put a new screen in its feature module. If two features need the same code, move it to a `core` module; don't add a dependency between features.
- Store money as a `Long` in minor units (paise or cents), never as a `Double`. Display it with `formatMoney`.
- Feature code uses theme colours (`MaterialTheme.colorScheme`, `LedgerTheme.colors`). Raw `Color(0x…)` values belong in `core/designsystem` only.
- Every new screen should have a mock path, so the `mock` flavor keeps running without a backend.
- Shortcuts that are deliberately minimal are marked with a `ponytail:` comment explaining what a full version needs.
