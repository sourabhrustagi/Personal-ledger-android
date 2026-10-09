package com.ledger.personal.feature.wallet

import com.ledger.personal.core.lookupWallet

fun openWallet(actor: String, owner: String) = lookupWallet(actor, owner)
