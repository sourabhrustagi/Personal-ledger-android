package com.ledger.personal.feature.auth.demo

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledger.personal.feature.auth.login.LoginViewModel
import com.ledger.personal.feature.auth.login.OtpLoginScreen
import com.ledger.personal.feature.auth.login.OtpLoginUiState
import com.ledger.personal.feature.auth.onboarding.OnboardingFlow
import com.ledger.personal.feature.auth.onboarding.OnboardingViewModel
import com.ledger.personal.mock.MockValues
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AuthDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PersonalLedgerTheme {
                AuthDemoScreen(
                    onAuthSuccess = { message ->
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun AuthDemoScreen(
    onAuthSuccess: (String) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Onboarding", "Login")

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
                    val onboardingVm: OnboardingViewModel = hiltViewModel()
                    val otpState by onboardingVm.otpState.collectAsStateWithLifecycle()
                    val resendCountdown by onboardingVm.resendCountdown.collectAsStateWithLifecycle()
                    OnboardingFlow(
                        otpState = otpState,
                        resendCountdown = resendCountdown,
                        onSendOtpClick = onboardingVm::onSendOtpClick,
                        onEditPhoneClick = onboardingVm::onEditPhoneClick,
                        onResendOtpClick = onboardingVm::onResendOtpClick,
                        onVerifyOtpClick = { code, phone ->
                            onboardingVm.onVerifyOtpClick(code, phone) {
                                onAuthSuccess("Onboarding finished!")
                            }
                        },
                        onDismissError = onboardingVm::dismissOtpError,
                    )
                }
                1 -> {
                    val loginVm: LoginViewModel = hiltViewModel()
                    val resendCountdown by loginVm.resendCountdown.collectAsStateWithLifecycle()
                    OtpLoginScreen(
                        uiState = OtpLoginUiState(
                            phone = MockValues.DEFAULT_PHONE,
                            demoOtp = true,
                        ),
                        resendCountdown = resendCountdown,
                        onResend = loginVm::startResendTimer,
                        onVerifyOtp = { code, onResult ->
                            loginVm.verifyOtp(code) { success ->
                                onResult(success)
                                if (success) onAuthSuccess("Login successful!")
                            }
                        },
                        onReset = {
                            loginVm.signOutEverywhere {
                                onAuthSuccess("Reset completed")
                            }
                        },
                    )
                }
            }
        }
    }
}
