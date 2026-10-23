package com.ledger.personal.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

class PinRotationManagerTest {

    private val primaryPin = "k2oTeyUbIhRgoZCG8Fsuy295P23/flTIOFhLpmfgG5o="
    private val backupPin = "r/mIkG3eEpVdm+u/ko/cwxzOMo1bk4TyHIlByiboA58="
    private val caPin = "YLh1dUR9y6Kja30RrAn7JKnbQG/uEtLMkBgFF2Fuihg="

    @Test
    fun multiPinConfigEnforcesDistinctPrimaryAndBackupPins() {
        val config = DomainPinConfig(
            domain = "api.personal-ledger.com",
            primaryPinSha256 = primaryPin,
            backupPinSha256 = backupPin,
            intermediateCaPinSha256 = caPin,
        )

        val manager = PinRotationManager()
        manager.registerDomainPins(config)

        val pinner = manager.buildCertificatePinner()
        assertNotNull(pinner)
        assertEquals(config, manager.getConfig("api.personal-ledger.com"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun throwsWhenBackupPinEqualsPrimaryPin() {
        DomainPinConfig(
            domain = "api.personal-ledger.com",
            primaryPinSha256 = primaryPin,
            backupPinSha256 = primaryPin, // Invalid: cannot be identical
        )
    }

    @Test
    fun emergencyRotationAppliesWhenSignedCorrectly() {
        val keyGen = KeyPairGenerator.getInstance("EC").apply { initialize(256) }
        val rootKey = keyGen.generateKeyPair()

        val manager = PinRotationManager()
        manager.registerDomainPins(
            DomainPinConfig(
                domain = "api.personal-ledger.com",
                primaryPinSha256 = primaryPin,
                backupPinSha256 = backupPin,
            ),
        )

        val newPrimary = "7HIpactEmOu2YysFYcG0ngM5hATkgWeUmbaFnwn4HSg="
        val newBackup = "WoiWRyIOVNa9ihaapVIgn+A1paJ+LW13rK/1PNJIDd8="
        val version = 100L

        val raw = "api.personal-ledger.com|$newPrimary|$newBackup|$version"
        val signer = Signature.getInstance("SHA256withECDSA").apply {
            initSign(rootKey.private)
            update(raw.toByteArray(Charsets.UTF_8))
        }
        val sigBase64 = Base64.getEncoder().encodeToString(signer.sign())

        val manifest = SignedPinManifest(
            domain = "api.personal-ledger.com",
            newPrimaryPinSha256 = newPrimary,
            newBackupPinSha256 = newBackup,
            manifestVersion = version,
            signatureBase64 = sigBase64,
        )

        val applied = manager.applyEmergencyRotation(manifest, rootKey.public)
        assertTrue(applied)

        val updated = manager.getConfig("api.personal-ledger.com")
        assertNotNull(updated)
        assertEquals(newPrimary, updated?.primaryPinSha256)
        assertEquals(newBackup, updated?.backupPinSha256)

        // Replay of same or older version must fail
        val replay = manager.applyEmergencyRotation(manifest, rootKey.public)
        assertFalse(replay)
    }

    @Test
    fun tamperedManifestFailsSignatureVerification() {
        val keyGen = KeyPairGenerator.getInstance("EC").apply { initialize(256) }
        val rootKey = keyGen.generateKeyPair()

        val manager = PinRotationManager()
        val manifest = SignedPinManifest(
            domain = "api.personal-ledger.com",
            newPrimaryPinSha256 = "AAAA",
            newBackupPinSha256 = "BBBB",
            manifestVersion = 1L,
            signatureBase64 = Base64.getEncoder().encodeToString("tampered".toByteArray()),
        )

        val result = manager.applyEmergencyRotation(manifest, rootKey.public)
        assertFalse(result)
    }
}
