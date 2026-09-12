const express = require('express');
const router = express.Router();
const db = require('../db');
const { pool } = require('../db');
const { authenticateToken } = require('../middleware/auth');
const { sanitizeText } = require('../utils/sanitize');

// ─────────────────────────────────────────────────────────────────────────
// Discipline Engine (app-lockout + alarm) — backend persistence layer.
// Backend-only: the native Android engine (UsageStatsManager foreground
// detection, SYSTEM_ALERT_WINDOW overlay, AlarmManager scheduling, the
// full-screen alarm Activity and the actual QR/photo+motion capture) is a
// separate, parallel agent's work (see ai_org/discipline_engine_squad.md).
// This file only persists preferences, session/alarm state, and dismissal
// events so the client has a server-side source of truth for streaks,
// analytics and (later) entitlement enforcement.
//
// Self-healing schema (idempotent inline migration), same convention as
// backend/src/db.js's pool.on('connect', ...) block, backend/src/routes/
// ecommerce.js's schema block and backend/src/routes/reflections.js's
// schema block: CREATE TABLE IF NOT EXISTS / CREATE INDEX IF NOT EXISTS
// rather than a separate migration runner, since none exists in this
// codebase. Runs once at module load against the live Supabase Postgres
// pool.
//
// Design notes (see supabase-postgres-best-practices skill):
//   - List-shaped fields (days_of_week, block_package_list,
//     verification_detail) are stored as JSONB, not native Postgres arrays.
//     node-pg needs an explicit ::type cast to bind an EMPTY array
//     parameter for a native array column ("could not determine data type
//     of parameter"), and nothing else in this codebase uses native array
//     columns — JSONB (already used for users.checkins, reflection_cards.
//     options, etc.) avoids that footgun entirely and node-pg
//     auto-deserializes it back to a JS array/object on read.
//   - Every FK column (user_id, alarm_schedule_id) is indexed.
//   - Enum-shaped scalar columns (dismissal_method, status, method) get a
//     DB-level CHECK as a second line of defense behind app-level
//     validation, matching reflection_sessions/reflection_cards.
// ─────────────────────────────────────────────────────────────────────────

// Default social-media lock targets. Broadened 2026-09-12 from Instagram-only per
// platform-research.md's open founder question #5 ("small fixed list vs. fully
// user-configurable") — resolved as both: a broader default, still a plain overridable
// list per user (nothing about that architecture changed). Package IDs are the
// long-stable, publicly documented Android application IDs for each app; TikTok in
// particular keeps its pre-rebrand "musical.ly"-era package id after the 2018 merger,
// not something with "tiktok" in the string, which is easy to get wrong from memory.
// Declared here, before the migration block below, because that block's template
// literal is evaluated once at module load and needs this value already defined.
const DEFAULT_LOCK_TARGETS = [
  { package: 'com.instagram.android', label: 'Instagram' },
  { package: 'com.google.android.youtube', label: 'YouTube' },
  { package: 'com.zhiliaoapp.musically', label: 'TikTok' },
  { package: 'com.facebook.katana', label: 'Facebook' },
  { package: 'com.twitter.android', label: 'X (Twitter)' },
  { package: 'com.snapchat.android', label: 'Snapchat' },
];
const DEFAULT_BLOCK_LIST = DEFAULT_LOCK_TARGETS.map((t) => t.package);
const DEFAULT_BLOCK_LIST_SQL = JSON.stringify(DEFAULT_BLOCK_LIST).replace(/'/g, "''");

if (pool && typeof pool.query === 'function') {
  pool.query(`
    CREATE TABLE IF NOT EXISTS discipline_preferences (
      id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
      user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
      wake_time TIME NOT NULL DEFAULT '06:00:00',
      days_of_week JSONB NOT NULL DEFAULT '[0,1,2,3,4,5,6]'::jsonb,
      sound VARCHAR(100) NOT NULL DEFAULT 'default',
      dismissal_method VARCHAR(20) NOT NULL DEFAULT 'qr' CHECK (dismissal_method IN ('qr', 'photo_motion')),
      block_package_list JSONB NOT NULL DEFAULT '${DEFAULT_BLOCK_LIST_SQL}'::jsonb,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE TABLE IF NOT EXISTS focus_sessions (
      id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
      user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      requested_duration_minutes INTEGER NOT NULL CHECK (requested_duration_minutes > 0),
      block_package_list JSONB NOT NULL DEFAULT '${DEFAULT_BLOCK_LIST_SQL}'::jsonb,
      status VARCHAR(20) NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'released', 'expired')),
      entitlement_check BOOLEAN NOT NULL DEFAULT false,
      started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      released_at TIMESTAMPTZ,
      expires_at TIMESTAMPTZ NOT NULL,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
    CREATE INDEX IF NOT EXISTS focus_sessions_user_status_idx ON focus_sessions (user_id, status);
    -- Enforces "at most one active focus session per user" at the DB level. The
    -- SELECT-then-INSERT check in POST /focus/start below is not by itself atomic under
    -- concurrent requests (proven live, 2026-09-12: two simultaneous starts both returned
    -- 201) — this partial unique index is the actual guarantee; the app-level check
    -- remains as the fast path that avoids hitting it on the common non-racing case.
    CREATE UNIQUE INDEX IF NOT EXISTS focus_sessions_one_active_per_user_idx
      ON focus_sessions (user_id) WHERE status = 'active';

    CREATE TABLE IF NOT EXISTS alarm_schedules (
      id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
      user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      local_time TIME NOT NULL,
      days_of_week JSONB NOT NULL DEFAULT '[0,1,2,3,4,5,6]'::jsonb,
      sound VARCHAR(100) NOT NULL DEFAULT 'default',
      dismissal_method VARCHAR(20) NOT NULL DEFAULT 'qr' CHECK (dismissal_method IN ('qr', 'photo_motion')),
      enabled BOOLEAN NOT NULL DEFAULT true,
      next_confirmed_occurrence TIMESTAMPTZ,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
    CREATE INDEX IF NOT EXISTS alarm_schedules_user_id_idx ON alarm_schedules (user_id);

    CREATE TABLE IF NOT EXISTS alarm_dismissal_events (
      id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
      user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      alarm_schedule_id UUID REFERENCES alarm_schedules(id) ON DELETE SET NULL,
      method VARCHAR(20) NOT NULL CHECK (method IN ('qr', 'photo_motion')),
      verified BOOLEAN NOT NULL DEFAULT false,
      verification_detail JSONB NOT NULL DEFAULT '{}'::jsonb,
      occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
    CREATE INDEX IF NOT EXISTS alarm_dismissal_events_user_id_idx ON alarm_dismissal_events (user_id);
    CREATE INDEX IF NOT EXISTS alarm_dismissal_events_alarm_schedule_id_idx ON alarm_dismissal_events (alarm_schedule_id);

    -- CREATE TABLE IF NOT EXISTS's own DEFAULT only applies to a table created fresh by
    -- this statement — on every environment where these tables already existed before
    -- 2026-09-12, that clause above is a no-op and the live column default never changed
    -- without these two explicit ALTERs. Safe to (re)run every boot: SET DEFAULT only
    -- affects rows inserted from now on, never rewrites already-saved user preferences.
    ALTER TABLE discipline_preferences ALTER COLUMN block_package_list SET DEFAULT '${DEFAULT_BLOCK_LIST_SQL}'::jsonb;
    ALTER TABLE focus_sessions ALTER COLUMN block_package_list SET DEFAULT '${DEFAULT_BLOCK_LIST_SQL}'::jsonb;
  `).catch((err) => {
    console.error('❌ Failed to ensure discipline_* schema:', err.message);
  });
}

// ─────────────────────────────────────────────────────────────────────────
// Constants & validation helpers
// ─────────────────────────────────────────────────────────────────────────
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const TIME_RE = /^([01]\d|2[0-3]):([0-5]\d)$/;
const DISMISSAL_METHODS = ['qr', 'photo_motion'];
// DEFAULT_LOCK_TARGETS/DEFAULT_BLOCK_LIST are declared above, before the migration
// block, since that block's template literal needs them at module-load time.

// Duration bounds and staleness grace period are engineering defaults, not confirmed
// founder decisions — flagged in the handoff report. 5-480 minutes covers "a single
// scroll-avoidance session" through "a full workday," and a 30-minute grace period
// gives a slow app-kill/network blip room before a session is presumed abandoned.
const MIN_FOCUS_MINUTES = 5;
const MAX_FOCUS_MINUTES = 480;
const FOCUS_STALE_GRACE_MINUTES = 30;

const MAX_VERIFICATION_DETAIL_JSON_LENGTH = 2000;

function isValidTimeString(v) {
  return typeof v === 'string' && TIME_RE.test(v);
}

// 0=Sunday .. 6=Saturday (JS Date#getDay() convention). Empty is rejected, not just
// "no days selected yet" — a schedule that can never fire on any day is a real mistake
// to catch at the API boundary, not a valid state; `enabled: false` already exists as
// the actual way to represent "off" for both alarms and preferences.
function isValidDaysOfWeek(v) {
  return Array.isArray(v) && v.length > 0 && v.length <= 7 && v.every((d) => Number.isInteger(d) && d >= 0 && d <= 6);
}

function isValidPackageList(v) {
  return Array.isArray(v) && v.length <= 50 && v.every((p) => typeof p === 'string' && p.length > 0 && p.length <= 200);
}

// Entitlement gate: schema-ready for real billing (per PRODUCT_DIRECTION.md's paid-locks
// direction), NOT real payment processing (explicitly out of scope for this pass). Reuses
// the `users.membership` column billing.js already reads/writes (GET /billing/status,
// POST /billing/subscribe, POST /billing/refund) rather than inventing a new field.
// New users default to membership='trial' (backend/src/db.js), so nobody is blocked by
// this gate today; only a user explicitly moved to 'free' (e.g. via POST /billing/refund)
// is treated as not entitled. Wiring in real subscription-tier logic later means changing
// only this one function — no schema change needed, since focus_sessions.entitlement_check
// already records the boolean this function returns at session-start time.
const NON_ENTITLED_MEMBERSHIPS = new Set(['free']);
async function checkFocusEntitlement(userId) {
  const result = await db.query('SELECT membership FROM users WHERE id = $1', [userId]);
  const membership = (result.rows[0] && result.rows[0].membership) || 'trial';
  return !NON_ENTITLED_MEMBERSHIPS.has(membership);
}

// Lazy staleness sweep: this codebase has no background worker/cron (reflections.js's
// "generating" resume happens on the next request for the same reason), so a focus
// session whose app process was killed without calling /release would otherwise stay
// 'active' forever. expires_at (set at INSERT time to started_at + requested duration +
// grace) makes that detectable without a scheduled job — every read that matters
// (GET /focus/active, POST /focus/start's already-active check) sweeps first.
async function expireStaleFocusSessions(userId) {
  await db.query(
    `UPDATE focus_sessions SET status = 'expired', updated_at = NOW()
     WHERE user_id = $1 AND status = 'active' AND expires_at < NOW()`,
    [userId]
  );
}

// Ownership-scoped alarm lookup. Returns null for both "doesn't exist" and "belongs to
// another user" — callers turn null into 404, never 403, matching the convention in
// habits.js and reflections.js's findOwnedSession.
async function findOwnedAlarm(userId, alarmId) {
  if (!UUID_RE.test(alarmId)) return null;
  const result = await db.query('SELECT * FROM alarm_schedules WHERE id = $1 AND user_id = $2', [alarmId, userId]);
  return result.rows[0] || null;
}

router.use(authenticateToken);

// ─────────────────────────────────────────────────────────────────────────
// GET /lock-targets — static catalog so the client can render real app names instead
// of asking a user to recognize their own block list as raw Android package ids.
// Deliberately just the in-file constant, not a table: this is a short, rarely-changing
// list maintained by whoever ships the next target, not user data.
// ─────────────────────────────────────────────────────────────────────────
router.get('/lock-targets', (req, res) => {
  res.json({ targets: DEFAULT_LOCK_TARGETS });
});

// ─────────────────────────────────────────────────────────────────────────
// GET/PUT /preferences
// ─────────────────────────────────────────────────────────────────────────
router.get('/preferences', async (req, res) => {
  try {
    const userId = req.user.id;
    let result = await db.query('SELECT * FROM discipline_preferences WHERE user_id = $1', [userId]);
    if (result.rows.length === 0) {
      const inserted = await db.query(
        `INSERT INTO discipline_preferences (user_id) VALUES ($1)
         ON CONFLICT (user_id) DO NOTHING RETURNING *`,
        [userId]
      );
      result = inserted.rows.length > 0
        ? inserted
        : await db.query('SELECT * FROM discipline_preferences WHERE user_id = $1', [userId]);
    }
    res.json(result.rows[0]);
  } catch (err) {
    console.error('GET /discipline/preferences error:', err);
    res.status(500).json({ error: 'Failed to load preferences' });
  }
});

router.put('/preferences', async (req, res) => {
  try {
    const userId = req.user.id;
    const { wake_time, days_of_week, sound, dismissal_method, block_package_list } = req.body || {};

    if (wake_time !== undefined && !isValidTimeString(wake_time)) {
      return res.status(400).json({ error: 'wake_time must be HH:MM (24h)' });
    }
    if (days_of_week !== undefined && !isValidDaysOfWeek(days_of_week)) {
      return res.status(400).json({ error: 'days_of_week must be an array of integers 0-6 (0=Sunday)' });
    }
    if (dismissal_method !== undefined && !DISMISSAL_METHODS.includes(dismissal_method)) {
      return res.status(400).json({ error: `dismissal_method must be one of ${DISMISSAL_METHODS.join(', ')}` });
    }
    if (block_package_list !== undefined && !isValidPackageList(block_package_list)) {
      return res.status(400).json({ error: 'block_package_list must be an array of up to 50 package name strings' });
    }
    const cleanSound = sound !== undefined ? sanitizeText(sound).slice(0, 100) : undefined;

    // Ensure a row exists first (same get-or-create as GET), then update only supplied
    // fields via COALESCE so a partial PUT never clobbers untouched columns.
    await db.query(
      'INSERT INTO discipline_preferences (user_id) VALUES ($1) ON CONFLICT (user_id) DO NOTHING',
      [userId]
    );

    const result = await db.query(
      `UPDATE discipline_preferences SET
         wake_time = COALESCE($2, wake_time),
         days_of_week = COALESCE($3, days_of_week),
         sound = COALESCE($4, sound),
         dismissal_method = COALESCE($5, dismissal_method),
         block_package_list = COALESCE($6, block_package_list),
         updated_at = NOW()
       WHERE user_id = $1
       RETURNING *`,
      [
        userId,
        wake_time !== undefined ? wake_time : null,
        days_of_week !== undefined ? JSON.stringify(days_of_week) : null,
        cleanSound !== undefined ? cleanSound : null,
        dismissal_method !== undefined ? dismissal_method : null,
        block_package_list !== undefined ? JSON.stringify(block_package_list) : null
      ]
    );
    res.json(result.rows[0]);
  } catch (err) {
    console.error('PUT /discipline/preferences error:', err);
    res.status(500).json({ error: 'Failed to update preferences' });
  }
});

// ─────────────────────────────────────────────────────────────────────────
// Focus sessions
// ─────────────────────────────────────────────────────────────────────────
router.post('/focus/start', async (req, res) => {
  try {
    const userId = req.user.id;
    const { duration_minutes, block_package_list } = req.body || {};

    if (!Number.isInteger(duration_minutes) || duration_minutes < MIN_FOCUS_MINUTES || duration_minutes > MAX_FOCUS_MINUTES) {
      return res.status(400).json({
        error: `duration_minutes must be an integer between ${MIN_FOCUS_MINUTES} and ${MAX_FOCUS_MINUTES}`
      });
    }
    if (block_package_list !== undefined && !isValidPackageList(block_package_list)) {
      return res.status(400).json({ error: 'block_package_list must be an array of up to 50 package name strings' });
    }

    await expireStaleFocusSessions(userId);

    const existingActive = await db.query(
      "SELECT * FROM focus_sessions WHERE user_id = $1 AND status = 'active'",
      [userId]
    );
    if (existingActive.rows.length > 0) {
      return res.status(409).json({
        error: 'A focus session is already active. Release it before starting another.',
        session: existingActive.rows[0]
      });
    }

    const entitled = await checkFocusEntitlement(userId);
    if (!entitled) {
      return res.status(402).json({ error: 'A subscription is required to start app blocking.' });
    }

    let packages = block_package_list;
    if (packages === undefined) {
      const prefs = await db.query('SELECT block_package_list FROM discipline_preferences WHERE user_id = $1', [userId]);
      packages = (prefs.rows[0] && prefs.rows[0].block_package_list) || DEFAULT_BLOCK_LIST;
    }

    let result;
    try {
      result = await db.query(
        `INSERT INTO focus_sessions
           (user_id, requested_duration_minutes, block_package_list, status, entitlement_check, started_at, expires_at)
         VALUES ($1, $2, $3, 'active', $4, NOW(), NOW() + make_interval(mins => $2) + make_interval(mins => $5))
         RETURNING *`,
        [userId, duration_minutes, JSON.stringify(packages), entitled, FOCUS_STALE_GRACE_MINUTES]
      );
    } catch (insertErr) {
      // The SELECT-then-INSERT check above is the fast path, not the guarantee — it is not
      // atomic under concurrent requests (proven live: two simultaneous starts both passed
      // it and both got a 201 before this fix). focus_sessions_one_active_per_user_idx is
      // the real guarantee; a race that slips past the check above lands here instead,
      // mirroring auth.js's identical 23505-race-as-409 pattern for registration.
      if (insertErr.code === '23505') {
        const nowActive = await db.query(
          "SELECT * FROM focus_sessions WHERE user_id = $1 AND status = 'active'",
          [userId]
        );
        return res.status(409).json({
          error: 'A focus session is already active. Release it before starting another.',
          session: nowActive.rows[0]
        });
      }
      throw insertErr;
    }
    res.status(201).json(result.rows[0]);
  } catch (err) {
    console.error('POST /discipline/focus/start error:', err);
    res.status(500).json({ error: 'Failed to start focus session' });
  }
});

router.post('/focus/:id/release', async (req, res) => {
  try {
    const userId = req.user.id;
    const sessionId = req.params.id;
    if (!UUID_RE.test(sessionId)) return res.status(404).json({ error: 'Focus session not found' });

    const existing = await db.query('SELECT * FROM focus_sessions WHERE id = $1 AND user_id = $2', [sessionId, userId]);
    if (existing.rows.length === 0) return res.status(404).json({ error: 'Focus session not found' });

    const session = existing.rows[0];
    if (session.status !== 'active') {
      // Idempotent: releasing an already-released/expired session is a no-op success, not
      // an error — the native client may retry a release call after a network hiccup
      // without knowing whether the first attempt actually landed server-side.
      return res.json(session);
    }

    const updated = await db.query(
      `UPDATE focus_sessions SET status = 'released', released_at = NOW(), updated_at = NOW()
       WHERE id = $1 RETURNING *`,
      [sessionId]
    );
    res.json(updated.rows[0]);
  } catch (err) {
    console.error('POST /discipline/focus/:id/release error:', err);
    res.status(500).json({ error: 'Failed to release focus session' });
  }
});

router.get('/focus/active', async (req, res) => {
  try {
    const userId = req.user.id;
    await expireStaleFocusSessions(userId);
    const result = await db.query(
      "SELECT * FROM focus_sessions WHERE user_id = $1 AND status = 'active' ORDER BY started_at DESC LIMIT 1",
      [userId]
    );
    res.json({ active: result.rows[0] || null });
  } catch (err) {
    console.error('GET /discipline/focus/active error:', err);
    res.status(500).json({ error: 'Failed to load active focus session' });
  }
});

// ─────────────────────────────────────────────────────────────────────────
// Alarm schedules
// ─────────────────────────────────────────────────────────────────────────
router.post('/alarms', async (req, res) => {
  try {
    const userId = req.user.id;
    const { local_time, days_of_week, sound, dismissal_method, enabled } = req.body || {};

    if (!isValidTimeString(local_time)) {
      return res.status(400).json({ error: 'local_time must be HH:MM (24h)' });
    }
    if (days_of_week !== undefined && !isValidDaysOfWeek(days_of_week)) {
      return res.status(400).json({ error: 'days_of_week must be an array of integers 0-6 (0=Sunday)' });
    }
    if (dismissal_method !== undefined && !DISMISSAL_METHODS.includes(dismissal_method)) {
      return res.status(400).json({ error: `dismissal_method must be one of ${DISMISSAL_METHODS.join(', ')}` });
    }
    const cleanSound = sound !== undefined ? sanitizeText(sound).slice(0, 100) : 'default';

    const result = await db.query(
      `INSERT INTO alarm_schedules (user_id, local_time, days_of_week, sound, dismissal_method, enabled)
       VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING *`,
      [
        userId,
        local_time,
        JSON.stringify(days_of_week !== undefined ? days_of_week : [0, 1, 2, 3, 4, 5, 6]),
        cleanSound,
        dismissal_method || 'qr',
        enabled === undefined ? true : !!enabled
      ]
    );
    res.status(201).json(result.rows[0]);
  } catch (err) {
    console.error('POST /discipline/alarms error:', err);
    res.status(500).json({ error: 'Failed to create alarm' });
  }
});

router.get('/alarms', async (req, res) => {
  try {
    const result = await db.query('SELECT * FROM alarm_schedules WHERE user_id = $1 ORDER BY created_at ASC', [req.user.id]);
    res.json(result.rows);
  } catch (err) {
    console.error('GET /discipline/alarms error:', err);
    res.status(500).json({ error: 'Failed to load alarms' });
  }
});

router.put('/alarms/:id', async (req, res) => {
  try {
    const userId = req.user.id;
    const alarm = await findOwnedAlarm(userId, req.params.id);
    if (!alarm) return res.status(404).json({ error: 'Alarm not found' });

    const { local_time, days_of_week, sound, dismissal_method, enabled } = req.body || {};
    if (local_time !== undefined && !isValidTimeString(local_time)) {
      return res.status(400).json({ error: 'local_time must be HH:MM (24h)' });
    }
    if (days_of_week !== undefined && !isValidDaysOfWeek(days_of_week)) {
      return res.status(400).json({ error: 'days_of_week must be an array of integers 0-6 (0=Sunday)' });
    }
    if (dismissal_method !== undefined && !DISMISSAL_METHODS.includes(dismissal_method)) {
      return res.status(400).json({ error: `dismissal_method must be one of ${DISMISSAL_METHODS.join(', ')}` });
    }
    const cleanSound = sound !== undefined ? sanitizeText(sound).slice(0, 100) : undefined;

    // Editing the schedule invalidates any previously-confirmed occurrence: the OS alarm
    // was scheduled against the OLD time/days, so next_confirmed_occurrence is reset to
    // NULL here — per NATIVE_HANDOFF.md, the server never assumes scheduling happened, it
    // only ever reflects what the client explicitly confirmed via POST .../confirm-scheduled.
    const result = await db.query(
      `UPDATE alarm_schedules SET
         local_time = COALESCE($2, local_time),
         days_of_week = COALESCE($3, days_of_week),
         sound = COALESCE($4, sound),
         dismissal_method = COALESCE($5, dismissal_method),
         enabled = COALESCE($6, enabled),
         next_confirmed_occurrence = NULL,
         updated_at = NOW()
       WHERE id = $1 AND user_id = $7
       RETURNING *`,
      [
        req.params.id,
        local_time !== undefined ? local_time : null,
        days_of_week !== undefined ? JSON.stringify(days_of_week) : null,
        cleanSound !== undefined ? cleanSound : null,
        dismissal_method !== undefined ? dismissal_method : null,
        enabled !== undefined ? !!enabled : null,
        userId
      ]
    );
    res.json(result.rows[0]);
  } catch (err) {
    console.error('PUT /discipline/alarms/:id error:', err);
    res.status(500).json({ error: 'Failed to update alarm' });
  }
});

router.delete('/alarms/:id', async (req, res) => {
  try {
    // alarm_dismissal_events.alarm_schedule_id is ON DELETE SET NULL, so past dismissal
    // history survives an alarm being deleted (for analytics/debugging) instead of
    // cascading away with it.
    const result = await db.query('DELETE FROM alarm_schedules WHERE id = $1 AND user_id = $2 RETURNING id', [
      req.params.id,
      req.user.id
    ]);
    if (result.rows.length === 0) return res.status(404).json({ error: 'Alarm not found' });
    res.json({ message: 'Alarm deleted' });
  } catch (err) {
    console.error('DELETE /discipline/alarms/:id error:', err);
    res.status(500).json({ error: 'Failed to delete alarm' });
  }
});

router.post('/alarms/:id/confirm-scheduled', async (req, res) => {
  try {
    const userId = req.user.id;
    const alarm = await findOwnedAlarm(userId, req.params.id);
    if (!alarm) return res.status(404).json({ error: 'Alarm not found' });

    const { next_occurrence } = req.body || {};
    const parsed = next_occurrence !== undefined && next_occurrence !== null ? new Date(next_occurrence) : null;
    if (!parsed || Number.isNaN(parsed.getTime())) {
      return res.status(400).json({ error: 'next_occurrence must be a valid ISO timestamp' });
    }

    const result = await db.query(
      `UPDATE alarm_schedules SET next_confirmed_occurrence = $1, updated_at = NOW()
       WHERE id = $2 AND user_id = $3 RETURNING *`,
      [parsed.toISOString(), req.params.id, userId]
    );
    res.json(result.rows[0]);
  } catch (err) {
    console.error('POST /discipline/alarms/:id/confirm-scheduled error:', err);
    res.status(500).json({ error: 'Failed to confirm scheduled alarm' });
  }
});

// ─────────────────────────────────────────────────────────────────────────
// POST /alarms/dismiss — records a dismissal event. Record only: no XP, no lock
// release, no entitlement change. Per the reflection-scroller precedent ("reflections
// never release locks or satisfy alarm dismissal"), this route is the actual
// dismissal record-keeper that rule was written in anticipation of.
// ─────────────────────────────────────────────────────────────────────────
router.post('/alarms/dismiss', async (req, res) => {
  try {
    const userId = req.user.id;
    const { alarm_schedule_id, method, verified, verification_detail } = req.body || {};

    if (!DISMISSAL_METHODS.includes(method)) {
      return res.status(400).json({ error: `method must be one of ${DISMISSAL_METHODS.join(', ')}` });
    }
    if (typeof verified !== 'boolean') {
      return res.status(400).json({ error: 'verified must be a boolean' });
    }

    let detail = verification_detail;
    if (detail !== undefined && detail !== null) {
      if (typeof detail !== 'object' || Array.isArray(detail)) {
        return res.status(400).json({ error: 'verification_detail must be a JSON object' });
      }
      const serialized = JSON.stringify(detail);
      if (serialized.length > MAX_VERIFICATION_DETAIL_JSON_LENGTH) {
        // This is a debugging/abuse-analysis record (e.g. a matched QR payload identifier
        // or a computed motion-magnitude number), not a sensor dump — reject oversized
        // payloads outright rather than silently truncating a JSON blob into something
        // misleading later.
        return res.status(400).json({
          error: `verification_detail must serialize to at most ${MAX_VERIFICATION_DETAIL_JSON_LENGTH} characters`
        });
      }
    } else {
      detail = {};
    }

    // alarm_schedule_id is nullable — a manually-triggered test/dismissal might not
    // reference a schedule at all — but when present it must belong to this user.
    let alarmScheduleId = null;
    if (alarm_schedule_id !== undefined && alarm_schedule_id !== null) {
      const alarm = await findOwnedAlarm(userId, alarm_schedule_id);
      if (!alarm) return res.status(404).json({ error: 'Alarm not found' });
      alarmScheduleId = alarm.id;
    }

    const result = await db.query(
      `INSERT INTO alarm_dismissal_events (user_id, alarm_schedule_id, method, verified, verification_detail, occurred_at)
       VALUES ($1, $2, $3, $4, $5, NOW())
       RETURNING *`,
      [userId, alarmScheduleId, method, verified, JSON.stringify(detail)]
    );
    res.status(201).json(result.rows[0]);
  } catch (err) {
    console.error('POST /discipline/alarms/dismiss error:', err);
    res.status(500).json({ error: 'Failed to record dismissal event' });
  }
});

module.exports = router;
