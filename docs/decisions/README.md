# Decision log

One file per decision that **closes off an alternative**: a schema shape, a platform API declined,
an enforcement boundary, a dependency taken on. Not for ordinary changes — the git history covers
those.

Name them `NNNN-short-title.md`, numbered in order, never renumbered. A decision that is later
reversed keeps its file and gets `Status: Superseded by NNNN`; deleting it destroys the reason the
reversal was needed.

## Why this exists

The git diff records *what* changed. This records *why*, and — more valuable — **what we already
considered and rejected.** Most wasted engineering time is spent rediscovering a dead end someone
already walked down. That risk multiplies when agents do the work, because an agent arriving cold
has no memory of the discussion, only the code.

## Template

```markdown
# NNNN — Title in plain language

- **Date:** YYYY-MM-DD
- **Status:** Accepted | Superseded by NNNN | Reversed

## Context
What forced a decision. The constraints as they actually were, not as we'd like them remembered.

## Options
What was genuinely considered, including the ones rejected — with the reason each was rejected.

## Decision
What we chose.

## Consequences
What this costs us, stated honestly. A decision with no downside was not a decision.

## Revisit when
The concrete trigger that should reopen this. A decision without an expiry condition quietly
becomes a permanent constraint nobody remembers choosing.
```

## Index

- [0001 — Schema changes run inline at boot, not through a migration runner](0001-inline-schema-migrations.md)
- [0002 — Enforcement never uses Device Admin or Accessibility Services](0002-no-device-admin-enforcement.md)
- [0003 — Movement is the default alarm dismissal, with a mandatory accessible alternative](0003-movement-first-alarm-dismissal.md)
- [0004 — All database connections go through the Supavisor pooler](0004-supavisor-pooler-required.md)
