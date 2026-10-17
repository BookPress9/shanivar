**Objective:** Interrupt unwanted digital behaviour reliably and create space for a deliberate, recorded choice — within real Android limits (UsageStatsManager detection + SYSTEM_ALERT_WINDOW overlay, FocusGuardService foreground service, no AccessibilityService).

**Traceability chain:** User problem → Hypothesis (UNVALIDATED unless evidence linked) → Requirement → Tickets (below) → Test cases (in each ticket + Epic F test plan) → Measured outcome (WIAC and epic metrics, Epic A/G).

**Release gates that apply:** G1–G6; real-device verification mandatory (§5); critical reliability defects block release. Gates: G1 Product alignment, G2 Design & technical review, G3 Implementation, G4 Independent testing, G5 CS readiness, G6 Release approval.

**Safety rules:** no breath pacing; no medical, therapeutic, energy or performance claims; teachers stay internal; self-reports never scores or health data; analytics collect no unnecessary sensitive data; protected-app list stays on device unless founder decides otherwise.

**Source:** strategy brief 2026-10-17 §9 Epic B. DEV has no Epic issue type (service-desk project); tickets are linked by a comment "Epic: <this key>".

**Existing tickets:** DEV-1, DEV-2, DEV-3, DEV-4, DEV-5, DEV-6, DEV-7, DEV-10, DEV-21

**New tickets (awaiting founder confirmation):** DEV-67 (Audit protection capabilities and Android platform constraints); DEV-68 (Specify protection-window behaviour and user controls); DEV-69 (Record the intervention choice on the block overlay (on-device)); DEV-70 (Detect and surface protection failures (service killed, permission revoked)); DEV-71 (QA suite: protection permissions, sessions, reboot and recovery on real devices)
