package com.ledger.personal

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.ledger.personal.ui.app.LedgerOneApp
import dagger.hilt.android.AndroidEntryPoint

/** Main activity. */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    /** Reacts to create. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LedgerOneApp() }
    }

    /** Keeps the latest link on the activity so navigation can open it after unlock. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
