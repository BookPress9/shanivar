# Finalization checklist: Onboarding, Routine, Sadhana (audit 13 Oct 2026)

Audited line by line: android-app/wireframes/initiation-v1/HANDOFF.md and sadhana-v1/HANDOFF.md against the code.

## Done
- Onboarding: persisted checkpoints, guest flow, 60 s first practice (monotonic timer, interruption on process death), access resume, orbit derivation, reduced motion.
- Routine: plan with windows, alarm scheduling, boot/time/timezone reconcile, focus-window catch-up, protection status.
- Routine: selected-but-uninstalled apps kept as "unavailable" (B1, ad984d0).
- Routine: reconcile on app update, MY_PACKAGE_REPLACED (B2, bbd2042).
- Sadhana: catalogue + launch gate (research-only locked), attempts, record, capability criteria v1, 33-stage spine, Saturn loader, root pulse, vagal layer, self-check, voice copy (18437eb).

## Built 13 Oct (DEV-35, done)
- B1 ad984d0: selected-but-uninstalled apps kept as unavailable.
- B2 bbd2042: reconcile on MY_PACKAGE_REPLACED.
- B3 ba56e76: local idempotent sync outbox (no network until DEV-25).
- B4 2828ccd: content review manifest; release launch needs a complete matching manifest (registry empty until founder reviews, so release launches nothing yet).
- B5 5048af5: Choose linked to a saved enabled Routine window.
- B6 1e4879c: onboarding 60 s attempt imported once into the Sadhana record.
- B7 a024596: edit actual minutes of a self-report.
- B8 f6ec0b7: accessibility tests (200% font, landscape).
- Full suite: 496 tests, 0 failures. Debug APK draft release: https://github.com/BookPress9/shanivar/releases/tag/untagged-620bd070cac9d2285653

## Can build now
- None outstanding.

## Needs the founder
- Progression and visual sign-off; spine counting direction; practice content reviews; source passage verification; NASA image commercial comfort.

## Needs paid services / secrets (DEV-25)
- Google OAuth, email provider, passwordless backend, /v1 routes, hosting + database, privacy notice; source-reader backend.

## Needs physical devices
- Alarms, reboot, DST, process death, incoming calls, TalkBack, 200% font, reduced motion on real hardware.
