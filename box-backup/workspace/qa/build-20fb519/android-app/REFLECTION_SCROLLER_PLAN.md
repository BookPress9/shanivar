# Reflection scroller Compose implementation plan

Owner: Codex. Date: 2026-09-10. Sources: CLAUDE_REFLECTION_SCROLLER_HANDOFF.md, CLAUDE_REFLECTION_SCROLLER_NATIVE_CONTRACT.md, backend/src/routes/reflections.js and ai_org/reflection_scroller_squad.md.

1. Extend authenticated transport with methods, idempotency headers, retry guidance and accepted-progress envelopes on 503. Model the exact session/history/card schema.
2. Persist account-bound cached envelopes, drafts and pending mutation keys in encrypted, backup-excluded application storage. Reconcile ambiguous acceptance via GET before another answer. Clear on logout/account switch.
3. Build Messages from Shanivar entry and disclosure, current card, explicit non-disclosing options, optional validated text, server-confirmed Continue/swipe gate, answered-history reading, pinned context labels, old-session choice, bounded generation polling/retry, completion, close/delete and unconditional navigation back/offline practices.
4. Keep entry point internal to Compose. Expose an integration seam for an actual blocked-app surface; read engine status on entry/resume. Reflection code receives no release or alarm commands. No fabricated blocked-app screen or enforcement claim.
5. Add focused transport/state/Compose tests and Android emulator screenshots, including large text. Verify native build and lint. Document fixture evidence separately from Claude's backend live verification.

Release limits: underlying native lock/alarm engine and real supported-device checks remain prerequisites. TalkBack manual traversal and representative live generated-content review must be recorded explicitly; emulator/mock tests cannot close those gates.

## Integration update during implementation

Claude delivered native engine sources while the final scroller regression run was in progress. Codex connected BlockOverlayActivity to the scroller using an internal state marker and supplied the real engine's read-only status adapter. The initial absent-engine assessment above is historical; real-device verification remains open. Current evidence is in REFLECTION_SCROLLER_QA.md.
