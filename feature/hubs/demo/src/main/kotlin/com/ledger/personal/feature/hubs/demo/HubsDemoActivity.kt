package com.ledger.personal.feature.hubs.demo

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledger.personal.feature.hubs.PayHub
import com.ledger.personal.feature.hubs.PromotionsHub
import com.ledger.personal.feature.hubs.PromotionsViewModel
import com.ledger.personal.ui.components.HubFlags
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HubsDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PersonalLedgerTheme {
                HubsDemoScreen(
                    onAction = { action ->
                        Toast.makeText(this, action, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun HubsDemoScreen(onAction: (String) -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var revealBalance by remember { mutableStateOf(false) }
    val tabs = listOf("Pay Hub", "Promotions")

    val flags = HubFlags(
        offline = false,
        empty = false,
        reveal = revealBalance,
        onToggleReveal = { revealBalance = !revealBalance },
    )

    Scaffold(
        topBar = {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> {
                    PayHub(
                        flags = flags,
                        onScanClick = { onAction("Navigate to Scan") },
                        onPayTo = { recipient, amount -> onAction("Pay $amount to $recipient") },
                    )
                }
                1 -> {
                    val promoVm: PromotionsViewModel = hiltViewModel()
                    val promoState by promoVm.uiState.collectAsStateWithLifecycle()
                    PromotionsHub(
                        uiState = promoState,
                        onSelect = promoVm::select,
                    )
                }
            }
        }
    }
}
