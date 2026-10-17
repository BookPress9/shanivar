package com.shanivar.nativeapp

import androidx.compose.runtime.Composable

/** Screen group 4: The Living Orbit and Your First Seven Days. Continuity, not a streak; nothing unlocks. */

@Composable fun OrbitScreen(c: InitiationController, reduceMotion: Boolean, run: IniRun) {
    val v = c.orbitView()
    val independent = c.orbit.all.any { it.type == OrbitEventType.INDEPENDENT_RECORD }
    IniHeader("THE LIVING ORBIT", "Presence,\nbuilt by you.")
    LivingOrbitView(v, still = false, reduceMotion = reduceMotion)
    IniCard(paper = true) { IniPaperTitle("Continuity, not a streak."); IniPaperBody("Your orbit records decisions. It stays whole when you miss a day.") }
    fun mark(b: Boolean) = if (b) "✓" else "○"
    IniSubtle(
        "${mark(v.ring)} Routine created\n" +
            "${mark(v.alarmPoint)} Alarm configured · ${c.alarmLabel()}\n" +
            "${mark(v.boundaryPoint)} Boundary saved · ${c.boundaryLabel()}\n" +
            "${mark(v.practicePoint)} First practice attempted\n" +
            "${mark(independent)} Independent practice · self-reported",
    )
    IniSecondary("I practised without the app") { run { c.independent() } }
    IniSpacer()
    IniPrimary("Back to my day →", { run { c.go(InitiationScreen.ROUTINE) } })
}

@Composable fun WeekScreen(c: InitiationController, run: IniRun) {
    val day = c.weekDay()
    IniHeader("YOUR FIRST SEVEN DAYS", "Make room.\nThen make it yours.")
    IniCard(paper = true) {
        IniEyebrow("DAY $day / OPTIONAL INVITATION", Ini.PaperEyebrow)
        IniPaperTitle(InitiationController.INVITES[day - 1])
        IniPaperBody("Nothing to unlock. Take what is useful.")
    }
    if (day == 3) IniSecondary("Add an activity") { run { c.go(InitiationScreen.EDIT) } }
    else IniSecondary("Try one minute") { run { c.go(InitiationScreen.SADHANA) } }
    IniSpacer()
    IniPrimary("Back to my day →", { run { c.go(InitiationScreen.ROUTINE) } })
    IniText("Skip this invitation") { run { c.go(InitiationScreen.ROUTINE) } }
}
