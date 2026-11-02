package com.ledger.personal.feature.history.switcher

/**
 * Enumeration of the state management and MVI architectures available
 * for rendering the History feature.
 */
enum class HistoryArchitecture(
    val id: String,
    val displayName: String,
    val authorOrCompany: String,
    val paradigm: String,
    val summary: String,
    val tags: List<String>,
) {
    STANDARD_VIEWMODEL(
        id = "standard_vm",
        displayName = "Android ViewModel",
        authorOrCompany = "Google / AndroidX",
        paradigm = "Jetpack ViewModel + PagingData Flow",
        summary = "Standard Android Architecture Components with cachedIn PagingData stream and cold flows.",
        tags = listOf("AndroidX", "ViewModel", "Paging 3", "StateFlow"),
    ),
    ORBIT_MVI(
        id = "orbit",
        displayName = "Orbit MVI",
        authorOrCompany = "Orbit MVI Community",
        paradigm = "ContainerHost + intent { reduce / postSideEffect }",
        summary = "Pragmatic coroutine-based MVI framework centered on ContainerHost with type-safe state transitions and side-effects.",
        tags = listOf("MVI", "ContainerHost", "SideEffects", "UDF"),
    ),
    CASHAPP_MOLECULE(
        id = "molecule",
        displayName = "Molecule",
        authorOrCompany = "CashApp (Jake Wharton)",
        paradigm = "Composable Presenter Function (@Composable () -> State)",
        summary = "Replaces ViewModel state operators with declarative Compose snapshot state, emitting a single StateFlow.",
        tags = listOf("CashApp", "Compose Runtime", "Snapshot State", "Presenter"),
    ),
    SLACK_CIRCUIT(
        id = "circuit",
        displayName = "Circuit",
        authorOrCompany = "Slack Technologies",
        paradigm = "CircuitPresenter + CircuitUiState with eventSink",
        summary = "Slack's Compose-driven architecture where state models embed an eventSink lambda, decoupling presenter from UI.",
        tags = listOf("Slack", "EventSink", "Presenter", "CircuitScreen"),
    ),
    ARKIVANOV_DECOMPOSE(
        id = "decompose",
        displayName = "Decompose",
        authorOrCompany = "Arkadii Ivanov",
        paradigm = "ComponentContext Tree + Value<State> + InstanceKeeper",
        summary = "Kotlin Multiplatform component architecture independent of Android ViewModel, managing lifecycle and back handling.",
        tags = listOf("Decompose", "ComponentContext", "Value<T>", "Retained"),
    ),
    ARKIVANOV_MVIKOTLIN(
        id = "mvikotlin",
        displayName = "MVIKotlin",
        authorOrCompany = "Arkadii Ivanov",
        paradigm = "Store<Intent, State, Label> + Executor + Reducer",
        summary = "Full pure-Kotlin MVI framework separating business execution (Executor) from pure state mutation (Reducer).",
        tags = listOf("MVIKotlin", "Store", "Executor", "Reducer", "Bootstrapper"),
    ),
    VOYAGER_SCREENMODEL(
        id = "voyager",
        displayName = "Voyager",
        authorOrCompany = "Adriel Café",
        paradigm = "Screen + StateScreenModel<State> (screenModelScope)",
        summary = "Compose Multiplatform navigation library featuring StateScreenModel for retained screen state management.",
        tags = listOf("Voyager", "ScreenModel", "StateScreenModel", "KMP"),
    ),
    AIRBNB_MAVERICKS(
        id = "mavericks",
        displayName = "Mavericks (MvRx)",
        authorOrCompany = "Airbnb",
        paradigm = "MavericksViewModel + Async<T> State Machine",
        summary = "Airbnb's state machine modeling async requests with Uninitialized, Loading, Success, Fail, and atomic setState.",
        tags = listOf("Airbnb", "MvRx", "Async<T>", "setState", "withState"),
    );

    companion object {
        val ALL = entries.toList()
    }
}
