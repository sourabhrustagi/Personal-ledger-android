# 🎯 7-Day Fast-Track Clone Blueprint: Personal Ledger (`LedgerOne`)

A practical, day-by-day engineering roadmap to build, clone, and master the **Personal Ledger (`LedgerOne`)** architecture from scratch. This plan takes you from an empty repository to a production-grade, multi-module Android banking application adhering strictly to financial double-entry bookkeeping, Jetpack Compose design systems, offline-first Room persistence, and standalone feature demo apps.

---

## 📌 Executive Summary

| Dimension | Target Specification |
| :--- | :--- |
| **Duration** | **7 Days** (Intensive: 4–6 hrs/day) or **14 Days** (Paced: 2–3 hrs/day) |
| **Target Architecture** | Three-Tier Multi-Module (`:app` ➔ `:feature:*` ➔ `:core:*`) |
| **Strict Boundary Rule** | **Zero cross-feature dependencies** (enforced by `ModuleBoundaryTest`) |
| **UI Framework** | 100% Jetpack Compose with Material 3 + Custom `:core:designsystem` |
| **Persistence & Sync** | Room SQLite (offline-first) + WorkManager background synchronization |
| **Financial Standard** | Strictly 64-bit integer (`Long`) minor units (paise/cents) — **Zero Float/Double** |
| **Accessibility (a11y)** | WCAG 2.1 AA/AAA compliance, TalkBack semantics, $\ge 48\,\text{dp}$ touch targets |

---

## 🗺️ Project Module Target Topography

Before writing code, understand the exact module topography you will construct:

```mermaid
graph TD
    subgraph AppShell [Application Shell]
        APP[":app<br/>(LedgerOneApp, LedgerNavHost, DI Wiring, Sync Worker)"]
    end

    subgraph Features [Feature Modules (com.android.library)]
        F_AUTH[":feature:auth"]
        F_HOME[":feature:home"]
        F_HISTORY[":feature:history"]
        F_ACCOUNT[":feature:account"]
        F_PROMO[":feature:promotions"]
        F_SCAN[":feature:scan"]
    end

    subgraph FeatureDemos [Standalone Runnable Demos (com.android.application)]
        D_AUTH[":feature:auth:demo"]
        D_HOME[":feature:home:demo"]
        D_HISTORY[":feature:history:demo"]
        D_ACCOUNT[":feature:account:demo"]
        D_PROMO[":feature:promotions:demo"]
        D_SCAN[":feature:scan:demo"]
    end

    subgraph CoreLayer [Core Shared Libraries (Pure Kotlin & Android Library)]
        C_MODEL[":core:model<br/>(Domain Models, Double-Entry Math, Anti-Fraud)"]
        C_DESIGN[":core:designsystem<br/>(Themes, Typography, Primitives, a11y)"]
        C_DATA[":core:database<br/>(Room Entities, DAOs, Migrations)"]
        C_NET[":core:network<br/>(Retrofit, SafeApiCall, Connectivity)"]
        C_MOCK[":core:mock<br/>(Deterministic Fixtures, MockCatalog)"]
        C_TEST[":core:testing<br/>(Test Rules, Fake Repositories)"]
    end

    APP --> F_AUTH & F_HOME & F_HISTORY & F_ACCOUNT & F_PROMO & F_SCAN
    D_AUTH --> F_AUTH
    D_HOME --> F_HOME
    D_HISTORY --> F_HISTORY
    D_ACCOUNT --> F_ACCOUNT
    D_PROMO --> F_PROMO
    D_SCAN --> F_SCAN

    F_AUTH & F_HOME & F_HISTORY & F_ACCOUNT & F_PROMO & F_SCAN --> C_MODEL & C_DESIGN
    F_PROMO --> C_DATA
    APP --> C_DATA & C_NET & C_MOCK
```

---

## 🛠️ Day 0: Environment Setup & Tooling Checklist

Complete this checklist before Day 1:
- [ ] **IDE:** Android Studio (Ladybug / Meerkat / Koala or newer).
- [ ] **JDK:** OpenJDK 17 or 21 configured under Project Structure.
- [ ] **Android SDK:** Platforms 34, 35, and 36 installed.
- [ ] **Device/Emulator:** Android 8.0+ (API 26+) physical device or emulator image.
- [ ] **Git:** Repository initialized with `.gitignore` (ignoring `.gradle/`, `.kotlin/`, `build/`).

---

## 📅 Day-by-Day Implementation Roadmap

### 🚀 Day 1: Gradle Multi-Module Skeleton & Core Domain (`:core:model`)
> **Goal:** Set up the root Gradle project, define central dependency catalogs, and write pure Kotlin financial invariants with 100% test coverage.

#### Tasks:
1. **Gradle Centralization:**
   - Create `gradle/libs.versions.toml` with version catalogs (Compose, Hilt, Room, Retrofit, KotlinX Coroutines).
   - Configure root `settings.gradle.kts` with dependency resolution repositories.
   - Configure root `build.gradle.kts` and root `gradle.properties`.
2. **Create Core Model Module (`:core:model`):**
   - Pure Kotlin library (`plugins { id("kotlin") }`).
   - Define domain entities: `Account`, `Transaction`, `User`, `Promotion`, `EmiSchedule`.
3. **Financial Math & Invariants:**
   - **Minor-Unit Integrity:** Represent all amounts as 64-bit integer `Long` paise/cents.
   - **Double-Entry Bookkeeping:** `post(legs: List<LedgerLeg>)` invariant ensuring $\sum \text{debits} == \sum \text{credits}$.
   - **Remainder-Preserving Expense Splitter:** `splitEvenly(totalMinor, parts)` allocating remainder paise to earliest parts.
   - **Loan EMI & Amortization:** Closed-form annuity calculation (`emiMinor`) and amortization table generation (`amortize`).
4. **Payments & Anti-Fraud Logic:**
   - Card Luhn check (`luhn(pan: String)`).
   - Card masking and PAN redactor (`redactPans`).
   - NPCI UPI intent string generator (`upiPayload`).
   - Payee 30-minute cooling period filter (`coolingBlocks`).
   - `TransferBook` with idempotency keys to prevent duplicate deductions.

#### Verification & Milestone:
```bash
./gradlew :core:model:test
```
* **Checkpoint:** All financial calculation tests pass without a single floating-point calculation.

---

### 🎨 Day 2: Design System, Theming & Accessible Primitives (`:core:designsystem`)
> **Goal:** Build the unified visual identity, WCAG-compliant color systems, tabular typography, and accessible UI widgets.

#### Tasks:
1. **Create Module `:core:designsystem`:**
   - Configure Android Library with Jetpack Compose enabled.
2. **Material 3 Theming Engine (`ui/theme/`):**
   - `Color.kt`: Base semantic palette (`Blue700`, `Slate100`, `Ink`, `Red700`, `Green700`).
   - `Theme.kt`: `LightScheme` and `DarkScheme` with high WCAG contrast:
     - `onSurface` ($> 12:1$ contrast) and `onSurfaceVariant` ($> 7:1$ contrast).
   - `LedgerColors`: Domain tokens for `success`, `warning`, and `danger`.
   - `PersonalLedgerTheme` and `LedgerOneTheme` supporting `ThemeMode` (System, Light, Dark).
3. **Typography & Tabular Figures:**
   - Configure **Inter** variable font family.
   - Enable OpenType tabular numbers (`fontFeatureSettings = "tnum"`) so currency figures align vertically.
4. **Accessible UI Primitives (`ui/components/`):**
   - `LedgerOneText`: Enforces hierarchy via `LedgerOneTextRole` (`Display`, `Title`, `Body`, `Label`).
   - `LedgerOneButton`: Primary action button with minimum $\ge 52\,\text{dp}$ touch height.
   - `LedgerOneTextField`: Material 3 outlined field with `isError` and `supportingText` semantics.
   - `LedgerOneCard`: Standard surface container with `8.dp` corner radius.
   - `NumericKeypad`: 3x4 keypad with $60\,\text{dp}$ / $72\,\text{dp}$ touch targets and $56\,\text{dp}$ backspace button.
   - `OtpField`: 6-digit discrete PIN input with auto-advance and TalkBack length announcement.
   - `MaskedAmount`: Account balance component that toggles between masked (`₹ • • • • •`) and spoken balance (`contentDescription = spokenAmount`).
   - `Skeleton`: Shimmer placeholder with `semantics { contentDescription = "Loading" }`.
   - `FeatureScaffold`: Standard screen scaffold with top bar, title, back arrow, and scroll support.
5. **Multilingual Architecture:**
   - Setup English `strings.xml` and complete Hindi `values-hi/strings.xml`.
   - Expose type-safe string resources via `AppStrings.kt`.

#### Verification & Milestone:
- Preview components in Android Studio using `@PreviewLightDark`.
- Run TalkBack on an emulator to verify that `MaskedAmount` announces *"Balance hidden"* when hidden and reads exact amount when revealed.

---

### 💾 Day 3: Data Layer — Offline-First Room DB, Network & Mock Harness
> **Goal:** Implement the local Room database, Retrofit network abstraction, and in-memory mock catalog.

#### Tasks:
1. **Create Core Data Modules:**
   - `:core:database` (Room database & DAOs).
   - `:core:network` (Retrofit, OkHttp, HTTP models).
   - `:core:mock` (Deterministic mock seeds).
2. **Room Database Setup (`:core:database`):**
   - Create `PromotionEntity` with `@PrimaryKey`, discount percentage, and expiry timestamp.
   - Create `PromotionDao` with `@Query`, `@Upsert`, and reactive `Flow<List<PromotionEntity>>`.
   - Build `LedgerDatabase` with Room database migrations.
3. **Network Layer (`:core:network`):**
   - Setup Retrofit 2 with Kotlin Serialization converter.
   - Implement `SafeApiCall` wrapper returning `Result<T>`:
     - **Critical Rule:** Never swallow `CancellationException`—rethrow it so coroutines cancel cleanly!
   - Reactive network connectivity observer via Android `ConnectivityManager`.
4. **Mock Catalog (`:core:mock`):**
   - Build `MockCatalog` and `MockValues` to supply seed data (accounts, cards, transactions, banners) for standalone feature demo apps.

#### Verification & Milestone:
```bash
./gradlew :core:database:test :core:network:test
```
* **Checkpoint:** Room DAO emits reactive lists; `SafeApiCall` handles HTTP 500, timeouts, and coroutine cancellation properly.

---

### 🔐 Day 4: Security Layer, Biometrics, Keystore & Payment FSM
> **Goal:** Implement hardware Keystore envelope encryption, biometric authentication, PII sanitization, and the bulletproof payment state machine.

#### Tasks:
1. **Hardware-Backed Keystore & Encryption:**
   - `AndroidKeyStoreMasterKeyVault`: Generate AES-256 Master Key inside hardware StrongBox / TEE.
   - Envelope encryption for database and local storage.
   - `PiiMasker`: Memory sanitization to zeroize sensitive byte arrays and char buffers immediately after use.
2. **Biometric Integration:**
   - Wrapper around AndroidX `BiometricPrompt` with fallback to device PIN / pattern.
3. **Payment Finite State Machine (`PaymentStateMachine.kt`):**
   - Implement the payment flow states:
     $$\text{Form} \longrightarrow \text{AwaitingAuth} \longrightarrow \text{Submitting} \longrightarrow \text{Success} \mid \text{InDoubt} \mid \text{Failed}$$
   - Support client-generated **Idempotency Keys** to prevent double-charging on retries.
   - Exponential backoff retry handler for dropped network requests.

#### Verification & Milestone:
- Unit test the Payment FSM covering transient network drops, terminal declines, and idempotency key consistency.

---

### 📱 Day 5: Feature Modules (Part 1) — Auth & Home Dashboard + Demo Apps
> **Goal:** Build `:feature:auth` and `:feature:home` as isolated libraries, accompanied by standalone runnable demo applications.

#### Tasks:
1. **Build `:feature:auth`:**
   - `PhoneStep`: Mobile number entry with regex validation.
   - `OtpStep`: 6-digit OTP entry with auto-focus, resend countdown timer, and loading progress bar.
   - `LoginViewModel` & `OnboardingViewModel`: StateFlow-driven UI state with simulated verification delays.
2. **Build `:feature:home`:**
   - Prominent balance card with masked/revealed toggle and account number.
   - Promotional offer banner carousel (`HorizontalPager`).
   - Quick action shortcuts (Send, Receive, Passbook, Scan).
   - Loading indicator with simulated API delay, error banner, and pull-to-refresh.
3. **Build Standalone Demo Companion Apps:**
   - Create `:feature:auth:demo` (application module) with `AuthDemoActivity`.
   - Create `:feature:home:demo` (application module) with `HomeDemoActivity`.
   - Bind navigation callbacks to lightweight Android `Toast` notifications.

#### Verification & Milestone:
```bash
./gradlew :feature:home:demo:installDebug
./gradlew :feature:auth:demo:installDebug
```
* **Checkpoint:** Both demo apps launch independently on the device in $< 3$ seconds without compiling `:app`!

---

### 📊 Day 6: Feature Modules (Part 2) — History, Account, Promotions & QR Scanner
> **Goal:** Build the remaining product features and their respective demo companion applications.

#### Tasks:
1. **Build `:feature:history` (Passbook):**
   - Grouped transaction list by date with sticky headers.
   - Category badges (`Food`, `Shopping`, `Transfer`, `Bills`) with semantic colors.
   - `LazyColumn` infinite scrolling with prefetch loading indicators.
   - Companion app: `:feature:history:demo`.
2. **Build `:feature:account`:**
   - User profile screen displaying user name, phone, KYC verification badge, and security settings.
   - Lock app and logout callbacks.
   - Companion app: `:feature:account:demo`.
3. **Build `:feature:promotions`:**
   - Offers and cashback coupons screen.
   - Room-backed caching with offline fallbacks.
   - Companion app: `:feature:promotions:demo`.
4. **Build `:feature:scan`:**
   - CameraX QR viewfinder with ML Kit barcode recognition.
   - Companion app: `:feature:scan:demo`.

#### Verification & Milestone:
```bash
./gradlew :feature:history:demo:installDebug :feature:account:demo:installDebug
```
* **Checkpoint:** History passbook smoothly scrolls at 120 FPS; Account screen renders user profile and handles lock/logout events.

---

### 🚢 Day 7: App Shell Integration, Navigation, Offline Sync & Quality Gates
> **Goal:** Assemble all feature modules into the `:app` monolith, configure WorkManager sync, and pass all architectural quality gates.

#### Tasks:
1. **Assemble `:app` Monolith:**
   - In `app/build.gradle.kts`, declare dependencies on all `:feature:*` modules and `:core:*` libraries.
2. **Compose Navigation (`LedgerNavHost.kt`):**
   - Implement bottom navigation bar with 5 items: **Home**, **Passbook**, **Scan**, **Offers**, **Account**.
   - Wire route transitions and backstack handling.
3. **Background Offline Synchronization:**
   - Setup `SyncWorker` (`WorkManager`) with constraints: `NetworkType.CONNECTED` + `BatteryNotLow`.
   - Schedule periodic synchronization every 15 minutes.
4. **Architectural Quality Gates & Enforcement:**
   - **`ModuleBoundaryTest`:** Automated Gradle test that scans every `build.gradle.kts` in `feature/` to guarantee **zero cross-feature dependencies**.
   - **Zero Raw Hex Colors:** Verify no `Color(0x...)` exists in feature modules (must use `MaterialTheme.colorScheme` or `LedgerTheme.colors`).
   - **Detekt Analysis:** Run static analysis checks (`./gradlew detekt`).
   - **Unit & UI Tests:** Execute all unit tests across all modules.

#### Verification & Milestone:
```bash
./gradlew testMockDebugUnitTest :core:model:test detekt
```
* **Final Checkpoint:** Working release APK builds successfully, all tests pass, and working tree is 100% clean.

---

## ⏱️ Daily Time Allocation & Milestones Table

| Day | Focus Area | Target Modules | Est. Hours | Key Milestone Deliverable |
| :---: | :--- | :--- | :---: | :--- |
| **Day 1** | Gradle Setup & Core Math | `:core:model` | 5 hrs | 100% green unit tests on double-entry & loan math |
| **Day 2** | Design System & a11y | `:core:designsystem` | 5 hrs | Interactive Compose Previews with WCAG compliance |
| **Day 3** | Room DB, Network & Mocks | `:core:database`, `:core:network`, `:core:mock` | 5 hrs | Offline Room DAO + safe Retrofit network client |
| **Day 4** | Security & Payment FSM | `:core:model`, Keystore, Biometrics | 4 hrs | Idempotent payment finite state machine tested |
| **Day 5** | Auth & Home + Demos | `:feature:auth`, `:feature:home` (+ demos) | 6 hrs | Runnable `:feature:home:demo` & `:feature:auth:demo` |
| **Day 6** | History, Account & Scan | `:feature:history`, `:feature:account`, `:feature:scan` | 6 hrs | All isolated feature screens verified in standalone demos |
| **Day 7** | App Shell, Sync & CI Gates| `:app`, Navigation, WorkManager, Detekt | 5 hrs | Full application assembled & passes `ModuleBoundaryTest` |

---

## 🏆 The 10 Invariants of a Successful Clone

Before marking the project as complete, verify that your implementation satisfies all 10 invariants:

1. [ ] **Minor-Unit Only:** Zero occurrences of `Double` or `Float` representing money (`Long` paise only).
2. [ ] **Zero Feature-to-Feature Coupling:** No feature module imports or depends on another feature module.
3. [ ] **Isolated Demo Apps:** Every feature module has a working, runnable `:demo` companion app.
4. [ ] **Balanced Double-Entry:** The ledger engine rejects any transaction where $\sum \text{debits} \neq \sum \text{credits}$.
5. [ ] **Idempotent Transfers:** Repeated transfers with the same `idempotencyKey` return identical records without duplicate debits.
6. [ ] **Never Swallow Cancellation:** Every `catch` block on `Exception` re-throws `CancellationException`.
7. [ ] **Accessible Touch Targets:** Every button, icon, and clickable surface is $\ge 48 \times 48\,\text{dp}$.
8. [ ] **Screen Reader Contrast:** All text meets WCAG AA ($> 4.5:1$) and titles meet WCAG AAA ($> 7:1$).
9. [ ] **Tabular Numerals:** Financial figures use Inter with OpenType `tnum` to align vertically.
10. [ ] **Green Build:** `./gradlew testMockDebugUnitTest :core:model:test detekt` passes without warnings.
