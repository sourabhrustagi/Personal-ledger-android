package com.ledger.personal.domain.session

/** Clears the bearer token. The ledger on the phone is left alone. */
interface Session {
    /** End. */
    fun end()
}
