package com.shanivar.nativeapp

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

/*
 * Initiation V1, build step 2 (HANDOFF.md:100): connect Routine mutations to the existing data model
 * (RoutinePlanV1 stays the local source); add revisions, idempotency, timezone and DST policy.
 * The local save always happens first and takes effect immediately; sync is best-effort and never
 * gates a local change (HANDOFF "local disabling must take effect immediately even if sync fails").
 * The server routes (PUT /v1/routine, POST/PUT /v1/protection-windows) do not exist yet: [RoutineRemote]
 * is the seam; [UnconfiguredRoutineRemote] answers Offline until a backend is deployed.
 */

/** HANDOFF Routine entity: IANA zone, anchors, practice time, activities, revision. */
data class SyncedRoutine(
    val timeZone: String,
    val plan: RoutinePlanV1,
    val practiceLocal: LocalTime?,
)

object RoutineSeed {
    /** HANDOFF Anchors row: "seed practice at wake + 5 min". Editable later. */
    fun practiceFor(wake: LocalTime?): LocalTime? = wake?.plusMinutes(5)
}

data class RoutineMutation(
    val idempotencyKey: String,
    val expectedRevision: Int,
    val routine: SyncedRoutine,
)

sealed interface RemoteSaveResult {
    data class Saved(val revision: Int) : RemoteSaveResult
    data class Conflict(val canonical: SyncedRoutine, val revision: Int) : RemoteSaveResult
    data class Invalid(val message: String) : RemoteSaveResult
    data class RateLimited(val retryAfterSeconds: Long) : RemoteSaveResult
    data object AuthExpired : RemoteSaveResult
    data object Offline : RemoteSaveResult
}

interface RoutineRemote {
    suspend fun putRoutine(mutation: RoutineMutation): RemoteSaveResult
}

object UnconfiguredRoutineRemote : RoutineRemote {
    override suspend fun putRoutine(mutation: RoutineMutation) = RemoteSaveResult.Offline
}

enum class SyncStatus { SYNCED, PENDING, OFFLINE, CONFLICT, NEEDS_SIGN_IN, REJECTED, LOCAL_ONLY }

data class RoutineSyncState(
    val serverRevision: Int = 0,
    val pending: RoutineMutation? = null,
    val status: SyncStatus = SyncStatus.LOCAL_ONLY,
    val conflict: RemoteSaveResult.Conflict? = null,
    val message: String? = null,
    val retryAfterSeconds: Long? = null,
)

/**
 * Full-replace PUT semantics: only the newest local routine needs sending, so a new edit replaces
 * the queued one but keeps its idempotency key only if the payload is identical (safe retry).
 */
class RoutineSyncCoordinator(
    private val local: suspend (RoutinePlanV1) -> Result<Unit>,
    private val remote: RoutineRemote,
    initial: RoutineSyncState = RoutineSyncState(),
    private val newKey: () -> String = { UUID.randomUUID().toString() },
) {
    var state: RoutineSyncState = initial
        private set

    /** Local first. Returns the local result; sync state only changes if the local save succeeded. */
    suspend fun save(routine: SyncedRoutine, signedIn: Boolean): Result<Unit> {
        val saved = local(routine.plan)
        if (saved.isFailure) return saved
        if (!signedIn) { state = state.copy(status = SyncStatus.LOCAL_ONLY, pending = null); return saved }
        val prev = state.pending
        val key = if (prev != null && prev.routine == routine) prev.idempotencyKey else newKey()
        state = state.copy(pending = RoutineMutation(key, state.serverRevision, routine), status = SyncStatus.PENDING, message = null)
        return saved
    }

    /** Sends the queued mutation once. Call on reconnect / app start / after save. */
    suspend fun flush(): RoutineSyncState {
        val m = state.pending ?: return state
        state = when (val r = remote.putRoutine(m)) {
            is RemoteSaveResult.Saved -> RoutineSyncState(serverRevision = r.revision, status = SyncStatus.SYNCED)
            is RemoteSaveResult.Conflict -> state.copy(status = SyncStatus.CONFLICT, conflict = r,
                message = "This routine changed on another device. Choose which to keep.")
            is RemoteSaveResult.Invalid -> state.copy(status = SyncStatus.REJECTED, message = r.message)
            is RemoteSaveResult.RateLimited -> state.copy(status = SyncStatus.PENDING, retryAfterSeconds = r.retryAfterSeconds)
            RemoteSaveResult.AuthExpired -> state.copy(status = SyncStatus.NEEDS_SIGN_IN, message = "Sign in again to sync. Your routine is saved on this phone.")
            RemoteSaveResult.Offline -> state.copy(status = SyncStatus.OFFLINE, message = "Saved on this phone. It will sync when you're online.")
        }
        return state
    }

    /** Conflict resolution is the person's choice; nothing is overwritten silently. */
    suspend fun keepMine(): RoutineSyncState {
        val c = state.conflict ?: return state
        val m = state.pending ?: return state
        state = state.copy(serverRevision = c.revision, conflict = null,
            pending = m.copy(idempotencyKey = newKey(), expectedRevision = c.revision), status = SyncStatus.PENDING)
        return flush()
    }

    suspend fun takeServer(): Result<Unit> {
        val c = state.conflict ?: return Result.success(Unit)
        val r = local(c.canonical.plan)
        if (r.isSuccess) state = RoutineSyncState(serverRevision = c.revision, status = SyncStatus.SYNCED)
        return r
    }
}

/**
 * HANDOFF.md step 4 DST policy: a nonexistent local time resolves to the next valid local instant
 * (the start of the gap's end); a repeated local time resolves to its first occurrence.
 * NOTE: java.time's default (used by today's FocusWindowRecurrence/AlarmRecurrence) shifts a gap time
 * forward by the gap length (02:30 -> 03:30). HANDOFF says verify against accepted behaviour before
 * shipping, so this is NOT yet wired into the schedulers. See tests.
 */
object HandoffTimePolicy {
    fun resolve(date: LocalDate, time: LocalTime, zone: ZoneId): ZonedDateTime {
        val ldt = date.atTime(time)
        val transition = zone.rules.getTransition(ldt)
        if (transition != null && transition.isGap) return transition.dateTimeAfter.atZone(zone)
        return ZonedDateTime.ofLocal(ldt, zone, null).withEarlierOffsetAtOverlap()
    }

    fun isValidZone(id: String): Boolean = runCatching { ZoneId.of(id) }.isSuccess && id.contains('/')
}
