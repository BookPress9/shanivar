package com.shanivar.nativeapp.analytics

/*
 * DEV-65 event dictionary, schema v1 (source of truth; docs/analytics/event-dictionary.v1.json is generated
 * from it and docs/analytics/EVENT_DICTIONARY.md describes it; tests keep the three in step).
 *
 * Privacy (DEV-65 requirements, SOP-09): properties are closed enums, bounded integers, booleans or
 * app-generated ids. There is no free-text type, so free text, protected-app package names, contact data,
 * commitment text and self-report answers cannot be expressed. Self-reports appear only as "a report of
 * kind X was submitted", never with their values, and never as scores.
 */

/** PII class of a property / event (an event takes the highest class of its fields). */
enum class PiiClass(val wire: String, val meaning: String) {
    NONE("none", "enum, bounded number or boolean; identifies nobody"),
    PSEUDONYMOUS("pseudonymous", "random app-generated id (auth uid, event/occasion/window/step id) or catalog id; links events of one install, names nobody"),
    QUASI("quasi", "time, time zone or app version: needed for the metrics, could narrow down a person when combined, so never joined with contact data"),
}

sealed interface PropType {
    val wire: String
    data class Enum(val values: List<String>) : PropType { override val wire get() = "enum" }
    data class IntRange(val min: Int, val max: Int) : PropType { override val wire get() = "int" }
    data object Bool : PropType { override val wire get() = "bool" }
    /** App-generated or catalog id: [A-Za-z0-9_.:-]{1,128}. Never user-typed text. */
    data object Id : PropType { override val wire get() = "id" }
    data object LocalDate : PropType { override val wire get() = "local_date" }
    data object LocalTime : PropType { override val wire get() = "local_time" }
}

data class PropSpec(val name: String, val type: PropType, val required: Boolean, val pii: PiiClass, val doc: String)

enum class Owner(val wire: String) { FRONTEND("Shanivar Frontend"), BACKEND("Shanivar Backend") }

data class EventSpec(
    val name: String,
    val owner: Owner,
    val source: String,
    val trigger: String,
    val props: List<PropSpec>,
    val feeds: List<String>,
) {
    /** Envelope fields are pseudonymous (ids) and quasi (time, tz, app version) for every event. */
    val pii: PiiClass get() = (props.map { it.pii } + PiiClass.QUASI).maxBy { it.ordinal }
    fun prop(name: String) = props.firstOrNull { it.name == name }
}

object EventDictionary {
    const val SCHEMA_VERSION = 1
    /** Oldest version the parser still reads (v0 = the Measurement-plan draft names, upgraded on read). */
    const val OLDEST_READABLE_VERSION = 0

    val SOURCES = listOf("initiation", "routine", "protection", "sadhana", "system")
    val APP_CATEGORIES = listOf("social", "video", "games", "messaging", "browser", "shopping", "news", "other")
    val OUTCOMES = listOf("completed", "ended", "interrupted")
    val CHOICES = listOf("return", "pause", "practice", "stop_protection", "essential")
    /** Intervention choices that count as an intentional alternative (WIAC, effectiveness). */
    val ALTERNATIVE_CHOICES = setOf("return", "pause", "practice")
    val GUIDANCE = listOf("full", "reduced", "minimal")
    val SEGMENTS = listOf("distraction", "routine", "meditation", "inquiry")

    private fun e(vararg v: String) = PropType.Enum(v.toList())
    private fun p(name: String, type: PropType, doc: String, required: Boolean = true, pii: PiiClass = PiiClass.NONE) = PropSpec(name, type, required, pii, doc)
    private fun id(name: String, doc: String, required: Boolean = true) = p(name, PropType.Id, doc, required, PiiClass.PSEUDONYMOUS)
    private val windowId = id("window_id", "protection window id (app-generated)")
    private val stepId = id("step_id", "planned routine activity id (app-generated)")
    private val schedDate = p("scheduled_local_date", PropType.LocalDate, "local date the step was planned for", pii = PiiClass.QUASI)
    private val schedTime = p("scheduled_local_time", PropType.LocalTime, "planned local time (HH:MM)", pii = PiiClass.QUASI)
    private val category = p("app_category", PropType.Enum(APP_CATEGORIES), "category of the protected app (Android ApplicationInfo.category mapped); never the package name")
    private val launchedFrom = p("launched_from", e("home", "intervention", "routine", "initiation"), "where the practice was chosen")
    private val practiceId = id("practice_id", "catalog practice id (e.g. natural, stage1)")

    val events: List<EventSpec> = listOf(
        EventSpec("app_first_opened", Owner.FRONTEND, "system", "first launch after install (once per install)", emptyList(),
            listOf("activation", "time_to_first_value", "retention_w1_w4")),
        EventSpec("onboarding_step_completed", Owner.FRONTEND, "initiation", "an Initiation step is completed",
            listOf(p("step", e("welcome", "goal", "routine", "protection", "first_practice", "done"), "Initiation step")),
            listOf("activation")),
        EventSpec("goal_selected", Owner.FRONTEND, "initiation", "the user picks a primary goal (segment)",
            listOf(p("segment", PropType.Enum(SEGMENTS), "thesis §3 segment A-D")), listOf("activation")),
        EventSpec("permission_state_changed", Owner.FRONTEND, "system", "a needed permission is granted or revoked (checked at start and resume)",
            listOf(p("permission", e("usage", "overlay", "exact_alarm", "notifications", "activity"), "permission"), p("granted", PropType.Bool, "new state")),
            listOf("technical_reliability")),
        EventSpec("routine_created", Owner.FRONTEND, "routine", "the user saves a routine for the first time",
            listOf(p("step_count", PropType.IntRange(0, 50), "planned activities", required = false), p("window_count", PropType.IntRange(0, 8), "protection windows"),
                p("overnight", PropType.Bool, "any window crosses midnight")),
            listOf("activation")),
        EventSpec("protection_window_saved", Owner.FRONTEND, "routine", "a protection window is created or edited",
            listOf(windowId, p("overnight", PropType.Bool, "crosses midnight")), listOf("activation")),
        EventSpec("routine_step_completed", Owner.FRONTEND, "routine", "the user marks a planned activity done",
            listOf(stepId, schedDate, schedTime), listOf("wiac", "routine_completion_rate", "activation", "time_to_first_value", "retention_w1_w4", "recovery_after_missed_days")),
        EventSpec("routine_step_uncompleted", Owner.FRONTEND, "routine", "the user undoes a done mark (retracts the completion)",
            listOf(stepId, schedDate), listOf("wiac", "routine_completion_rate")),
        EventSpec("routine_step_missed", Owner.FRONTEND, "routine", "a planned activity's slot ends without a done mark (device-side, once per step and date)",
            listOf(stepId, schedDate, schedTime), listOf("routine_completion_rate")),
        EventSpec("routine_recovered", Owner.FRONTEND, "routine", "the first step completed after one or more missed steps",
            listOf(p("missed_steps", PropType.IntRange(1, 1000), "missed steps since the last completion"), p("gap_days", PropType.IntRange(0, 365), "local days since the last completion")),
            listOf("recovery_after_missed_days")),
        EventSpec("protection_window_started", Owner.FRONTEND, "protection", "a protection window becomes active",
            listOf(windowId, p("armed_by_engine", PropType.Bool, "the enforcement engine is actually running")), listOf("technical_reliability")),
        EventSpec("protection_window_ended", Owner.FRONTEND, "protection", "a protection window ends",
            listOf(windowId, p("end_reason", e("scheduled", "user_stop", "failure"), "why it ended"),
                p("scheduled_minutes", PropType.IntRange(0, 1440), "planned length"), p("active_minutes", PropType.IntRange(0, 1440), "minutes enforcement was actually active")),
            listOf("protection_window_effectiveness", "technical_reliability", "less_dependence_signal")),
        EventSpec("protection_failure_detected", Owner.FRONTEND, "protection", "enforcement stops while a window is scheduled",
            listOf(p("cause", e("service_killed", "permission_revoked", "doze", "other"), "cause")), listOf("technical_reliability")),
        EventSpec("intervention_shown", Owner.FRONTEND, "protection", "the block overlay is shown (mints occasion_id)",
            listOf(windowId, category), listOf("protection_window_effectiveness", "less_dependence_signal")),
        EventSpec("intervention_choice", Owner.FRONTEND, "protection", "the user picks an option on the block overlay (same occasion_id)",
            listOf(windowId, category, p("choice", PropType.Enum(CHOICES), "option picked")),
            listOf("wiac", "protection_window_effectiveness", "activation", "time_to_first_value", "retention_w1_w4", "recovery_after_missed_days")),
        EventSpec("practice_started", Owner.FRONTEND, "sadhana", "a practice the user chose starts (mints occasion_id unless one is inherited)",
            listOf(practiceId, launchedFrom), listOf("meditation_return_rate", "activation")),
        EventSpec("practice_ended", Owner.FRONTEND, "sadhana", "a practice attempt ends (same occasion_id as its start)",
            listOf(practiceId, launchedFrom, p("outcome", PropType.Enum(OUTCOMES), "attempt outcome"), p("active_seconds", PropType.IntRange(0, 86_400), "active seconds")),
            listOf("wiac", "activation", "time_to_first_value", "retention_w1_w4", "recovery_after_missed_days")),
        EventSpec("self_report_submitted", Owner.FRONTEND, "sadhana", "a self-report is saved (kind only; the answers stay in the user-owned record)",
            listOf(p("kind", e("stage1_check", "independent", "reflection"), "which self-report")), listOf("wiac_sr", "activation", "self_reported_usefulness")),
        EventSpec("alarm_fired", Owner.FRONTEND, "routine", "the wake alarm rings",
            listOf(p("delay_seconds", PropType.IntRange(-3600, 86_400), "actual minus scheduled ring time")), listOf("technical_reliability")),
        EventSpec("alarm_dismissed", Owner.FRONTEND, "routine", "the wake alarm is dismissed (never a WIAC action by itself)",
            listOf(p("method", e("movement", "qr", "photo", "accessible"), "dismissal path")), listOf("activation", "technical_reliability")),
        EventSpec("app_returned_after_gap", Owner.FRONTEND, "system", "the app is opened after 2+ local days without opening",
            listOf(p("gap_days", PropType.IntRange(2, 3650), "local days since the last open")), listOf("recovery_after_missed_days")),
        EventSpec("guidance_level_changed", Owner.FRONTEND, "sadhana", "the user changes the guidance level",
            listOf(p("from", PropType.Enum(GUIDANCE), "before"), p("to", PropType.Enum(GUIDANCE), "after")), listOf("less_dependence_signal")),
        EventSpec("sync_flush_completed", Owner.BACKEND, "system", "a DEV-45 outbox flush finishes (one per run)",
            listOf(p("sent", PropType.IntRange(0, 10_000), "acked items"), p("retried", PropType.IntRange(0, 10_000), "items left for a retry"),
                p("dead_lettered", PropType.IntRange(0, 10_000), "items moved to the dead-letter list")), listOf("technical_reliability")),
        EventSpec("sync_dead_lettered", Owner.BACKEND, "system", "an outbox item is dead-lettered (no payload)",
            listOf(p("kind", e("attempt", "self_report", "routine", "window", "orbit", "reflection", "other"), "outbox kind"),
                p("error_class", e("http_4xx", "retry_limit", "delete_unsupported", "bad_payload", "unknown_kind", "other"), "error class")),
            listOf("technical_reliability")),
        EventSpec("feedback_submitted", Owner.FRONTEND, "system", "the user sends in-app feedback (category only; the text goes to the CS channel, not analytics)",
            listOf(p("category", e("bug", "idea", "content", "protection", "alarm", "other"), "DEV-86 taxonomy (draft)")), listOf("cs_insights")),
    )
    private val byName = events.associateBy { it.name }
    fun spec(name: String): EventSpec? = byName[name]

    /** v0 (Measurement-plan draft, 17 Oct) -> v1 renames, applied on read so old events still parse. */
    val V0_RENAMES = mapOf(
        "routine_activity_completed" to "routine_step_completed",
        "routine_activity_uncompleted" to "routine_step_uncompleted",
        "routine_saved" to "routine_created",
    )
}
