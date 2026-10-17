const express = require('express');
const router = express.Router();
const db = require('../db');
const { pool } = require('../db');
const { authenticateToken, requireAdmin } = require('../middleware/auth');

// ─────────────────────────────────────────────────────────────────────────
// Alarm delivery telemetry.
//
// Why this exists, and why it is not in discipline.js: alarm_dismissal_events
// records what happened AFTER an alarm woke someone. It cannot record the
// failure that actually matters for this product — an alarm the operating
// system killed, deferred past usefulness, or never delivered. That writes no
// row at all, which makes "Xiaomi silently dropped the alarm" and "this person
// stopped using the app" the same shape in the data.
//
// Deciding whether physical-friction wake-ups work at all depends on being able
// to tell those apart, per device manufacturer, because aggressive OEM battery
// management is the single most likely cause of failure (see
// shanivar-vbt/discipline-engine/platform-research.md). This route is the
// ingest side of that measurement.
//
// Separate file, deliberately: discipline.js is under concurrent edit by the
// frontend agent for the movement-dismissal contract, and telemetry ingest is
// its own concern — analytics.js currently exposes only an admin read route,
// so there is no client-writable event path anywhere in this codebase.
//
// Self-healing schema, matching the convention in db.js / discipline.js /
// reflections.js: idempotent DDL at module load, since this codebase has no
// migration runner. Note CREATE TABLE IF NOT EXISTS is a no-op against an
// existing table, so anything added later needs its own ALTER.
// ─────────────────────────────────────────────────────────────────────────

const DELIVERY_PATHS = ['full_screen_intent', 'notification_only', 'activity_direct', 'unknown'];
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

if (pool && typeof pool.query === 'function') {
  pool.query(`
    CREATE TABLE IF NOT EXISTS alarm_fire_events (
      id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
      user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      alarm_schedule_id UUID REFERENCES alarm_schedules(id) ON DELETE SET NULL,
      scheduled_for TIMESTAMPTZ,
      fired_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      delivery_path VARCHAR(24) NOT NULL DEFAULT 'unknown'
        CHECK (delivery_path IN ('full_screen_intent', 'notification_only', 'activity_direct', 'unknown')),
      device_manufacturer VARCHAR(60),
      device_model VARCHAR(80),
      android_sdk INTEGER,
      app_version VARCHAR(24),
      client_event_id UUID,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
    CREATE INDEX IF NOT EXISTS alarm_fire_events_user_id_idx ON alarm_fire_events (user_id);
    CREATE INDEX IF NOT EXISTS alarm_fire_events_schedule_idx ON alarm_fire_events (alarm_schedule_id);
    CREATE INDEX IF NOT EXISTS alarm_fire_events_manufacturer_idx ON alarm_fire_events (device_manufacturer);
    -- The device retries this call when it fires offline and reconnects later, so the same
    -- physical alarm can arrive more than once. Without this, a flaky network would look like
    -- better alarm reliability than actually occurred, which is the exact opposite of useful.
    CREATE UNIQUE INDEX IF NOT EXISTS alarm_fire_events_client_event_idx
      ON alarm_fire_events (user_id, client_event_id) WHERE client_event_id IS NOT NULL;
  `).catch((err) => {
    console.error('❌ Failed to ensure alarm_fire_events schema:', err.message);
  });
}

function cleanString(value, maxLength) {
  if (typeof value !== 'string') return null;
  const trimmed = value.trim();
  return trimmed.length === 0 ? null : trimmed.slice(0, maxLength);
}

function parseTimestamp(value) {
  if (value === undefined || value === null) return null;
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? undefined : parsed.toISOString();
}

router.use(authenticateToken);

// ─────────────────────────────────────────────────────────────────────────
// POST /alarm-fired — the device reporting that an alarm actually reached the user.
// Records only. This never dismisses an alarm, never grants XP and never touches a
// streak; the client must not gate the ringing UI on it succeeding.
// ─────────────────────────────────────────────────────────────────────────
router.post('/alarm-fired', async (req, res) => {
  try {
    const userId = req.user.id;
    const {
      alarm_schedule_id, scheduled_for, fired_at, delivery_path,
      device_manufacturer, device_model, android_sdk, app_version, client_event_id,
    } = req.body || {};

    if (delivery_path !== undefined && !DELIVERY_PATHS.includes(delivery_path)) {
      return res.status(400).json({ error: `delivery_path must be one of ${DELIVERY_PATHS.join(', ')}` });
    }
    if (client_event_id !== undefined && client_event_id !== null && !UUID_RE.test(String(client_event_id))) {
      return res.status(400).json({ error: 'client_event_id must be a UUID' });
    }
    if (android_sdk !== undefined && android_sdk !== null
        && (!Number.isInteger(android_sdk) || android_sdk < 1 || android_sdk > 100)) {
      return res.status(400).json({ error: 'android_sdk must be an integer between 1 and 100' });
    }

    const scheduledFor = parseTimestamp(scheduled_for);
    const firedAt = parseTimestamp(fired_at);
    if (scheduledFor === undefined) return res.status(400).json({ error: 'scheduled_for must be a valid ISO timestamp' });
    if (firedAt === undefined) return res.status(400).json({ error: 'fired_at must be a valid ISO timestamp' });

    // An alarm belonging to someone else must not be attributable to this account, but a
    // missing/deleted schedule is still worth recording — the fire genuinely happened.
    let scheduleId = null;
    if (alarm_schedule_id !== undefined && alarm_schedule_id !== null) {
      if (!UUID_RE.test(String(alarm_schedule_id))) {
        return res.status(400).json({ error: 'alarm_schedule_id must be a UUID' });
      }
      const owned = await db.query(
        'SELECT id FROM alarm_schedules WHERE id = $1 AND user_id = $2',
        [alarm_schedule_id, userId]
      );
      if (owned.rows.length === 0) return res.status(404).json({ error: 'Alarm not found' });
      scheduleId = owned.rows[0].id;
    }

    try {
      const inserted = await db.query(
        `INSERT INTO alarm_fire_events
           (user_id, alarm_schedule_id, scheduled_for, fired_at, delivery_path,
            device_manufacturer, device_model, android_sdk, app_version, client_event_id)
         VALUES ($1, $2, $3, COALESCE($4, NOW()), COALESCE($5, 'unknown'), $6, $7, $8, $9, $10)
         RETURNING *`,
        [
          userId, scheduleId, scheduledFor, firedAt, delivery_path,
          cleanString(device_manufacturer, 60), cleanString(device_model, 80),
          android_sdk ?? null, cleanString(app_version, 24), client_event_id ?? null,
        ]
      );
      return res.status(201).json(inserted.rows[0]);
    } catch (insertErr) {
      // Same retry-as-success contract used for the auth-registration and focus-start races:
      // a duplicate report of one real alarm is the client doing the right thing, not an error.
      if (insertErr.code === '23505') {
        const existing = await db.query(
          'SELECT * FROM alarm_fire_events WHERE user_id = $1 AND client_event_id = $2',
          [userId, client_event_id]
        );
        return res.status(200).json(existing.rows[0]);
      }
      throw insertErr;
    }
  } catch (err) {
    console.error('POST /telemetry/alarm-fired error:', err);
    res.status(500).json({ error: 'Failed to record alarm fire event' });
  }
});

// ─────────────────────────────────────────────────────────────────────────
// GET /alarm-reliability — the OEM matrix, admin-only.
//
// This is the number the physical-device test cohort exists to produce: of the alarms that
// fired on each manufacturer's devices, how many reached a verified dismissal. A low
// verified rate on one manufacturer points at that vendor's battery management; a low rate
// everywhere points at the dismissal mechanism itself.
//
// It deliberately cannot show alarms that never fired at all — nothing on a killed device
// can report in. Compare `fires` here against alarm_schedules.next_confirmed_occurrence to
// estimate those; the gap is the silent-failure rate.
// ─────────────────────────────────────────────────────────────────────────
router.get('/alarm-reliability', requireAdmin, async (req, res) => {
  try {
    const days = Number.isInteger(Number(req.query.days)) ? Math.min(Math.max(Number(req.query.days), 1), 365) : 30;
    const result = await db.query(
      `SELECT
         COALESCE(f.device_manufacturer, 'unknown') AS manufacturer,
         COUNT(*)::int AS fires,
         COUNT(DISTINCT f.user_id)::int AS users,
         COUNT(d.id)::int AS verified_dismissals,
         ROUND(100.0 * COUNT(d.id) / NULLIF(COUNT(*), 0), 1) AS verified_rate_percent
       FROM alarm_fire_events f
       LEFT JOIN alarm_dismissal_events d
         ON d.user_id = f.user_id
        AND d.verified = true
        AND d.occurred_at BETWEEN f.fired_at AND f.fired_at + INTERVAL '2 hours'
       WHERE f.fired_at > NOW() - make_interval(days => $1)
       GROUP BY 1
       ORDER BY fires DESC`,
      [days]
    );
    res.json({ window_days: days, by_manufacturer: result.rows });
  } catch (err) {
    console.error('GET /telemetry/alarm-reliability error:', err);
    res.status(500).json({ error: 'Failed to load alarm reliability' });
  }
});

module.exports = router;
