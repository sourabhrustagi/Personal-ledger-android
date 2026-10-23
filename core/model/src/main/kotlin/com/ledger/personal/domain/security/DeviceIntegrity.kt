package com.ledger.personal.domain.security

/**
 * Types of security threats detected on high-risk devices.
 */
enum class SecurityThreat {
    ROOT_BINARY_PRESENT,
    TEST_KEYS_BUILD,
    MAGISK_DIRECTORY_DETECTED,
    DEBUGGER_ATTACHED,
    PTRACE_TRACER_ATTACHED,
    FRIDA_OR_HOOK_DETECTED,
    ACTIVE_PROXY_DETECTED,
    SIGNATURE_MISMATCH,
    DEBUGGABLE_IN_PRODUCTION,
}

/**
 * Overall security integrity verdict for fintech runtime enforcement.
 */
enum class IntegrityVerdict {
    SECURE,
    SUSPICIOUS,
    COMPROMISED,
}

/**
 * Detailed evaluation report of device security checks.
 */
data class DeviceSecurityReport(
    val verdict: IntegrityVerdict,
    val threats: List<SecurityThreat>,
    val isRooted: Boolean,
    val isHookedOrTraced: Boolean,
    val isProxyActive: Boolean,
    val isSignatureVerified: Boolean,
)

/**
 * Pure parsing and evaluation logic for runtime integrity checks.
 */
object IntegrityEvaluator {

    /** Known paths where su or root management binaries typically reside. */
    val DEFAULT_SU_PATHS = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/system/su",
        "/vendor/bin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
    )

    /** Known root management or system-tampering directories. */
    val ROOT_DIRECTORIES = listOf(
        "/sbin/.magisk",
        "/data/adb/magisk",
        "/data/adb/modules",
        "/system/addon.d",
    )

    /** Suspect libraries indicating dynamic instrumentation engines like Frida, Xposed, or Substrate. */
    val HOOKING_LIBRARIES = listOf(
        "frida-agent",
        "frida-gadget",
        "libfrida",
        "xposed",
        "substrate",
    )

    /** Inspects build tags for test-keys or unofficial custom ROM builds. */
    fun isTestKeysBuild(tags: String?): Boolean =
        tags?.contains("test-keys", ignoreCase = true) == true

    /** Parses TracerPid from /proc/self/status content to detect active ptrace debuggers. */
    fun parseTracerPid(procStatusContent: String): Int {
        val line = procStatusContent.lineSequence()
            .firstOrNull { it.startsWith("TracerPid:", ignoreCase = true) }
            ?: return 0
        return line.substringAfter(":").trim().toIntOrNull() ?: 0
    }

    /** Scans /proc/self/maps lines for injected dynamic instrumentation or hooking engines. */
    fun detectHookingLibraries(procMapsContent: String): Boolean {
        val lower = procMapsContent.lowercase()
        return HOOKING_LIBRARIES.any { lower.contains(it) }
    }

    /** Evaluates threats and yields an overall security integrity verdict. */
    fun evaluate(threats: List<SecurityThreat>): DeviceSecurityReport {
        val isRooted = threats.contains(SecurityThreat.ROOT_BINARY_PRESENT) ||
            threats.contains(SecurityThreat.TEST_KEYS_BUILD) ||
            threats.contains(SecurityThreat.MAGISK_DIRECTORY_DETECTED)

        val isHooked = threats.contains(SecurityThreat.DEBUGGER_ATTACHED) ||
            threats.contains(SecurityThreat.PTRACE_TRACER_ATTACHED) ||
            threats.contains(SecurityThreat.FRIDA_OR_HOOK_DETECTED)

        val isProxy = threats.contains(SecurityThreat.ACTIVE_PROXY_DETECTED)
        val isSignatureTampered = threats.contains(SecurityThreat.SIGNATURE_MISMATCH)

        val verdict = when {
            isRooted || isHooked || isSignatureTampered -> IntegrityVerdict.COMPROMISED
            isProxy -> IntegrityVerdict.SUSPICIOUS
            else -> IntegrityVerdict.SECURE
        }

        return DeviceSecurityReport(
            verdict = verdict,
            threats = threats,
            isRooted = isRooted,
            isHookedOrTraced = isHooked,
            isProxyActive = isProxy,
            isSignatureVerified = !isSignatureTampered,
        )
    }
}
