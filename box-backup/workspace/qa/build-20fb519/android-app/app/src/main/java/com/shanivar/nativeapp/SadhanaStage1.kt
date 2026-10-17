package com.shanivar.nativeapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.LocalDate

/*
 * Sadhana Stage 1, the base (founder spec 14 Oct 2026, decision 0018).
 * Attention is brought down to the base of the spine and kept there. Afterwards an optional,
 * reflective self-check: did attention reach the spine, how many times it drifted (the user's own
 * estimate, within this sitting), and what pulled it away. Self-report only; never a score, never evidence
 * of ability, never an energy or health reading.
 */
enum class Stage1Reach(val label: String) { YES("Yes"), PARTLY("Partly"), NO("No") }

enum class DriftReason(val label: String) {
    THOUGHTS("Thoughts"), SOUNDS("Sounds"), BODY("Body discomfort"), PHONE("Phone / notification urge"),
    SLEEPY("Sleepiness"), EMOTION("Emotion"), OTHER("Something else"),
}

data class Stage1Check(
    val id: String,
    val attemptId: String,
    val localDate: LocalDate,
    val reach: Stage1Reach? = null,
    /** Own estimate within this sitting; 10 means "10 or more". Null = not answered. */
    val drifts: Int? = null,
    val reasons: Set<DriftReason> = emptySet(),
    val note: String? = null,
) {
    val evidenceType: String get() = "self_report"
    val answered: Boolean get() = reach != null || drifts != null || reasons.isNotEmpty() || !note.isNullOrBlank()
}

object Stage1 {
    const val ID = "stage1-base"
    const val MAX_DRIFTS = 10
    const val PROMPT = "Bring your attention down to the base of the spine. Let it rest there."
    fun guidance(elapsed: Long, intended: Long): String = when {
        elapsed < 15 -> PROMPT
        elapsed < intended / 2 -> "If attention moved, notice where it went. Then bring it back down to the base."
        else -> "Stay with the base of the spine. End whenever you choose."
    }
    fun driftLabel(n: Int?) = when (n) { null -> "—"; MAX_DRIFTS -> "10+"; else -> n.toString() }

    private fun enc(s: String?) = URLEncoder.encode(s.orEmpty(), "UTF-8")
    private fun dec(s: String) = URLDecoder.decode(s, "UTF-8").ifBlank { null }
    fun encode(c: Stage1Check) = listOf(c.id, c.attemptId, c.localDate, c.reach?.name.orEmpty(), c.drifts?.toString().orEmpty(),
        c.reasons.joinToString(",") { it.name }, enc(c.note)).joinToString("|")
    fun decode(s: String): Stage1Check? = runCatching {
        val f = s.split("|")
        Stage1Check(f[0], f[1], LocalDate.parse(f[2]), f[3].ifBlank { null }?.let(Stage1Reach::valueOf), f[4].toIntOrNull(),
            f[5].split(",").filter { it.isNotBlank() }.map(DriftReason::valueOf).toSet(), dec(f.getOrElse(6) { "" }))
    }.getOrNull()

    private fun q(s: String?) = if (s == null) "null" else "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
    /** Row for Supabase self_reports (kind = stage1_check). */
    fun payload(c: Stage1Check) = """{"id":${q(c.id)},"kind":"stage1_check","attempt_id":${q(c.attemptId)},"local_date":${q(c.localDate.toString())},"practice_id":"$ID","reached_spine":${q(c.reach?.name?.lowercase())},"drift_count":${c.drifts ?: "null"},"drift_reasons":[${c.reasons.joinToString(",") { q(it.name.lowercase()) }}],"note":${q(c.note)}}"""
}

@Composable fun Stage1CheckCard(c: SadhanaController) {
    val chk = c.stage1Check ?: return
    SCard {
        SMeta("SELF-CHECK · YOUR REPORT")
        SCardTitle("Where did your attention go?")
        SCardBody("Answer only what you noticed. This is your own report, not a score.")
        Text("Could you bring your attention down to the spine?", color = Sad.Ivory, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stage1Reach.entries.forEach { r -> SChoice(r.label, chk.reach == r, { c.stage1Reach(r) }, Modifier.weight(1f)) }
        }
        Text("How many times was it carried away? Your estimate.", color = Sad.Ivory, fontSize = 13.sp, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            SSecondary("−", { c.stage1Drifts(-1) }, Modifier.width(64.dp))
            Text(Stage1.driftLabel(chk.drifts), color = Sad.Ivory, fontSize = 22.sp, modifier = Modifier.padding(horizontal = 18.dp))
            SSecondary("+", { c.stage1Drifts(1) }, Modifier.width(64.dp))
        }
        Text("What pulled it away?", color = Sad.Ivory, fontSize = 13.sp, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
        DriftReason.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                row.forEach { r -> SChoice(r.label, r in chk.reasons, { c.stage1Reason(r) }, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        OutlinedTextField(chk.note.orEmpty(), { c.stage1Note(it) }, label = { Text("Anything else? (optional)") },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        SFine("Stays on this phone unless you sign in to sync. Reflection only, not a measurement.")
        SSecondary(if (c.stage1Saved) "Saved" else "Save my report", { c.saveStage1Check() })
    }
}
