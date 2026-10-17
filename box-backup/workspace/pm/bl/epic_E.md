**Objective:** Dependable, secure foundation: authn/authz and per-user isolation, secrets, sensitive logging and retention, duplicates/idempotency, migrations, release/rollback/incident docs. Protected-app list stays on device unless founder decides otherwise.

**Traceability chain:** User problem → Hypothesis (UNVALIDATED unless evidence linked) → Requirement → Tickets (below) → Test cases (in each ticket + Epic F test plan) → Measured outcome (WIAC and epic metrics, Epic A/G).

**Release gates that apply:** G2, G3, G4, G6 — any unresolved critical security/data-isolation defect blocks release. Gates: G1 Product alignment, G2 Design & technical review, G3 Implementation, G4 Independent testing, G5 CS readiness, G6 Release approval.

**Safety rules:** no breath pacing; no medical, therapeutic, energy or performance claims; teachers stay internal; self-reports never scores or health data; analytics collect no unnecessary sensitive data; protected-app list stays on device unless founder decides otherwise.

**Source:** strategy brief 2026-10-17 §9 Epic E. DEV has no Epic issue type (service-desk project); tickets are linked by a comment "Epic: <this key>".

**Existing tickets:** DEV-8, DEV-9, DEV-17, DEV-18, DEV-22, DEV-25, DEV-38, DEV-40, DEV-41, DEV-42, DEV-43, DEV-44, DEV-45, DEV-46, DEV-47, DEV-48, DEV-50

**New tickets (awaiting founder confirmation):** DEV-79 (Cross-user access test suite for every Supabase table, RPC and Edge Function); DEV-80 (Sensitive logging, retention, deletion and backup review); DEV-81 (Release, rollback and incident-response runbook)
