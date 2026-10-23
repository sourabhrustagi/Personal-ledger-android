package com.ledger.personal.data.remote

import okhttp3.CertificatePinner
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Certificate Pinning Specification for a specific domain.
 * Enforces PCI/enterprise requirement: never pin a single key.
 */
data class DomainPinConfig(
    val domain: String,
    val primaryPinSha256: String,
    val backupPinSha256: String,
    val intermediateCaPinSha256: String? = null,
) {
    init {
        require(primaryPinSha256.isNotBlank()) { "Primary pin cannot be blank" }
        require(backupPinSha256.isNotBlank()) { "Backup pin cannot be blank: multi-pin strategy required" }
        require(primaryPinSha256 != backupPinSha256) { "Backup pin must be distinct from primary pin for rotation" }
    }
}

/**
 * State representing the active stage in a zero-downtime certificate rotation.
 */
enum class PinRotationStage {
    /** Live server serves Primary cert. Backup key resides in escrow and is pre-pinned in clients. */
    NORMAL,
    /** Server switches live TLS cert to the pre-pinned Backup key. Both old and new clients connect. */
    ROTATING,
    /** New app update published promoting Backup to Primary, with a fresh escrow Backup pin. */
    ROTATED,
}

/**
 * Cryptographically signed dynamic pin manifest payload for emergency over-the-air pin rotation.
 * Prevents app bricking during unexpected leaf/intermediate CA revocations.
 */
data class SignedPinManifest(
    val domain: String,
    val newPrimaryPinSha256: String,
    val newBackupPinSha256: String,
    val manifestVersion: Long,
    val signatureBase64: String,
)

/**
 * Manager handling Certificate Pinning configuration, multi-pin policy enforcement,
 * and zero-downtime rotation workflows.
 */
@Singleton
class PinRotationManager @Inject constructor() {

    private val activeConfigs = mutableMapOf<String, DomainPinConfig>()
    private var currentManifestVersion: Long = 0L

    /**
     * Registers a domain pin configuration with mandatory primary and backup pins.
     */
    fun registerDomainPins(config: DomainPinConfig) {
        activeConfigs[config.domain] = config
    }

    /**
     * Returns the active configuration for [domain], if present.
     */
    fun getConfig(domain: String): DomainPinConfig? = activeConfigs[domain]

    /**
     * Builds an OkHttp [CertificatePinner] adhering to the multi-pin strategy.
     */
    fun buildCertificatePinner(): CertificatePinner {
        val builder = CertificatePinner.Builder()
        for ((domain, config) in activeConfigs) {
            builder.add(domain, formatPin(config.primaryPinSha256))
            builder.add(domain, formatPin(config.backupPinSha256))
            config.intermediateCaPinSha256?.takeIf { it.isNotBlank() }?.let { caPin ->
                builder.add(domain, formatPin(caPin))
            }
        }
        return builder.build()
    }

    /**
     * Applies an emergency signed pin manifest after verifying the ECDSA signature against the root [trustedAuthorityPublicKey].
     * Returns true if successfully verified and updated.
     */
    fun applyEmergencyRotation(
        manifest: SignedPinManifest,
        trustedAuthorityPublicKey: PublicKey,
    ): Boolean {
        if (manifest.manifestVersion <= currentManifestVersion) {
            return false // Replay protection
        }

        val rawPayload = "${manifest.domain}|${manifest.newPrimaryPinSha256}|${manifest.newBackupPinSha256}|${manifest.manifestVersion}"
        val verified = runCatching {
            val verifier = Signature.getInstance("SHA256withECDSA")
            verifier.initVerify(trustedAuthorityPublicKey)
            verifier.update(rawPayload.toByteArray(Charsets.UTF_8))
            verifier.verify(Base64.getDecoder().decode(manifest.signatureBase64))
        }.getOrDefault(false)

        if (!verified) return false

        activeConfigs[manifest.domain] = DomainPinConfig(
            domain = manifest.domain,
            primaryPinSha256 = manifest.newPrimaryPinSha256,
            backupPinSha256 = manifest.newBackupPinSha256,
        )
        currentManifestVersion = manifest.manifestVersion
        return true
    }

    private fun formatPin(hash: String): String =
        if (hash.startsWith("sha256/")) hash else "sha256/$hash"

    companion object {
        /**
         * Reconstitutes an EC public key from X.509 DER encoded bytes.
         */
        fun parseEcPublicKey(derBytes: ByteArray): PublicKey {
            val keySpec = X509EncodedKeySpec(derBytes)
            return KeyFactory.getInstance("EC").generatePublic(keySpec)
        }
    }
}
