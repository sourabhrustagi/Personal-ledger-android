package com.ledger.personal.domain.session

/** Bearer session. Lock clears it; unlock must call [begin] again. */
interface Session {
    /** Drops the bearer token. The ledger on the phone is left alone. */
    fun end()

    /** Asks the server for a new token and stores it. */
    suspend fun begin(): Result<Unit>

    /** Submits a valid 10-digit mobile number to the server to send an OTP code. Returns the server confirmation message (e.g. "OTP sent"). */
    suspend fun sendOtp(phone: String): Result<String>
}

