package com.shanivar.nativeapp

/**
 * Distinct failure kinds `DisciplineGateway` callers need to tell apart, per NATIVE_HANDOFF.md's
 * "preserve permission-denied and entitlement errors distinctly" requirement - the UI's copy/retry
 * behavior differs for "you need to grant Android permissions" vs. "your plan doesn't include this"
 * vs. "we couldn't reach the server, try again."
 */
sealed class DisciplineError(message: String) : Exception(message) {
    class PermissionRequired(message: String) : DisciplineError(message)
    class EntitlementRequired(message: String) : DisciplineError(message)
    class Network(message: String) : DisciplineError(message)
}
