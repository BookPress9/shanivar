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
items 1-4, so a new table cannot ship without the baseline. Items 5-6 have their own guard files.
