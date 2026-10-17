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
import com.shanivar.nativeapp.sync.GuestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
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
    override fun flushNow() {
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

/** The parts that need disk / Keystore to build: made once, on an IO thread ([Dev45SyncEngineFactory]). */
internal class Dev45Core(
    val outbox: SyncOutbox,
    val session: SupabaseSession,
    val flusher: OutboxFlusher,
    val routine: RoutineSyncStore,
    val tracker: SyncStatusTracker,
)

/**
 * The DEV-45 engine handed to DEV-47 (implements the agreed [SyncEngine]). It is usable the moment the
 * factory returns: the Keystore session decrypt and the outbox read run on an IO dispatcher, never the
 * main thread. Until they finish, [session] has no user, [status] shows LocalOnly, and flush requests go to
 * WorkManager (the worker waits for the engine). Extra surface for DEV-52: [routineConflict] +
 * [keepMineRoutine] / [takeServerRoutine]; [outbox] / [deadLetters] block until ready (not on main).
 */
class Dev45SyncEngine internal constructor(
    private val ready: java.util.concurrent.CompletableFuture<Dev45Core>,
    private val workScheduler: WorkSyncScheduler,
    private val backoff: SyncBackoff,
    private val flows: SyncStatusFlows,
    private val nowMs: () -> Long,
) : SyncEngine {
    private val _conflict = MutableStateFlow<RoutineConflictInfo?>(null)
    val routineConflict: StateFlow<RoutineConflictInfo?> = _conflict.asStateFlow()

    private fun core(): Dev45Core = ready.get()
    private fun coreOrNull(): Dev45Core? = if (ready.isDone && !ready.isCompletedExceptionally) ready.getNow(null) else null
    /** True once the IO-side build finished (diagnostics / tests). */
    val isReady: Boolean get() = coreOrNull() != null
    /** Blocks until built; never call on the main thread. */
    fun awaitReady(): Dev45SyncEngine { core(); return this }

    val outbox: SyncOutbox get() = core().outbox
    val deadLetters: List<DeadLetter> get() = core().outbox.deadLetters
    /** Same as [SyncSession.sessionLost] (kept for pre-follow-up callers). */
    val sessionLost: Boolean get() = lazySession.sessionLost

    private val lazySession = object : SyncSession {
        override val userId: String? get() = coreOrNull()?.session?.userId
        override val sessionLost: Boolean get() = coreOrNull()?.session?.sessionLost == true
        override suspend fun ensureGuest(): GuestResult = withContext(Dispatchers.IO) {
            runCatching { core() }.fold({ it.session.ensureGuestBlocking() }, { GuestResult.Failed("engine_init") })
        }
        override suspend fun signOut() { runCatching { withContext(Dispatchers.IO) { core() } }.getOrNull()?.session?.signOut() }
    }
    override val session: SyncSession get() = lazySession
    override val scheduler: SyncScheduler get() = workScheduler
    override val status: SyncStatusSource get() = flows

    internal fun publishConflict(c: RoutineConflictInfo?) { _conflict.value = c }

    fun flushNow() = workScheduler.flushNow()

    /** Worker body (background thread): flush until nothing is dirty; on a retryable failure schedule the backoff run. */
    internal fun runFlushes() {
        val c = runCatching { core() }.getOrNull() ?: return
        if (!workScheduler.running.compareAndSet(false, true)) { workScheduler.dirty.set(true); return }
        try {
            while (true) {
                workScheduler.dirty.set(false)
                when (val o = c.flusher.flush()) {
                    is FlushOutcome.RetryLater -> { workScheduler.scheduleRetry(backoff.next(o.afterMs)); break }
                    FlushOutcome.NoSession -> break
                    is FlushOutcome.Done -> {
                        backoff.reset()
                        // D6: never Synced while anything is parked
                        if (c.flusher.sendableCount() == 0 && c.flusher.parkedCount() == 0) c.tracker.markSynced(nowMs())
                        if (!workScheduler.dirty.get()) break
                    }
                }
            }
        } finally { workScheduler.running.set(false); c.tracker.refresh() }
    }

    /** DEV-52 keepMine: re-send the local routine against the server's revision with a new idempotency key. */
    fun keepMineRoutine() {
        val c = core(); val conflict = c.routine.conflict ?: return
        c.routine.serverRevision = conflict.revision; c.routine.conflict = null; publishConflict(null)
        c.outbox.requeue("routine:current")
    }

    /** DEV-52 takeServer: the caller saves [RoutineConflictInfo.planJson] locally without enqueueing. */
    fun takeServerRoutine(): RoutineConflictInfo? {
        val c = core(); val conflict = c.routine.conflict ?: return null
        c.routine.serverRevision = conflict.revision; c.routine.conflict = null; publishConflict(null)
        c.outbox.drop("routine:current"); c.tracker.refresh()
        return conflict
    }

    /** Reset (wipe this device): cancel work, clear tokens, drop queue + dead letters + lastSyncedAt. */
    override suspend fun reset() = withContext(Dispatchers.IO) { resetBlocking() }

    internal fun resetBlocking() {
        workScheduler.cancel()
        val c = core()
        c.session.clear(); c.outbox.clearAll(); c.routine.clear()
        publishConflict(null); c.tracker.clear()
    }
}

/** Process-wide engine (the worker finds it here). Set by [Dev45SyncEngineFactory]. */
object Dev45Sync {
    @Volatile var current: Dev45SyncEngine? = null
        internal set
}

/**
 * Bound as `SyncEngines.dev45` (sync/SyncContracts.kt). [build] returns at once on any thread; the engine's
 * disk / Keystore work (session decrypt, outbox and routine reads, lastSyncedAt) runs on [io].
 * [cipher], [outbox] and [io] are test seams (Robolectric has no AndroidKeyStore).
 */
object Dev45SyncEngineFactory : SyncEngineFactory {
    override fun create(context: Context, supabaseUrl: String, anonKey: String): SyncEngine =
        build(context, supabaseUrl, anonKey)

    fun build(
        context: Context, supabaseUrl: String, anonKey: String,
        cipher: TokenCipher = KeystoreTokenCipher(),
        outbox: () -> SyncOutbox = { AppSyncOutbox.get(context) },
        nowMs: () -> Long = System::currentTimeMillis,
        io: java.util.concurrent.Executor = Dispatchers.IO.asExecutor(),
    ): Dev45SyncEngine {
        val app = context.applicationContext
        val backoff = SyncBackoff()
        val scheduler = WorkSyncScheduler(app, backoff)
        val flows = SyncStatusFlows()
        val ready = java.util.concurrent.CompletableFuture<Dev45Core>()
        val engine = Dev45SyncEngine(ready, scheduler, backoff, flows, nowMs)
        Dev45Sync.current = engine
        io.execute {
            runCatching {
                val dir = File(app.filesDir, "sync")
                val http = SupabaseHttp(supabaseUrl, anonKey, nowMs = nowMs)
                val session = SupabaseSession(http, EncryptedSessionStore(FileDraftStore(File(dir, "session.enc")), cipher), nowMs)
                val box = outbox()
                val routine = RoutineSyncStore(FileDraftStore(File(dir, "routine.properties")))
                val flusher = OutboxFlusher(box, session, SupabaseOutboxSender(http, routine), routine) { engine.publishConflict(it) }
                var trackerRef: SyncStatusTracker? = null
                val network = AndroidNetworkMonitor(app) { scheduler.flushNow(); trackerRef?.refresh() }
                val tracker = SyncStatusTracker(FileDraftStore(File(dir, "last_synced_at")), { session.userId != null },
                    { flusher.sendableCount() }, { box.deadLetters.size }, { network.online }, { flusher.parkedCount() }, flows)
                trackerRef = tracker
                box.ownerProvider = { session.userId }
                box.listener = { scheduler.requestFlush(); tracker.refresh() }
                session.onSignedIn = { uid -> box.stampOwner(uid); tracker.refresh(); scheduler.requestFlush() }
                session.onSignedOut = { scheduler.cancel(); tracker.refresh() }
                engine.publishConflict(routine.conflict)
                tracker.refresh()
                Dev45Core(box, session, flusher, routine, tracker)
            }.fold({ ready.complete(it) }, { ready.completeExceptionally(it) })
            // queued before the engine was ready (e.g. offline edits at cold start): flush once it is
            if (ready.isDone && !ready.isCompletedExceptionally && runCatching { ready.get().flusher.sendableCount() }.getOrDefault(0) > 0) scheduler.requestFlush()
        }
        return engine
    }
}
