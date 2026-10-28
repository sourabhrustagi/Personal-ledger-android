package com.ledger.personal.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.mock.MockValues
import com.ledger.personal.ui.components.BannerTone
import com.ledger.personal.ui.components.PrimaryButton
import com.ledger.personal.ui.components.StatusBanner
import com.ledger.personal.ui.theme.AppStrings
import com.ledger.personal.ui.theme.PersonalLedgerTheme

private const val PHONE_LENGTH = 10

/** Shows phone number input step. */
@Composable
fun PhoneStep(
    phone: String,
    onPhone: (String) -> Unit,
    uiState: OtpUiState = OtpUiState(),
    onSendClick: () -> Unit = {},
) {
    var consent by rememberSaveable { mutableStateOf(false) }
    val isSending = uiState.isSending
    val errorMessage = uiState.errorMessage

    Text(
        text = stringResource(AppStrings.TITLE_ENTER_MOBILE),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = stringResource(AppStrings.SUBTITLE_PHONE_VERIFICATION),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (errorMessage != null) {
        StatusBanner(errorMessage, BannerTone.ERROR)
    }
    OutlinedTextField(
        value = phone,
        onValueChange = { next ->
            if (next.length <= PHONE_LENGTH && next.all(Char::isDigit)) {
                onPhone(next)
            }
        },
        label = { Text(stringResource(AppStrings.LABEL_MOBILE_NUMBER)) },
        prefix = { Text(stringResource(AppStrings.PHONE_PREFIX)) },
        supportingText = { Text(stringResource(AppStrings.LABEL_MOBILE_NUMBER_HINT)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        isError = errorMessage != null,
        enabled = !isSending,
        modifier = Modifier.fillMaxWidth(),
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = consent, onCheckedChange = { consent = it }, enabled = !isSending)
        Text(
            text = stringResource(AppStrings.LABEL_TERMS_AND_PRIVACY),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    PrimaryButton(
        text = if (isSending) {
            stringResource(AppStrings.ACTION_SENDING_CODE)
        } else {
            stringResource(AppStrings.ACTION_SEND_CODE)
        },
        onClick = onSendClick,
        enabled = consent && phone.length == PHONE_LENGTH && !isSending,
        icon = Icons.Outlined.Sms,
        modifier = Modifier.fillMaxWidth(),
    )
}

@PreviewLightDark
@Composable
private fun PhoneStepPreview() {
    PersonalLedgerTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PhoneStep(MockValues.DEFAULT_PHONE, {})
            }
        }
    }
}
