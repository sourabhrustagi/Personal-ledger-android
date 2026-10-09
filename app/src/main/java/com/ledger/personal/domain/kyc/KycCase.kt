package com.ledger.personal.domain.kyc

enum class CaseStatus {
    NEW,
    APPROVED,
}

data class KycCase(
    val id: String,
    val status: CaseStatus,
)

fun KycCase.transitionTo(next: CaseStatus): KycCase {
    if (status == CaseStatus.NEW && next == CaseStatus.APPROVED) {
        error("Illegal transition NEW -> APPROVED")
    }
    return copy(status = next)
}
