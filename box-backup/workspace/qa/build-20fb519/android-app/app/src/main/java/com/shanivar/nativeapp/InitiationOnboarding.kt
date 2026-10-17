package com.shanivar.nativeapp

import java.io.File
import java.io.StringReader
import java.io.StringWriter
import java.util.Properties

/*
 * Initiation V1, build step 1 (android-app/wireframes/initiation-v1/HANDOFF.md:99, Jira DEV-24):
 * onboarding draft + local persistence, account adapters, guest migration.
 * Pure Kotlin so it is unit-testable; Android wiring passes a FileDraftStore under filesDir.
 * Real Google / email-code providers are NOT configured: they need the founder's OAuth client,
 * email provider and backend passwordless auth (HANDOFF "Proposed API boundary").
 */

/** Persisted checkpoints, in wireframe order (HANDOFF "Screen and transition contract"). */
enum class InitiationCheckpoint { ARRIVAL, IDENTITY, AUTHENTICATION, PROPOSITION, ANCHORS, ALARM, APPS, WINDOW, ACCESS, FOUNDATION, COMPLETE }

enum class IdentityMode { NONE, GUEST, GOOGLE, EMAIL }

data class OnboardingDraft(
    val schemaVersion: Int = SCHEMA_VERSION,
    val checkpoint: InitiationCheckpoint = InitiationCheckpoint.ARRIVAL,
    val displayName: String = "",
    val identityMode: IdentityMode = IdentityMode.NONE,
    val accountId: String? = null,
    val completedAt: Long? = null,
    val revision: Int = 0,
) {
    companion object { const val SCHEMA_VERSION = 1 }
}

/** Raw storage seam. */
interface DraftStore {
    fun read(): String?
    fun write(value: String)
    fun clear()
}

/** Atomic file store: write temp then rename, so process death never leaves a half-written draft. */
class FileDraftStore(private val file: File) : DraftStore {
    override fun read(): String? = if (file.exists()) file.readText() else null
    override fun write(value: String) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(value)
        if (!tmp.renameTo(file)) { file.delete(); check(tmp.renameTo(file)) { "Could not save onboarding draft" } }
    }
    override fun clear() { file.delete() }
}

object OnboardingDraftCodec {
    fun encode(d: OnboardingDraft): String {
        val p = Properties()
        p["schemaVersion"] = d.schemaVersion.toString()
        p["checkpoint"] = d.checkpoint.name
        p["displayName"] = d.displayName
        p["identityMode"] = d.identityMode.name
        d.accountId?.let { p["accountId"] = it }
        d.completedAt?.let { p["completedAt"] = it.toString() }
        p["revision"] = d.revision.toString()
        return StringWriter().also { p.store(it, null) }.toString()
    }

    /** Returns null for missing, corrupt or unknown-version data: caller starts fresh, never crashes. */
    fun decode(raw: String?): OnboardingDraft? = runCatching {
        if (raw.isNullOrBlank()) return null
        val p = Properties().apply { load(StringReader(raw)) }
        val version = p.getProperty("schemaVersion")?.toInt() ?: return null
        if (version != OnboardingDraft.SCHEMA_VERSION) return null
        OnboardingDraft(
            schemaVersion = version,
            checkpoint = InitiationCheckpoint.valueOf(p.getProperty("checkpoint")),
            displayName = p.getProperty("displayName", ""),
            identityMode = IdentityMode.valueOf(p.getProperty("identityMode", "NONE")),
            accountId = p.getProperty("accountId"),
            completedAt = p.getProperty("completedAt")?.toLong(),
            revision = p.getProperty("revision", "0").toInt(),
        )
    }.getOrNull()
}

sealed interface DraftResult {
    data class Ok(val draft: OnboardingDraft) : DraftResult
    data class Blocked(val draft: OnboardingDraft, val reason: String) : DraftResult
}

/** Owns the draft. Every change is persisted before it is returned, so any interruption resumes at the last valid checkpoint. */
class OnboardingDraftRepository(private val store: DraftStore) {
    var draft: OnboardingDraft = OnboardingDraftCodec.decode(store.read()) ?: OnboardingDraft()
        private set

    private fun commit(next: OnboardingDraft): DraftResult.Ok {
        val saved = next.copy(revision = draft.revision + 1)
        store.write(OnboardingDraftCodec.encode(saved))
        draft = saved
        return DraftResult.Ok(saved)
    }

    fun setDisplayName(name: String): DraftResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return DraftResult.Blocked(draft, "Enter your name to continue.")
        if (trimmed.length > 60) return DraftResult.Blocked(draft, "Use 60 characters or fewer.")
        return commit(draft.copy(displayName = trimmed, checkpoint = maxOf(draft.checkpoint, InitiationCheckpoint.AUTHENTICATION)))
    }

    fun signedIn(mode: IdentityMode, accountId: String?): DraftResult {
        if (mode == IdentityMode.NONE) return DraftResult.Blocked(draft, "Choose a way in.")
        if (mode != IdentityMode.GUEST && draft.displayName.isEmpty()) return DraftResult.Blocked(draft, "Enter your name first.")
        return commit(draft.copy(identityMode = mode, accountId = accountId, checkpoint = maxOf(draft.checkpoint, InitiationCheckpoint.PROPOSITION)))
    }

    /** Moves forward one step from a checkpoint after identity. Identity steps go through setDisplayName / signedIn. */
    fun advanceFrom(current: InitiationCheckpoint): DraftResult {
        if (current < InitiationCheckpoint.PROPOSITION || draft.identityMode == IdentityMode.NONE)
            return DraftResult.Blocked(draft, "Finish identity first.")
        if (current == InitiationCheckpoint.COMPLETE) return DraftResult.Ok(draft)
        val next = InitiationCheckpoint.entries[current.ordinal + 1]
        return commit(draft.copy(checkpoint = maxOf(draft.checkpoint, next)))
    }

    /** Forward jump allowed by the wireframe (e.g. "Not now" skips to Foundation). Never moves backwards. */
    fun reach(target: InitiationCheckpoint): DraftResult {
        if (target >= InitiationCheckpoint.PROPOSITION && draft.identityMode == IdentityMode.NONE)
            return DraftResult.Blocked(draft, "Finish identity first.")
        if (target <= draft.checkpoint) return DraftResult.Ok(draft)
        return commit(draft.copy(checkpoint = target))
    }

    fun complete(nowMillis: Long): DraftResult =
        if (draft.checkpoint < InitiationCheckpoint.FOUNDATION) DraftResult.Blocked(draft, "Foundation not reached yet.")
        else commit(draft.copy(checkpoint = InitiationCheckpoint.COMPLETE, completedAt = nowMillis))

    /** The prototype's Reset: clears onboarding only. */
    fun reset() { store.clear(); draft = OnboardingDraft() }
}

// ---- Account adapters ----

data class AuthSession(val accountId: String, val token: String)

sealed interface AuthOutcome {
    data class Success(val session: AuthSession) : AuthOutcome
    data object Cancelled : AuthOutcome
    data object Offline : AuthOutcome
    data object CodeExpired : AuthOutcome
    data object CodeInvalid : AuthOutcome
    data class Error(val message: String) : AuthOutcome
}

data class EmailChallenge(val challengeId: String, val expiresAtMillis: Long, val resendAfterMillis: Long)

interface GoogleAuthAdapter { suspend fun signIn(): AuthOutcome }

interface EmailCodeAuthAdapter {
    suspend fun requestCode(email: String): Result<EmailChallenge>
    suspend fun verify(challengeId: String, code: String): AuthOutcome
}

/** Placeholders until the founder supplies a Google OAuth client and an email provider + backend routes. */
object UnconfiguredGoogleAuth : GoogleAuthAdapter {
    override suspend fun signIn(): AuthOutcome = AuthOutcome.Error("Google sign-in is not available yet. Continue on this device instead.")
}
object UnconfiguredEmailCodeAuth : EmailCodeAuthAdapter {
    override suspend fun requestCode(email: String): Result<EmailChallenge> = Result.failure(IllegalStateException("Email sign-in is not available yet."))
    override suspend fun verify(challengeId: String, code: String): AuthOutcome = AuthOutcome.Error("Email sign-in is not available yet.")
}

/** Applies auth outcomes to the draft. Any failure keeps the draft and stays on Authentication; guest is always available. */
class InitiationAuthCoordinator(
    private val repo: OnboardingDraftRepository,
    private val google: GoogleAuthAdapter = UnconfiguredGoogleAuth,
    private val email: EmailCodeAuthAdapter = UnconfiguredEmailCodeAuth,
) {
    fun continueAsGuest(): DraftResult = repo.signedIn(IdentityMode.GUEST, null)

    suspend fun signInWithGoogle(): DraftResult = apply(IdentityMode.GOOGLE, google.signIn())

    suspend fun verifyEmailCode(challengeId: String, code: String): DraftResult {
        if (!code.matches(Regex("\\d{6}"))) return DraftResult.Blocked(repo.draft, "Enter the 6-digit code.")
        return apply(IdentityMode.EMAIL, email.verify(challengeId, code))
    }

    private fun apply(mode: IdentityMode, outcome: AuthOutcome): DraftResult = when (outcome) {
        is AuthOutcome.Success -> repo.signedIn(mode, outcome.session.accountId)
        AuthOutcome.Cancelled -> DraftResult.Blocked(repo.draft, "Sign-in cancelled. Your progress is kept.")
        AuthOutcome.Offline -> DraftResult.Blocked(repo.draft, "You're offline. Your progress is kept. You can continue on this device.")
        AuthOutcome.CodeExpired -> DraftResult.Blocked(repo.draft, "That code expired. Request a new one.")
        AuthOutcome.CodeInvalid -> DraftResult.Blocked(repo.draft, "That code didn't match. Try again.")
        is AuthOutcome.Error -> DraftResult.Blocked(repo.draft, outcome.message)
    }
}

// ---- Guest migration ----

/** A locally created record with a client-generated id (HANDOFF: client UUIDs, idempotent creation). */
data class LocalRecord(val clientId: String, val kind: String, val payload: String)

interface MigrationSink {
    suspend fun existingClientIds(accountId: String): Set<String>
    suspend fun upsert(accountId: String, record: LocalRecord)
}

data class MigrationReport(val uploaded: Int, val skippedExisting: Int, val failed: List<String>)

/** Moves guest data to a signed-in account without duplicates; safe to re-run after an interruption. */
class GuestMigrator(private val sink: MigrationSink) {
    suspend fun migrate(accountId: String, records: List<LocalRecord>): MigrationReport {
        val existing = sink.existingClientIds(accountId)
        var uploaded = 0; var skipped = 0; val failed = mutableListOf<String>()
        for (r in records.distinctBy { it.clientId }) {
            if (r.clientId in existing) { skipped++; continue }
            runCatching { sink.upsert(accountId, r) }.onSuccess { uploaded++ }.onFailure { failed += r.clientId }
        }
        return MigrationReport(uploaded, skipped, failed)
    }
}
