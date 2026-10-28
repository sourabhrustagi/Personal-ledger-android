package com.ledger.personal.feature.scan.demo

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ledger.personal.feature.scan.ui.ScanScreen
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScanDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PersonalLedgerTheme {
                ScanScreen(
                    liveCamera = false,
                    onBack = { finish() },
                    onTarget = { target ->
                        Toast.makeText(this, "Scanned: ${target.vpa} (${target.amountMinor} paise)", Toast.LENGTH_LONG).show()
                    },
                )
            }
        }
    }
}
