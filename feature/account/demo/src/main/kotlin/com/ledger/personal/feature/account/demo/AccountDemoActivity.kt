package com.ledger.personal.feature.account.demo

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ledger.personal.feature.account.AccountScreen
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AccountDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PersonalLedgerTheme {
                AccountScreen(
                    name = "Asha Rao (Demo)",
                    phone = "+91 98765 43210",
                    onLock = {
                        Toast.makeText(this, "Demo: App Locked", Toast.LENGTH_SHORT).show()
                    },
                    onLogout = {
                        Toast.makeText(this, "Demo: Logged Out", Toast.LENGTH_SHORT).show()
                    },
                    onBack = { finish() },
                )
            }
        }
    }
}
