# Supabase pgTAP tests (DEV-42)

Run against the LOCAL stack only: `supabase start && supabase db reset && supabase test db`.

## Baseline every table must carry (DEV-42 AC6, applies to each new table from DEV-47..DEV-55)

1. `alter table ... enable row level security` with own-row policies on `(select auth.uid())` for `authenticated`.
2. `created_at timestamptz not null default now()`.
3. Mutable tables: `updated_at timestamptz not null default now()` plus
   `create trigger set_updated_at before insert or update on <t> for each row execute function public.set_updated_at();`
   Append-only tables (no `updated_at`): the same with `public.set_created_at()`.
4. User-owned tables: `user_id uuid not null default auth.uid() references auth.users on delete cascade`.
5. Synced tables: `client_updated_at timestamptz` (client clock, stamped at enqueue; DEV-45 conflict rule).
6. Size caps on free-form jsonb / arrays / text ids, and `revoke all on <t> from anon;` with only the
   privileges the policies need granted to `authenticated`.
7. A pgTAP file here proving cross-user isolation for the new table.

`01_baseline_guard.test.sql` iterates over every table in `public` and fails while any table misses
items 1-4, so a new table cannot ship without the baseline.

Items 5-7 are only partly guarded automatically:
- Item 5: `03_client_updated_at.test.sql` checks `client_updated_at` on the sync tables it names
  (routine_plans, protection_windows, practice_attempts, self_reports, orbit_events, practice_reflections). A new synced table
  must be added to that list by hand.
- Item 6, size caps: `04_size_caps.test.sql` tests the specific DEV-42 caps on the five DEV-42 tables only
  (DEV-54 caps: `07_sadhana_record.test.sql`, PM length caps: `08_dev54_length_caps.test.sql`). It
  does not detect a new table without caps; each new table needs its own cap tests.
- Item 6, privileges: `05_privileges.test.sql` iterates over every table in `public`: anon has no
  privilege on any table, and authenticated has no TRUNCATE/REFERENCES/TRIGGER on any table. That
  authenticated keeps select/insert/update/delete is checked for the tables it names (routine_plans,
  protection_windows, practice_attempts, self_reports, practice_reflections); orbit_events is append-only
  since DEV-54 and is checked for select + insert only.
- Item 7: no guard; each new table needs its own isolation test file (as `02_rls_isolation.test.sql`
  does for the five DEV-42 tables and `07_sadhana_record.test.sql` for practice_reflections).
