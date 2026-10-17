# Research and product decisions

Historical research retained. Active wireframes now follow [confirmed v1](../../INITIAL_MVP_SCOPE_2026-09-13.md), [marketing handoff](../../../marketing/shanivar/v1-product-direction-handoff.md) and [Alarmy/one sec patterns](../../../research/shanivar-v1-alarm-focus-patterns.md). The previous visual implementation is preserved in [the complete backup](../saturn-dark-backup-20260913-221107/index.html). Earlier charts, scroller and broad navigation are not v1 requirements.

Updated 12 September 2026. These findings guide the wireframes; they do not turn Shanivar into a medical or diagnostic product.

## Focus windows

The strongest time-specific recommendation is an evening boundary. The American Academy of Sleep Medicine advises turning off handheld electronics 30–60 minutes before bed. Its cited 2025 survey is US-based, so it supports the sleep-timing principle rather than an India-specific adoption claim. The prototype therefore offers **Night · 45 minutes before sleep** as an adjustable midpoint. [AASM bedtime screen guidance](https://aasm.org/americans-are-doomscrolling-at-bedtime-prioritizing-screen-time-over-sleep/)

A 2025 randomized trial with 125 healthy university students tested reducing smartphone use to at most two hours per day for three weeks. It found small-to-medium improvements in several measured outcomes, followed by a rapid rise in screen time after the intervention. This supports modest, sustainable restrictions and follow-up learning; it does not establish a universal 30-minute morning block. [Randomized screen-time reduction trial](https://pubmed.ncbi.nlm.nih.gov/39985031/)

Product defaults:

- **Morning · 30 minutes after waking** is a testable Shanivar hypothesis aimed at interrupting automatic first-use behavior. It is personalized from the chosen wake time and must be described as a suggestion.
- **Night · 45 minutes before sleep** sits inside AASM’s 30–60 minute guidance and adapts from the user’s quiet/bedtime setting.
- **Custom transition** lets the user protect study, prayer, family, commuting or deep-work time.
- Start with one chosen app and one window. Expand only after the person sees value.
- Keep calls and essential services available. State the exact blocked apps, start time, end time and native confirmation status.

## Daily direction and notifications

Notification studies show that a prompt can increase near-term app engagement, while personalization and adaptive timing do not work uniformly for everyone or necessarily improve long-term retention. Shanivar should begin with one user-timed daily direction, measure opt-outs and action rates, and avoid claiming that an algorithm knows the perfect moment. [Personalized-notification study](https://pubmed.ncbi.nlm.nih.gov/35621413/), [micro-randomized notification trial](https://pubmed.ncbi.nlm.nih.gov/37294612/)

On Android 13 and later, ordinary notifications require runtime permission. Request it when the user turns on the daily direction, after explaining its purpose. Use separate notification channels so alarm, daily direction and service status can be controlled independently. [Android notification permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission), [Android notification channels](https://developer.android.com/develop/ui/compose/notifications)

Daily direction should contain one relevant observation and one optional action. Personalization can use verified chart context when birth data quality is adequate, but every note must remain interpretive, show why it appeared, allow correction, and fall back to a general reflection. Birth details are optional; “better outcomes” is replaced with the narrower, defensible promise of **more relevant chart-based reflections**.

## Movement alarm

QR scanning is removed from the default experience. The preferred sequence is:

1. The person chooses a familiar destination such as morning light, the kitchen, the washroom, or a custom place.
2. At alarm time, Shanivar counts a small chosen number of steps using Android’s step detector.
3. If the device lacks a step detector, use a tested accelerometer fallback that requires sustained movement rather than a single shake.
4. A scene check is optional secondary evidence for destinations such as a window or kitchen. Never make generic image classification the only path to dismissal.
5. Offer a setup-time non-walking alternative for people who cannot safely walk on waking.

Android’s step detector emits an event for each detected step and requires `ACTIVITY_RECOGNITION` on Android 10 and later. Request that permission when the user enables movement verification. [Android motion sensors](https://developer.android.com/develop/sensors-and-location/sensors/sensors_motion)

ML Kit can label images on-device, but generic labels are not proof that a person reached a specific room. Use confidence thresholds and validation data before treating a scene as supporting evidence. ML Kit states that image input and output processing occurs on-device; its SDK can still collect diagnostics and device/app information that must be reflected in Play Data Safety disclosures. [ML Kit image labeling](https://developers.google.com/ml-kit/vision/image-labeling/android), [ML Kit terms and privacy](https://developers.google.com/ml-kit/terms), [ML Kit data disclosure](https://developers.google.com/ml-kit/android-data-disclosure)

Morning light is presented as a concrete destination and circadian cue, not a treatment. Reviews find timing matters and the evidence across ordinary real-world exposure is mixed. [Systematic review of light timing](https://pubmed.ncbi.nlm.nih.gov/30670164/), [review of everyday light exposure](https://pubmed.ncbi.nlm.nih.gov/34420891/)

The alarm remains a user-facing exact-alarm feature. Check `canScheduleExactAlarms()`, explain special access before opening system settings, confirm the scheduled occurrence after returning, and use the full-screen surface only for the user-configured alarm. [Android exact alarms](https://developer.android.com/develop/background-work/services/alarms), [Android 14 full-screen-intent rules](https://developer.android.com/about/versions/14/behavior-changes-14)

## English and Hindi

Both languages use the same information architecture and confirmation language. Hindi uses **दिनचर्या (Dincharya)** for the daily rhythm. The Hindi path may respectfully acknowledge Shani as a symbol of time, karma/consequence and patient discipline, while making faith optional and never inferring belief from language, age, name or location.

Before release, Hindi copy needs review by a fluent Indian Hindi editor across age groups. Translation QA must cover meaning, line wrapping, formal/informal address, Devanagari rendering, TalkBack order and mixed Hindi-English product terms.
