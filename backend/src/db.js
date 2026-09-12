require('dotenv').config();
const { Pool, types } = require('pg');

// node-postgres parses SQL DATE columns (OID 1082, e.g. users.last_practice_date,
// user_chart.birth_date) into a JS Date object set to LOCAL midnight, then JSON
// serialization converts that to UTC — so on any server whose local timezone has a
// positive UTC offset (India, most of Asia, etc.), the calendar day silently shifts
// back by one once it crosses the API (e.g. a practice logged "today" comes back as
// yesterday). Verified against the live Supabase DB: a row stored as '2026-09-09'
// round-tripped through db.query() as `2026-09-08T22:00:00.000Z` on a UTC+2 host.
// Returning the raw 'YYYY-MM-DD' string instead avoids the Date-object detour entirely.
types.setTypeParser(1082, (val) => val);

// ─────────────────────────────────────────────
// PostgreSQL Pool (via Supabase direct connection)
// ─────────────────────────────────────────────
const DATABASE_URL = process.env.DATABASE_URL || '';
const isSupabase = DATABASE_URL.includes('supabase');
const hasRealPassword = DATABASE_URL.length > 0 && !DATABASE_URL.includes('[YOUR-DB-PASSWORD]');

const pool = hasRealPassword
  ? new Pool({
      connectionString: DATABASE_URL,
      ssl: isSupabase ? { rejectUnauthorized: false } : false,
      connectionTimeoutMillis: 8000,
      idleTimeoutMillis: 30000,
      max: 10
    })
  : null;

if (pool) {
  pool.on('error', (err) => {
    console.error('❌ Unexpected DB pool error:', err.message);
  });
  pool.on('connect', () => {
    console.log('✅ PostgreSQL pool connected to Supabase');
    // Align database columns
    pool.query(`
      ALTER TABLE users ADD COLUMN IF NOT EXISTS checkins JSONB DEFAULT '[]'::jsonb;
      ALTER TABLE users ADD COLUMN IF NOT EXISTS membership VARCHAR(50) DEFAULT 'trial';
      ALTER TABLE users ADD COLUMN IF NOT EXISTS membership_since TIMESTAMPTZ;
    `).catch(err => {
      console.error('❌ Failed to ensure user checkins/membership columns:', err.message);
    });
    // practice_completions had no DB-level guard against double-completion (unlike
    // habit_logs, which has UNIQUE(habit_id, completed_at)) — routes/practices.js relied
    // solely on an application-level INSERT...WHERE NOT EXISTS check. Verified live against
    // this Supabase Postgres instance: 10 truly concurrent completion attempts for the same
    // user/practice/day only inserted 1 row in practice, but that's not a guarantee under all
    // timing/pooling conditions without an actual constraint. Add one so a race becomes a
    // 23505 unique-violation (caught in practices.js) instead of a silent duplicate/double-XP.
    // Note: completed_at::date is not usable directly in an index expression — the cast is
    // timezone-dependent (STABLE, not IMMUTABLE) and Postgres rejects it at index-creation
    // time ("functions in index expression must be marked IMMUTABLE", confirmed live).
    // Pinning to UTC via AT TIME ZONE first makes the expression a fixed-offset, immutable one.
    pool.query(`
      CREATE UNIQUE INDEX IF NOT EXISTS practice_completions_user_practice_day_key
      ON practice_completions (user_id, practice_id, ((completed_at AT TIME ZONE 'UTC')::date));
    `).catch(err => {
      console.error('❌ Failed to ensure practice_completions uniqueness index:', err.message);
    });
  });
}

// ─────────────────────────────────────────────
// Unified query interface
// ─────────────────────────────────────────────
const query = async (text, params) => {
  if (!pool) {
    throw new Error(
      'DATABASE_URL not configured. Set your Supabase DB password in backend/.env'
    );
  }
  return pool.query(text, params);
};

module.exports = { query, pool };
