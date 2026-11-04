package com.shanivar.nativeapp

import java.util.Properties
import java.util.UUID

/**
 * B3 / DEV-38 / DEV-45: local-first sync outbox. Attempts, self-reports and routine changes are queued on
 * device under the idempotency key `kind:id`; re-enqueueing the same key replaces the payload and moves
 * it to the tail, so only the newest version of a row is ever pending (per-key ordering).
 *
 * DEV-45 item metadata (persisted, format v2):
 * - [OutboxItem.op]: UPSERT, or DELETE for a [SyncPayloads.tombstone] payload (sent as a tombstones-table
 *   upsert by the engine, never as a bare DELETE).
 * - [OutboxItem.clientUpdatedAtMs]: UTC epoch ms stamped at enqueue (every enqueue runs inside the local
 *   edit); sent as `client_updated_at` for the server's last-writer-wins rule (DEV-42/45).
 * - [OutboxItem.ownerUserId]: the session user at enqueue, or null before a guest session exists (then
 *   stamped once by [stampOwner] on the first session). An owned item is never sent under another user.
 * - [OutboxItem.idemKey]: routine only; kept while the payload is identical, a new UUID otherwise.
 * - [OutboxItem.seq] / [OutboxItem.parked]: send bookkeeping (see [ack], [park]).
 * - [OutboxItem.failures]: item-level failed sends of this version ([recordFailure]); a re-enqueue resets it.
 *
 * Locking (DEV-45 G1): enqueue never waits on the network. The engine takes a [snapshot] under the lock,
 * sends outside it, and then [ack]s / [deadLetter]s an item only if its key still holds the same `seq`;
 * an item re-enqueued during the send stays queued and goes out next. Every writer in the process must
 * share one instance per file ([AppSyncOutbox]).
 */
enum class OutboxOp { UPSERT, DELETE }

data class OutboxItem(
    val kind: String,
    val id: String,
    val payload: String,
    val op: OutboxOp = if (SyncPayloads.isTombstone(payload)) OutboxOp.DELETE else OutboxOp.UPSERT,
    val clientUpdatedAtMs: Long = 0L,
    val ownerUserId: String? = null,
    val idemKey: String? = null,
    val seq: Long = 0L,
    val parked: String? = null,
    val failures: Int = 0,
) { val key get() = "$kind:$id" }

/** DEV-45 AC4: what is kept about a rejected item. Never the payload. */
data class DeadLetter(val key: String, val status: Int, val code: String, val atMs: Long)

fun interface SyncSender { /** Return true only when the server acknowledged the key. */ fun send(item: OutboxItem): Boolean }

class SyncOutbox(
    private val store: DraftStore,
    /** DEV-47 hook: called after every enqueue, outside the lock (AppSync.onEnqueue -> requestFlush). */
    private val onEnqueue: () -> Unit = {},
    private val clock: () -> Long = System::currentTimeMillis,
    private val newKey: () -> String = { UUID.randomUUID().toString() },
) {
    private val lock = Any()
    private val items = LinkedHashMap<String, OutboxItem>()
    private val dead = ArrayList<DeadLetter>()
    private var nextSeq = 1L

    /** Engine hook (DEV-45): current session user, read at enqueue. In-memory, no I/O. */
    @Volatile var ownerProvider: () -> String? = { null }
    /** Engine hook (DEV-45): flush request + status refresh after an enqueue, outside the lock. */
    @Volatile var listener: (() -> Unit)? = null

    init { load() }

    val pending: List<OutboxItem> get() = synchronized(lock) { items.values.toList() }
    val deadLetters: List<DeadLetter> get() = synchronized(lock) { dead.toList() }

    fun enqueue(kind: String, id: String, payload: String) {
        synchronized(lock) {
            val key = "$kind:$id"
            val prev = items[key]
            val idem = if (kind == "routine") prev?.takeIf { it.payload == payload }?.idemKey ?: newKey() else null
            val item = OutboxItem(kind, id, payload, clientUpdatedAtMs = clock(), ownerUserId = ownerProvider(),
                idemKey = idem, seq = nextSeq++)
            items.remove(key); items[key] = item; save()
        }
        runCatching { onEnqueue() }
        runCatching { listener?.invoke() }
    }

    /** Consistent copy for sending outside the lock. */
    fun snapshot(): List<OutboxItem> = pending

    /** Stamps every unowned item with the first session user. Owned items are never re-stamped. */
    fun stampOwner(userId: String): Int = synchronized(lock) {
        var n = 0
        items.replaceAll { _, it -> if (it.ownerUserId == null) { n++; it.copy(ownerUserId = userId) } else it }
        if (n > 0) save(); n
    }

    /** Removes the key only if it was not re-enqueued since [sent] was snapshotted. */
    fun ack(sent: OutboxItem): Boolean = synchronized(lock) {
        val same = items[sent.key]?.seq == sent.seq
        if (same) { items.remove(sent.key); save() }
        same
    }

    /** Moves the item to the dead-letter list (no payload) and lets the rest continue. */
    fun deadLetter(sent: OutboxItem, status: Int, code: String) = synchronized(lock) {
        dead.add(DeadLetter(sent.key, status, code.take(64), clock()))
        while (dead.size > MAX_DEAD) dead.removeAt(0)
        if (items[sent.key]?.seq == sent.seq) items.remove(sent.key)
        save()
    }

    /** Counts an item-level failed send of this version; returns the new count (0 if it was re-enqueued). */
    fun recordFailure(sent: OutboxItem): Int = synchronized(lock) {
        val cur = items[sent.key]?.takeIf { it.seq == sent.seq } ?: return@synchronized 0
        val next = cur.copy(failures = cur.failures + 1); items[sent.key] = next; save(); next.failures
    }

    /** Keeps the item queued but marks why it is held (routine conflict / missing RPC). A re-enqueue clears it. */
    fun park(sent: OutboxItem, reason: String) = synchronized(lock) {
        val cur = items[sent.key]
        if (cur != null && cur.seq == sent.seq && cur.parked != reason) { items[sent.key] = cur.copy(parked = reason); save() }
    }

    /** Unparks [key] as a new version (new seq, new idempotency key, fresh client time); e.g. routine keepMine. */
    fun requeue(key: String) {
        val changed = synchronized(lock) {
            val cur = items[key] ?: return@synchronized false
            items.remove(key)
            items[key] = cur.copy(parked = null, failures = 0, seq = nextSeq++, clientUpdatedAtMs = clock(), idemKey = if (cur.kind == "routine") newKey() else cur.idemKey)
            save(); true
        }
        if (changed) runCatching { listener?.invoke() }
    }

    /** Drops a key regardless of version (routine takeServer). */
    fun drop(key: String) = synchronized(lock) { if (items.remove(key) != null) save() }

    /** Reset (wipe this device): queue and dead letters. */
    fun clearAll() = synchronized(lock) { items.clear(); dead.clear(); save() }

    /** Legacy boolean drain (tests): sends outside the lock in order and stops at the first false. */
    fun drain(sender: SyncSender): Int {
        var sent = 0
        for (item in snapshot()) {
            if (!runCatching { sender.send(item) }.getOrDefault(false)) break
            ack(item); sent++
        }
        return sent
    }

    private fun load() {
        val raw = store.read() ?: return
        val p = Properties().apply { load(raw.reader()) }
        val v2 = p.getProperty("v") == "2"
        val loadedAt = clock()
        p.getProperty("n")?.toIntOrNull()?.let { n ->
            for (i in 0 until n) {
                val k = p.getProperty("k$i") ?: continue
                val parts = k.split(":", limit = 2); if (parts.size != 2) continue
                val payload = p.getProperty("p$i", "")
                // Pre-DEV-45 entries: op from the payload, no owner, stamped at first load (DraftStore has no mtime).
                val op = if (v2) runCatching { OutboxOp.valueOf(p.getProperty("o$i")) }.getOrNull() else null
                items[k] = OutboxItem(parts[0], parts[1], payload,
                    op = op ?: if (SyncPayloads.isTombstone(payload)) OutboxOp.DELETE else OutboxOp.UPSERT,
                    clientUpdatedAtMs = p.getProperty("t$i")?.toLongOrNull() ?: loadedAt,
                    ownerUserId = p.getProperty("u$i")?.takeIf { it.isNotEmpty() },
                    idemKey = p.getProperty("i$i")?.takeIf { it.isNotEmpty() } ?: if (parts[0] == "routine") newKey() else null,
                    seq = nextSeq++,
                    parked = p.getProperty("x$i")?.takeIf { it.isNotEmpty() },
                    failures = p.getProperty("f$i")?.toIntOrNull() ?: 0)
            }
        }
        p.getProperty("dn")?.toIntOrNull()?.let { n ->
            for (i in 0 until n) {
                val f = p.getProperty("d$i")?.split("|", limit = 4) ?: continue
                if (f.size == 4) dead.add(DeadLetter(f[3], f[0].toIntOrNull() ?: 0, f[1], f[2].toLongOrNull() ?: 0L))
            }
        }
        if (!v2 && items.isNotEmpty()) save()
    }

    private fun save() {
        val p = Properties()
        p["v"] = "2"
        p["n"] = items.size.toString()
        items.values.forEachIndexed { i, it ->
            p["k$i"] = it.key; p["p$i"] = it.payload; p["o$i"] = it.op.name; p["t$i"] = it.clientUpdatedAtMs.toString()
            p["u$i"] = it.ownerUserId.orEmpty(); p["i$i"] = it.idemKey.orEmpty(); p["x$i"] = it.parked.orEmpty()
            if (it.failures > 0) p["f$i"] = it.failures.toString()
        }
        p["dn"] = dead.size.toString()
        dead.forEachIndexed { i, d -> p["d$i"] = "${d.status}|${d.code.replace("|", "/")}|${d.atMs}|${d.key}" }
        val w = java.io.StringWriter(); p.store(w, null); store.write(w.toString())
    }

    private companion object { const val MAX_DEAD = 200 }
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
