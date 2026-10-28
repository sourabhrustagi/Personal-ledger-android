package com.ledger.personal.feature.history.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ledger.personal.feature.history.LedgerHistoryScreen
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HistoryDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PersonalLedgerTheme {
                LedgerHistoryScreen(
                    onBack = { finish() },
                    title = "Passbook (Demo)",
                )
            }
        }
    }
}
