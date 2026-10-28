package com.ledger.personal.feature.promotions.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.ledger.personal.feature.promotions.PromotionsScreen
import com.ledger.personal.feature.promotions.PromotionsViewModel
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PromotionsDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PersonalLedgerTheme {
                val viewModel: PromotionsViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                PromotionsScreen(
                    uiState = uiState,
                    onSelect = viewModel::select,
                )
            }
        }
    }
}
