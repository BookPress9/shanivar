# Shanivar Saturn dark wireframes

Interactive bilingual mobile wireframes for the native Android experience. Open `index.html` directly in a browser, choose screens from the left rail, use the on-screen actions, or move with the left and right arrow keys. Select English or Hindi on the first screen; the language button below the phone switches the entire prototype at any point.

## Screen set

| # | Screen | Product decision shown |
| --- | --- | --- |
| 01 | Language | Choose English or Hindi before product explanation |
| 02 | Account | Create an account, sign in, or explore first |
| 03 | Birth context | Optional date, time and location with a clear purpose |
| 04 | How it works | Explain focus, movement and daily direction before permissions |
| 05 | Arrival | Resolve setup into a three-part mental model |
| 06 | Today | Personal daily direction, focus suggestion and sourced practice |
| 07 | Focus setup | Compare morning, night and custom windows before starting |
| 08 | Focus held | Amber appears only after native protection confirmation |
| 09 | Dincharya | Separate saved preferences from scheduled/delivered states |
| 10 | Practice | Quiet passage inside structural, live lock chrome |
| 11 | Messages | A response is required before the next reflection |
| 12 | Movement cue | Choose steps, destination and optional private scene check |
| 13 | Alarm | Verify sustained movement without a QR scanner |
| 14 | Return | Report the completed practice and actual lock outcome |

## Visual system

- Void `#0A0A0C` is the base; lead and iron create structure without glass effects.
- Aged bronze `#A68A4A` marks navigation, progress and chosen actions.
- Signal amber `#D97706` is reserved for native-confirmed focus enforcement and a live alarm.
- Discipline controls use 4–10 px corners. The practice pause control becomes a pill because it belongs to the contemplative register.
- Rings express time, a closing boundary and completion. There are no lotus, mandala or chakra motifs.
- Motion is limited to screen transitions in this wireframe. Production enforcement motion should latch once and hold still.

## Compose translation

The prototype maps to the existing `Today`, `Focus`, `Rhythm`, `Practice` and `ReflectionScreen` destinations and adds the missing onboarding and movement-choice surfaces. Reusable Compose pieces should include `SaturnRing`, `StructuralCard`, `TruthStateLabel`, `LiveDisciplineBanner`, `SourceCitation`, `RhythmTimeline`, `MovementCuePicker`, and `ShanivarBottomNavigation`.

UI labels must continue to reflect backend/native truth:

- `Protection confirmed` only after the discipline engine reports active.
- `Alarm confirmed` only after Android returns the scheduled occurrence.
- `Saved on this device` for local routine preferences.
- `Streak update waiting` until account confirmation succeeds.

## Export

From the repository root:

```powershell
node android-app/wireframes/saturn-dark/capture.mjs
```

This writes 14 English phone-frame PNGs, 14 Hindi phone-frame PNGs and two workspace overviews to `exports-v2/` using the Playwright installation already present under `frontend/`. The earlier `exports/` folder is the superseded nine-screen revision.

See [research and product decisions](RESEARCH_AND_PRODUCT_DECISIONS.md) for the evidence boundary behind focus windows, notifications and movement verification.
