package com.shanivar.nativeapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.shanivar.nativeapp.sync.SyncEngine
import com.shanivar.nativeapp.sync.SyncEngineFactory
import com.shanivar.nativeapp.sync.SyncScheduler
import com.shanivar.nativeapp.sync.SyncSession
import com.shanivar.nativeapp.sync.SyncStatusSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.security.KeyStore
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/*
 * DEV-45 Android glue: Keystore token cipher, WorkManager worker + scheduler, connectivity, and the
 * engine/factory DEV-47 binds through SyncEngines (sync/SyncContracts.kt).
 */

/** AES-256-GCM with a non-exportable Android Keystore key; output = 12-byte IV + ciphertext. */
class KeystoreTokenCipher(private val alias: String = "shanivar_sync_tokens_v1") : TokenCipher {
    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build())
        return gen.generateKey()
    }
    override fun encrypt(plain: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        return c.iv + c.doFinal(plain)
    }
    override fun decrypt(sealed: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, sealed, 0, 12))
        return c.doFinal(sealed, 12, sealed.size - 12)
    }
}

/** Online = a validated default network. */
class AndroidNetworkMonitor(context: Context, private val onRegained: () -> Unit) {
    private val cm = context.applicationContext.getSystemService(ConnectivityManager::class.java)
    @Volatile var online: Boolean = runCatching {
        cm?.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }.getOrDefault(false)
        private set
    init {
        runCatching {
            cm?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { val was = online; online = true; if (!was) runCatching(onRegained) }
                override fun onLost(network: Network) { online = false }
            })
        }
    }
}

/**
 * AC3: WorkManager unique work [UNIQUE] ("outbox-flush") with NetworkType.CONNECTED. Backoff is the
 * engine's own ([SyncBackoff], 30 s..1 h with jitter) because WorkManager's has no jitter and caps at 5 h.
 * - [requestFlush]: main-thread safe, no I/O, debounced 2 s, KEEP (never cancels a running flush; a
 *   running flush re-checks the dirty flag before it finishes).
 * - [flushNow] (foreground / network regained): resets the backoff and replaces a waiting backoff run.
 */
class WorkSyncScheduler(
    context: Context,
    private val backoff: SyncBackoff,
    private val debounceMs: Long = 2_000,
) : SyncScheduler {
    private val app = context.applicationContext
    private val timer = Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "sync-debounce").apply { isDaemon = true } }
    @Volatile private var pending: ScheduledFuture<*>? = null
    internal val dirty = AtomicBoolean(false)
    internal val running = AtomicBoolean(false)

    private fun wm() = WorkManager.getInstance(app)

    override fun requestFlush() {
        dirty.set(true)
        synchronized(this) {
            pending?.cancel(false)
            pending = timer.schedule({ runCatching { wm().enqueueUniqueWork(UNIQUE, ExistingWorkPolicy.KEEP, request(0)) } },
                debounceMs, TimeUnit.MILLISECONDS)
        }
    }

    /** Foreground / reconnect: reset backoff and flush immediately (contract C7). */
    fun flushNow() {
        backoff.reset(); dirty.set(true)
        if (!running.get()) runCatching { wm().enqueueUniqueWork(UNIQUE, ExistingWorkPolicy.REPLACE, request(0)) }
    }

    override fun schedulePeriodic() {
        runCatching {
            wm().enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS).setConstraints(CONSTRAINTS).build())
        }
    }

    /** Called by the running worker: the next run waits [delayMs], appended after the current one. */
    internal fun scheduleRetry(delayMs: Long) {
        runCatching { wm().enqueueUniqueWork(UNIQUE, ExistingWorkPolicy.APPEND_OR_REPLACE, request(delayMs)) }
    }

    fun cancel() {
        synchronized(this) { pending?.cancel(false); pending = null }
        runCatching { wm().cancelUniqueWork(UNIQUE); wm().cancelUniqueWork(PERIODIC) }
    }

    companion object {
        const val UNIQUE = "outbox-flush"
        const val PERIODIC = "outbox-flush-periodic"
        val CONSTRAINTS: Constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        fun request(delayMs: Long): OneTimeWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(CONSTRAINTS).setInitialDelay(delayMs, TimeUnit.MILLISECONDS).addTag(UNIQUE).build()
    }
}

/**
 * Runs flushes for the process-wide engine on WorkManager's background executor (blocking I/O, never the
 * main thread). Sync off (no engine) = immediate success. Retries are scheduled by the engine, not Result.retry().
 */
class SyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        runCatching { Dev45Sync.current?.runFlushes() }
        return Result.success()
    }
}

/**
 * The DEV-45 engine handed to DEV-47 (implements the agreed [SyncEngine]). Extra surface for DEV-47/52:
 * [flushNow] (foreground), [reset] (wipe), [routineConflict] + [keepMineRoutine] / [takeServerRoutine].
 */
class Dev45SyncEngine internal constructor(
    val outbox: SyncOutbox,
    private val supabaseSession: SupabaseSession,
    private val flusher: OutboxFlusher,
    private val routineStore: RoutineSyncStore,
    private val workScheduler: WorkSyncScheduler,
    private val backoff: SyncBackoff,
    private val tracker: SyncStatusTracker,
    private val nowMs: () -> Long,
) : SyncEngine {
    override val session: SyncSession get() = supabaseSession
    override val scheduler: SyncScheduler get() = workScheduler
    override val status: SyncStatusSource get() = tracker
    private val _conflict = MutableStateFlow(routineStore.conflict)
    val routineConflict: StateFlow<RoutineConflictInfo?> = _conflict.asStateFlow()
    val deadLetters: List<DeadLetter> get() = outbox.deadLetters
    val sessionLost: Boolean get() = supabaseSession.sessionLost

    internal fun publishConflict(c: RoutineConflictInfo?) { _conflict.value = c }

    fun flushNow() = workScheduler.flushNow()

    /** Worker body: flush until nothing is dirty; on a retryable failure schedule the backoff run. */
    internal fun runFlushes() {
        if (!workScheduler.running.compareAndSet(false, true)) { workScheduler.dirty.set(true); return }
        try {
            while (true) {
                workScheduler.dirty.set(false)
                when (val o = flusher.flush()) {
                    is FlushOutcome.RetryLater -> { workScheduler.scheduleRetry(backoff.next(o.afterMs)); break }
                    FlushOutcome.NoSession -> break
                    is FlushOutcome.Done -> {
                        backoff.reset()
                        if (flusher.sendableCount() == 0) tracker.markSynced(nowMs())
                        if (!workScheduler.dirty.get()) break
                    }
                }
            }
        } finally { workScheduler.running.set(false); tracker.refresh() }
    }

    /** DEV-52 keepMine: re-send the local routine against the server's revision with a new idempotency key. */
    fun keepMineRoutine() {
        val c = routineStore.conflict ?: return
        routineStore.serverRevision = c.revision; routineStore.conflict = null; publishConflict(null)
        outbox.requeue("routine:current")
    }

    /** DEV-52 takeServer: the caller saves [RoutineConflictInfo.planJson] locally without enqueueing. */
    fun takeServerRoutine(): RoutineConflictInfo? {
        val c = routineStore.conflict ?: return null
        routineStore.serverRevision = c.revision; routineStore.conflict = null; publishConflict(null)
        outbox.drop("routine:current"); tracker.refresh()
        return c
    }

    /** Reset (wipe this device): cancel work, clear tokens, drop queue + dead letters + lastSyncedAt. */
    fun reset() {
        workScheduler.cancel(); supabaseSession.clear(); outbox.clearAll(); routineStore.clear()
        publishConflict(null); tracker.clear()
    }
}

/** Process-wide engine (the worker finds it here). Set by [Dev45SyncEngineFactory]. */
object Dev45Sync {
    @Volatile var current: Dev45SyncEngine? = null
        internal set
}

/**
 * DEV-47 binds this as `SyncEngines.dev45`. Builds the engine over the shared [AppSyncOutbox]; tokens are
 * encrypted with [KeystoreTokenCipher]. [cipher] is a test seam (Robolectric has no AndroidKeyStore).
 */
object Dev45SyncEngineFactory : SyncEngineFactory {
    override fun create(context: Context, supabaseUrl: String, anonKey: String): SyncEngine =
        build(context, supabaseUrl, anonKey)

    fun build(
        context: Context, supabaseUrl: String, anonKey: String,
        cipher: TokenCipher = KeystoreTokenCipher(),
        outbox: SyncOutbox = AppSyncOutbox.get(context),
        nowMs: () -> Long = System::currentTimeMillis,
    ): Dev45SyncEngine {
        val dir = File(context.applicationContext.filesDir, "sync")
        val http = SupabaseHttp(supabaseUrl, anonKey, nowMs = nowMs)
        val session = SupabaseSession(http, EncryptedSessionStore(FileDraftStore(File(dir, "session.enc")), cipher), nowMs)
        val routine = RoutineSyncStore(FileDraftStore(File(dir, "routine.properties")))
        val backoff = SyncBackoff()
        val scheduler = WorkSyncScheduler(context, backoff)
        var engineRef: Dev45SyncEngine? = null
        val flusher = OutboxFlusher(outbox, session, SupabaseOutboxSender(http, routine), routine) { engineRef?.publishConflict(it) }
        var trackerRef: SyncStatusTracker? = null
        val network = AndroidNetworkMonitor(context) { scheduler.flushNow(); trackerRef?.refresh() }
        val tracker = SyncStatusTracker(FileDraftStore(File(dir, "last_synced_at")), { session.userId != null },
            { flusher.sendableCount() }, { outbox.deadLetters.size }, { network.online })
        trackerRef = tracker
        val engine = Dev45SyncEngine(outbox, session, flusher, routine, scheduler, backoff, tracker, nowMs)
        engineRef = engine
        outbox.ownerProvider = { session.userId }
        outbox.listener = { scheduler.requestFlush(); tracker.refresh() }
        session.onSignedIn = { uid -> outbox.stampOwner(uid); tracker.refresh(); scheduler.requestFlush() }
        session.onSignedOut = { scheduler.cancel(); tracker.refresh() }
        tracker.refresh()
        Dev45Sync.current = engine
        return engine
    }
}
