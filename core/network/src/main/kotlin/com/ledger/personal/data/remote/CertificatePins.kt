package com.ledger.personal.data.remote

import okhttp3.CertificatePinner

/** Certificate pinner. */
fun certificatePinner(primarySha256: String, backupSha256: String): CertificatePinner? {
    if (primarySha256.isBlank() || backupSha256.isBlank()) return null
    return CertificatePinner.Builder()
        .add("api.personal-ledger.com", "sha256/$primarySha256")
        .add("api.personal-ledger.com", "sha256/$backupSha256")
        .build()
}
