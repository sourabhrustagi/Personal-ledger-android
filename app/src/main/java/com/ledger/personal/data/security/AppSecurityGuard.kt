package com.ledger.personal.data.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Debug
import com.ledger.personal.domain.security.DeviceSecurityReport
import com.ledger.personal.domain.security.IntegrityEvaluator
import com.ledger.personal.domain.security.SecurityThreat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enterprise-grade runtime security auditor defending against:
 * 1. Rooted/jailbroken operating systems and Magisk hide mounts.
 * 2. Dynamic instrumentation and hooking frameworks (Frida, Xposed, ptrace).
 * 3. Man-in-the-Middle (MitM) proxies and unauthorized VPN intercepts.
 * 4. Application tampering, sideload repackaging, and debugger attachments.
 */
@Singleton
class AppSecurityGuard @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    /**
     * Executes a full device security audit and produces a [DeviceSecurityReport].
     *
     * @param expectedSigningCertificateSha256 Expected release signing certificate fingerprint.
     */
    fun performSecurityAudit(
        expectedSigningCertificateSha256: String? = null,
    ): DeviceSecurityReport {
        val detectedThreats = mutableListOf<SecurityThreat>()

        // 1. Root Detection
        if (checkSuBinaryExists()) {
            detectedThreats += SecurityThreat.ROOT_BINARY_PRESENT
        }
        if (IntegrityEvaluator.isTestKeysBuild(Build.TAGS)) {
            detectedThreats += SecurityThreat.TEST_KEYS_BUILD
        }
        if (checkRootManagementDirectories()) {
            detectedThreats += SecurityThreat.MAGISK_DIRECTORY_DETECTED
        }

        // 2. Anti-Debugging & Hooking (Frida / Xposed / TracerPid)
        if (Debug.isDebuggerConnected()) {
            detectedThreats += SecurityThreat.DEBUGGER_ATTACHED
        }
        if (checkPtraceTracerAttached()) {
            detectedThreats += SecurityThreat.PTRACE_TRACER_ATTACHED
        }
        if (checkFridaOrHookingPresent()) {
            detectedThreats += SecurityThreat.FRIDA_OR_HOOK_DETECTED
        }

        // 3. MitM Proxy & Network Interception
        if (checkProxyOrVpnActive()) {
            detectedThreats += SecurityThreat.ACTIVE_PROXY_DETECTED
        }

        // 4. Anti-Tampering & Signature Verification
        if (expectedSigningCertificateSha256 != null && !verifySigningCertificate(expectedSigningCertificateSha256)) {
            detectedThreats += SecurityThreat.SIGNATURE_MISMATCH
        }
        if (checkDebuggableInProduction()) {
            detectedThreats += SecurityThreat.DEBUGGABLE_IN_PRODUCTION
        }

        return IntegrityEvaluator.evaluate(detectedThreats)
    }

    private fun checkSuBinaryExists(): Boolean =
        IntegrityEvaluator.DEFAULT_SU_PATHS.any { path ->
            runCatching { File(path).exists() }.getOrDefault(false)
        }

    private fun checkRootManagementDirectories(): Boolean =
        IntegrityEvaluator.ROOT_DIRECTORIES.any { path ->
            runCatching { File(path).exists() }.getOrDefault(false)
        }

    private fun checkPtraceTracerAttached(): Boolean {
        val statusFile = File("/proc/self/status")
        if (!statusFile.canRead()) return false
        val content = runCatching { statusFile.readText() }.getOrNull() ?: return false
        return IntegrityEvaluator.parseTracerPid(content) > 0
    }

    private fun checkFridaOrHookingPresent(): Boolean {
        // A. Check /proc/self/maps for injected agent libraries
        val mapsFile = File("/proc/self/maps")
        if (mapsFile.canRead()) {
            val mapsContent = runCatching { mapsFile.readText() }.getOrNull()
            if (mapsContent != null && IntegrityEvaluator.detectHookingLibraries(mapsContent)) {
                return true
            }
        }

        // B. Probe default Frida server socket port
        return runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("127.0.0.1", FRIDA_DEFAULT_PORT), FRIDA_SOCKET_TIMEOUT_MS)
                true
            }
        }.getOrDefault(false)
    }

    private fun checkProxyOrVpnActive(): Boolean {
        val proxyHost = System.getProperty("http.proxyHost")
        val proxyPort = System.getProperty("http.proxyPort")
        if (!proxyHost.isNullOrBlank() || !proxyPort.isNullOrBlank()) {
            return true
        }

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }

    private fun verifySigningCertificate(expectedSha256: String): Boolean {
        val pm = context.packageManager
        val packageName = context.packageName
        return runCatching {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            val certBytes = signatures?.firstOrNull()?.toByteArray() ?: return false
            val digest = MessageDigest.getInstance("SHA-256").digest(certBytes)
            val certFingerprintHex = digest.joinToString("") { "%02X".format(it) }
            val cleanExpected = expectedSha256.replace(":", "").uppercase()
            certFingerprintHex.equals(cleanExpected, ignoreCase = true)
        }.getOrDefault(false)
    }

    private fun checkDebuggableInProduction(): Boolean {
        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        // In real production builds, debuggable flag must be false
        return isDebuggable && !Build.FINGERPRINT.contains("generic", ignoreCase = true)
    }

    private companion object {
        const val FRIDA_DEFAULT_PORT = 27042
        const val FRIDA_SOCKET_TIMEOUT_MS = 50
    }
}
