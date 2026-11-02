package com.ledger.personal.feature.scan.model

/** UI state for the scan screen. */
data class ScanUiState(
    val liveCamera: Boolean = false,
    val cameraGranted: Boolean = false,
    val invalid: Boolean = false,
    val manual: String = "",
)

