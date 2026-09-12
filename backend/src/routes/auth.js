const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const rateLimit = require('express-rate-limit');
const db = require('../db');
const { pool } = require('../db');
const { JWT_SECRET } = require('../middleware/auth');
const { computeNatalChart } = require('../utils/astrology');
const router = express.Router();

function isFiniteNumber(n) {
  return typeof n === 'number' && Number.isFinite(n);
}

// Credential-stuffing/brute-force throttle, tighter than the blanket API-wide limiter in
// index.js — this is the one surface where a loose limit is actually dangerous.
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 20,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many attempts, please try again later.' },
});

// Register
router.post('/register', authLimiter, async (req, res) => {
  try {
    const { email, username, password, display_name, phone, birth_date, birth_time, birth_location, preferred_language, utcOffset, lat, lon } = req.body;
    if (!email || !password) {
      return res.status(400).json({ error: 'Email and password are required' });
    }
    if (preferred_language !== undefined && !['en', 'hi'].includes(preferred_language)) {
      return res.status(400).json({ error: 'preferred_language must be en or hi' });
    }
    if (birth_date !== undefined && birth_date !== '' && !/^\d{4}-\d{2}-\d{2}$/.test(birth_date)) {
      return res.status(400).json({ error: 'birth_date must be YYYY-MM-DD' });
    }
    if (birth_time !== undefined && birth_time !== '' && !/^([01]\d|2[0-3]):[0-5]\d$/.test(birth_time)) {
      return res.status(400).json({ error: 'birth_time must be HH:MM' });
    }
    // Normalize email case so "Foo@x.com" and "foo@x.com" are the same account both at
    // signup and login — case-sensitive matching here would be a confusing login-failure
    // trap for users, not a meaningful security boundary.
    const normalizedEmail = String(email).trim().toLowerCase();
    const resolvedUsername = username || `member_${crypto.randomUUID().replace(/-/g, '').slice(0, 12)}`;
    const existing = await db.query('SELECT id FROM users WHERE email = $1 OR username = $2', [normalizedEmail, resolvedUsername]);
    if (existing.rows.length > 0) {
      return res.status(409).json({ error: 'Email or username already exists' });
    }


    const password_hash = await bcrypt.hash(password, 12);
    let result;
    try {
      result = await db.query(
        `INSERT INTO users
           (email, username, password_hash, display_name, phone_number, whatsapp_opt_in,
            preferred_language, birth_date_input, birth_time_input, birth_location_input)
         VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
         RETURNING id, email, username, display_name, role, xp, level, current_stage, preferred_language`,
        [normalizedEmail, resolvedUsername, password_hash, display_name || resolvedUsername, phone || null, false,
          preferred_language || 'en', birth_date || null, birth_time || null,
          birth_location ? String(birth_location).trim().slice(0, 160) : null]
      );
    } catch (insertErr) {
      // Unique-constraint violation: another request won the race between our existence
      // check above and this insert (concurrent signups with the same email/username).
      // Surface it as a normal conflict rather than a raw 500.
      if (insertErr.code === '23505') {
        return res.status(409).json({ error: 'Email or username already exists' });
      }
      throw insertErr;
    }
    const user = result.rows[0];

    // Only compute a real chart when we actually have everything computeNatalChart needs.
    // Previously this seeded a hardcoded dummy chart (Leo/Scorpio/Aries, lat/lon 0,0)
    // whenever birth_date was present alone — that produced fake astrology data for every
    // user who didn't separately call POST /chart/generate (confirmed live: 100 of 101 real
    // users had zero real planet/house data). No lat/lon at registration means no chart row
    // at all now; downstream features must treat a missing chart as "no personalized data
    // yet," not silently invent one. The user can still generate a real chart later via
    // POST /chart/generate once birth location is available.
    if (birth_date && birth_time && utcOffset && isFiniteNumber(lat) && isFiniteNumber(lon)) {
      try {
        const chartData = computeNatalChart({ birthDate: birth_date, birthTime: birth_time, utcOffset, lat, lon });
        const client = await pool.connect();
        try {
          await client.query('BEGIN');
          const chartInsert = await client.query(`
            INSERT INTO user_chart (user_id, birth_date, birth_time, utc_offset, latitude, longitude, sun_sign, moon_sign, rising_sign, moon_phase)
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
            RETURNING id
          `, [user.id, birth_date, birth_time, utcOffset, lat, lon, chartData.sun_sign, chartData.moon_sign, chartData.rising_sign, chartData.moon_phase]);
          const chartId = chartInsert.rows[0].id;
          for (const planet of chartData.planets) {
            await client.query(
              'INSERT INTO planet_positions (chart_id, planet_name, zodiac_sign, degree, house) VALUES ($1, $2, $3, $4, $5)',
              [chartId, planet.name, planet.sign, planet.degree, planet.house]
            );
          }
          for (const house of chartData.houses) {
            await client.query(
              'INSERT INTO houses (chart_id, house_number, zodiac_sign, degree) VALUES ($1, $2, $3, $4)',
              [chartId, house.house, house.sign, house.degree]
            );
          }
          await client.query('COMMIT');
        } catch (dbErr) {
          await client.query('ROLLBACK');
          throw dbErr;
        } finally {
          client.release();
        }
      } catch (err) {
        console.error('Failed to create birth chart at registration', err.message);
      }
    }
    const token = jwt.sign({ id: user.id, email: user.email, role: user.role }, JWT_SECRET, { expiresIn: '7d' });
    
    // Alpha Analytics
    await require('../utils/AnalyticsEngine').logEvent(user.id, 'signup_conversion', { source: 'organic' });

    res.status(201).json({ user, token });
  } catch (err) {
    console.error('Register error:', err);
    res.status(500).json({ error: 'Registration failed' });
  }
});

// Login
// Constant-time-ish placeholder hash used when no user is found, so that
// bcrypt.compare always runs and login response time doesn't leak whether
// an email is registered (timing-based user enumeration).
const DUMMY_PASSWORD_HASH = '$2a$12$CwTycUXWue0Thq9StjUM0uJ8p9tI6ptZ0R9qwj0R0BE0KaSDoixQm';

router.post('/login', authLimiter, async (req, res) => {
  try {
    const { email, password } = req.body;
    if (!email || !password) {
      return res.status(400).json({ error: 'Email and password are required' });
    }
    const normalizedEmail = String(email).trim().toLowerCase();
    const result = await db.query('SELECT * FROM users WHERE email = $1', [normalizedEmail]);
    const user = result.rows[0];
    const valid = await bcrypt.compare(password, user ? user.password_hash : DUMMY_PASSWORD_HASH);
    if (!user || !valid) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }
    const token = jwt.sign({ id: user.id, email: user.email, role: user.role }, JWT_SECRET, { expiresIn: '7d' });
    const { password_hash, ...safeUser } = user;
    res.json({ user: safeUser, token });
  } catch (err) {
    console.error('Login error:', err);
    res.status(500).json({ error: 'Login failed' });
  }
});

module.exports = router;
