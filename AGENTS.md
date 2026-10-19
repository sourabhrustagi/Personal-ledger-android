# Agent Coding Rules & Engineering Guidelines

This document outlines the architectural rules, coding standards, and Kotlin coroutine practices for the Personal Ledger (`LedgerOne`) codebase. All automated agents and human contributors must adhere to these guidelines.

---

## 1. Kotlin Coroutines & Asynchronous Concurrency

### 1.1 Structured Concurrency
- **Lifecycle Scopes:** Always launch coroutines within a well-defined lifecycle scope. In ViewModels, use `viewModelScope.launch`. In composables, use `LaunchedEffect` or `rememberCoroutineScope`. In domain/data layers, use `coroutineScope { }` or `supervisorScope { }`.
- **No GlobalScope:** Never use `GlobalScope`. It breaks lifecycle boundaries and leaks memory and background work.

```kotlin
// CORRECT
class WalletViewModel @Inject constructor(...) : ViewModel() {
    fun sendMoney(id: String, amount: Long) {
        viewModelScope.launch {
            repository.transfer(id, amount)
        }
    }
}

// INCORRECT
GlobalScope.launch { repository.transfer(id, amount) }
```

### 1.2 Cancellation and Exception Propagation
- **Never Swallow `CancellationException`:** Coroutine cancellation in Kotlin is cooperative and implemented by throwing `CancellationException`. If you catch `Exception` or `Throwable`, you MUST rethrow `CancellationException`.
- **`runCatching` with Coroutines:** Standard `runCatching` captures `Throwable`, including `CancellationException`. When wrapping suspend calls, always propagate cancellation using `.onFailure { if (it is CancellationException) throw it }`.

```kotlin
// CORRECT: try-catch with cancellation rethrow
try {
    api.submitPayment(payload)
} catch (e: CancellationException) {
    throw e
} catch (e: IOException) {
    Result.failure(e)
}

// CORRECT: runCatching with cancellation rethrow
val result = runCatching { api.fetchStatement() }
    .onFailure { if (it is CancellationException) throw it }

// INCORRECT: Swallows cancellation, preventing coroutine termination
try {
    api.submitPayment(payload)
} catch (e: Exception) {
    Log.e(TAG, "Error", e)
}
```

- **Loop Cancellation Checks:** Long-running or repetitive suspend loops must yield control to check cancellation periodically using `delay(...)`, `yield()`, or checking `coroutineContext.isActive`.

### 1.3 Composing Suspending Functions
- **Sequential Execution:** Suspend calls within the same coroutine execute sequentially by default. Use sequential execution when subsequent calls depend on previous results.
- **Concurrent Execution with `async`:** When two or more suspend calls are independent, run them concurrently inside `coroutineScope` using `async` and await both results.

```kotlin
// CORRECT: Concurrent independent execution
suspend fun loadDashboardData(): DashboardData = coroutineScope {
    val versionDeferred = async { api.version() }
    val statusDeferred = async { api.status() }
    
    DashboardData(
        version = versionDeferred.await(),
        status = statusDeferred.await()
    )
}
```

### 1.4 Dispatchers & Context Switching
- **Switch Threads with `withContext`:** Use `withContext` to move work off the main thread:
  - `Dispatchers.IO`: Blocking disk I/O, file operations (e.g. PDF generation), SQLite database transactions, network requests.
  - `Dispatchers.Default`: CPU-intensive computations (cryptographic key generation, JSON parsing, large array transformations).
  - `Dispatchers.Main`: UI event dispatching and Compose state updates.
- **Main-Safety:** Suspend functions in repositories and data sources must be main-safe (safe to call from the main thread).

```kotlin
// CORRECT: Main-safe repository method
suspend fun writePdf(context: Context, summary: StatementSummary): File =
    withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "statement.pdf")
        // ... write file ...
        file
    }
```

### 1.5 Asynchronous Flows & State
- **Cold Flows for Streams:** Repositories and DAOs expose cold `Flow<T>`.
- **Hot State in ViewModels:** Expose state to Compose via read-only `StateFlow<T>` using `stateIn`:
  ```kotlin
  val uiState: StateFlow<UiState> = repository.observeItems()
      .map { UiState.Success(it) }
      .catch { emit(UiState.Error(it.message)) }
      .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5_000),
          initialValue = UiState.Loading
      )
  ```
- **State Updates:** Use atomic updates with `MutableStateFlow.update { it.copy(...) }`.

### 1.6 Shared Mutable State & Thread Safety
- **Coroutines Concurrency:** Do not use plain non-thread-safe collections (`mutableListOf()`, `mutableSetOf()`, `hashMapOf()`) across multiple coroutines or background threads.
- **`Mutex.withLock` for Critical Sections:** Protect shared mutable writes across suspend functions using `kotlinx.coroutines.sync.Mutex`. Never use Java's `synchronized` on suspend functions or block a thread with `sleep()`.
- **Concurrent Collections:** For lock-free concurrent shared state across threads:
  - `ConcurrentHashMap` for maps.
  - `ConcurrentHashMap.newKeySet<T>()` for sets.
  - `AtomicInteger`, `AtomicLong`, `AtomicBoolean` for atomic counters and flags.

```kotlin
// CORRECT: Mutex in ViewModel
class AssistantViewModel @Inject constructor(...) : ViewModel() {
    private val writes = Mutex()
    private val history = mutableListOf<Entry>()

    fun recordTurn(entry: Entry) {
        viewModelScope.launch {
            writes.withLock {
                history.add(entry)
            }
        }
    }
}
```

### 1.7 Channels vs Flows
- Default to `Flow` for reactive streams.
- Use `Channel` only for fan-in/fan-out pipelines or single-consumer queue buffers (e.g. one-off UI side-effect events).
- Avoid experimental `select` expressions unless explicitly racing multiple deferred computations.

---

## 2. Project Architecture & Modularization

### 2.1 Three-Tier Architecture
- **`:app`:** Application shell, navigation graph integration, flavors, DI wiring, and top-level hubs/tools.
- **`:core:*`:** Reusable domain, network, database, design system, and testing libraries.
- **`:feature:*`:** Distinct product features (e.g. `:feature:ledger`, `:feature:wallet`, `:feature:banking`).

### 2.2 Strict Feature Isolation
- **No Feature-to-Feature Dependencies:** Feature modules must NEVER declare dependencies on or import from other feature modules.
- **Enforced by `ModuleBoundaryTest`:** Any cross-feature dependency will break the build.
- **Code Sharing via Core:** If two features need common models, utilities, or components, move that code to an appropriate `core/*` module.

### 2.3 Single Responsibility Principle (SRP)
- Every UI component, model, and utility must have a single responsibility.
- Do not combine multiple independent UI components into a single file (e.g., keep button, text, text field, and card in distinct files such as `LedgerOneButton.kt`, `LedgerOneText.kt`, `LedgerOneTextField.kt`, `LedgerOneCard.kt`).
- Keep `@PreviewLightDark` composable previews in the same file as the component they display.

---

## 3. Financial Integrity & Domain Business Rules

### 3.1 Minor-Unit Money Representation
- **Strictly `Long`:** Money amounts must always be stored, computed, and passed as 64-bit integers (`Long`) in minor currency units (paise or cents).
- **No Floating-Point Math:** Never use `Double` or `Float` to represent currency balances, transaction amounts, or ledger values.
- **Formatting:** Use `formatMoney` from `core/designsystem` with proper currency symbols and Indian digit grouping (`₹1,23,456.78`).

### 3.2 Double-Entry Bookkeeping Invariants
- All entries must balance: $\sum \text{debits} == \sum \text{credits}$.
- Every leg must be strictly non-negative and specify either a debit or a credit, never both.
- Empty leg lists are invalid and must be rejected.

### 3.3 Expense Splitting (`splitEvenly`)
- When dividing minor units among $N$ parts, any remainder paise (`totalMinor % parts`) must be allocated one-by-one to the earliest parts to ensure zero leakage:
  $\sum \text{parts} == \text{totalMinor}$.

### 3.4 Payments & Anti-Fraud Invariants
- **Idempotency Keys:** Every transfer write must pass an idempotency key to prevent double charging on retries (`TransferBook`).
- **Payee Cooling Period:** Transfers to new payees are blocked for 30 minutes following addition (`coolingBlocks`).
- **Card Luhn Check & PAN Masking:** Card numbers (13–19 digits) must pass the Luhn algorithm; any card numbers displayed or logged must be masked (`****`).

---

## 4. Jetpack Compose & UI Design Standards

### 4.1 Design System Adherence
- Screens must use theme colors from `MaterialTheme.colorScheme` or `LedgerTheme.colors`.
- **Zero Raw Hex Colors:** Never use `Color(0x...)` or hardcoded hex values in `feature/**`. All colors must originate from `core/designsystem`. `ModuleBoundaryTest` enforces this rule.
- Base typography on `LedgerOneText` with appropriate `LedgerOneTextRole` (Display, Title, Body, Label).

### 4.2 Offline-First UI
- Screens observe Room database entities mapped to domain models.
- When a user performs an action, update the local database immediately with a pending sync state (`PENDING_CREATE`, `PENDING_UPDATE`, `PENDING_DELETE`).
- Sync workers push changes in the background when connectivity is restored.

---

## 5. Kotlin Idioms & Coding Style

- **Exception Handling:**
  - Use `require` / `requireNotNull` for validating arguments.
  - Use `check` / `error` for illegal object states.
  - Use `firstOrNull`, `getOrNull`, `toLongOrNull` instead of catching `NoSuchElementException` or `NumberFormatException`.
  - Custom exceptions extend `Exception` and supply a descriptive message and optional cause.
  - Never catch `Error` or `VirtualMachineError`.
- **Functions:**
  - Default and named arguments instead of method overloads.
  - Single-expression function syntax (`fun calculate() = ...`) when the body is a single expression.
  - Mark recursive functions `tailrec` only when the recursive call is the terminal operation outside try-catch.
- **Lambdas & Scope Functions:**
  - Trailing lambdas placed outside parentheses `()`.
  - Use `it` for single parameter; `_` for unused parameters.
  - Use callable references (`Entity::toDomain`) over redundant lambda wraps (`{ it.toDomain() }`).
  - `apply` to configure object properties; `also` for side effects; `let` for non-null scoping; `run` for scoping a transformation. Do not nest scope functions.
- **Properties:**
  - Use `val` with getter for argument-free reads (`val size: Int get() = ...`).
  - Use `private set` for values mutated internally (`var charges: Int = 0 private set`).
  - Use `const val` for compile-time constants.
  - Use `by lazy` for expensive one-time initialization.
- **Detekt Limits:**
  - Respect `ThrowsCount`: maximum 2 `throw` statements per function.

---

## 6. Testing, Commits & Version Control

- **Unit Tests:** Run before pushing:
  ```bash
  ./gradlew testMockDebugUnitTest :core:model:test detekt
  ```
- **Commit Granularity:** Always commit changes file-by-file (one file per commit) with clear, imperative messages matching project history.
- **Synchronize Remote:** Push changes to `origin/main` upon completion and verify working tree is clean (`git status`).
