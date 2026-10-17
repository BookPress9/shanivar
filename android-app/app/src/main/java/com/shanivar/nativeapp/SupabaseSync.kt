package com.shanivar.nativeapp

import com.shanivar.nativeapp.sync.GuestResult
import com.shanivar.nativeapp.sync.SyncSession
import com.shanivar.nativeapp.sync.SyncStatusSource
import com.shanivar.nativeapp.sync.SyncUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Properties

/*
 * DEV-45 sync engine, pure-JVM part (no android.* so every error class is unit-tested with MockWebServer).
 * Android glue (Keystore cipher, WorkManager worker/scheduler, connectivity, factory) is in SyncAndroid.kt.
 *
 * Wire contract (handoffs/backend-to-frontend-dev45-contract.md, agreed):
 * - Every request: `apikey: <anon key>` + `Authorization: Bearer <user access token>`. Nothing else.
 * - Upsert: POST /rest/v1/<table>?on_conflict=user_id,id (reflection: user_id,attempt_id), Prefer
 *   resolution=merge-duplicates (orbit, append-only since DEV-54: resolution=ignore-duplicates), body = the
 *   outbox payload plus "client_updated_at" (ISO-8601 UTC, stamped at enqueue). The client never sends user_id.
 * - Delete: POST /rest/v1/tombstones?on_conflict=user_id,kind,id with {kind,id,client_updated_at}; the
 *   server deletes the row (supabase/migrations/20261018090000_dev45_sync_conflicts.sql).
 * - Routine: POST /rest/v1/rpc/put_routine (DEV-52). Until it exists (404 / PGRST202) the item is parked,
 *   and a 409 {code:"revision_conflict", current_revision, current_plan} parks it and is published.
 * - No payloads, tokens or keys in logs: this file does not log at all.
 */
object SupabaseTables {
    fun forKind(kind: String): String? = when (kind) {
        "attempt" -> "practice_attempts"; "self_report" -> "self_reports"; "routine" -> "routine_plans"
        "window" -> "protection_windows"; "orbit" -> "orbit_events"; "reflection" -> "practice_reflections"; else -> null
    }
    /** PostgREST on_conflict target per kind: the table's primary key minus nothing (reflections key on attempt_id). */
    fun conflictTarget(kind: String): String = if (kind == "reflection") "user_id,attempt_id" else "user_id,id"
    /** DEV-54: orbit_events is append-only (no update privilege), so a replay is ignored, never merged. */
    fun appendOnly(kind: String): Boolean = kind == "orbit"
    /**
     * Kinds the tombstones table accepts (supabase check constraint). Routine is never deleted; DEV-54 made orbit
     * append-only (delete only via account cascade) and added reflection (cleared reflection = tombstone by attempt id).
     */
    val tombstoneKinds = setOf("attempt", "self_report", "window", "reflection")
}

object SyncPayloads {
    private fun q(s: String?): String {
        if (s == null) return "null"
        val b = StringBuilder(s.length + 2).append('"')
        for (c in s) when {
            c == '"' -> b.append("\\\""); c == '\\' -> b.append("\\\\")
            c == '\n' -> b.append("\\n"); c == '\r' -> b.append("\\r"); c == '\t' -> b.append("\\t")
            c < ' ' -> b.append(String.format("\\u%04x", c.code))
            else -> b.append(c)
        }
        return b.append('"').toString()
    }
    private fun ts(ms: Long?) = q(ms?.let { Instant.ofEpochMilli(it).toString() })

    /** Server cap on practice_attempts.events (octet_length(events::text) <= 32768); keep a margin for jsonb spacing. */
    const val EVENTS_MAX_BYTES = 32_000
    fun attempt(a: SadhanaAttempt) = """{"id":${q(a.id)},"practice_id":${q(a.practiceId)},"content_version":${q(a.contentVersion)},"started_at":${ts(a.startedAtUtc)},"zone_id":${q(a.zoneId)},"intended_seconds":${a.intendedSeconds},"active_ms":${a.activeMillis},"outcome":${q(a.outcome.name)},"ended_at":${ts(a.endedAtUtc)},"anchor":${q(a.anchor.name)},"anchor_changes":${a.anchorChanges},"events":${events(a.events)}}"""

    /**
     * DEV-54: attempt events as a JSON array of {type, at, active_seconds, detail}, written with jsonb's own spacing
     * so the byte count here is the byte count the server checks. Over the cap, the oldest middle events are
     * dropped: the first (start) and the most recent events are kept.
     */
    fun events(list: List<SadhanaEvent>): String {
        val items = list.map { """{"type": ${q(it.type.name)}, "at": ${ts(it.atUtcMillis)}, "active_seconds": ${it.activeSeconds}, "detail": ${q(it.detail)}}""" }.toMutableList()
        fun size() = 2 + items.sumOf { it.toByteArray(Charsets.UTF_8).size } + 2 * maxOf(0, items.size - 1)
        while (items.size > 2 && size() > EVENTS_MAX_BYTES) items.removeAt(1)
        while (items.isNotEmpty() && size() > EVENTS_MAX_BYTES) items.removeAt(0)
        return items.joinToString(", ", "[", "]")
    }
    /** DEV-38: a deleted row, queued under the same kind:id key as its upsert (op = DELETE in the outbox). */
    fun tombstone(id: String) = """{"id":${q(id)},"deleted":true}"""
    fun isTombstone(payload: String) = payload.startsWith("{\"id\":") && payload.endsWith(",\"deleted\":true}")
    /**
     * DEV-38: routine_plans row. One plan per user (id "current", permanent); plan is the local RoutinePlanCodec
     * JSON. DEV-45: protectedPackages is stripped until DEV-52 decides where packages live.
     */
    fun routine(plan: RoutinePlanV1): String {
        val json = org.json.JSONObject(RoutinePlanCodec.encode(plan)).apply { remove("protectedPackages") }
        return """{"id":"current","schema_version":${RoutinePlanV1.SCHEMA_VERSION},"plan":$json}"""
    }
    fun selfReport(r: IndependentPracticeRecord) = """{"id":${q(r.id)},"local_date":${q(r.localDate.toString())},"intended_minutes":${r.intendedMinutes ?: "null"},"actual_minutes":${r.actualMinutes ?: "null"},"practice_id":${q(r.practiceId)},"private_by_default":${r.privateByDefault}}"""
    /** DEV-54 orbit_events row (append-only). local_date carries the app's OrbitEvent.occurrenceDate. */
    fun orbit(e: OrbitEvent) = """{"id":${q(e.id)},"type":${q(e.type.name)},"local_date":${q(e.occurrenceDate.toString())},"source":${q(e.source.name)},"entity_id":${q(e.entityId)}}"""
    /** DEV-54 practice_reflections row, keyed by attempt_id (outbox kind "reflection", id = attemptId). */
    fun reflection(r: PracticeReflectionRecord) = """{"attempt_id":${q(r.attemptId)},"usefulness":${q(r.usefulness?.name?.lowercase())},"note":${q(r.note)}}"""

    internal fun quote(s: String) = q(s)
    internal fun isoUtc(ms: Long) = Instant.ofEpochMilli(ms).toString()

    /** Adds "client_updated_at" to a flat JSON object payload. */
    internal fun withClientTime(payload: String, ms: Long): String {
        val t = payload.trimEnd()
        require(t.startsWith("{") && t.endsWith("}")) { "payload is not a JSON object" }
        val body = t.substring(1, t.length - 1).trim()
        return "{" + body + (if (body.isEmpty()) "" else ",") + "\"client_updated_at\":" + q(isoUtc(ms)) + "}"
    }
}

/** Tiny, dependency-free JSON field reader for auth/RPC replies (org.json is a stub in plain unit tests). */
internal object MiniJson {
    private fun valueStart(json: String, key: String): Int? {
        val m = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*").find(json) ?: return null
        return m.range.last + 1
    }
    fun str(json: String, key: String): String? {
        val i = valueStart(json, key) ?: return null
        if (i >= json.length || json[i] != '"') return null
        val sb = StringBuilder(); var j = i + 1
        while (j < json.length) {
            val c = json[j]
            if (c == '\\' && j + 1 < json.length) { sb.append(json[j + 1]); j += 2; continue }
            if (c == '"') return sb.toString()
            sb.append(c); j++
        }
        return null
    }
    fun long(json: String, key: String): Long? {
        val i = valueStart(json, key) ?: return null
        return Regex("-?\\d+").matchAt(json, i)?.value?.toLongOrNull()
    }
    /** Raw text of an object/array value, brace-matched and string-aware. */
    fun raw(json: String, key: String): String? {
        val i = valueStart(json, key) ?: return null
        if (i >= json.length || (json[i] != '{' && json[i] != '[')) return null
        var depth = 0; var inStr = false; var j = i
        while (j < json.length) {
            val c = json[j]
            if (inStr) { if (c == '\\') j++ else if (c == '"') inStr = false }
            else when (c) { '"' -> inStr = true; '{', '[' -> depth++; '}', ']' -> { depth--; if (depth == 0) return json.substring(i, j + 1) } }
            j++
        }
        return null
    }
    /** Supabase auth reply: user.id (the "user" object's first-level id). */
    fun userId(json: String): String? = raw(json, "user")?.let { str(it, "id") }
}

/** Retry-After: delta-seconds or an HTTP-date. Returns a delay in ms (never negative), or null. */
object RetryAfter {
    fun parseMs(value: String?, nowMs: Long): Long? {
        val v = value?.trim().orEmpty(); if (v.isEmpty()) return null
        v.toLongOrNull()?.let { return (it.coerceAtLeast(0) * 1000) }
        return runCatching { ZonedDateTime.parse(v, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() - nowMs }
            .getOrNull()?.coerceAtLeast(0)
    }
}

data class HttpReply(val code: Int, val body: String, val retryAfterMs: Long? = null)

/** Blocking HTTP to Supabase. Throws IOException (incl. SocketTimeoutException) on transport failure. */
class SupabaseHttp(
    private val baseUrl: String,
    private val anonKey: String,
    private val connectTimeoutMs: Int = 10_000,
    private val readTimeoutMs: Int = 15_000,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    fun call(method: String, path: String, body: String?, bearer: String?, extra: Map<String, String> = emptyMap()): HttpReply {
        val c = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            c.requestMethod = method; c.connectTimeout = connectTimeoutMs; c.readTimeout = readTimeoutMs
            c.useCaches = false
            c.setRequestProperty("apikey", anonKey)
            c.setRequestProperty("Authorization", "Bearer " + (bearer ?: anonKey))
            c.setRequestProperty("Content-Type", "application/json")
            extra.forEach { (k, v) -> c.setRequestProperty(k, v) }
            if (body != null) { c.doOutput = true; c.outputStream.use { it.write(body.toByteArray()) } }
            val code = c.responseCode
            val text = (if (code < 400) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            return HttpReply(code, text, RetryAfter.parseMs(c.getHeaderField("Retry-After"), nowMs()))
        } finally { c.disconnect() }
    }
}

// ── Session (AC1) ─────────────────────────────────────────────────────────────────────────────

/** Keystore-backed in the app ([KeystoreTokenCipher]); tests inject a fake. */
interface TokenCipher {
    fun encrypt(plain: ByteArray): ByteArray
    fun decrypt(sealed: ByteArray): ByteArray
}

data class StoredSession(
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMs: Long,
    /** Refresh token rejected by the server: stop sending, never create a new guest automatically. */
    val lost: Boolean = false,
)

/** Tokens at rest are only ever written encrypted (base64 of the cipher output). */
class EncryptedSessionStore(private val store: DraftStore, private val cipher: TokenCipher) {
    fun load(): StoredSession? {
        val raw = store.read()?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val p = Properties().apply { load(String(cipher.decrypt(Base64.getDecoder().decode(raw)), Charsets.UTF_8).reader()) }
        return StoredSession(p.getProperty("u") ?: return null, p.getProperty("a") ?: return null, p.getProperty("r") ?: return null,
            p.getProperty("e")?.toLongOrNull() ?: 0L, p.getProperty("l") == "1")
    }
    fun save(s: StoredSession) {
        val p = Properties().apply { put("u", s.userId); put("a", s.accessToken); put("r", s.refreshToken); put("e", s.expiresAtMs.toString()); put("l", if (s.lost) "1" else "0") }
        val w = java.io.StringWriter(); p.store(w, null)
        store.write(Base64.getEncoder().encodeToString(cipher.encrypt(w.toString().toByteArray(Charsets.UTF_8))))
    }
    fun clear() = store.clear()
}

sealed interface TokenResult {
    data class Valid(val token: String) : TokenResult
    data object NoSession : TokenResult
    data object Lost : TokenResult
    data class Offline(val retryAfterMs: Long?) : TokenResult
}

/**
 * DEV-45 AC1: guest = Supabase anonymous sign-in once per install. Tokens persist encrypted and survive
 * restarts; refresh when < 60 s remain and once after a 401. No anonymous sign-up ever happens while a
 * refresh token is stored (including a revoked one: the session is marked lost and stays so until an
 * explicit [signOut]/[clear]).
 */
class SupabaseSession(
    private val http: SupabaseHttp,
    private val store: EncryptedSessionStore,
    private val nowMs: () -> Long = System::currentTimeMillis,
    private val enabled: Boolean = true,
) : SyncSession {
    private val lock = Any()
    @Volatile private var current: StoredSession? = runCatching { store.load() }.getOrNull()
    /** Engine hooks: stamp unowned outbox items + flush on a session; cancel work on sign-out. */
    @Volatile var onSignedIn: (String) -> Unit = {}
    @Volatile var onSignedOut: () -> Unit = {}

    override val userId: String? get() = current?.takeUnless { it.lost }?.userId
    /** Diagnostics: true when the refresh token was rejected (shown as session_lost, never to end users). */
    val sessionLost: Boolean get() = current?.lost == true
    /** True while a refresh token is stored (lost or not). */
    val hasRefreshToken: Boolean get() = current != null

    override suspend fun ensureGuest(): GuestResult = withContext(Dispatchers.IO) { ensureGuestBlocking() }

    fun ensureGuestBlocking(): GuestResult {
        if (!enabled) return GuestResult.Disabled
        val result = synchronized(lock) {
            current?.let { s -> return@synchronized if (s.lost) GuestResult.Failed("session_lost") else GuestResult.Ok(s.userId) }
            val reply = try { http.call("POST", "/auth/v1/signup", "{}", bearer = null) } catch (e: IOException) { return@synchronized GuestResult.Offline }
            when {
                reply.code in 200..299 -> {
                    val s = parseSession(reply.body) ?: return@synchronized GuestResult.Failed("bad_auth_reply")
                    try { store.save(s) } catch (e: Exception) { return@synchronized GuestResult.Failed("token_store") }
                    current = s; GuestResult.Ok(s.userId)
                }
                reply.code == 429 || reply.code >= 500 -> GuestResult.Offline
                else -> GuestResult.Failed("http_${reply.code}")
            }
        }
        if (result is GuestResult.Ok) runCatching { onSignedIn(result.userId) }
        return result
    }

    /** A valid access token for the current user, refreshing first when it expires within 60 s. */
    fun accessToken(): TokenResult = synchronized(lock) {
        val s = current ?: return TokenResult.NoSession
        if (s.lost) return TokenResult.Lost
        if (s.expiresAtMs - nowMs() < REFRESH_MARGIN_MS) refreshLocked(s) else TokenResult.Valid(s.accessToken)
    }

    /** After a 401 with [used]: refresh once (unless another caller already did). */
    fun refreshAfter401(used: String): TokenResult = synchronized(lock) {
        val s = current ?: return TokenResult.NoSession
        if (s.lost) return TokenResult.Lost
        if (s.accessToken != used) TokenResult.Valid(s.accessToken) else refreshLocked(s)
    }

    private fun refreshLocked(s: StoredSession): TokenResult {
        val reply = try {
            http.call("POST", "/auth/v1/token?grant_type=refresh_token", "{\"refresh_token\":${SyncPayloads.quote(s.refreshToken)}}", bearer = null)
        } catch (e: IOException) { return TokenResult.Offline(null) }
        return when {
            reply.code in 200..299 -> {
                val n = parseSession(reply.body)?.copy(userId = s.userId) ?: return TokenResult.Offline(null)
                runCatching { store.save(n) }
                current = n; TokenResult.Valid(n.accessToken)
            }
            reply.code in setOf(400, 401, 403) -> {
                val l = s.copy(lost = true); runCatching { store.save(l) }; current = l; TokenResult.Lost
            }
            else -> TokenResult.Offline(reply.retryAfterMs)
        }
    }

    private fun parseSession(body: String): StoredSession? {
        val access = MiniJson.str(body, "access_token") ?: return null
        val refresh = MiniJson.str(body, "refresh_token") ?: return null
        val uid = MiniJson.userId(body) ?: return null
        val exp = MiniJson.long(body, "expires_at")?.times(1000)
            ?: MiniJson.long(body, "expires_in")?.let { nowMs() + it * 1000 } ?: (nowMs() + 3_600_000)
        return StoredSession(uid, access, refresh, exp)
    }

    /** Clears tokens. Queued items stay stamped to the old user and are never sent under another. */
    override suspend fun signOut() = withContext(Dispatchers.IO) { clear() }

    fun clear() {
        synchronized(lock) { runCatching { store.clear() }; current = null }
        runCatching { onSignedOut() }
    }

    private companion object { const val REFRESH_MARGIN_MS = 60_000L }
}

// ── Sender (AC4, AC6) ─────────────────────────────────────────────────────────────────────────

sealed interface SendResult {
    data object Acked : SendResult
    data class Retry(val afterMs: Long?) : SendResult
    data object Unauthorized : SendResult
    data class Rejected(val status: Int, val code: String) : SendResult
    data class Park(val reason: String) : SendResult
    data class RoutineConflict(val revision: Int, val planJson: String) : SendResult
}

/** The canonical server routine published on a put_routine 409 (DEV-52 resolves it: keepMine / takeServer). */
data class RoutineConflictInfo(val revision: Int, val planJson: String)

/** Engine-owned routine state, persisted (sync/routine.properties): last server revision + an open conflict. */
class RoutineSyncStore(private val store: DraftStore) {
    private val lock = Any()
    private var props = Properties().apply { runCatching { store.read()?.let { load(it.reader()) } } }
    var serverRevision: Int
        get() = synchronized(lock) { props.getProperty("rev")?.toIntOrNull() ?: 0 }
        set(v) = synchronized(lock) { props["rev"] = v.toString(); save() }
    var conflict: RoutineConflictInfo?
        get() = synchronized(lock) { props.getProperty("crev")?.toIntOrNull()?.let { RoutineConflictInfo(it, props.getProperty("cplan", "{}")) } }
        set(v) = synchronized(lock) {
            if (v == null) { props.remove("crev"); props.remove("cplan") } else { props["crev"] = v.revision.toString(); props["cplan"] = v.planJson }
            save()
        }
    fun clear() = synchronized(lock) { props = Properties(); store.clear() }
    private fun save() { val w = java.io.StringWriter(); props.store(w, null); store.write(w.toString()) }
}

/** Maps one outbox item to one Supabase request and classifies the reply. Never logs. */
class SupabaseOutboxSender(
    private val http: SupabaseHttp,
    private val routine: RoutineSyncStore,
    private val zoneId: () -> String = { ZoneId.systemDefault().id },
) {
    fun send(item: OutboxItem, bearer: String): SendResult {
        val table = SupabaseTables.forKind(item.kind) ?: return SendResult.Rejected(0, "unknown_kind")
        return try {
            when {
                item.op == OutboxOp.DELETE -> deleteViaTombstone(item, bearer)
                item.kind == "routine" -> putRoutine(item, bearer)
                else -> classify(http.call("POST", "/rest/v1/$table?on_conflict=${SupabaseTables.conflictTarget(item.kind)}",
                    SyncPayloads.withClientTime(item.payload, item.clientUpdatedAtMs), bearer,
                    if (SupabaseTables.appendOnly(item.kind)) APPEND else UPSERT))
            }
        } catch (e: IOException) {
            SendResult.Retry(null)
        } catch (e: IllegalArgumentException) {
            SendResult.Rejected(0, "bad_payload")
        }
    }

    private fun deleteViaTombstone(item: OutboxItem, bearer: String): SendResult {
        if (item.kind !in SupabaseTables.tombstoneKinds) return SendResult.Rejected(0, "delete_unsupported")
        val body = "{\"kind\":${SyncPayloads.quote(item.kind)},\"id\":${SyncPayloads.quote(item.id)},\"client_updated_at\":${SyncPayloads.quote(SyncPayloads.isoUtc(item.clientUpdatedAtMs))}}"
        return classify(http.call("POST", "/rest/v1/tombstones?on_conflict=user_id,kind,id", body, bearer, UPSERT))
    }

    /** DEV-52 put_routine with expected_revision read at send time; parks until the RPC exists. */
    private fun putRoutine(item: OutboxItem, bearer: String): SendResult {
        val plan = MiniJson.raw(item.payload, "plan") ?: return SendResult.Rejected(0, "bad_payload")
        val schema = MiniJson.long(item.payload, "schema_version") ?: RoutinePlanV1.SCHEMA_VERSION.toLong()
        val body = "{\"p_plan\":$plan,\"p_schema_version\":$schema,\"p_time_zone\":${SyncPayloads.quote(zoneId())}," +
            "\"p_expected_revision\":${routine.serverRevision},\"p_idempotency_key\":${SyncPayloads.quote(item.idemKey ?: item.key)}}"
        val r = http.call("POST", "/rest/v1/rpc/put_routine", body, bearer, mapOf("Prefer" to "return=representation"))
        if (r.code in 200..299) {
            MiniJson.long(r.body, "revision")?.let { routine.serverRevision = it.toInt() }
            return SendResult.Acked
        }
        if (r.code == 404 || MiniJson.str(r.body, "code") == "PGRST202") return SendResult.Park(PARK_RPC_MISSING)
        if (r.code == 409) {
            val rev = MiniJson.long(r.body, "current_revision")?.toInt()
            val plan2 = MiniJson.raw(r.body, "current_plan")
            return if (rev != null && plan2 != null) SendResult.RoutineConflict(rev, plan2) else SendResult.Park(PARK_CONFLICT)
        }
        return classify(r)
    }

    companion object {
        const val PARK_RPC_MISSING = "put_routine_missing"
        const val PARK_CONFLICT = "revision_conflict"
        private val UPSERT = mapOf("Prefer" to "resolution=merge-duplicates,return=minimal")
        private val APPEND = mapOf("Prefer" to "resolution=ignore-duplicates,return=minimal")

        /** AC4 error classes. Codes only, never payloads. */
        fun classify(r: HttpReply): SendResult = when {
            r.code in 200..299 -> SendResult.Acked
            r.code == 401 -> SendResult.Unauthorized
            r.code == 429 || r.code >= 500 -> SendResult.Retry(r.retryAfterMs)
            // 404: table/RPC missing = server not migrated yet; 408: request timeout. Retry, never drop.
            r.code == 404 || r.code == 408 -> SendResult.Retry(r.retryAfterMs)
            else -> SendResult.Rejected(r.code, MiniJson.str(r.body, "code")?.takeIf { it.matches(Regex("[A-Za-z0-9_]{1,32}")) } ?: "http_${r.code}")
        }
    }
}

// ── Flush loop ────────────────────────────────────────────────────────────────────────────────

sealed interface FlushOutcome {
    data class Done(val acked: Int, val deadLettered: Int, val parked: Int) : FlushOutcome
    data class RetryLater(val afterMs: Long?, val acked: Int) : FlushOutcome
    data object NoSession : FlushOutcome
}

/**
 * One flush: snapshot under the outbox lock, send outside it (G1). Items owned by another user are never
 * sent (AC2). A rejected item is dead-lettered and the rest continue; a retryable failure ends the run
 * (backoff). A 401 refreshes once and retries the same item once.
 */
class OutboxFlusher(
    private val outbox: SyncOutbox,
    private val session: SupabaseSession,
    private val sender: SupabaseOutboxSender,
    private val routine: RoutineSyncStore,
    private val onConflict: (RoutineConflictInfo) -> Unit = {},
) {
    private val flushLock = Any()

    fun flush(): FlushOutcome = synchronized(flushLock) {
        val uid = session.userId ?: return FlushOutcome.NoSession
        outbox.stampOwner(uid)
        var acked = 0; var dead = 0; var parked = 0
        for (item in outbox.snapshot()) {
            if (item.ownerUserId != uid) continue
            if (item.parked == SupabaseOutboxSender.PARK_CONFLICT) continue
            var token = when (val t = session.accessToken()) {
                is TokenResult.Valid -> t.token
                is TokenResult.Offline -> return FlushOutcome.RetryLater(t.retryAfterMs, acked)
                TokenResult.Lost, TokenResult.NoSession -> return FlushOutcome.NoSession
            }
            var r = sender.send(item, token)
            if (r == SendResult.Unauthorized) {
                token = when (val t = session.refreshAfter401(token)) {
                    is TokenResult.Valid -> t.token
                    is TokenResult.Offline -> return FlushOutcome.RetryLater(t.retryAfterMs, acked)
                    TokenResult.Lost, TokenResult.NoSession -> return FlushOutcome.NoSession
                }
                r = sender.send(item, token)
                if (r == SendResult.Unauthorized) return FlushOutcome.RetryLater(null, acked)
            }
            when (r) {
                SendResult.Acked -> { outbox.ack(item); acked++ }
                is SendResult.Rejected -> { outbox.deadLetter(item, r.status, r.code); dead++ }
                is SendResult.Park -> { outbox.park(item, r.reason); parked++ }
                is SendResult.RoutineConflict -> {
                    val c = RoutineConflictInfo(r.revision, r.planJson)
                    routine.conflict = c
                    outbox.park(item, SupabaseOutboxSender.PARK_CONFLICT); parked++
                    runCatching { onConflict(c) }
                }
                is SendResult.Retry -> return FlushOutcome.RetryLater(r.afterMs, acked)
                SendResult.Unauthorized -> return FlushOutcome.RetryLater(null, acked)
            }
        }
        FlushOutcome.Done(acked, dead, parked)
    }

    /** Items this user could still send now (dead letters and parked items excluded). */
    fun sendableCount(): Int {
        val uid = session.userId
        return outbox.pending.count { (it.ownerUserId == null || it.ownerUserId == uid) && it.parked == null }
    }
}

// ── Backoff (AC3) ─────────────────────────────────────────────────────────────────────────────

/** Exponential backoff with equal jitter, 30 s up to 1 h; Retry-After wins when longer (capped at 24 h). */
class SyncBackoff(
    private val baseMs: Long = 30_000,
    private val capMs: Long = 3_600_000,
    private val random: () -> Double = Math::random,
) {
    @Volatile var attempt: Int = 0
        private set
    fun next(retryAfterMs: Long?): Long {
        val exp = (baseMs shl attempt.coerceAtMost(20)).coerceAtMost(capMs)
        attempt++
        val jittered = (exp / 2 + (random() * (exp / 2)).toLong()).coerceIn(baseMs, capMs)
        return maxOf(jittered, (retryAfterMs ?: 0L).coerceAtMost(24 * 3_600_000L))
    }
    fun reset() { attempt = 0 }
}

// ── Status (DEV-47 AC4 source) ────────────────────────────────────────────────────────────────

/** Quiet status: no counts shown to users; deadLetterCount is for the diagnostics view only. */
class SyncStatusTracker(
    private val store: DraftStore,
    private val hasSession: () -> Boolean,
    private val sendable: () -> Int,
    private val deadLetters: () -> Int,
    private val online: () -> Boolean,
) : SyncStatusSource {
    @Volatile private var lastSyncedAtMs: Long? = store.read()?.trim()?.toLongOrNull()
    private val _state = MutableStateFlow<SyncUiState>(SyncUiState.LocalOnly)
    private val _dead = MutableStateFlow(0)
    override val state: StateFlow<SyncUiState> = _state.asStateFlow()
    override val deadLetterCount: StateFlow<Int> = _dead.asStateFlow()
    init { refresh() }

    /** End of a flush that left nothing sendable. */
    fun markSynced(atMs: Long) { lastSyncedAtMs = atMs; runCatching { store.write(atMs.toString()) }; refresh() }
    fun clear() { lastSyncedAtMs = null; runCatching { store.clear() }; refresh() }

    fun refresh() {
        val pending = runCatching { sendable() }.getOrDefault(0)
        val last = lastSyncedAtMs
        _state.value = when {
            !hasSession() -> SyncUiState.LocalOnly
            pending > 0 && !online() -> SyncUiState.WaitingForNetwork
            pending == 0 && last != null -> SyncUiState.Synced(Instant.ofEpochMilli(last))
            else -> SyncUiState.LocalOnly
        }
        _dead.value = runCatching { deadLetters() }.getOrDefault(0)
    }
}
