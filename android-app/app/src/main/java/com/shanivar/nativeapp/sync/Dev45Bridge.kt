package com.shanivar.nativeapp.sync

/**
 * DEV-47 ↔ DEV-45 bridge, so this branch compiles alone (dev-38 base, no engine) and binds the engine
 * automatically once `dev-45-sync-engine` is merged, without editing the shared SyncContracts.kt.
 *
 * - Engine: [SyncEngines.dev45] if set, else `com.shanivar.nativeapp.Dev45SyncEngineFactory` if present.
 * - Extras on the concrete `Dev45SyncEngine` that the agreed interfaces lack (contract D2): `flushNow()`
 *   (foreground: resets backoff, flushes now), `reset()` (AC6 wipe) and `sessionLost` (diagnostics).
 *
 * TODO(after DEV-45 merges): replace the reflection with direct references, or move `flushNow` / `reset`
 * into SyncContracts.kt by agreement with Backend.
 */
internal object Dev45Bridge {
    private const val FACTORY = "com.shanivar.nativeapp.Dev45SyncEngineFactory"

    fun factory(): SyncEngineFactory? = SyncEngines.dev45 ?: runCatching {
        Class.forName(FACTORY).getField("INSTANCE").get(null) as? SyncEngineFactory
    }.getOrNull()

    /** Calls a no-arg method on the engine if it has one; false when absent or it threw. */
    fun call(engine: SyncEngine, method: String): Boolean = runCatching {
        engine.javaClass.getMethod(method).invoke(engine); true
    }.getOrDefault(false)

    fun flag(engine: SyncEngine, getter: String): Boolean? = runCatching {
        engine.javaClass.getMethod(getter).invoke(engine) as? Boolean
    }.getOrNull()
}
