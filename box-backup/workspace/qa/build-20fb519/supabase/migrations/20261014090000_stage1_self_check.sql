-- Sadhana Stage 1 self-check (decision 0018). Self-report columns on self_reports; RLS unchanged (own rows only).
alter table public.self_reports
  add column kind text not null default 'independent' check (kind in ('independent','stage1_check')),
  add column attempt_id text,
  add column reached_spine text check (reached_spine in ('yes','partly','no')),
  add column drift_count int check (drift_count between 0 and 10), -- 10 = "10 or more", own estimate within the sitting
  add column drift_reasons text[] not null default '{}',
  add column note text check (char_length(note) <= 500);
alter table public.self_reports alter column local_date set not null;
