import json
E={
"A":("Strategy and measurement","Make the thesis (Protect. Become Aware. Enter Flow.) measurable: canonical Notion thesis, feature-vs-journey map, event taxonomy and North Star (WIAC), baselines, experiments, assumption and decision log.",[],"G1, G2, G5, G6"),
"B":("Protection and intentional choice","Interrupt unwanted digital behaviour reliably and create space for a deliberate, recorded choice — within real Android limits (UsageStatsManager detection + SYSTEM_ALERT_WINDOW overlay, FocusGuardService foreground service, no AccessibilityService).",["DEV-1","DEV-2","DEV-3","DEV-4","DEV-5","DEV-6","DEV-7","DEV-10","DEV-21"],"G1–G6; real-device verification mandatory (§5); critical reliability defects block release"),
"C":("Routine and action execution","Turn intentions into small repeatable actions: routine model and completion semantics, first-action flow (Initiation), missed-step recovery, alarms/notifications verified on devices, routine analytics.",["DEV-11","DEV-12","DEV-13","DEV-23","DEV-24","DEV-26","DEV-52","DEV-53"],"G1–G6; alarm behaviour verified on real devices"),
"D":("Sadhana and awareness","A small set of short practices, each mapped to a user need and expected action, with optional reflection. No breath pacing; no medical/therapeutic/energy/performance claims; teachers internal; self-reports never scores or health data.",["DEV-16","DEV-27","DEV-28","DEV-29","DEV-30","DEV-31","DEV-32","DEV-33","DEV-34","DEV-35","DEV-36","DEV-37","DEV-39","DEV-51","DEV-54"],"G1–G6; content safety review before G4"),
"E":("Security, privacy and reliability","Dependable, secure foundation: authn/authz and per-user isolation, secrets, sensitive logging and retention, duplicates/idempotency, migrations, release/rollback/incident docs. Protected-app list stays on device unless founder decides otherwise.",["DEV-8","DEV-9","DEV-17","DEV-18","DEV-22","DEV-25","DEV-38","DEV-40","DEV-41","DEV-42","DEV-43","DEV-44","DEV-45","DEV-46","DEV-47","DEV-48","DEV-50"],"G2, G3, G4, G6 — any unresolved critical security/data-isolation defect blocks release"),
"F":("QA and release readiness","Independently verify every epic: traceable test cases, E2E and regression, analytics verification, defect register, release-readiness report.",["DEV-14","DEV-15","DEV-19","DEV-55"],"G4, G6 (owns the evidence for both)"),
"G":("Customer Success and growth","Know whether users reach the intended outcome: activation/retention dashboards, feedback and churn taxonomy, lapsed-user recovery, accurate positioning and acquisition experiments, recurring product review.",["DEV-49"],"G5, G6; marketing claims verified against shipped features"),
}
for k,(t,obj,ex,g) in E.items():
  d=f"""**Objective:** {obj}

**Traceability chain:** User problem → Hypothesis (UNVALIDATED unless evidence linked) → Requirement → Tickets (below) → Test cases (in each ticket + Epic F test plan) → Measured outcome (WIAC and epic metrics, Epic A/G).

**Release gates that apply:** {g}. Gates: G1 Product alignment, G2 Design & technical review, G3 Implementation, G4 Independent testing, G5 CS readiness, G6 Release approval.

**Safety rules:** no breath pacing; no medical, therapeutic, energy or performance claims; teachers stay internal; self-reports never scores or health data; analytics collect no unnecessary sensitive data; protected-app list stays on device unless founder decides otherwise.

**Source:** strategy brief 2026-10-17 §9 Epic {k}. DEV has no Epic issue type (service-desk project); tickets are linked by a comment "Epic: <this key>".

**Existing tickets:** {', '.join(ex) or 'none'}
"""
  open(f"epic_{k}.md","w").write(d)
json.dump({k:[t,ex] for k,(t,o,ex,g) in E.items()},open("epics.json","w"))
