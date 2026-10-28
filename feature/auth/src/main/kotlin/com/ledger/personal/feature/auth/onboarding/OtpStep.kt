package com.ledger.personal.feature.auth.onboarding

import com.ledger.personal.feature.auth.model.AppConstants
import com.ledger.personal.feature.auth.model.OtpUiState

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.mock.MockValues
import com.ledger.personal.ui.components.BannerTone
import com.ledger.personal.ui.components.OtpField
import com.ledger.personal.ui.components.PrimaryButton
import com.ledger.personal.ui.components.StatusBanner
import com.ledger.personal.ui.components.TextLink
import com.ledger.personal.ui.theme.AppStrings
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Shows OTP verification step with timer state managed by ViewModel StateFlow. */
@Composable
fun OtpStep(
    phone: String,
    uiState: OtpUiState = OtpUiState(),
    resendCountdown: Int = uiState.resendCountdown,
    onEdit: () -> Unit,
    onResend: (() -> Unit)? = null,
    onVerifyClick: (String) -> Unit = {},
    onDismissError: () -> Unit = {},
) {
    val sentMessage = uiState.sentMessage
    val errorMessage = uiState.errorMessage
    val isResending = uiState.isSending
    var code by rememberSaveable { mutableStateOf("") }
    val isError = uiState.isOtpError

    Text(
        text = stringResource(AppStrings.TITLE_ENTER_CODE),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = stringResource(AppStrings.SUBTITLE_CODE_SENT_TO, phone),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    TextLink(stringResource(AppStrings.ACTION_CHANGE_NUMBER), onEdit)
    OtpField(code, {
        code = it
        onDismissError()
    })
    if (isError) StatusBanner(stringResource(AppStrings.ERROR_INVALID_OTP), BannerTone.ERROR)
    errorMessage?.let { StatusBanner(it, BannerTone.ERROR) }
    StatusBanner(sentMessage, BannerTone.INFO)
    StatusBanner(stringResource(AppStrings.INFO_DEMO_OTP, MockValues.OTP), BannerTone.INFO)
    if (resendCountdown > 0) {
        val timerText = "0:${resendCountdown.toString().padStart(2, '0')}"
        Text(
            text = stringResource(AppStrings.LABEL_RESEND_CODE_TIMER, timerText),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        TextLink(
            text = if (isResending) {
                stringResource(AppStrings.ACTION_RESENDING_CODE)
            } else {
                stringResource(AppStrings.ACTION_RESEND_CODE)
            },
            onClick = {
                if (!isResending) {
                    onResend?.invoke()
                }
            },
        )
    }
    PrimaryButton(
        text = stringResource(AppStrings.ACTION_VERIFY),
        onClick = { onVerifyClick(code) },
        enabled = code.length == AppConstants.OTP_CODE_LENGTH,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Backward-compatible overload for legacy callers and previews. */
@Composable
fun OtpStep(
    phone: String,
    uiState: OtpUiState = OtpUiState(),
    resendCountdown: Int = uiState.resendCountdown,
    onEdit: () -> Unit,
    onResend: (() -> Unit)? = null,
    onVerified: () -> Unit,
) = OtpStep(
    phone = phone,
    uiState = uiState,
    resendCountdown = resendCountdown,
    onEdit = onEdit,
    onResend = onResend,
    onVerifyClick = { if (it == MockValues.OTP) onVerified() },
    onDismissError = {},
)

@PreviewLightDark
@Composable
private fun OtpStepPreview() {
    PersonalLedgerTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OtpStep(MockValues.DEFAULT_PHONE, onEdit = {}, onVerified = {})
            }
        }
    }
}
