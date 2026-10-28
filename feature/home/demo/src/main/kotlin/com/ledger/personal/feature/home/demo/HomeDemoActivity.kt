package com.ledger.personal.feature.home.demo

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.ledger.personal.feature.home.HomeScreen
import com.ledger.personal.feature.home.HomeViewModel
import com.ledger.personal.mock.MockCatalog
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PersonalLedgerTheme {
                val homeVm: HomeViewModel = hiltViewModel()
                var revealBalance by remember { mutableStateOf(true) }
                HomeScreen(
                    name = "Asha Rao (Demo)",
                    balanceMinor = MockCatalog.bankAccounts.first().balanceMinor,
                    revealBalance = revealBalance,
                    offline = false,
                    onToggleReveal = { revealBalance = !revealBalance },
                    onScanClick = {
                        Toast.makeText(this, "Demo: Scan & Pay clicked", Toast.LENGTH_SHORT).show()
                    },
                    onAccountClick = {
                        Toast.makeText(this, "Demo: Account clicked", Toast.LENGTH_SHORT).show()
                    },
                    viewModel = homeVm,
                )
            }
        }
    }
}
