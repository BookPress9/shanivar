# V1 wireframe design decisions

One home offers independent alarm and focus actions. The olive, charcoal and aged-bronze palette keeps the Saturn reference while reducing decoration in utility screens. Light editor chrome separates design annotations from the dark mobile product.

## Reviewable flows

- Welcome → optional guest pause → home, without identity or permissions.
- Helper guide → participant handback; optional plain/Shani presentation is changeable.
- Home → editable focus preset/activity, target apps and duration → schematic account/access → active → blocked → optional pause → device home.
- Focus → deliberate early-end confirmation or simulated normal completion → optional review → device home.
- Home → alarm time/repeat days/dismissal choice → relevant account/readiness → scheduled → ringing → movement or non-walking dismissal → next occurrence.
- Failed setup → truthful recovery; alarm dismissal preserves an existing focus session.

## Prototype boundaries

All account, permission, scheduling, steps and protection states are simulated and reset on reload. The account screen is schematic and collects no credentials. Sound preview plays a short low-volume browser tone only after a click. Focus time is a static preview; the one-minute pause timer runs locally. The device-home screen demonstrates routing rather than leaving the browser.

The behavioral wording follows [accepted decision 0009](../../../docs/decisions/0009-behavioral-return-loop.md): the user chooses an action, protects the time, notices an urge, and may review whether it helped. The review answers are optional; choosing a shorter next focus time changes the local prototype plan only after Done. Recent choices are in-memory demonstration events, not analytics or a claim of native persistence.

The alarm interaction starts with an editable 18-step/window cue; both destination and step target can be changed. The non-walking choice is available at setup and while ringing. Sound-watchdog, Android recurrence, reboot, time changes and physical enforcement require implementation verification beyond this prototype.

English is the current copy review. Hindi and age-specific comprehension remain explicit delivery gates. The earlier bilingual broad-product design is preserved intact in the backup; do not confuse its Hindi strings with translation coverage for this new flow.

No charts, scroller, payment, monitoring dashboard, intelligence rank or advanced mission catalogue appears in v1 navigation. Read the confirmed scope and Claude task for engine and release requirements.
