package com.ledger.personal.ui.screens.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.components.EmptyState
import com.ledger.personal.ui.components.ErrorState
import com.ledger.personal.ui.components.LoadingState
import com.ledger.personal.ui.theme.AppStrings
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Shows the edit transaction screen. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditTransactionScreen(
    uiState: EditTransactionUiState,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit,
    quickCategories: List<String> = emptyList(),
    categoriesLoading: Boolean = false,
) {

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(AppStrings.TITLE_EDIT_TRANSACTION), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoadingInitial -> {
                    LoadingState(message = stringResource(AppStrings.LOADING_TRANSACTION_DATA))
                }
                uiState.errorMessage != null && uiState.transactionId.isBlank() -> {
                    ErrorState(
                        errorMessage = uiState.errorMessage,
                        onRetry = onRetry
                    )
                }
                uiState.isEmpty -> {
                    EmptyState(
                        title = stringResource(AppStrings.NO_MATCHING_TRANSACTIONS),
                        message = stringResource(AppStrings.EMPTY_TRANSACTION_NOT_FOUND),
                        actionLabel = stringResource(AppStrings.ACTION_BACK_TO_LIST),
                        onActionClick = onNavigateBack
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        if (uiState.errorMessage != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = uiState.errorMessage,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                OutlinedTextField(
                                    value = uiState.amountInput,
                                    onValueChange = onAmountChange,
                                    label = { Text(stringResource(AppStrings.LABEL_AMOUNT)) },
                                    prefix = { Text("$ ") },
                                    isError = uiState.amountError != null,
                                    supportingText = {
                                        if (uiState.amountError != null) {
                                            Text(uiState.amountError, color = MaterialTheme.colorScheme.error)
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = uiState.category,
                                    onValueChange = onCategoryChange,
                                    label = { Text(stringResource(AppStrings.LABEL_CATEGORY)) },
                                    isError = uiState.categoryError != null,
                                    supportingText = {
                                        if (uiState.categoryError != null) {
                                            Text(uiState.categoryError, color = MaterialTheme.colorScheme.error)
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Text(
                                    text = stringResource(AppStrings.LABEL_QUICK_SELECT_CATEGORY),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (categoriesLoading) {
                                    Text(stringResource(AppStrings.LABEL_WAITING_FOR_CATEGORIES), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    quickCategories.forEach { cat ->
                                        FilterChip(
                                            selected = uiState.category == cat,
                                            onClick = { onCategoryChange(cat) },
                                            label = { Text(cat) }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = uiState.description,
                                    onValueChange = onDescriptionChange,
                                    label = { Text(stringResource(AppStrings.LABEL_DESCRIPTION)) },
                                    minLines = 2,
                                    maxLines = 4,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Button(
                            onClick = onSubmit,
                            enabled = !uiState.isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (uiState.isSubmitting) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(stringResource(AppStrings.ACTION_SAVING_CHANGES))
                                }
                            } else {
                                Text(stringResource(AppStrings.ACTION_UPDATE_TRANSACTION), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Shows edit transaction loading preview. */
@PreviewLightDark
@Composable
private fun EditTransactionLoadingPreview() {
    PersonalLedgerTheme {
        EditTransactionScreen(
            uiState = EditTransactionUiState(isLoadingInitial = true),
            onAmountChange = {},
            onCategoryChange = {},
            onDescriptionChange = {},
            onSubmit = {},
            onNavigateBack = {},
            onRetry = {}
        )
    }
}

/** Shows edit transaction error preview. */
@PreviewLightDark
@Composable
private fun EditTransactionErrorPreview() {
    PersonalLedgerTheme {
        EditTransactionScreen(
            uiState = EditTransactionUiState(
                isLoadingInitial = false,
                errorMessage = "Failed to load transaction"
            ),
            onAmountChange = {},
            onCategoryChange = {},
            onDescriptionChange = {},
            onSubmit = {},
            onNavigateBack = {},
            onRetry = {}
        )
    }
}

/** Shows edit transaction empty preview. */
@PreviewLightDark
@Composable
private fun EditTransactionEmptyPreview() {
    PersonalLedgerTheme {
        EditTransactionScreen(
            uiState = EditTransactionUiState(isLoadingInitial = false),
            onAmountChange = {},
            onCategoryChange = {},
            onDescriptionChange = {},
            onSubmit = {},
            onNavigateBack = {},
            onRetry = {}
        )
    }
}

/** Shows edit transaction form preview. */
@PreviewLightDark
@Composable
private fun EditTransactionFormPreview() {
    PersonalLedgerTheme {
        EditTransactionScreen(
            uiState = EditTransactionUiState(
                isLoadingInitial = false,
                transactionId = "tx-preview-1",
                amountInput = "45.50",
                category = "Dining",
                description = "Team lunch"
            ),
            onAmountChange = {},
            onCategoryChange = {},
            onDescriptionChange = {},
            onSubmit = {},
            onNavigateBack = {},
            onRetry = {}
        )
    }
}

/** Shows edit transaction validation preview. */
@PreviewLightDark
@Composable
private fun EditTransactionValidationPreview() {
    PersonalLedgerTheme {
        EditTransactionScreen(
            uiState = EditTransactionUiState(
                isLoadingInitial = false,
                transactionId = "tx-preview-1",
                amountError = "Enter a valid positive amount",
                categoryError = "Category cannot be blank"
            ),
            onAmountChange = {},
            onCategoryChange = {},
            onDescriptionChange = {},
            onSubmit = {},
            onNavigateBack = {},
            onRetry = {}
        )
    }
}

/** Shows edit transaction submitting preview. */
@PreviewLightDark
@Composable
private fun EditTransactionSubmittingPreview() {
    PersonalLedgerTheme {
        EditTransactionScreen(
            uiState = EditTransactionUiState(
                isLoadingInitial = false,
                transactionId = "tx-preview-1",
                amountInput = "45.50",
                category = "Dining",
                isSubmitting = true
            ),
            onAmountChange = {},
            onCategoryChange = {},
            onDescriptionChange = {},
            onSubmit = {},
            onNavigateBack = {},
            onRetry = {}
        )
    }
}
