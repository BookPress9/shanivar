package com.shanivar.nativeapp

import java.util.Properties

/**
 * B3: local-first sync outbox. Attempts, self-reports and routine changes are queued on device with an
 * idempotency key (kind:id); re-enqueueing the same key replaces the payload. Nothing is sent until a
 * real [SyncSender] exists (DEV-25: backend not built yet). No network here.
 *
 * DEV-38 deletes: a delete re-enqueues the same key with a tombstone payload ([SyncPayloads.tombstone],
 * `{"id":..,"deleted":true}`), so it supersedes any still-pending upsert of that row and the sender
 * issues a delete instead of an upsert. Every writer in the process must share one instance per file
 * ([AppSyncOutbox]); two instances over the same file would overwrite each other's entries.
 */
data class OutboxItem(val kind: String, val id: String, val payload: String) { val key get() = "$kind:$id" }

fun interface SyncSender { /** Return true only when the server acknowledged the key. */ fun send(item: OutboxItem): Boolean }

class SyncOutbox(
    private val store: DraftStore,
    /** DEV-47 hook: called after every enqueue, outside the lock (AppSync.onEnqueue -> requestFlush). */
    private val onEnqueue: () -> Unit = {},
) {
    private val items = LinkedHashMap<String, OutboxItem>()

    init {
        val p = Properties().apply { store.read()?.let { load(it.reader()) } }
        p.getProperty("n")?.toIntOrNull()?.let { n ->
            for (i in 0 until n) {
                val k = p.getProperty("k$i") ?: continue
                val (kind, id) = k.split(":", limit = 2)
                items[k] = OutboxItem(kind, id, p.getProperty("p$i", ""))
            }
        }
    }

    val pending: List<OutboxItem> @Synchronized get() = items.values.toList()

    fun enqueue(kind: String, id: String, payload: String) {
        synchronized(this) {
            val item = OutboxItem(kind, id, payload)
            items.remove(item.key); items[item.key] = item; save()
        }
        runCatching { onEnqueue() }
    }

    /** Sends in order; stops at the first failure so ordering is kept. Returns the count acknowledged. */
    @Synchronized fun drain(sender: SyncSender): Int {
        var sent = 0
        for (item in pending) {
            if (!runCatching { sender.send(item) }.getOrDefault(false)) break
            items.remove(item.key); sent++
        }
        save(); return sent
    }

    private fun save() {
        val p = Properties()
        p["n"] = items.size.toString()
        items.values.forEachIndexed { i, it -> p["k$i"] = it.key; p["p$i"] = it.payload }
        val w = java.io.StringWriter(); p.store(w, null); store.write(w.toString())
    }
}

/** DEV-38: the one process-wide device outbox (Sadhana record and Routine plan changes share it). */
object AppSyncOutbox {
    @Volatile private var instance: SyncOutbox? = null
    fun get(context: android.content.Context): SyncOutbox = instance ?: synchronized(this) {
        instance ?: SyncOutbox(FileDraftStore(java.io.File(context.applicationContext.filesDir, "sadhana/outbox.properties")),
            onEnqueue = { com.shanivar.nativeapp.sync.AppSync.current.onEnqueue() }).also { instance = it }
    }

    /** Tests only: drop the process-wide instance so the next [get] binds to the current test's filesDir. */
    @androidx.annotation.VisibleForTesting
    internal fun reset() = synchronized(this) { instance = null }
}
