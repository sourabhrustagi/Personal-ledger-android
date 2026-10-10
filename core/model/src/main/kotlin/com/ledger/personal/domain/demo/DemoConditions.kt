package com.ledger.personal.domain.demo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Demo levers the shell can read. A live build never turns these on. */
interface DemoConditions {
    /** True when the demo should behave as if the radio is down. */
    val offline: StateFlow<Boolean>

    /** True when the demo should show empty accounts and lists. */
    val empty: StateFlow<Boolean>
}

/** Live binding. Both levers stay off. */
class SteadyConditions : DemoConditions {
    override val offline: StateFlow<Boolean> = MutableStateFlow(false)
    override val empty: StateFlow<Boolean> = MutableStateFlow(false)
}
