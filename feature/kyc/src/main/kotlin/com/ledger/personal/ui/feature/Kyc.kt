package com.ledger.personal.ui.feature

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.personal.core.CapturedField
import com.ledger.personal.core.FieldMerge
import com.ledger.personal.core.mergeField
import com.ledger.personal.core.needsHumanReview
import com.ledger.personal.data.local.KycDraftStore
import com.ledger.personal.data.local.MemoryKycDraftStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KycUiState(
    val isLoading: Boolean = false,
    val name: String = "",
    val confidence: Double = 1.0,
    val humanReview: Boolean = false,
    val captureUri: String? = null,
    val edge: IntRange? = null,
    val conflict: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class KycViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val drafts: KycDraftStore,
) : ViewModel() {
    constructor(savedState: SavedStateHandle) : this(savedState, MemoryKycDraftStore())

    private val state = MutableStateFlow(
        KycUiState(name = savedState.get<String>(NAME) ?: ""),
    )
    val uiState: StateFlow<KycUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            if (!savedState.get<String>(NAME).isNullOrBlank()) return@launch
            val loaded = drafts.load() ?: return@launch
            savedState[NAME] = loaded.first
            state.update {
                it.copy(name = loaded.first, confidence = loaded.second, captureUri = loaded.third)
            }
        }
    }

    fun onName(value: String) {
        savedState[NAME] = value
        state.update { it.copy(name = value) }
        persist()
    }

    fun onConfidence(value: Double) {
        val review = needsHumanReview(listOf(CapturedField(required = true, confidence = value)))
        state.update { it.copy(confidence = value, humanReview = review) }
        persist()
    }

    fun capture() {
        val edge = documentSpan(intArrayOf(0, 10, 80, 90, 5))
        state.update {
            it.copy(
                captureUri = "content://com.ledger.personal.fileprovider/docs/capture.jpg",
                edge = edge,
            )
        }
        persist()
    }

    fun incomingEdit(other: String) {
        val merged = mergeField(state.value.name, other)
        state.update {
            it.copy(conflict = if (merged is FieldMerge.Conflict) "Sync conflict on name" else null)
        }
    }

    private fun persist() {
        val current = state.value
        viewModelScope.launch { drafts.save(current.name, current.confidence, current.captureUri) }
    }

    private companion object {
        const val NAME = "kycName"
    }
}

fun documentSpan(luma: IntArray, threshold: Int = 40): IntRange? {
    val start = luma.indexOfFirst { it >= threshold }
    if (start < 0) return null
    return start..luma.indexOfLast { it >= threshold }
}
