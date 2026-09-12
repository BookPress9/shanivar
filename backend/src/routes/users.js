const express = require('express');
const db = require('../db');
const { authenticateToken } = require('../middleware/auth');
const router = express.Router();

// Get current user
router.get('/me', authenticateToken, async (req, res) => {
  try {
    const result = await db.query(
      'SELECT id, email, username, display_name, avatar_url, role, xp, level, current_stage, focus_score, awareness_score, discipline_score, resilience_score, creation_score, streak_days, last_practice_date, created_at FROM users WHERE id = $1',
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
    const { display_name, avatar_url } = req.body;
    const result = await db.query(
      'UPDATE users SET display_name = COALESCE($1, display_name), avatar_url = COALESCE($2, avatar_url), updated_at = NOW() WHERE id = $3 RETURNING id, email, username, display_name, avatar_url, role, xp, level',
      [display_name, avatar_url, req.user.id]
    );
    res.json(result.rows[0]);
  } catch (err) {
    console.error('Update user error:', err);
    res.status(500).json({ error: 'Failed to update user' });
  }
});

module.exports = router;
