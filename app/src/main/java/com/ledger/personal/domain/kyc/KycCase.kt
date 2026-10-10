package com.ledger.personal.domain.kyc

/** Allowed values of case status. */
enum class CaseStatus {
    NEW,
    APPROVED,
}

/** Kyc case. */
data class KycCase(
    val id: String,
    val status: CaseStatus,
)

/** Transition to. */
fun KycCase.transitionTo(next: CaseStatus): KycCase {
    if (status == CaseStatus.NEW && next == CaseStatus.APPROVED) {
        error("Illegal transition NEW -> APPROVED")
    }
    return copy(status = next)
}
