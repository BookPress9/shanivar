# Shanivar Saturn dark wireframes

Interactive mobile wireframes for the native Android experience. Open `index.html` directly in a browser, choose screens from the left rail, use the on-screen actions, or move with the left and right arrow keys.

## Screen set

| # | Screen | Product decision shown |
| --- | --- | --- |
| 01 | Arrival | Explain the structure before asking for Android access |
| 02 | Today | One time-aware action with explicit confirmation states |
| 03 | Focus setup | Review duration, blocked apps and required access |
| 04 | Focus held | Amber appears only after native protection confirmation |
| 05 | Rhythm | Separate saved preferences from scheduled/delivered states |
| 06 | Practice | Quiet passage inside structural, live lock chrome |
| 07 | Messages | A response is required before the next reflection |
| 08 | Alarm | A full-screen, verified physical dismissal action |
| 09 | Return | Report the completed practice and actual lock outcome |

## Visual system

- Void `#0A0A0C` is the base; lead and iron create structure without glass effects.
- Aged bronze `#A68A4A` marks navigation, progress and chosen actions.
- Signal amber `#D97706` is reserved for native-confirmed focus enforcement and a live alarm.
- Discipline controls use 4–10 px corners. The practice pause control becomes a pill because it belongs to the contemplative register.
- Rings express time, a closing boundary and completion. There are no lotus, mandala or chakra motifs.
- Motion is limited to screen transitions in this wireframe. Production enforcement motion should latch once and hold still.

## Compose translation

The prototype maps to the existing `Today`, `Focus`, `Rhythm`, `Practice` and `ReflectionScreen` destinations. Reusable Compose pieces should include `SaturnRing`, `StructuralCard`, `TruthStateLabel`, `LiveDisciplineBanner`, `SourceCitation`, `RhythmTimeline`, and `ShanivarBottomNavigation`.

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

This writes nine phone-frame PNGs and one workspace overview to `exports/` using the Playwright installation already present under `frontend/`.
