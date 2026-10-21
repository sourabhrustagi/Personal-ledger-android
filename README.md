# LedgerOne (Personal Ledger)

LedgerOne is an Android banking and personal-finance app. It tracks your transactions, sends and requests money by UPI, manages cards, loans and deposits, and keeps working offline.

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

- the sign-up and sign-in code is `123456` (`MockValues.OTP`);
- Profile → **Mock controls** lets you simulate a slow network, server errors, going offline, empty data, or a sync conflict.

## Build flavors

The project defines one flavor dimension, `environment`, with four distinct flavors, combined with `debug` and `release` build types:

| Flavor | Variant | Application id | API Endpoint | Purpose & Data Source |
|---|---|---|---|---|
| `mock` | `mockDebug` (default) | `com.ledger.personal.mock.debug` | `http://10.0.2.2:8080/retail/v1/` | Fully in-memory & offline mock backend using seeded catalog; no network connection required |
| `dev` | `devDebug` / `devRelease` | `com.ledger.personal.dev` (plus `.debug` on debug) | `https://dev-api.personal-ledger.com/retail/v1/` | Development environment for active integration testing against real services |
| `staging` | `stagingDebug` / `stagingRelease` | `com.ledger.personal.staging` (plus `.debug` on debug) | `https://staging-api.personal-ledger.com/retail/v1/` | Pre-production QA environment for release qualification and staging tests |
| `prod` | `prodDebug` / `prodRelease` | `com.ledger.personal` (plus `.debug` on debug) | `https://api.personal-ledger.com/retail/v1/` | Live production banking infrastructure |

All four flavor apps can be installed side by side on the same device or emulator because each has a unique package name. Debug builds enable HTTP inspection (Chucker), Timber debug logging, and attach `.debug` to the application id. Release builds enable R8 shrinking and ProGuard optimization.

Certificate pinning is pre-configured, with `PIN_SHA256` and `PIN_SHA256_BACKUP` in `app/build.gradle.kts` to be filled with server SPKI hashes before releasing to `prod`.

## Mock values & demo credentials

All mock values and seed catalogs are centralized in `:core:mock` so they can be easily reviewed, edited, or extended in one place without touching UI or business logic files:

### Central constants (`MockValues.kt`)

Defined in [`core/mock/src/main/kotlin/com/ledger/personal/mock/MockValues.kt`](core/mock/src/main/kotlin/com/ledger/personal/mock/MockValues.kt):

- **SMS OTP Verification Code (`MockValues.OTP`):** `123456` — used on Onboarding and OTP Login screens.
- **Universal Demo MPIN (`MockValues.MPIN`):** `1111` — used for MPIN operations in Security centre.
- **Default User Name (`MockValues.DEFAULT_NAME`):** `Asha Rao` — used for demo profiles and previews.
- **Default Phone Number (`MockValues.DEFAULT_PHONE`):** `9876543210` — used for onboarding phone input and previews.
- **Default Session Token (`MockValues.SESSION_TOKEN`):** `mock-session` — used for local authenticated requests.

### Central mock catalog (`MockCatalog.kt`)

Defined in [`core/mock/src/main/kotlin/com/ledger/personal/mock/MockCatalog.kt`](core/mock/src/main/kotlin/com/ledger/personal/mock/MockCatalog.kt):

- **Bank Accounts:** Savings (`...4821`, balance ₹2,45,678.90), Salary (`...1093`, balance ₹86,420.15), and Fixed Deposit (`...7710`, balance ₹5,00,000.00).
- **Contacts & Payees:** `Asha Rao` (`asha@okaxis`), `Rahul Mehta` (`rahul.m@okicici`), `Priya Shah` (`priya@oksbi`), `Vikram Iyer` (`vikram@okhdfc`).
- **Utility Billers:** Electricity BESCOM (`₹500.00`), Airtel Postpaid (`₹799.00`), ACT Broadband (`₹1,179.00`), BWSSB Water (`₹320.00`).
- **Cards:** Savings Debit (Visa `...4821`), Rewards Credit (RuPay `...9034`).
- **Historical Transactions:** 200 seeded transactions across Groceries, Dining, Transport, Salary, Utilities, and Entertainment.

To customize test data, simply update the constants in `MockValues.kt` or records in `MockCatalog.kt`.

## Force update

Force update applies only when all of these are true:

- the launch call to `GET static/version` succeeds;
- that response includes a minimum `versionCode`;
- this install's `versionCode` is **strictly less** than that minimum (`current < required`).

It does **not** apply when the version call fails or omits a minimum (a downed static API must not lock people out), or when this install is equal to or newer than the published minimum.

When it applies, `LedgerOneApp` shows **Update required** and nothing else: onboarding, login, and the tabs stay blocked. **Update now** opens the Play listing for this application id (the `.debug` suffix is stripped so a debug build still points at the store app).

A soft update applies when `GET static/status` succeeds with `softUpdate: true` and a `versionCode` this install has not skipped, and force update does not apply. `AppStatus.softUpdate` is what shows **Update available**; onboarding, login, and the tabs stay usable. **Not now** remembers that `versionCode`. **Update** opens the same Play listing. A missing or failed status call shows nothing.

| Light | Dark |
| --- | --- |
| <img src="docs/images/force-update-light.png" width="240" alt="Force update, light"> | <img src="docs/images/force-update-dark.png" width="240" alt="Force update, dark"> |

Raise the published `versionCode` only when a build must be retired — broken crypto, a payment bug, or an unsupported Play services floor. Set `softUpdate` on `GET static/status` for a regular store update the person can skip.

## What's in the app

The app provides five primary navigation tabs:

- **Home:** current bank account balance card (masked by default, tap the eye icon to reveal), sync conflict alerts, and quick actions.
- **Pay:** scan & pay with camera QR scanner, and send to anyone by entering mobile number, UPI ID, choosing from device contacts, or paying utility bills.
- **Passbook:** real-time transaction history stream (`LedgerHistory`) with pagination, debit/credit indicators, category badges, and tap-to-view details.
- **Accounts:** user profile details (avatar, name, phone), settings & mock controls, app security lock, and logout.
- **Offers:** dynamic promotional shelves and marketing campaign banners.

From any transaction you can inspect its full metadata, double-entry legs, and sync state, or tap **Report a problem** to file a dispute recorded in the local dispute log. Transactions maintain an immutable ledger history to ensure accounting integrity.

Screens below are the light theme on mock data.

### Opening the app

| Welcome | Sign in |
| --- | --- |
| <img src="docs/images/onboarding.png" width="200" alt="Welcome"> | <img src="docs/images/login.png" width="200" alt="Sign in with OTP"> |

### Tabs

| Home | Pay | Offers |
| --- | --- | --- |
| <img src="docs/images/home.png" width="200" alt="Home"> | <img src="docs/images/pay.png" width="200" alt="Pay"> | <img src="docs/images/offers.png" width="200" alt="Offers"> |

### Products & Tools

| Send money | Cards | Loans |
| --- | --- | --- |
| <img src="docs/images/wallet.png" width="200" alt="Send money"> | <img src="docs/images/cards.png" width="200" alt="Cards"> | <img src="docs/images/loans.png" width="200" alt="Loans"> |

| Deposits | Statements | Transactions |
| --- | --- | --- |
| <img src="docs/images/deposits.png" width="200" alt="Deposits"> | <img src="docs/images/statements.png" width="200" alt="Statements"> | <img src="docs/images/transactions.png" width="200" alt="Transactions"> |

| Transaction | New transaction | Edit transaction |
| --- | --- | --- |
| <img src="docs/images/transaction.png" width="200" alt="Transaction"> | <img src="docs/images/new-transaction.png" width="200" alt="New transaction"> | <img src="docs/images/edit-transaction.png" width="200" alt="Edit transaction"> |

### Pay

| Scan |
| --- |
| <img src="docs/images/scan.png" width="200" alt="Scan a UPI QR code"> |

| Accounts and UPI | Report a problem | Your disputes |
| --- | --- | --- |
| <img src="docs/images/banking.png" width="200" alt="Accounts and UPI"> | <img src="docs/images/dispute.png" width="200" alt="Report a problem"> | <img src="docs/images/disputes.png" width="200" alt="Your disputes"> |

### Planning and markets

| Charts | Rate alerts |
| --- | --- |
| <img src="docs/images/charts.png" width="200" alt="Charts"> | <img src="docs/images/fx.png" width="200" alt="Rate alerts"> |

| Markets |
| --- |
| <img src="docs/images/markets.png" width="200" alt="Markets"> |

### Profile

| Profile | Security |
| --- | --- |
| <img src="docs/images/profile.png" width="200" alt="Profile"> | <img src="docs/images/security.png" width="200" alt="Security centre"> |

| Notifications | Settings | Categories |
| --- | --- | --- |
| <img src="docs/images/notifications.png" width="200" alt="Notifications"> | <img src="docs/images/settings.png" width="200" alt="Settings"> | <img src="docs/images/categories.png" width="200" alt="Categories"> |

| Help | Mock controls | Sync conflicts |
| --- | --- | --- |
| <img src="docs/images/help.png" width="200" alt="Help"> | <img src="docs/images/mock-controls.png" width="200" alt="Mock controls"> | <img src="docs/images/conflicts.png" width="200" alt="Sync conflicts"> |

## Project layout

```
app/                 App shell: navigation graph (Screen), build flavors, DI wiring, and ui/app (auth, hubs, account, scan, tools)
core/model/          Plain Kotlin: domain types (Transaction, Money), repository interfaces, business rules (Rules.kt)
core/network/        Ktor API, DTOs, auth interceptor, certificate pinning, ticker socket, device binding
core/database/       Room database, migrations, offline-first repository, sync worker, DataStore settings
core/designsystem/   Theme, colours, money formatting, shared Compose components (FeatureScaffold, keypad, banners)
core/ui/             Shared Compose UI effects and window security utilities (SecureWindow)
core/mock/           Mock backend, seeded demo catalog, mock switches
core/testing/        Shared test samples and fixtures
feature/<name>/      One module per feature: ledger, wallet, ticker, banking
docs/                Architecture document and decision records
```

Feature modules never depend on each other; `ModuleBoundaryTest` fails the build if one does. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for how the pieces fit together.
See [docs/MODULES.md](docs/MODULES.md) to add a `core/` or `feature/` module.

## Architecture & How the app works

The application is structured into a modular three-tier architecture (`:app` → `:feature:*` → `:core:*`) that enforces strict feature isolation and an **offline-first** reactive data flow:

```mermaid
flowchart TD
    UI[Compose Screen] -->|UI Events| VM[ViewModel]
    VM -->|Observe Flow| Repo[OfflineTransactionRepository]
    Repo -->|Local Read/Write| Room[(Room Database)]
    Room -.->|Live Updates| VM
    VM -.->|StateFlow| UI

    Worker[TransactionSyncWorker] -->|Sync Schedule| Repo
    Repo <-->|Push Pending / Pull Remote| Net[Ktor Remote / Mock Repository]
```

### 1. Offline-first local source of truth
- Every user write (transaction creation, draft changes, bill payments) is immediately persisted to the local **Room** database with a synchronization status: `SYNCED`, `PENDING_CREATE`, `PENDING_UPDATE`, or `PENDING_DELETE`.
- Screens never read directly from network endpoints. Jetpack Compose screens observe read-only `StateFlow` streams exposed by ViewModels, which in turn collect cold `Flow` emissions directly from Room DAOs. This guarantees instantaneous UI updates and seamless offline functionality.

### 2. Resilient background synchronization & conflict resolution
- **WorkManager** schedules `TransactionSyncWorker` with network constraints (`NetworkType.CONNECTED`).
- Pending local mutations are pushed with exponential backoff (`withRetry`). Network calls use unique, client-generated idempotency keys (`TransferBook`) to prevent duplicate transactions during retries.
- Remote changes are pulled and merged into Room without overwriting local un-synced edits. When a server responds with `409 Conflict`, the competing states are diverted into `ConflictInbox` for side-by-side user resolution.

### 3. Build flavor dependency injection
- Dagger Hilt dynamically binds repositories based on the build flavor:
  - `mockDebug`: uses in-memory `MockTransactionRepository` seeded with accounts, contacts, and transactions, alongside `MockSwitches` to simulate network latency, offline mode, and server errors.
  - `dev`, `staging`, and `prod`: inject `KtorTransactionRepository` connecting to remote HTTPS endpoints with TLS certificate pinning and hardware-backed device binding.

### 4. Multi-layer banking security
- **MPIN & Keystore:** MPIN keys are stretched with PBKDF2 (600,000 iterations) and sealed using Android Keystore keys.
- **Biometric Device Binding:** Devices register hardware-backed ECDSA P-256 keys in the Keystore, signing cryptographic server challenge nonces (`SHA256withECDSA`) upon biometric verification.
- **Window Protection (`SecureWindow`):** Sensitive screens dynamically block screenshot capture and recent-app previews using Android's `FLAG_SECURE`.

## View models

Screens bind to state-holding Jetpack `ViewModel`s scoped to the navigation back stack or the Activity lifecycle. State is exposed as read-only `StateFlow`s, and all asynchronous work runs structured in `viewModelScope`.

| Module | ViewModel | Responsibility |
| --- | --- | --- |
| `:app` | `AppViewModel` | Session lifecycle, force update, MPIN & biometric auth, device binding, and preferences |
| `:app` | `DemoViewModel` | Mock environment switches and connection status for testing |
| `:app` | `ToolsViewModel` | Database export/import, monthly statement PDF generation, and dispute logs |
| `:app` | `PromotionsViewModel` | Marketing promotional tabs and offer banners |
| `:feature:ledger` | `HomeViewModel` | Dashboard balances, recent transactions, pull-to-refresh sync, and dynamic layout tabs |
| `:feature:ledger` | `TransactionListViewModel` | Searchable, category-filtered, paginated transaction history via Paging 3 |
| `:feature:ledger` | `TransactionDetailViewModel` | Single transaction inspection, metadata loading, and retry |
| `:feature:ledger` | `CreateTransactionViewModel` | Transaction creation form validation and submission |
| `:feature:ledger` | `EditTransactionViewModel` | Transaction editing, draft state, and conflict handling |
| `:feature:ledger` | `LedgerHistoryViewModel` | Reactive audit log stream observing `TransactionDao` |
| `:feature:wallet` | `WalletViewModel` | P2P wallet transfers, step-up authentication, idempotency (`TransferBook`), and retry |
| `:feature:ticker` | `TickerViewModel` | Real-time WebSocket currency quote ticker stream |
| `:feature:banking` | `BankingViewModel` | Bank accounts, card controls, deposits, EMI loan amortization, and cooling period payees |

## State Management: `remember`, `rememberSaveable` & App State Architecture

State in this application represents any data that can change over time and causes the UI to update. Because Android components undergo configuration changes (e.g. device rotation, theme switching, language change) and system-initiated background process termination, the application manages state across a structured **4-tier hierarchy**:

```mermaid
flowchart TD
    subgraph UI ["Tier 1 & 2: Local UI State (Compose)"]
        R["remember: Ephemeral UI (animation clocks, current scroll offset)"]
        RS["rememberSaveable: Rotation & process-death safe UI (inputs, wizard step, sheet toggles)"]
    end

    subgraph VM ["Tier 3: Screen & Business State (ViewModel)"]
        SF["StateFlow: Read-only observable UI state stream"]
        SSH["SavedStateHandle: Navigation arguments surviving process death"]
    end

    subgraph Storage ["Tier 4: Durable Local Persistence"]
        Room[("Room Database: Offline-first single source of truth")]
        DS[("DataStore: Persistent user preferences")]
    end

    Room -->|Cold Flow| VM
    DS -->|Cold Flow| VM
    VM -->|collectAsStateWithLifecycle| UI
    UI -->|User Events & Intents| VM
    VM -->|Local Mutation Writes| Room
    VM -->|Preference Updates| DS
```

---

### 1. `remember` vs `rememberSaveable`

| Mechanism | Survives Recomposition? | Survives Device Rotation? | Survives Process Death? | Where Data Is Stored |
|---|---|---|---|---|
| **`remember { ... }`** | **Yes** | ❌ **No** (resets to initial) | ❌ **No** | Composition memory tree |
| **`rememberSaveable { ... }`** | **Yes** | **Yes** (restored) | **Yes** (restored) | Android `SavedStateRegistry` (`Bundle`) |

#### Why `remember` Alone Is Not Enough
In Compose, `remember { mutableStateOf(initialValue) }` keeps state alive across recompositions. However, when a user rotates their device or toggles dark mode, the host Android `Activity` is completely destroyed and recreated. All plain `remember` allocations are wiped out and reset to their initial value.

#### How `rememberSaveable` Works Under the Hood
`rememberSaveable` saves and restores values via Android's **`SavedStateRegistry`** (the same underlying mechanism as `onSaveInstanceState(Bundle)`):
1. **During execution:** Reads and mutates the in-memory state object like standard `remember`.
2. **On configuration change or background process termination:** Serializes the value into the Android `Bundle`.
3. **Upon recreation:** Restores the saved value from the `Bundle` instead of resetting to the default initializer.

> **Supported Types:** Primitives (`Int`, `Long`, `Float`, `Boolean`), `String`, `Bundle`, and `Parcelable` are supported automatically. Custom complex objects can be saved by supplying a custom Compose `Saver` (`rememberSaveable(stateSaver = ...)`).

---

### 2. Real-World Uses of `rememberSaveable` in LedgerOne

#### A. Transient Form Inputs & Typing Buffers
Users should not lose their input while filling forms if they rotate their screen before pressing submit.
- **Wallet transfer amount input (`WalletScreen.kt`):**
  ```kotlin
  var amountText by rememberSaveable { mutableStateOf(initialAmount) }
  ```
- **Quick recipient input (`Hubs.kt`):**
  ```kotlin
  var recipientInput by rememberSaveable { mutableStateOf("") }
  ```
- **Loan prepayment text buffer (`BankProducts.kt`):**
  ```kotlin
  var prepayText by rememberSaveable { mutableStateOf("") }
  ```

#### B. Multi-Step Wizard Progression
Prevents multi-step flows from resetting back to the very first step upon device rotation.
- **Onboarding flow step (`Onboarding.kt`):**
  ```kotlin
  var step by rememberSaveable { mutableStateOf(OnboardStep.PHONE) }
  var phone by rememberSaveable { mutableStateOf("") }
  ```

#### C. Bottom Sheet & Dialog Visibility Toggles
Ensures that open modals and confirmation dialogs remain open during configuration changes.
- **Banking modal sheet selection (`BankingScreen.kt`):**
  ```kotlin
  var sheet by rememberSaveable { mutableStateOf<BankSheet?>(null) }
  ```
- **Logout confirmation dialog (`Profile.kt`):**
  ```kotlin
  var confirmLogOut by rememberSaveable { mutableStateOf(false) }
  ```

#### D. Authentication Resend Countdown Timers
Preserves the remaining seconds on SMS OTP cooldowns across activity recreation (`Login.kt`):
```kotlin
var seconds by rememberSaveable { mutableIntStateOf(RESEND_SECONDS) }
```

#### E. Scroll Position & Viewport Form Access (`rememberScrollState`)
In Jetpack Compose, standard containers like `Column` and `Row` are **not scrollable by default**. If content exceeds the physical height of the screen, it is simply clipped unless a scroll modifier is attached.

`rememberScrollState()` creates and remembers a `ScrollState` instance to make layout columns scrollable via `Modifier.verticalScroll(rememberScrollState())`. Under the hood, Compose implements it directly on top of `rememberSaveable`:
```kotlin
@Composable
fun rememberScrollState(initial: Int = 0): ScrollState {
    return rememberSaveable(saver = ScrollState.Saver) {
        ScrollState(initial = initial)
    }
}
```

##### Why `rememberScrollState` is Critical in LedgerOne:
1. **Preventing Content Clipping Across Varied Screen Sizes:**
   Android runs on thousands of screen resolutions, aspect ratios, and split-screen multi-window modes. In `CreateTransactionScreen.kt` and `EditTransactionScreen.kt`, input forms contain amount fields, category chips, notes, and action buttons. Attaching `.verticalScroll(rememberScrollState())` guarantees that all content remains accessible regardless of screen height.
2. **On-Screen Keyboard (IME) Inset Adaptation:**
   When a user taps an input field to enter an amount, name, or phone number, the software keyboard rises, consuming over 40–50% of the display height. Combined with `Modifier.safeDrawingPadding()`, `rememberScrollState()` allows the container to compress and scroll smoothly, preventing primary submission buttons and text fields from being trapped behind the keyboard (`PhoneStep.kt`, `OtpStep.kt`, `Onboarding.kt`).
3. **Surviving Screen Rotation & Process Death:**
   Because `rememberScrollState()` delegates to `rememberSaveable` with `ScrollState.Saver`, the user's exact scroll offset (in pixels) is serialized into the Android `Bundle`. If a user rotates their device midway through filling out a transaction form or onboarding flow, their viewport position is seamlessly restored rather than resetting back to the top.
4. **Programmatic Scrolling & Observability:**
   `ScrollState` exposes both reactive properties (`value`, `maxValue`, `isScrollInProgress`) and suspend functions (`scrollTo(position)`, `animateScrollTo(position)`). This allows composables to observe user scrolling activity (e.g., dynamically elevating top app bars) and programmatically scroll to validation errors.
5. **Architectural Choice: `rememberScrollState` vs `rememberLazyListState`:**
   - **`rememberScrollState()` with `Column`:** Used for fixed-size, moderate-length layouts where all elements are known and should be kept in the composition simultaneously (forms, auth steps, modal dialogs, `FeatureScaffold`).
   - **`rememberLazyListState()` with `LazyColumn`:** Used for large or unbounded data streams (such as `TransactionListScreen` and `LedgerHistory`), where list items must be virtualized and recycled on-demand to avoid memory bloat.

##### Key Usages in LedgerOne:
- **Shared feature scaffold (`FeatureScaffold.kt`):**
  ```kotlin
  modifier = (if (scrollable) base.verticalScroll(rememberScrollState()) else base)
  ```
- **Transaction creation & editing forms (`CreateTransactionScreen.kt` & `EditTransactionScreen.kt`):**
  ```kotlin
  Column(
      modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
  )
  ```
- **Onboarding and authentication wizards (`Onboarding.kt`, `PhoneStep.kt`, `OtpStep.kt`):**
  ```kotlin
  Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp)
  ```

---

### 3. Screen & Business State: `ViewModel` + `StateFlow`

While `rememberSaveable` handles screen-level UI state, domain logic and async data are owned by Hilt-injected `ViewModel`s:
- **`StateFlow` for UI State:** ViewModels expose a single, immutable `StateFlow<UiState>` using `stateIn(SharingStarted.WhileSubscribed(5_000))`. The 5-second timeout keeps the upstream flow active across brief configuration changes without restarting expensive Room queries.
- **Atomic State Updates:** Mutations apply atomically using `MutableStateFlow.update { it.copy(...) }`, eliminating race conditions between concurrent coroutines.
- **`SavedStateHandle` for Process Death:** Navigation arguments (e.g. `transactionId` in `TransactionDetailViewModel.kt`) are retrieved from `SavedStateHandle`, ensuring the screen can re-fetch its data even after Android kills the process in the background.

---

### 4. Architectural Patterns Used Across the Project

#### A. State Hoisting (Unidirectional Data Flow)
In Compose screens, stateful containers are separated from stateless presentation components:
- **State flows down:** Child composables receive immutable values via arguments (`value: String`, `enabled: Boolean`).
- **Events flow up:** Child composables emit user intents through callback lambdas (`onValueChange: (String) -> Unit`, `onSubmit: () -> Unit`).
- **Benefit:** Child components are 100% previewable with `@PreviewLightDark` and unit-testable in isolation.

#### B. `collectAsStateWithLifecycle()` vs `collectAsState()`
All screens consume ViewModel `StateFlow`s using `collectAsStateWithLifecycle()`.
- **Why?** Plain `collectAsState()` continues collecting flow emissions in the background even when the app is minimized. `collectAsStateWithLifecycle()` automatically suspends collection when the screen drops below `Lifecycle.State.STARTED`, preventing battery drain and background work.

#### C. Edge-to-Edge Window Inset Management (`safeDrawingPadding`)
Modern Android applications draw **edge-to-edge** by default (`enableEdgeToEdge()`), allowing background surfaces and gradients to bleed seamlessly beneath the system status bar and gesture navigation bar. However, interactive UI components (buttons, headers, text inputs) must never be obstructed by system furniture or physical hardware contours.

`Modifier.safeDrawingPadding()` applies dynamic padding matching `WindowInsets.safeDrawing`, which is the union of four critical system boundaries:
1. **Status Bar (top):** Prevents titles and icons from colliding with the device clock, battery indicator, or network icons.
2. **Navigation Bar (bottom):** Prevents primary action buttons from overlapping the gesture bar pill or 3-button navigation area.
3. **Display Cutouts & Notches:** Protects content from being masked by camera punch-holes, teardrop notches, and rounded corner radii.
4. **Software Keyboard (IME - Input Method Editor):** Unlike older `Modifier.systemBarsPadding()` (which only considers status/navigation bars), `safeDrawingPadding()` dynamically incorporates `WindowInsets.ime`. When the software keyboard slides up, the bottom padding instantly and smoothly expands to push up the content.

##### The Power of Combining `safeDrawingPadding()` with `rememberScrollState()`
In authentication and transaction entry screens, chaining:
```kotlin
Modifier
    .safeDrawingPadding()
    .verticalScroll(rememberScrollState())
    .padding(24.dp)
```
produces a resilient UI container:
- `safeDrawingPadding()` keeps the outer bounds of the screen completely clear of camera cutouts, navigation bars, and the keyboard.
- `rememberScrollState()` ensures that when the keyboard takes up 40–50% of the screen height, the user can freely scroll the remaining form fields, helper text, and CTA buttons into comfortable view.

##### Key Usages in LedgerOne:
- **Mandatory App Update Dialog (`ForceUpdate.kt`):** Ensures critical update messaging is never clipped by top camera cutouts or bottom navigation pills:
  ```kotlin
  Modifier.safeDrawingPadding().padding(24.dp)
  ```
- **Login Screen (`Login.kt`):** Centers authentication cards safely away from system bars:
  ```kotlin
  Modifier.safeDrawingPadding().padding(24.dp)
  ```
- **Onboarding & OTP Flow (`Onboarding.kt`, `PhoneStep.kt`, `OtpStep.kt`):**
  ```kotlin
  Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp)
  ```

#### D. Layout Flow: Main Axis Arrangements vs Cross Axis Alignments in `Column` & `Row`
In Jetpack Compose, the placement of child composables is governed by two orthogonal axes:
- **`Column`:** Main Axis runs **vertically (&#8595;)**, Cross Axis runs **horizontally (&#8594;)**.
- **`Row`:** Main Axis runs **horizontally (&#8594;)**, Cross Axis runs **vertically (&#8595;)**.

> **Rule of Thumb:**
> - **`Arrangement`** dictates **spacing, distribution, and layout flow along the Main Axis**.
> - **`Alignment`** dictates **positioning across the Cross Axis**.

<p align="center">
  <img src="docs/images/compose-arrangements.svg" alt="Jetpack Compose Column and Row Arrangements" width="100%">
</p>

##### Comparison of Arrangement Modes

| Arrangement Mode | Visual Distribution Behavior | Common Production Use Case |
|---|---|---|
| **`Arrangement.Top` / `Start`** | Packs all children flush to the start of the layout. Any extra space remains at the end. (Default) | Standard vertical reading content, forms, documents |
| **`Arrangement.Center`** | Groups all children tightly together in the exact middle of the available space. | Centered loading spinners, empty state illustrations, PIN verification dials |
| **`Arrangement.Bottom` / `End`** | Packs all children flush against the bottom or end edge. | Sticky bottom action bars, trailing action icon clusters |
| **`Arrangement.SpaceBetween`** | Pins the first child to the very start edge, the last child to the end edge, and divides all free space equally between interior items (zero space at the outer boundaries). | Card headers with trailing actions, transaction summary rows (`Title` on left, `₹Amount` on right) |
| **`Arrangement.SpaceAround`** | Distributes available space evenly between children, placing **half-sized space** at the start and end edges. | Social share buttons, 3-step timeline indicators |
| **`Arrangement.SpaceEvenly`** | Distributes available space such that **all gaps** (start margin, inter-child gaps, and end margin) are **100% equal**. | Evenly spaced dashboard navigation tiles, keypad rows |
| **`Arrangement.spacedBy(dp)`** | Places a fixed, precise physical gap (e.g. `16.dp`) between adjacent children. Unlike manual `Spacer` elements, `spacedBy` does not add redundant spacing before the first or after the last child. | Vertical form fields, category filter chip rows, list cards |

##### Key Examples from LedgerOne Codebase:

1. **`Arrangement.SpaceBetween` for Key-Value Rows (`BankProducts.kt`, `TransactionListScreen.kt`):**
   ```kotlin
   Row(
       modifier = Modifier.fillMaxWidth(),
       horizontalArrangement = Arrangement.SpaceBetween,
       verticalAlignment = Alignment.CenterVertically
   ) {
       LedgerOneText("Transfer Limit", role = LedgerOneTextRole.Body)
       LedgerOneText("₹50,000.00", role = LedgerOneTextRole.Title)
   }
   ```
2. **`Arrangement.spacedBy(dp)` for Form Fields & Carousels (`CreateTransactionScreen.kt`, `HomeScreen.kt`):**
   ```kotlin
   // Vertical form with consistent 16.dp gaps between inputs
   Column(
       modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
       verticalArrangement = Arrangement.spacedBy(16.dp)
   ) {
       LedgerOneTextField(...)
       CategoryPicker(...)
       LedgerOneButton(...)
   }

   // Horizontal category filter carousel with 12.dp gaps
   LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
       items(categories) { category -> CategoryChip(category) }
   }
   ```
3. **`Arrangement.Center` for Centered Identity & Loading Hubs (`Profile.kt`, `TickerScreen.kt`):**
   ```kotlin
   Column(
       horizontalAlignment = Alignment.CenterHorizontally,
       verticalArrangement = Arrangement.spacedBy(12.dp)
   ) {
       AvatarCircle(initials = "SR")
       LedgerOneText("Sourabh Rustagi", role = LedgerOneTextRole.Title)
   }
   ```

---

### 5. Application State Mechanism Comparison Matrix

| State Tool | Storage Location | Survives Recomposition? | Survives Screen Rotation? | Survives Process Death? | Survives Phone Reboot? | Primary Application Use Case |
|---|---|---|---|---|---|---|
| **`remember`** | Composition Memory | **Yes** | ❌ No | ❌ No | ❌ No | Ephemeral UI: animation clocks, transient gestures |
| **`rememberSaveable`** | `SavedStateRegistry` (Bundle) | **Yes** | **Yes** | **Yes** | ❌ No | Form text fields, wizard step index, dialog/sheet open state |
| **`rememberScrollState`** | `SavedStateRegistry` (`ScrollState.Saver`) | **Yes** | **Yes** | **Yes** | ❌ No | Viewport scroll position in `Column`/`Row`, form keyboard adaptation |
| **`ViewModel` (`StateFlow`)** | `ViewModelStore` (Heap) | **Yes** | **Yes** | ❌ No | ❌ No | Business logic, fetched accounts, live calculations, search queries |
| **`SavedStateHandle`** | `SavedStateRegistry` + ViewModel | **Yes** | **Yes** | **Yes** | ❌ No | Navigation parameters (`transactionId`), search criteria across process death |
| **`Room Database`** | SQLite on Disk | **Yes** | **Yes** | **Yes** | **Yes** | Durable single source of truth: ledger transactions, sync states |
| **`DataStore`** | Disk (`.preferences_pb`) | **Yes** | **Yes** | **Yes** | **Yes** | User preferences: theme mode, masked balance toggle, skipped app version |

## Jetpack Compose Effects & Lifecycle Side-Effect Handling

In Jetpack Compose, composable functions should be fast, pure, and free of side effects. Because composables can recompose frequently, in any order, or be cancelled mid-execution, executing side effects (such as modifying non-Compose state, starting coroutines, registering system callbacks, or allocating system resources) directly inside a composable body can lead to memory leaks, repeated execution, and erratic app state.

Jetpack Compose provides built-in effect APIs to handle side effects in a controlled, lifecycle-aware manner.

---

### 1. `DisposableEffect` (Cleanup-Aware Side Effects)

`DisposableEffect` is specifically designed for side effects that require **teardown or cleanup** when the composable leaves the composition or when its observation keys change. It is intended for non-coroutine operations such as registering and unregistering listeners, releasing executors, or toggling window flags.

Every `DisposableEffect` must end with an `onDispose { ... }` block where the teardown logic is executed.

#### How `DisposableEffect` works:
1. When entering composition (or when a key changes), the effect block executes.
2. When the composable leaves the composition (or before re-running with new keys), `onDispose` executes first to release held resources.

#### Real-world examples in LedgerOne:

#### A. Window Security (`SecureWindow.kt` in `:core:ui`)
Prevents Android from taking screenshots or showing task switcher previews on sensitive banking screens:

```kotlin
@Composable
fun SecureWindow() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        // 1. Add FLAG_SECURE when the screen enters composition
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        
        // 2. Clean up: clear FLAG_SECURE when the screen leaves composition
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
```

#### B. Background Session Auto-Lock (`LedgerOneApp.kt` in `:app`)
Monitors application background duration using AndroidX `LifecycleEventObserver` and auto-locks the session if backgrounded for more than 30 seconds:

```kotlin
@Composable
private fun LockWhenBackgrounded(onLock: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val activity = LocalActivity.current
    DisposableEffect(owner) {
        var stoppedAt = 0L
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> if (activity?.isChangingConfigurations != true) {
                    stoppedAt = System.currentTimeMillis()
                }
                Lifecycle.Event.ON_START -> {
                    if (stoppedAt != 0L && System.currentTimeMillis() - stoppedAt > LOCK_AFTER_MS) {
                        onLock()
                    }
                    stoppedAt = 0L
                }
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        // Clean up: unregister the lifecycle observer when navigating away
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}
```

#### C. Network Connectivity Monitoring (`Link.kt` in `:app`)
Registers an Android system `ConnectivityManager.NetworkCallback` to keep the UI state synchronized with the device's internet connection:

```kotlin
@Composable
fun rememberConnected(): Boolean {
    val context = LocalContext.current
    val manager = remember { context.getSystemService(ConnectivityManager::class.java) }
    var connected by remember { mutableStateOf(hasInternet(manager)) }
    
    DisposableEffect(manager) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { connected = hasInternet(manager) }
            override fun onLost(network: Network) { connected = hasInternet(manager) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                connected = hasInternet(manager)
            }
        }
        manager.registerDefaultNetworkCallback(callback)
        // Clean up: unregister callback from system service to avoid memory leaks
        onDispose { manager.unregisterNetworkCallback(callback) }
    }
    return connected
}
```

#### D. Camera Executor Teardown (`QrCamera.kt` in `:app`)
Allocates a single-thread background executor to process QR camera frames with ML Kit, shutting down the background executor cleanly when leaving composition:

```kotlin
DisposableEffect(owner) {
    if (context.findActivity() == null) return@DisposableEffect onDispose {}
    val executor = Executors.newSingleThreadExecutor()
    bindQrScanner(context, owner, preview, executor) { raw ->
        if (!done) {
            done = true
            ContextCompat.getMainExecutor(context).execute { onCode(raw) }
        }
    }
    // Clean up: shut down background thread pool when camera view is dismissed
    onDispose { executor.shutdown() }
}
```

#### E. Deep Link Intent Listener (`LedgerNavHost.kt` in `:app`)
Hooks into `ComponentActivity.addOnNewIntentListener` to intercept external payment deep links and passes them to `NavController`:

```kotlin
val activity = LocalActivity.current as? ComponentActivity
if (activity != null) DisposableEffect(activity, navController) {
    val listener = Consumer<Intent> { navController.handleDeepLink(it) }
    activity.addOnNewIntentListener(listener)
    // Clean up: remove the listener when NavHost leaves composition
    onDispose { activity.removeOnNewIntentListener(listener) }
}
```

---

### 2. `LaunchedEffect` (Coroutine-Scoped Side Effects)

`LaunchedEffect` executes a suspend block within a `CoroutineScope` tied to the composable's lifecycle.

- **Trigger:** Runs when entering composition or when any of its specified `key` parameters change.
- **Cancellation:** If the composable leaves composition or if a key changes while the coroutine is still running, the existing coroutine is cancelled cooperatively.

#### Real-world examples in LedgerOne:
- **Countdown Timers:** `LaunchedEffect(seconds)` in `Login.kt` and `OtpStep.kt` runs a 1-second delay loop to update OTP resend timers.
- **Dynamic Permission Requests:** `LaunchedEffect(liveCapture)` in `Scan.kt` triggers the Android system camera permission prompt when camera scanning is activated.
- **Deep Link Navigation Prefill:** `LaunchedEffect(prefillHandle, prefillMinor)` in `WalletScreen.kt` pre-populates payee details when launched via UPI intent.
- **Pagination Triggers:** `LaunchedEffect(shouldLoadMore)` in `TransactionListScreen.kt` triggers `viewModel.loadNextPage()` when the user scrolls near the list bottom.

---

### 3. `SideEffect` (Publishing Compose State to Non-Compose Code)

`SideEffect` executes after **every successful recomposition**. Unlike `LaunchedEffect`, it does not support coroutines and does not take keys.

- **When to use:** Use `SideEffect` to publish Compose state to external, non-Compose objects (such as analytics managers, custom logging frameworks, or system properties) that do not require cleanup.
- **Example:**
  ```kotlin
  @Composable
  fun ScreenTracker(screenName: String, analytics: AnalyticsTracker) {
      SideEffect {
          analytics.setCurrentScreen(screenName)
      }
  }
  ```

---

### 4. `rememberCoroutineScope` (Event-Driven Coroutines)

`rememberCoroutineScope` returns a `CoroutineScope` bound to the composable's point in composition.

- **When to use:** Use this when a coroutine needs to be launched from an **event callback** (such as a Button's `onClick`, a swipe gesture, or showing a Snackbar), where suspend functions cannot be called directly.
- **Example:**
  ```kotlin
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  Button(onClick = {
      scope.launch {
          snackbarHostState.showSnackbar("Transaction reported")
      }
  }) {
      Text("Report")
  }
  ```

---

### 5. `rememberUpdatedState` (Stable Callback References)

`rememberUpdatedState` captures a changing value or lambda so that a long-running effect (such as a `LaunchedEffect` or `DisposableEffect`) can read the latest value without cancelling and restarting the effect.

- **When to use:** When passing a callback (e.g., `onTimeout: () -> Unit`) into an effect with a timer or delay, ensuring the latest lambda is invoked without restarting the timer on recomposition.

---

### 6. `produceState` (Converting Async Sources to Compose State)

`produceState` launches a coroutine scoped to the composition that can push values into a returned Compose `State<T>`.

- **When to use:** Ideal for converting external non-Compose asynchronous streams, callback-based APIs, or suspend calls into declarative Compose state.

---

### 7. `derivedStateOf` (Calculation Optimization)

`derivedStateOf` creates a Compose `State` derived from other Compose state objects, recomputing its value only when input boundaries change.

- **When to use:** When state changes rapidly (such as `LazyListState.firstVisibleItemIndex` during scrolling), but the UI only needs to react when a specific condition is met (e.g. scroll offset passing a threshold).
- **LedgerOne Example:** In `TransactionListScreen.kt`, `derivedStateOf` calculates `shouldLoadMore` by inspecting visible item indexes to determine when to trigger the next page load.

---

### Summary of Compose Effect Handlers

| Effect API | Coroutine? | Cleanup (`onDispose`)? | Re-executes On | Primary Use Case |
|---|---|---|---|---|
| **`DisposableEffect`** | No | **Yes** | Key change / leaves composition | Listeners, broadcast receivers, window flags, background executors |
| **`LaunchedEffect`** | **Yes** | Cooperative cancellation | Key change / leaves composition | Suspend tasks, animations, countdown timers, Flow collection |
| **`SideEffect`** | No | No | Every successful recomposition | Updating non-Compose external objects with latest state |
| **`rememberCoroutineScope`** | **Yes** | Cancelled on exit | Manual `scope.launch` | User interaction callbacks (`onClick`, gestures) |
| **`rememberUpdatedState`** | No | No | Updates internal reference | Capturing latest callback in long-lived effects |
| **`produceState`** | **Yes** | Cooperative cancellation | Key change / leaves composition | Bridging external async data into Compose `State<T>` |
| **`derivedStateOf`** | No | No | Derived inputs change | Reducing recomposition frequency for high-velocity state |

## Application Enums: Flow Steps & Operational Statuses

Enums are used across the application to model type-safe state machines, multi-step navigation wizards, operation lifecycle statuses, and design system tokens. By restricting states to closed enum sets, the compiler enforces exhaustive `when` branches and prevents invalid state transitions.

| Enum | Module / File | Values | Category | Purpose in Application |
|---|---|---|---|---|
| **`OnboardStep`** | `:app` (`Onboarding.kt`) | `PHONE`, `OTP` | Wizard Step | Controls the 2-step onboarding wizard progression (phone input → SMS OTP verification) |
| **`TransferStep`** | `:feature:wallet` (`Wallet.kt`) | `FORM`, `REVIEW`, `RECEIPT` | Wizard Step | Drives the peer-to-peer wallet transfer wizard from amount entry to confirmation and receipt |
| **`TransferStatus`** | `:feature:wallet` (`Wallet.kt`) | `IDLE`, `INSUFFICIENT`, `STEP_UP`, `PENDING`, `FAILED`, `DONE` | Lifecycle Status | Manages the wallet transfer state machine (balance checks, step-up biometric auth, in-flight posting, and terminal results) |
| **`BankSheet`** | `:feature:banking` (`BankingSheets.kt`) | `UPI`, `PAYEE` | UI Modal Step | Determines which bottom sheet modal is open in the Banking hub (managing UPI ID vs adding a new payee) |
| **`SyncPhase`** | `:core:model` (`SyncCoordinator.kt`) | `IDLE`, `ENQUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`, `BLOCKED` | System Status | Domain-level coordinator phase reported by WorkManager background sync jobs to update UI headers |
| **`SyncState`** | `:core:designsystem` (`SyncIndicator.kt`) | `SYNCED`, `SYNCING`, `WAITING`, `FAILED` | UI Status | Visual sync indicator combining user-facing status label, semantic tone (`BannerTone`), and cloud status icon |
| **`DisputeReason`** | `:app` (`Tools.kt`) | `UNAUTHORISED`, `WRONG_AMOUNT`, `NOT_RECEIVED`, `DUPLICATE` | Business Status | Customer grievance categorization codes recorded when reporting a transaction dispute |
| **`PrepayMode`** | `:feature:banking` (`BankProducts.kt`) | `TENURE`, `EMI` | Calculation Strategy | Loan prepayment calculation strategy selecting between shortening loan tenure or reducing monthly instalment |
| **`MoneyDirection`** | `:core:designsystem` (`Money.kt`) | `CREDIT`, `DEBIT`, `NEUTRAL` | Financial Indicator | Controls currency formatting colors (green credit, primary debit, neutral zero) and +/- prefixes |
| **`BannerTone`** | `:core:designsystem` (`BannerTone.kt`) | `INFO`, `SUCCESS`, `WARNING`, `ERROR` | Design Token | Maps semantic visual intent to theme container colors and icon tints for banners, alerts, and badges |
| **`ThemeMode`** | `:core:designsystem` (`Theme.kt`) | `SYSTEM`, `LIGHT`, `DARK` | User Preference | User interface appearance mode persisted in DataStore |
| **`LedgerOneTextRole`** | `:core:designsystem` (`LedgerOneText.kt`) | `Display`, `Title`, `Body`, `Label` | Typography Token | Design system typography roles enforcing consistent font sizes, weights, and letter spacings |

---

### Key Step & Status State Machines

#### 1. Peer-to-Peer Transfer (`TransferStep` & `TransferStatus`)
The wallet transfer flow decouples screen steps from the underlying transaction status:
- **`TransferStep.FORM`**: The user specifies amount and recipient. If the amount exceeds current balance, status immediately reflects `TransferStatus.INSUFFICIENT`.
- **`TransferStep.REVIEW`**: Shows transfer fee and split legs. If high-value transfer thresholds are exceeded, `TransferStatus.STEP_UP` prompts for biometric confirmation.
- **`TransferStep.RECEIPT`**: When the network worker posts the transaction (`TransferStatus.PENDING` → `TransferStatus.DONE`), the idempotency key and receipt reference are displayed. If rejected, `TransferStatus.FAILED` displays the failure reason with retry.

#### 2. Onboarding Registration (`OnboardStep`)
Scoped directly to `OnboardingFlow`, `OnboardStep` preserves the user's progress through registration:
- **`OnboardStep.PHONE`**: Collects the user's mobile number and terms consent.
- **`OnboardStep.OTP`**: Validates the 6-digit SMS verification code. Tapping "Edit" navigates cleanly back to `OnboardStep.PHONE` while retaining the entered phone number.

#### 3. Dual-Layer Synchronization (`SyncPhase` vs `SyncState`)
- **Domain layer (`SyncPhase`):** Emitted as a cold `Flow<SyncPhase>` by `SyncCoordinator`. Tracks granular worker lifecycle phases: `IDLE`, `ENQUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`, and `BLOCKED` (waiting for network constraints).
- **Presentation layer (`SyncState`):** Displayed in `SyncIndicator`. Maps domain phases into visual badges: `SYNCED` (green cloud checkmark), `SYNCING` (blue rotating sync), `WAITING` (amber cloud off), and `FAILED` (red error outline).

#### 4. Loan Amortization Prepayment Strategy (`PrepayMode`)
When prepaying an existing EMI loan, `PrepayMode` determines the amortization adjustment:
- **`PrepayMode.TENURE` ("Reduce tenure"):** Keeps the monthly EMI payment constant while reducing the remaining months until loan maturity (`tenureFor`).
- **`PrepayMode.EMI` ("Reduce EMI"):** Recalculates lower monthly EMI payments while keeping the original loan tenure unchanged (`emiMinor`).

## Design System: ColorScheme, Semantic Colors & Theme Architecture

The visual foundation of LedgerOne is built inside `:core:designsystem` around **Material 3 (M3)** design principles tailored specifically for financial technology. It provides a harmonious, accessible color hierarchy across light and dark modes, and extends standard Material palettes with domain-specific financial semantic colors.

> **Zero Raw Hex Colors In Features:**
> Feature modules must NEVER hardcode `Color(0x...)` or raw hex values. All colors must originate from `MaterialTheme.colorScheme` or `LedgerTheme.colors`. The project's `ModuleBoundaryTest` verifies this rule and fails the build on any violation.

---

### 1. Palette Foundation (`Color.kt`)

Rather than arbitrary colors, LedgerOne defines a tailored, cohesive primitive palette:
- **Neutral Slate Scale (`Slate950` to `Slate50`):** Replaces pure flat black and stark white with rich, layered slate tones for premium depth and eye comfort.
- **Brand Primary Blues (`Blue900` to `Blue100`, `BlueInk`):** A confident royal blue family used for primary call-to-actions, focused elements, and active indicators.
- **Semantic Green (`Green950` to `Green100`):** Signifies deposits, inflows, credit transactions, verified states, and healthy balances.
- **Semantic Red (`Red950` to `Red100`):** Signifies debits, expenses, critical errors, security warnings, and failed syncs.
- **Semantic Amber (`Amber950` to `Amber100`):** Signifies pending transfers, cooldown timers, sync waiting states, and alerts.

---

### 2. Material 3 ColorSchemes: Light vs. Dark (`Theme.kt`)

LedgerOne configures distinct Material 3 color schemes for light and dark modes:

| Material 3 Role | Light Scheme (`LightScheme`) | Dark Scheme (`DarkScheme`) | Purpose & UI Role |
|---|---|---|---|
| **`primary`** | `Blue700` (`#1D4ED8`) | `Blue300` (`#93C5FD`) | Primary buttons, active tabs, focused outlines, primary brand accents |
| **`onPrimary`** | `Color.White` (`#FFFFFF`) | `BlueInk` (`#0B1B3F`) | Text and icon labels drawn over `primary` surfaces |
| **`primaryContainer`** | `Blue100` (`#DBEAFE`) | `Blue900` (`#1E3A8A`) | Subtle brand highlights, active navigation pill backgrounds, selected chips |
| **`onPrimaryContainer`** | `Blue900` (`#1E3A8A`) | `Blue100` (`#DBEAFE`) | Text and icons placed on top of `primaryContainer` |
| **`secondary`** | `Slate600` (`#475569`) | `Slate300` (`#CBD5E1`) | Subordinate UI elements, secondary action buttons, subtitles |
| **`onSecondary`** | `Color.White` (`#FFFFFF`) | `Slate950` (`#0B1220`) | Text and icons drawn on `secondary` buttons |
| **`secondaryContainer`** | `Slate100` (`#F1F5F9`) | `Slate850` (`#1C2740`) | Inactive pill backgrounds, neutral category chip containers |
| **`onSecondaryContainer`**| `Ink` (`#0F172A`) | `Slate100` (`#F1F5F9`) | Text and icons drawn on `secondaryContainer` |
| **`tertiary`** | `Amber700` (`#B45309`) | `Amber400` (`#FBBF24`) | Special accent highlights, promotional banners, offer tags |
| **`onTertiary`** | `Color.White` (`#FFFFFF`) | `Slate950` (`#0B1220`) | Text/icons drawn on `tertiary` elements |
| **`background`** | `Slate50` (`#F8FAFC`) | `Slate950` (`#0B1220`) | Root screen background behind scrollable containers |
| **`onBackground`** | `Ink` (`#0F172A`) | `Slate100` (`#F1F5F9`) | Default text color rendered directly on screen background |
| **`surface`** | `Color.White` (`#FFFFFF`) | `Slate900` (`#111A2E`) | Card containers, modal sheets, elevated dialog surfaces |
| **`onSurface`** | `Ink` (`#0F172A`) | `Slate100` (`#F1F5F9`) | High-emphasis body, title, and heading typography |
| **`surfaceVariant`** | `Slate100` (`#F1F5F9`) | `Slate850` (`#1C2740`) | Subtle card backgrounds, input field containers, inactive segments |
| **`onSurfaceVariant`** | `Slate700` (`#334155`) | `Slate300` (`#CBD5E1`) | Medium-emphasis secondary text, timestamps, captions, helper hints |
| **`outline`** | `Slate300` (`#CBD5E1`) | `Slate700` (`#334155`) | Input field borders, card division lines, table separators |
| **`outlineVariant`** | `Slate200` (`#E2E8F0`) | `Slate850` (`#1C2740`) | Ultra-subtle dividers and inactive border outlines |
| **`error`** | `Red700` (`#B91C1C`) | `Red300` (`#FCA5A5`) | Critical errors, validation failures, destructive actions |
| **`onError`** | `Color.White` (`#FFFFFF`) | `Red950` (`#450A0A`) | Text and icon labels drawn on `error` surfaces |
| **`errorContainer`** | `Red100` (`#FEE2E2`) | `Red950` (`#450A0A`) | Error banner backgrounds, failed transaction badge pills |
| **`onErrorContainer`** | `Red700` (`#B91C1C`) | `Red300` (`#FCA5A5`) | Error text and warning icon tint rendered inside `errorContainer` |

---

### 3. Domain Financial Colors: `LedgerTheme.colors`

Standard Material 3 only defines an `error` state. In financial applications, **credits, inflows, approvals, warnings, and cooling periods** require dedicated, first-class semantic colors that guarantee WCAG AA contrast ratios in both light and dark modes:

```kotlin
@Immutable
data class LedgerColors(
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
)
```

| Semantic Slot | Light Theme Value | Dark Theme Value | Financial & UI Application |
|---|---|---|---|
| **`success`** | `Green700` (`#15803D`) | `Green400` (`#4ADE80`) | Credit (+) transaction amounts, successful transfer confirmations, synced status |
| **`successContainer`** | `Green100` (`#DCFCE7`) | `Green950` (`#052E16`) | Background pill for success badges, synced status chips, inflow banners |
| **`warning`** | `Amber700` (`#B45309`) | `Amber400` (`#FBBF24`) | Beneficiary cooling period warnings, in-flight sync notices, dispute under review |
| **`warningContainer`** | `Amber100` (`#FEF3C7`) | `Amber950` (`#451A03`) | Cooldown badge container, caution banner background, step-up auth cards |
| **`danger`** | `Red700` (`#B91C1C`) | `Red300` (`#FCA5A5`) | Debit (-) transaction amounts, payment declines, session auto-lock alerts |
| **`dangerContainer`** | `Red100` (`#FEE2E2`) | `Red950` (`#450A0A`) | Decline banner container, failed transfer card surface |

#### Accessing Financial Colors in Compose:
```kotlin
// Example: Rendering credit vs debit amounts in Money.kt
val color = when (direction) {
    MoneyDirection.CREDIT -> LedgerTheme.colors.success
    MoneyDirection.DEBIT -> MaterialTheme.colorScheme.onSurface
    MoneyDirection.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
}

// Example: Displaying a semantic banner in StatusBanner.kt
val (background, content) = when (tone) {
    BannerTone.INFO -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    BannerTone.SUCCESS -> LedgerTheme.colors.successContainer to LedgerTheme.colors.success
    BannerTone.WARNING -> LedgerTheme.colors.warningContainer to LedgerTheme.colors.warning
    BannerTone.ERROR -> LedgerTheme.colors.dangerContainer to LedgerTheme.colors.danger
}
```

---

### 4. Theme Modes & Dynamic Switching (`ThemeMode`)

LedgerOne supports dynamic user theme selection via `ThemeMode`, persisted in DataStore preferences:

```kotlin
enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
}
```

- **`ThemeMode.SYSTEM` (Default):** Observes the Android OS system dark theme setting via `isSystemInDarkTheme()`. Adapts automatically when the user changes device settings or when night-mode triggers on a schedule.
- **`ThemeMode.LIGHT`:** Forces the application into `LightScheme` and `LightLedger` regardless of device configuration.
- **`ThemeMode.DARK`:** Forces the application into `DarkScheme` and `DarkLedger` for low-light battery savings on AMOLED displays.

Themes are applied seamlessly via `LedgerOneTheme(mode = userPrefs.themeMode)` at the root of `LedgerOneApp.kt`:
```kotlin
@Composable
fun PersonalLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalLedgerColors provides if (darkTheme) DarkLedger else LightLedger) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = LedgerTypography,
            shapes = LedgerShapes,
            content = content,
        )
    }
}
```

## Business logic

The application's domain rules are concentrated in pure Kotlin within `core/model`, ensuring they can be tested independently of the Android runtime and UI frameworks.

### Financial calculations & accounting invariants

- **Minor-unit precision:** Money is stored exclusively as positive 64-bit integers (`Long`) in minor currency units (paise or cents). Floating-point (`Double`/`Float`) representations are forbidden for financial data to eliminate rounding inaccuracies.
- **Double-entry bookkeeping (`post`):** Every financial transaction decomposes into balanced ledger legs. Each leg is strictly a debit or a credit (`debitMinor == 0L || creditMinor == 0L`). A transaction is rejected unless $\sum \text{debits} == \sum \text{credits}$ and the leg list is non-empty.
- **Remainder-preserving expense splitting (`splitEvenly`):** Splitting a total minor amount into $N$ equal parts allocates `totalMinor / parts` to each participant, distributing any leftover paise (`totalMinor % parts`) one-by-one to the earliest participants (`base + 1`). This guarantees that $\sum \text{parts} == \text{totalMinor}$ down to the last paisa.
- **Equal Monthly Instalment annuity calculation (`emiMinor`):** Monthly EMI is calculated using closed-form annuity compounding based on annual basis points ($1\% = 100\text{ bps}$):
  $$\text{EMI} = \text{round}\left( P \cdot r \cdot \frac{(1+r)^n}{(1+r)^n - 1} \right)$$
  where $P$ is principal in minor units, $r = \frac{\text{annualBps}}{12 \times 10\,000}$, and $n$ is duration in months.
- **Loan amortization schedules (`amortize`):** Generates month-by-month payment schedules tracking balance reduction. Each instalment splits into interest ($balance \times r$) and principal ($\min(\text{EMI} - interest, balance)$). The final instalment dynamically adjusts its principal payment to settle the residual balance down to zero.
- **Loan prepayment tenure adjustment (`tenureFor`):** Calculates reduced loan duration when an upfront prepayment is made while holding the existing EMI payment constant. Rejects adjustments where monthly interest exceeds the EMI.

### Payments, UPI & fraud prevention

- **Idempotency tracking (`TransferBook`):** Every peer-to-peer transfer requires a unique client-generated idempotency key. The transfer book records transactions by key and returns the identical amount on repeated calls, preventing duplicate deductions during network retries.
- **New payee cooling period (`coolingBlocks`):** Anti-fraud rule blocking fund transfers to newly added beneficiaries for 30 minutes ($30 \times 60 \times 1\,000\text{ ms}$) following addition.
- **NPCI-compliant UPI payload generation (`upiPayload`):** Constructs standard UPI intent deep links (`upi://pay?pa=$vpa&am=$rupees.$paise`) formatting amounts to 2 decimal places with leading zero padding for paise.
- **Card Luhn checksum validation (`luhn`):** Validates primary card account numbers (13–19 digits) using the standard Luhn algorithm (doubling alternate digits from right to left).
- **PAN data protection (`redactPans`):** Automatically redacts card numbers matching Luhn validity from audit strings and display text by masking the number with `****`.

### Security, authentication & cryptography

- **MPIN authentication (`mpinAccepted`):** PIN authorization requires exactly four numerical digits (`pin.length == 4 && pin.all { it.isDigit() }`).
- **Cryptographic device binding:** Hardware-backed ECDSA key pairs (P-256) are generated inside the Android Keystore. During registration, the server issues a random challenge nonce that the device signs. The server verifies the public key signature and prevents nonce replay before associating the device with an account.
- **End-to-end encrypted utility bill pay (`PayBox`):** Utility bill queries and payments are protected using Elliptic Curve Diffie-Hellman (ECDH P-256) key agreement with AES-GCM (128-bit) authenticated encryption and unique 12-byte initialization vectors (IVs). Neither bill amounts nor customer identifiers travel in plaintext.
- **Background auto-lock:** The app starts a 30-second countdown upon backgrounding. Exceeding this threshold locks the session, requiring biometric or MPIN authentication upon foregrounding.

### Offline-first synchronization & conflict handling

- **Write-ahead local truth:** All writes commit first to Room with a synchronization state: `SYNCED`, `PENDING_CREATE`, `PENDING_UPDATE`, or `PENDING_DELETE`. The UI observes local Room flows and reflects changes immediately.
- **Exponential backoff retry (`withRetry`):** Network requests retry up to 3 times on transient connection drops (`IOException` or `ServerBusy`), doubling delay (1s, 2s, 4s). Terminal rejections (`TransactionRejected`) fail immediately without retry. Coroutine `CancellationException` is always propagated.
- **Three-way sync conflict resolution (`ConflictInbox`, `mergeField`):** Remote 409 conflict responses preserve both local and server transaction states in a persistent disk inbox (`ConflictInbox`). Competing field updates are flagged via `FieldMerge.Conflict(left, right)`, presenting differing fields side-by-side for manual user resolution rather than silent overwrite.


## Dependencies

Versions live in `gradle/libs.versions.toml`. A library is added only when the Android or Kotlin standard library does not already solve the problem. Why this stack instead of Retrofit, SQLDelight, Koin, and the rest is in [docs/LIBRARIES.md](docs/LIBRARIES.md).

| Dependency | Purpose |
| --- | --- |
| Kotlin, coroutines, Flow | Language, background work, and streams for Room, network, and UI state |
| kotlinx.serialization | Compile-time JSON for Ktor DTOs and type-safe navigation routes |
| Jetpack Compose, Material 3, Compose Foundation / Animation | Screens, theme, motion, and shared components |
| Compose UI Tooling | `@Preview` / `@PreviewLightDark` in the app and library modules |
| Navigation Compose | Back stack, deep links, and typed routes; only `:app` joins feature screens |
| Lifecycle, Activity Compose, ViewModel | Configuration-change state and lifecycle-aware collection |
| Hilt, Hilt Navigation, Hilt Work | Compile-time graphs for repositories, Ktor clients, ViewModels, and workers |
| Room, Room Paging | Local source of truth, migrations, and paged transaction history |
| DataStore Preferences | Small settings: theme, privacy, categories, skipped update code |
| WorkManager | Durable sync work that survives process death |
| Paging 3 | Incremental transaction lists from Room into Compose |
| Ktor Client (OkHttp engine) | REST, static API, device binding, and the market WebSocket |
| OkHttp, logging interceptor | TLS, certificate pinning, HTTP cache; debug-only request summaries |
| CameraX | Lifecycle-aware camera for UPI QR capture |
| ML Kit barcode scanning | On-device QR decode from CameraX frames |
| AndroidX Biometric | System biometric prompt; keys stay in Android Keystore |
| Timber | App logging; debug tree only on debug builds |
| Chucker | On-device HTTP inspector in debug; no-op artifact in release |
| LeakCanary | Debug-only leak detection for Activities and Compose hosts |
| JUnit, AndroidX Test, Espresso, Compose UI Test, Ktor MockEngine | Local rules, instrumented UI, and HTTP tests without a server |
| Detekt, ktlint | Complexity, correctness, and formatting in CI |
| Android Gradle Plugin, Compose / serialization compiler, KSP | Variants, R8, Compose, serializers, Hilt, and Room codegen |

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
- Follow [Kotlin exception classes](https://kotlinlang.org/docs/exceptions.html#exception-classes): `require`/`requireNotNull` for bad arguments, `check`/`error` for illegal state, and `getOrNull` / `firstOrNull` / `toLongOrNull` instead of `IndexOutOfBoundsException`, `NoSuchElementException`, or `NumberFormatException`. Custom types extend `Exception` with a message and an optional cause. Catch the most specific type first; rethrow `CancellationException`; do not catch `Error`.
- Follow [Kotlin functions](https://kotlinlang.org/docs/functions.html#tail-recursive-functions): default and named arguments instead of overloads, a data class instead of `Pair` when the values mean different things, and a single-expression body when the function is one expression. Mark a self-call `tailrec` only when it is the last operation (not inside `try`/`catch`/`finally`, not on an `open` member) so the compiler turns it into a loop.
- Follow [Kotlin lambdas](https://kotlinlang.org/docs/lambdas.html): trailing lambda outside the `()`, `it` for a single used parameter, `_` when unused, and a callable reference (`TransactionEntity::toDomain`) instead of `{ x -> toDomain(x) }`. Keep `Unit` in a function type. Use an anonymous function only when you need a local `return` or an explicit return type.
- Follow [Kotlin inline functions](https://kotlinlang.org/docs/inline-functions.html): mark a small higher-order function `inline` so the lambda is copied into the call site. Use `reified` when the body needs the type (`is`/`as`, `navDeepLink<T>`). Mark a lambda `noinline` if it is stored or passed on, `crossinline` if it is called from a nested object or function. Do not inline a large function. A public inline body may only use public or `@PublishedApi` declarations.
- Follow [Kotlin classes](https://kotlinlang.org/docs/classes.html): a `data class` for a named record (`Leg`), an `object` when there is no instance state (`SteadyConditions`). Follow [extensions](https://kotlinlang.org/docs/extensions.html) on the type they extend (`BannerTone.toneColor()`). Follow [sealed classes](https://kotlinlang.org/docs/sealed-classes.html) with an exhaustive `when` (`FieldMerge`). Follow [enum classes](https://kotlinlang.org/docs/enum-classes.html) for a closed set of constants (`BannerTone`, `DisputeReason`). Follow [inline classes](https://kotlinlang.org/docs/inline-classes.html) only as `@JvmInline value class` wrapping one primitive or `String` when the type itself is the API — do not wrap money; keep a `Long` in minor units.
- Follow [functional interfaces](https://kotlinlang.org/docs/fun-interfaces.html): `fun interface` plus a SAM lambda (`AppRelease { }`, `CategoryCatalog { }`, `TickerSocket { }`, `HomeBoard { }`, `AssistantBackend { }`) when there is one abstract member. Use a function type when the API only needs a function; keep a plain interface when there are several members.
- Follow [properties](https://kotlinlang.org/docs/properties.html) for argument-free reads (`TransferBook.size`), `private set` for a value the type mutates (`ChargeQueue.charges`), `const` for compile-time primitives, and `lateinit` only for injection or test setup. Follow [delegated properties](https://kotlinlang.org/docs/delegated-properties.html): `by lazy` for a one-time init (`DeviceKeys.store`); do not write a custom delegate the stdlib already covers.
- Follow [null safety](https://kotlinlang.org/docs/null-safety.html): `?.` / `?:` / `orEmpty()` / `as?` instead of `!!` or `as`. Use `requireNotNull` when null is a programming error. Do not invent a nullable type you immediately assert.
- Follow [Kotlin scope functions](https://kotlinlang.org/docs/scope-functions.html#context-object-this-or-it): `apply` (`this`, returns the object) to configure members; `also` (`it`, returns the object) for a side effect that takes the object as an argument; `let` (`it`, returns the lambda) for a non-null value or a local name; `run` (`this`, returns the lambda) to use members and compute a result, or non-extension `run` where an expression is required; `with` to group calls. Prefer `takeIf` / `takeUnless` over an `if` that only filters one value. Do not nest scope functions.
- Follow the [Kotlin coroutines guide](https://kotlinlang.org/docs/coroutines-guide.html): structured concurrency (`viewModelScope`, `coroutineScope`) — no `GlobalScope`. Independent suspend work runs concurrently with `async`/`await` ([composing](https://kotlinlang.org/docs/composing-suspending-functions.html)); sequential when the next call needs the previous result. Rethrow `CancellationException` ([cancellation](https://kotlinlang.org/docs/coroutines-cancellation.html), [exceptions](https://kotlinlang.org/docs/exception-handling.html)). Switch threads with `withContext` (`Dispatchers.IO` / `Default`) ([dispatchers](https://kotlinlang.org/docs/coroutine-context-and-dispatchers.html)). Streams are [Flow](https://kotlinlang.org/docs/coroutines-flow.html) with [operators](https://kotlinlang.org/docs/coroutines-flow-operators.html) (`catch`, `sample`, `stateIn`); `delay` is the cancellation check in a loop. Shared mutable writes use `Mutex.withLock` ([shared state](https://kotlinlang.org/docs/shared-mutable-state-and-concurrency.html)). Do not add a [Channel](https://kotlinlang.org/docs/channels.html) ([channels and coroutines](https://kotlinlang.org/docs/coroutines-and-channels.html)) or experimental [select](https://kotlinlang.org/docs/select-expression.html) unless you need fan-in/fan-out or the first of several deferreds.
