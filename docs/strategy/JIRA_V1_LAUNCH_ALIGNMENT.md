# Shaniwar by The Hybrid Monk — Jira Ticket Alignment & V1 Launch Segregation

> **Founder Directive:** Focus 100% on V1 (Dincharya: Wake Alarm + Focus Block + Sacred Anchor). Segregate and park all extraneous tickets so the team executes without backlog clutter.

---

## 1. Master Ticket Segregation Matrix

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                 SHANIWAR JIRA AUDIT                                    │
│                             (Project: DEV · Total: 35+ Issues)                         │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │
            ┌───────────────────────────────┴───────────────────────────────┐
            │                                                               │
            ▼                                                               ▼
   🚀 SPRINT 1: V1 MVP LAUNCH                                  📦 POST-LAUNCH & PARKED
   (Strict Focus: Living Dincharya)                            (Segregated for Future Sprints)
   ├── DEV-V1-01: Wake Ignition (Step Check)                   ├── SPRINT 2: Retention & Wallpaper
   ├── DEV-V1-02: Obsidian Focus Shield (Shortcuts)            │   ├── DEV-S2-01: Lock Screen Wallpaper
   ├── DEV-V1-03: Living Dincharya 24h Arc                     │   ├── DEV-S2-02: Duo Partner Sync
   ├── DEV-V1-04: Sacred Naam Japa Haptics                     │   ├── DEV-S2-03: Household Blackout
   ├── DEV-V1-05: Monk Pass Subscriptions                      │   └── DEV-S2-04: Live Monks Counter
   └── DEV-V1-06: Local-First Outbox Persistence               └── PARKED / ARCHIVED BACKLOG
                                                                   ├── DEV-27..33: 33 Spine Journey
                                                                   ├── DEV-23: Circles Visual System
                                                                   └── Astrological Chart Scroller
```

---

## 2. SPRINT 1: V1 MVP (Launch Ready in Codebase)

All tickets in Sprint 1 are **strictly implemented and verified** in `hybrid-monk/src/` and tagged under `v1.0.0-mvp`.

| Ticket ID | Issue Summary | Scope & Acceptance Criteria | Status in Repo |
|---|---|---|---|
| **`DEV-V1-01`** | **Wake Ignition & CoreMotion Walk Verification** | • Uncompromising wake alarm with zero snooze option.<br>• CoreMotion step detection requiring genuine walking movement.<br>• Ambulatory verification before alarm can dismiss. | `READY_IN_CODE` (`MissionRunner.ts`) |
| **`DEV-V1-02`** | **Obsidian Focus Shield & iOS Shortcuts Intercept** | • Deep link intercept (`shaniwar://shield`) when user taps Instagram/TikTok.<br>• Triggers double-thump sacred heartbeat vibration.<br>• Enforces 10-second conscious breath check before allowing app access. | `READY_IN_CODE` (`InstagramShieldIntercept.tsx`) |
| **`DEV-V1-03`** | **Living Dincharya (दिनचर्या) Sovereign Arc** | • Replaces static database tables with a 24-hour living timeline.<br>• Dynamic time detection highlights active block (Dawn, Focus, Dusk, Night).<br>• 4 Archetypes: Builder, Student, Parent, and Modern Householder. | `READY_IN_CODE` (`RoutineHome.tsx`) |
| **`DEV-V1-04`** | **Sacred Naam Japa Anchor & Heartbeat Haptics** | • Universal Naam Japa preset selector + custom mantra entry.<br>• 108 Mala sacred chant counter with circular progress aura.<br>• Distinct tactile double-thump (`lub-dub`) on every app notification. | `READY_IN_CODE` (`naamJapaService.ts`) |
| **`DEV-V1-05`** | **Monk Pass Subscriptions & Billing Engine** | • High-converting paywall screen with annual/monthly switcher.<br>• 3 Pricing tiers: Solitary ($9.99/mo), Duo ($14.99/mo), Household ($19.99/mo).<br>• 7-day free trial CTA, purchase restore, and offline entitlement caching. | `READY_IN_CODE` (`PaywallScreen.tsx`) |
| **`DEV-V1-06`** | **Local-First Persistence & Supabase Outbox Queue** | • Complete offline resilience via AsyncStorage.<br>• Idempotent replay outbox for user completions, streaks, and habits.<br>• Zero data loss or UI freezes under offline/spotty cellular conditions. | `READY_IN_CODE` (`syncOutbox.ts`) |

---

## 3. SPRINT 2: Post-Launch Growth & Retention (Segregated for Next Release)

These tickets are scheduled immediately following the V1 rollout to drive organic word-of-mouth and retention:

| Ticket ID | Issue Summary | Feature Narrative | Priority |
|---|---|---|---|
| **`DEV-S2-01`** | **Dynamic Lock Screen Wallpaper Generator** | Automatically renders today’s calibrated Dincharya arc + active Naam Japa anchor + daily quote into a 9:16 high-res lock screen wallpaper. Every time the user wakes their phone, their sovereign intention confronts them. | 🔥 **P1 (High)** |
| **`DEV-S2-02`** | **Duo Monks Partner Sync** | Couple/partner accountability: alerts your partner if you unlock late-night social apps during the sleep sanctuary window; sends a morning ignition flame notification when you rise. | **P2** |
| **`DEV-S2-03`** | **Household Monastery Sanctuary** | Family pack feature: enables the family head/parent to initiate a dinner table or family focus blackout across all enrolled family devices simultaneously. | **P2** |
| **`DEV-S2-04`** | **Live Global Monks In Presence Counter** | Real-time Supabase / WebSocket presence engine broadcasting the live count of monks currently locked into deep work or reciting Naam Japa globally. | **P3** |

---

## 4. PARKED & ARCHIVED BACKLOG (De-complicated from Active View)

To preserve product clarity and avoid distracting the team from the core routine loop, the following legacy tickets are segregated and marked **PARKED / ARCHIVED**:

| Ticket ID | Original Title | Reason for Deferral / Archiving |
|---|---|---|
| **`DEV-23`** | *Circles visual system* | Parked. Replaced by the cleaner Living Dincharya Arc timeline. |
| **`DEV-27`..`DEV-33`** | *Sadhana S1–S7 33-Vertebra Anatomical Journey* | **Parked.** The medical vertebra visual adds cognitive friction. Kept as optional deep breathwork in the background; removed from the main user navigation. |
| **`DEV-22`** | *Future sign-in methods (Web3/multi-provider)* | Parked. Email and anonymous local-first auth are sufficient for V1. |
| **`AST-01`** | *Astrological birth-chart calculator* | Parked. Astrological reflection scroller is slated for a future standalone update. |
| **`GAM-01`** | *Spiritual XP & Leaderboards* | **Discarded.** Violates the monastic ethos. No competitive points or spiritual leaderboards. |
