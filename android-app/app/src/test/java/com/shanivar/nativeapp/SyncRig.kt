package com.shanivar.nativeapp

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

/** Test cipher: reversible but never plaintext (Robolectric/JVM have no AndroidKeyStore). */
class XorCipher : TokenCipher {
    override fun encrypt(plain: ByteArray) = ByteArray(plain.size) { (plain[it].toInt() xor 0x5A).toByte() }.reversedArray()
    override fun decrypt(sealed: ByteArray) = sealed.reversedArray().let { b -> ByteArray(b.size) { (b[it].toInt() xor 0x5A).toByte() } }
}

const val UID_A = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
const val UID_B = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"

fun authBody(uid: String = UID_A, access: String = "acc-1", refresh: String = "ref-1", expiresAtSec: Long = 4_000_000_000L) =
    """{"access_token":"$access","token_type":"bearer","expires_in":3600,"expires_at":$expiresAtSec,"refresh_token":"$refresh","user":{"id":"$uid","aud":"authenticated","role":"authenticated","app_metadata":{"provider":"anonymous"}}}"""

fun authReply(uid: String = UID_A, access: String = "acc-1", refresh: String = "ref-1", expiresAtSec: Long = 4_000_000_000L) =
    MockResponse().setBody(authBody(uid, access, refresh, expiresAtSec))

/** Pure-JVM engine wiring over a MockWebServer (or a real local Supabase URL). */
class SyncRig(
    baseUrl: String,
    anonKey: String = "anon",
    signedInAs: String? = UID_A,
    val clock: FakeNow = FakeNow(),
    val sessionStore: MemDraftStore = MemDraftStore(),
    val outboxStore: MemDraftStore = MemDraftStore(),
    readTimeoutMs: Int = 15_000,
    accessExpiresAtMs: Long = 4_000_000_000_000L,
) {
    constructor(server: MockWebServer, signedInAs: String? = UID_A, readTimeoutMs: Int = 15_000) :
        this(server.url("/").toString(), signedInAs = signedInAs, readTimeoutMs = readTimeoutMs)

    val http = SupabaseHttp(baseUrl, anonKey, connectTimeoutMs = 2_000, readTimeoutMs = readTimeoutMs, nowMs = clock)
    val encrypted = EncryptedSessionStore(sessionStore, XorCipher())
    init { if (signedInAs != null) encrypted.save(StoredSession(signedInAs, "acc-1", "ref-1", accessExpiresAtMs)) }
    val session = SupabaseSession(http, encrypted, clock)
    val outbox = SyncOutbox(outboxStore, clock = clock).also { o -> o.ownerProvider = { session.userId } }
    val routine = RoutineSyncStore(MemDraftStore())
    val conflicts = mutableListOf<RoutineConflictInfo>()
    val sender = SupabaseOutboxSender(http, routine) { "Europe/Berlin" }
    val flusher = OutboxFlusher(outbox, session, sender, routine) { conflicts += it }
}

class FakeNow(var ms: Long = 1_760_700_000_000L) : () -> Long { override fun invoke() = ms }
