package com.ledger.personal.feature.update.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ledger.personal.feature.update.ForceUpdateScreen
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UpdateDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PersonalLedgerTheme {
                ForceUpdateScreen(versionName = "2.0.0 (Mock Environment)")
            }
        }
    }
}
