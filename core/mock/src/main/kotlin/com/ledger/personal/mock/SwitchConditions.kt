package com.ledger.personal.mock

import com.ledger.personal.domain.demo.DemoConditions
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Mock flavor binding. The control screen is the only writer of these switches. */
class SwitchConditions @Inject constructor() : DemoConditions {
    override val offline: StateFlow<Boolean> = MockSwitches.offline
    override val empty: StateFlow<Boolean> = MockSwitches.empty
}
