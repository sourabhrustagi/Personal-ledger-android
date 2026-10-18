package com.ledger.personal.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.ledger.personal.di.AuthenticatedClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.Serializable
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

const val SIGNATURE_ALGORITHM = "SHA256withECDSA"

/** Contract for binding server. */
interface BindingServer {
    /** Challenge. */
    suspend fun challenge(): ByteArray

    /** Returns the server's device id once it has verified [signature] over [challenge] with [publicKey]. */
    suspend fun register(publicKey: ByteArray, challenge: ByteArray, signature: ByteArray): Result<String>
}

/** Mock-flavor server: issues random challenges and really verifies the ECDSA signature. */
@Singleton
class LocalBindingServer @Inject constructor() : BindingServer {
    private val issued = mutableSetOf<String>()
    private val random = SecureRandom()

    /** Challenge. */
    override suspend fun challenge(): ByteArray = ByteArray(32).also {
        random.nextBytes(it)
        issued += Base64.getEncoder().encodeToString(it)
    }

    /** Register. */
    override suspend fun register(publicKey: ByteArray, challenge: ByteArray, signature: ByteArray): Result<String> = runCatching {
        require(issued.remove(Base64.getEncoder().encodeToString(challenge))) { "Unknown or reused challenge" }
        val key = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(publicKey))
        val ok = Signature.getInstance(SIGNATURE_ALGORITHM).run {
            initVerify(key)
            update(challenge)
            verify(signature)
        }
        require(ok) { "Signature did not verify" }
        "dev-" + publicKey.takeLast(4).joinToString("") { "%02x".format(it) }
    }
}

/** Network payload for challenge. */
@Serializable
data class ChallengeDto(val challenge: String)

/** Network payload for bind request. */
@Serializable
data class BindRequestDto(val publicKey: String, val challenge: String, val signature: String)

/** Network payload for bind response. */
@Serializable
data class BindResponseDto(val deviceId: String)

/** Ktor-backed device binding server. */
@Singleton
class KtorBindingServer @Inject constructor(
    @param:AuthenticatedClient private val client: HttpClient,
) : BindingServer {
    /** Challenge. */
    override suspend fun challenge(): ByteArray =
        Base64.getDecoder().decode(client.post("devices/challenge").body<ChallengeDto>().challenge)

    /** Register. */
    override suspend fun register(publicKey: ByteArray, challenge: ByteArray, signature: ByteArray): Result<String> = runCatching {
        val encoder = Base64.getEncoder()
        val body = BindRequestDto(encoder.encodeToString(publicKey), encoder.encodeToString(challenge), encoder.encodeToString(signature))
        client.post("devices/bind") { setBody(body) }.body<BindResponseDto>().deviceId
    }.onFailure { if (it is CancellationException) throw it }
}

/** Hardware-backed EC key that can only sign after a strong biometric check, and dies if biometrics are re-enrolled. */
@Singleton
class DeviceKeys @Inject constructor() {
    private val store by lazy { KeyStore.getInstance(KEYSTORE).apply { load(null) } }

    /** Creates key. */
    fun createKey(): ByteArray {
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE)
        generator.initialize(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
                .build(),
        )
        return generator.generateKeyPair().public.encoded
    }

    /** Signer. */
    fun signer(): Signature = Signature.getInstance(SIGNATURE_ALGORITHM).apply {
        val key = requireNotNull(store.getKey(ALIAS, null) as? PrivateKey) { "Device binding key is missing" }
        initSign(key)
    }

    /** Delete. */
    fun delete() {
        if (store.containsAlias(ALIAS)) store.deleteEntry(ALIAS)
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "ledgerone_device_binding"
    }
}
