package com.ledger.personal.ui.app

import androidx.lifecycle.ViewModel
import com.ledger.personal.domain.demo.DemoConditions
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Exposes the demo levers selected for this flavor. */
@HiltViewModel
class DemoViewModel @Inject constructor(conditions: DemoConditions) : ViewModel() {
    val offline = conditions.offline
    val empty = conditions.empty
}
