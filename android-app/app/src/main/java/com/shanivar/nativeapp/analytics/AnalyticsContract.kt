package com.shanivar.nativeapp.analytics

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

/*
 * DEV-65 analytics contract: the event envelope, the parser (schema versions 0..1) and the validator.
 * Wire shape (one JSON object per event):
 *   {"schema_version":1,"event_id":"<uuid>","occasion_id":"<uuid>"|null,"user_id":"<auth uid>",
 *    "name":"<event>","ts_utc":"2026-10-12T06:00:00Z","local_date":"2026-10-12","tz":"Europe/Berlin",
 *    "app_version":"0.9.0","content_version":"v1"|null,"source":"sadhana","props":{...}}
 * Pure JVM (no android.*, no org.json) so the contract is unit-tested and usable by any emitter.
 */

data class AnalyticsEvent(
    val schemaVersion: Int,
    val eventId: String,
    val occasionId: String?,
    val userId: String,
    val name: String,
    val tsUtc: Instant,
    val localDate: LocalDate,
    val tz: ZoneId,
    val appVersion: String,
    val contentVersion: String?,
    val source: String,
    val props: Map<String, Any?>,
) {
    fun str(k: String) = props[k] as? String
    fun int(k: String) = (props[k] as? Number)?.toInt()
    fun bool(k: String) = props[k] as? Boolean
}

sealed interface Validation {
    data class Accepted(val event: AnalyticsEvent, val upgradedFrom: Int? = null) : Validation
    /** Rejected events are never stored or counted; [reasons] are codes only (never field values). */
    data class Rejected(val reasons: List<String>) : Validation
}

object EventContract {
    private val UUID = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    private val ID = Regex("^[A-Za-z0-9_.:-]{1,128}$")
    private val VERSION = Regex("^[0-9A-Za-z.+-]{1,32}$")
    private val CONTENT = Regex("^[A-Za-z0-9_.:-]{1,64}$")
    private val ENVELOPE = setOf("schema_version", "event_id", "occasion_id", "user_id", "name", "ts_utc", "local_date", "tz",
        "app_version", "content_version", "source", "props")

    /** Parses one JSON line and validates it. [consented] = the user's analytics consent at the time of the event. */
    fun parse(json: String, consented: Boolean = true): Validation {
        val obj = runCatching { TinyJson.parse(json) }.getOrNull() as? Map<*, *> ?: return Validation.Rejected(listOf("not_json_object"))
        @Suppress("UNCHECKED_CAST")
        return validate(obj as Map<String, Any?>, consented)
    }

    fun validate(raw: Map<String, Any?>, consented: Boolean = true): Validation {
        if (!consented) return Validation.Rejected(listOf("no_consent"))
        val version = (raw["schema_version"] as? Number)?.toInt() ?: return Validation.Rejected(listOf("missing_schema_version"))
        if (version > EventDictionary.SCHEMA_VERSION || version < EventDictionary.OLDEST_READABLE_VERSION)
            return Validation.Rejected(listOf("unsupported_schema_version"))
        val m = if (version == 0) upgradeV0(raw) else raw
        val bad = mutableListOf<String>()
        (m.keys - ENVELOPE).forEach { bad += "unknown_field" }
        val name = m["name"] as? String
        val spec = name?.let(EventDictionary::spec)
        if (spec == null) bad += "unknown_event"
        val eventId = (m["event_id"] as? String)?.takeIf { UUID.matches(it) }.also { if (it == null) bad += "invalid_event_id" }
        val occasion = m["occasion_id"]
        if (occasion != null && !(occasion is String && UUID.matches(occasion))) bad += "invalid_occasion_id"
        val userId = (m["user_id"] as? String)?.takeIf { UUID.matches(it) }.also { if (it == null) bad += "invalid_user_id" }
        val ts = (m["ts_utc"] as? String)?.takeIf { it.endsWith("Z") }?.let { runCatching { Instant.parse(it) }.getOrNull() }
            .also { if (it == null) bad += "invalid_ts_utc" }
        val tzRaw = m["tz"] as? String
        val tz = when {
            tzRaw.isNullOrBlank() -> { bad += "missing_tz"; null }
            else -> runCatching { ZoneId.of(tzRaw) }.getOrNull().also { if (it == null) bad += "invalid_tz" }
        }
        val localDate = (m["local_date"] as? String)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            .also { if (it == null) bad += "invalid_local_date" }
        if (ts != null && tz != null && localDate != null && ts.atZone(tz).toLocalDate() != localDate) bad += "local_date_mismatch"
        val appVersion = (m["app_version"] as? String)?.takeIf { VERSION.matches(it) }.also { if (it == null) bad += "invalid_app_version" }
        val cv = m["content_version"]
        if (cv != null && !(cv is String && CONTENT.matches(cv))) bad += "invalid_content_version"
        val source = m["source"] as? String
        if (spec != null && source != spec.source) bad += "invalid_source"
        @Suppress("UNCHECKED_CAST")
        val props = when (val p = m["props"]) { null -> emptyMap(); is Map<*, *> -> p as Map<String, Any?>; else -> { bad += "invalid_props"; emptyMap() } }
        if (spec != null) bad += checkProps(spec, props)
        if (bad.isNotEmpty()) return Validation.Rejected(bad.distinct())
        return Validation.Accepted(AnalyticsEvent(version.coerceAtLeast(EventDictionary.SCHEMA_VERSION), eventId!!, occasion as String?, userId!!, name!!,
            ts!!, localDate!!, tz!!, appVersion!!, cv as String?, source!!, props.mapValues { (_, v) -> if (v is Number) v.toLong().toInt() else v }),
            upgradedFrom = if (version < EventDictionary.SCHEMA_VERSION) version else null)
    }

    /** Data minimisation: only dictionary properties, each within its type. Unknown keys are rejected, not dropped. */
    private fun checkProps(spec: EventSpec, props: Map<String, Any?>): List<String> {
        val bad = mutableListOf<String>()
        props.keys.filter { spec.prop(it) == null }.forEach { bad += "unknown_prop:$it" }
        for (ps in spec.props) {
            val v = props[ps.name]
            if (v == null) { if (ps.required) bad += "missing_prop:${ps.name}"; continue }
            val ok = when (val t = ps.type) {
                is PropType.Enum -> v is String && v in t.values
                is PropType.IntRange -> v is Number && v.toDouble() == Math.floor(v.toDouble()) && v.toLong() in t.min..t.max
                PropType.Bool -> v is Boolean
                PropType.Id -> v is String && ID.matches(v)
                PropType.LocalDate -> v is String && runCatching { LocalDate.parse(v) }.isSuccess
                PropType.LocalTime -> v is String && v.length == 5 && runCatching { LocalTime.parse(v) }.isSuccess
            }
            if (!ok) bad += "invalid_prop:${ps.name}"
        }
        return bad
    }

    /** v0 = Measurement-plan draft: routine_activity_* / routine_saved names and activity_id. */
    private fun upgradeV0(raw: Map<String, Any?>): Map<String, Any?> {
        val m = raw.toMutableMap()
        val name = m["name"] as? String
        EventDictionary.V0_RENAMES[name]?.let { m["name"] = it }
        @Suppress("UNCHECKED_CAST")
        val props = (m["props"] as? Map<String, Any?>)?.toMutableMap() ?: mutableMapOf()
        if (name == "routine_activity_completed" || name == "routine_activity_uncompleted") {
            props.remove("activity_id")?.let { props["step_id"] = it }
            if ("scheduled_local_date" !in props) props["scheduled_local_date"] = m["local_date"]
        }
        m["props"] = props
        m["schema_version"] = EventDictionary.SCHEMA_VERSION
        return m
    }

    /** Serialises an event to the v1 wire shape (keys in envelope order, props in dictionary order). */
    fun toJson(e: AnalyticsEvent): String {
        val spec = EventDictionary.spec(e.name)
        val keys = spec?.props?.map { it.name }?.filter { it in e.props } ?: e.props.keys.toList()
        val props = keys.joinToString(",") { "${TinyJson.q(it)}:${TinyJson.value(e.props[it])}" }
        return "{\"schema_version\":${e.schemaVersion},\"event_id\":${TinyJson.q(e.eventId)},\"occasion_id\":${TinyJson.value(e.occasionId)}," +
            "\"user_id\":${TinyJson.q(e.userId)},\"name\":${TinyJson.q(e.name)},\"ts_utc\":${TinyJson.q(e.tsUtc.toString())}," +
            "\"local_date\":${TinyJson.q(e.localDate.toString())},\"tz\":${TinyJson.q(e.tz.id)},\"app_version\":${TinyJson.q(e.appVersion)}," +
            "\"content_version\":${TinyJson.value(e.contentVersion)},\"source\":${TinyJson.q(e.source)},\"props\":{$props}}"
    }
}

/** Machine-readable dictionary (docs/analytics/event-dictionary.v1.json is exactly this output). */
object DictionaryJson {
    fun render(): String {
        val sb = StringBuilder("{\n  \"schema_version\": ${EventDictionary.SCHEMA_VERSION},\n  \"oldest_readable_version\": ${EventDictionary.OLDEST_READABLE_VERSION},\n")
        sb.append("  \"pii_classes\": {").append(PiiClass.entries.joinToString(", ") { "${TinyJson.q(it.wire)}: ${TinyJson.q(it.meaning)}" }).append("},\n")
        sb.append("  \"events\": [\n")
        sb.append(EventDictionary.events.joinToString(",\n") { e ->
            val props = e.props.joinToString(", ") { p ->
                val t = when (val t = p.type) {
                    is PropType.Enum -> ", \"values\": [${t.values.joinToString(", ") { TinyJson.q(it) }}]"
                    is PropType.IntRange -> ", \"min\": ${t.min}, \"max\": ${t.max}"
                    else -> ""
                }
                "{\"name\": ${TinyJson.q(p.name)}, \"type\": ${TinyJson.q(p.type.wire)}$t, \"required\": ${p.required}, \"pii\": ${TinyJson.q(p.pii.wire)}}"
            }
            "    {\"name\": ${TinyJson.q(e.name)}, \"owner\": ${TinyJson.q(e.owner.wire)}, \"source\": ${TinyJson.q(e.source)}, \"pii\": ${TinyJson.q(e.pii.wire)},\n" +
                "     \"trigger\": ${TinyJson.q(e.trigger)},\n     \"props\": [$props],\n     \"feeds\": [${e.feeds.joinToString(", ") { TinyJson.q(it) }}]}"
        })
        sb.append("\n  ],\n  \"metrics\": [\n")
        sb.append(Metrics.DEFINITIONS.joinToString(",\n") { d ->
            "    {\"id\": ${TinyJson.q(d.id)}, \"brief\": ${TinyJson.q(d.briefName)}, \"events\": [${d.events.joinToString(", ") { TinyJson.q(it) }}], \"other_sources\": [${d.otherSources.joinToString(", ") { TinyJson.q(it) }}]}"
        })
        sb.append("\n  ]\n}\n")
        return sb.toString()
    }
}

/** Minimal JSON reader/writer (org.json is a stub in plain unit tests). Objects -> Map, arrays -> List, numbers -> Long/Double. */
internal object TinyJson {
    fun parse(s: String): Any? { val r = Reader(s); val v = r.value(); r.ws(); require(r.i == s.length) { "trailing" }; return v }
    fun q(s: String): String {
        val b = StringBuilder("\"")
        for (c in s) when {
            c == '"' -> b.append("\\\""); c == '\\' -> b.append("\\\\"); c == '\n' -> b.append("\\n")
            c < ' ' -> b.append(String.format("\\u%04x", c.code)); else -> b.append(c)
        }
        return b.append('"').toString()
    }
    fun value(v: Any?): String = when (v) { null -> "null"; is String -> q(v); is Boolean, is Number -> v.toString(); else -> q(v.toString()) }

    private class Reader(val s: String) {
        var i = 0
        fun ws() { while (i < s.length && s[i].isWhitespace()) i++ }
        fun value(): Any? {
            ws(); require(i < s.length)
            return when (s[i]) {
                '{' -> obj(); '[' -> arr(); '"' -> str()
                't' -> lit("true", true); 'f' -> lit("false", false); 'n' -> lit("null", null)
                else -> num()
            }
        }
        fun lit(w: String, v: Any?): Any? { require(s.startsWith(w, i)); i += w.length; return v }
        fun obj(): Map<String, Any?> {
            val m = LinkedHashMap<String, Any?>(); i++; ws()
            if (s[i] == '}') { i++; return m }
            while (true) {
                ws(); val k = str(); ws(); require(s[i] == ':'); i++
                require(k !in m) { "duplicate key" }; m[k] = value(); ws()
                when (s[i]) { ',' -> i++; '}' -> { i++; return m }; else -> error("bad object") }
            }
        }
        fun arr(): List<Any?> {
            val l = ArrayList<Any?>(); i++; ws()
            if (s[i] == ']') { i++; return l }
            while (true) { l += value(); ws(); when (s[i]) { ',' -> i++; ']' -> { i++; return l }; else -> error("bad array") } }
        }
        fun str(): String {
            require(s[i] == '"'); i++
            val b = StringBuilder()
            while (true) {
                val c = s[i++]
                when (c) {
                    '"' -> return b.toString()
                    '\\' -> when (val e = s[i++]) {
                        'n' -> b.append('\n'); 't' -> b.append('\t'); 'r' -> b.append('\r'); 'b' -> b.append('\b'); 'f' -> b.append('\u000c')
                        'u' -> { b.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                        else -> b.append(e)
                    }
                    else -> b.append(c)
                }
            }
        }
        fun num(): Number {
            val st = i
            while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++
            val t = s.substring(st, i); require(t.isNotEmpty())
            return t.toLongOrNull() ?: t.toDouble()
        }
    }
}
