package com.ledger.personal.feature.wallet.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.hilt.navigation.compose.hiltViewModel
import com.ledger.personal.feature.wallet.ui.WalletScreen
import com.ledger.personal.feature.wallet.ui.WalletViewModel
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WalletDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PersonalLedgerTheme {
                val walletVm: WalletViewModel = hiltViewModel()
                WalletScreen(
                    viewModel = walletVm,
                    onBack = { finish() },
                )
            }
        }
    }
}
