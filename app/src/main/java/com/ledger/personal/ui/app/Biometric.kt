package com.ledger.personal.ui.app

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

private val authenticators: Int
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    } else {
        BiometricManager.Authenticators.BIOMETRIC_STRONG
    }

fun biometricAvailable(context: Context): Boolean =
    BiometricManager.from(context).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS

fun promptBiometric(context: Context, title: String, onResult: (Boolean) -> Unit) {
    val activity = context as? FragmentActivity ?: return onResult(false)
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onResult(true)
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onResult(false)
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder().setTitle(title).setAllowedAuthenticators(authenticators)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) info.setNegativeButtonText("Use MPIN")
    prompt.authenticate(info.build())
}

/** Strong-biometric prompt that unlocks [signature]; the callback gets the unlocked signer or null. */
fun promptWithCrypto(context: Context, title: String, signature: java.security.Signature, onResult: (java.security.Signature?) -> Unit) {
    val activity = context as? FragmentActivity ?: return onResult(null)
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onResult(result.cryptoObject?.signature)
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onResult(null)
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle("Creates a key on this phone that only your biometrics can use")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        .setNegativeButtonText("Cancel")
        .build()
    prompt.authenticate(info, BiometricPrompt.CryptoObject(signature))
}
