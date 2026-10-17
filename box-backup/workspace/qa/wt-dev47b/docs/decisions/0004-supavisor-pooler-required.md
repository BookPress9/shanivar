# 0004 — All database connections go through the Supavisor pooler, never the direct host

- **Date:** 2026-09-12
- **Status:** Accepted

## Context

Supabase exposes a Postgres database two ways:

- **Direct:** `db.<project-ref>.supabase.co:5432`
- **Pooled (Supavisor):** `aws-1-ap-northeast-2.pooler.supabase.com:5432`, user `postgres.<ref>`

The direct host is the one the Supabase dashboard shows most prominently, and it is the obvious
thing to copy into an environment variable.

## Decision

`DATABASE_URL` always points at the **pooler**. The direct host is never used from any deployed
environment.

## Consequences / why

Two independent reasons, either one sufficient:

1. **The direct host is IPv6-only. Render's egress is IPv4-only.** Connecting to it from Render
   fails with `ENETUNREACH 2406:da12:...` — which surfaces as a generic startup crash, not as
   anything resembling a DNS or addressing problem. This cost a full deploy cycle to diagnose. The
   error gives you almost nothing; without this note the next person pays the same cost.
2. **Connection exhaustion.** Postgres holds a hard `max_connections`, and a `pg` Pool per instance
   multiplies against it. One instance is fine; several, or anything serverless, exhausts the
   database and every route starts failing at once. Supavisor exists precisely to sit in front of
   that, so routing through it now means the scaling fix is already in place rather than being an
   emergency later.

Cost: pooled connections in transaction mode don't support session-level features — `LISTEN/NOTIFY`,
session advisory locks, and some prepared-statement patterns. Nothing currently uses these. Anything
that needs them must not silently switch to the direct host to get them.

## Gotcha for whoever touches this next

The pooler hostname's region prefix is **not** guessable. This project is on the newer `aws-1-`
cluster; the older, more commonly documented `aws-0-` prefix returns a confusing **"tenant not
found"** rather than a connection error. Copy the exact host from the Supabase dashboard's
connection-pooling section.

## Revisit when

- Render (or whatever replaces it) supports IPv6 egress **and** we have a reason to want session-mode
  connections.
- We move off Supabase entirely.
