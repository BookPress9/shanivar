# Founder direction: movement-first alarm dismissal

Recorded 12 September 2026. This supersedes the earlier product choice that presented QR scanning and photo-plus-motion as equal primary dismissal methods.

## Product decision

- QR scanning is no longer part of the default Shanivar alarm setup or dismissal experience.
- The default is a user-chosen destination and sustained movement: morning light/window, kitchen, washroom or a custom place.
- The user chooses or confirms a small step target during setup. The alarm counts steps only after firing.
- An optional on-device scene check can support movement verification. It cannot be the only dismissal condition until real-device accuracy is validated.
- Provide a non-walking alternative during setup for mobility, balance, injury or environmental constraints.
- Never claim the alarm is impossible to dismiss. Continue to describe it as a chosen action that is deliberately difficult to complete from bed.

## Native change required

The current `AlarmActivity`, `QrDismissalActivity`, `PhotoMotionDismissalActivity`, `DailyRhythm` defaults and discipline preference contract still expose `qr` and `photo_motion`. They do not implement this new flow.

Claude’s next native-contract revision should define:

1. a movement-cue preference containing destination label, step target, optional scene mode and accessible fallback;
2. `TYPE_STEP_DETECTOR` as the primary signal where available, with `ACTIVITY_RECOGNITION` requested at the first alarm setup;
3. a calibrated sustained-accelerometer fallback for devices without the step sensor;
4. optional CameraX plus on-device image labeling, with no captured image retained;
5. verification status and reason codes that the alarm UI can display and the backend can record;
6. migration of existing `qr` preferences to a review-required movement choice rather than silently selecting a new method.

The backend may preserve legacy dismissal values for old records, but new product copy and new preferences should use the revised movement contract. Do not remove tested QR code until preference migration and rollback decisions are agreed; remove it from new-user UI first.

Wireframes and evidence boundaries: [Saturn dark prototype](wireframes/saturn-dark/index.html) and [research decisions](wireframes/saturn-dark/RESEARCH_AND_PRODUCT_DECISIONS.md).
