const express = require('express');
const db = require('../db');
const { authenticateToken } = require('../middleware/auth');
const router = express.Router();

// Get current user
router.get('/me', authenticateToken, async (req, res) => {
  try {
    const result = await db.query(
      'SELECT id, email, username, display_name, avatar_url, role, xp, level, current_stage, focus_score, awareness_score, discipline_score, resilience_score, creation_score, streak_days, last_practice_date, preferred_language, birth_date_input, birth_time_input, birth_location_input, created_at FROM users WHERE id = $1',
      [req.user.id]
    );
    if (result.rows.length === 0) return res.status(404).json({ error: 'User not found' });
    res.json(result.rows[0]);
  } catch (err) {
    console.error('Get user error:', err);
    res.status(500).json({ error: 'Failed to get user' });
  }
});

// Update current user
router.put('/me', authenticateToken, async (req, res) => {
  try {
    const { display_name, avatar_url, preferred_language, birth_date, birth_time, birth_location } = req.body;
    if (preferred_language !== undefined && !['en', 'hi'].includes(preferred_language)) {
      return res.status(400).json({ error: 'preferred_language must be en or hi' });
    }
    if (birth_date !== undefined && birth_date !== null && birth_date !== '' && !/^\d{4}-\d{2}-\d{2}$/.test(birth_date)) {
      return res.status(400).json({ error: 'birth_date must be YYYY-MM-DD' });
    }
    if (birth_time !== undefined && birth_time !== null && birth_time !== '' && !/^([01]\d|2[0-3]):[0-5]\d$/.test(birth_time)) {
      return res.status(400).json({ error: 'birth_time must be HH:MM' });
    }
    const cleanLocation = birth_location === undefined || birth_location === null
      ? null
      : String(birth_location).trim().slice(0, 160);
    const result = await db.query(
      `UPDATE users SET
         display_name = COALESCE($1, display_name), avatar_url = COALESCE($2, avatar_url),
         preferred_language = COALESCE($3, preferred_language),
         birth_date_input = COALESCE($4, birth_date_input),
         birth_time_input = COALESCE($5, birth_time_input),
         birth_location_input = COALESCE($6, birth_location_input), updated_at = NOW()
       WHERE id = $7
       RETURNING id, email, username, display_name, avatar_url, role, xp, level, preferred_language, birth_date_input, birth_time_input, birth_location_input`,
      [display_name, avatar_url, preferred_language,
        birth_date === '' ? null : birth_date,
        birth_time === '' ? null : birth_time,
        cleanLocation || null, req.user.id]
    );
    res.json(result.rows[0]);
  } catch (err) {
    console.error('Update user error:', err);
    res.status(500).json({ error: 'Failed to update user' });
  }
});

module.exports = router;
