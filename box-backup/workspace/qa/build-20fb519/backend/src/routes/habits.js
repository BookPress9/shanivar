const express = require('express');
const db = require('../db');
const { authenticateToken } = require('../middleware/auth');
const router = express.Router();

// Get habits
router.get('/', authenticateToken, async (req, res) => {
  try {
    const habits = await db.query(
      'SELECT * FROM habits WHERE user_id = $1 AND is_active = TRUE ORDER BY created_at',
      [req.user.id]
    );
    // Get today's completions
    const logs = await db.query(
      'SELECT habit_id FROM habit_logs WHERE user_id = $1 AND completed_at = CURRENT_DATE',
      [req.user.id]
    );
    const completedToday = new Set(logs.rows.map(l => l.habit_id));
    const result = habits.rows.map(h => ({
      ...h,
      completed_today: completedToday.has(h.id)
    }));
    res.json(result);
  } catch (err) {
    res.status(500).json({ error: 'Failed to get habits' });
  }
});

// Create habit
router.post('/', authenticateToken, async (req, res) => {
  try {
    const { name, description, frequency, target_count, color } = req.body;
    if (!name) return res.status(400).json({ error: 'Name is required' });
    if (frequency !== undefined && !['daily', 'weekly', 'custom'].includes(frequency)) {
      return res.status(400).json({ error: 'frequency must be one of daily, weekly, custom' });
    }
    if (target_count !== undefined && (typeof target_count !== 'number' || !Number.isInteger(target_count) || target_count < 1)) {
      return res.status(400).json({ error: 'target_count must be a positive integer' });
    }
    const result = await db.query(
      'INSERT INTO habits (user_id, name, description, frequency, target_count, color) VALUES ($1,$2,$3,$4,$5,$6) RETURNING *',
      [req.user.id, name, description, frequency || 'daily', target_count || 1, color || '#c9a84c']
    );
    res.status(201).json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: 'Failed to create habit' });
  }
});

// Log habit completion
router.post('/:id/log', authenticateToken, async (req, res) => {
  try {
    // Authorization: verify the habit actually belongs to the requesting user before logging
    // against it. Without this check any authenticated user could log completions (and, if a
    // matching habit_id happens to exist for another user) create habit_logs rows tied to a
    // habit_id they don't own, purely by guessing/incrementing IDs in the URL param.
    const habit = await db.query(
      'SELECT id FROM habits WHERE id = $1 AND user_id = $2 AND is_active = TRUE',
      [req.params.id, req.user.id]
    );
    if (habit.rows.length === 0) return res.status(404).json({ error: 'Habit not found' });

    const result = await db.query(
      'INSERT INTO habit_logs (habit_id, user_id, notes) VALUES ($1, $2, $3) ON CONFLICT (habit_id, completed_at) DO NOTHING RETURNING *',
      [req.params.id, req.user.id, req.body.notes]
    );
    if (result.rows.length === 0) return res.status(400).json({ error: 'Already logged today' });
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: 'Failed to log habit' });
  }
});

// Delete habit
router.delete('/:id', authenticateToken, async (req, res) => {
  try {
    await db.query('UPDATE habits SET is_active = FALSE WHERE id = $1 AND user_id = $2', [req.params.id, req.user.id]);
    res.json({ message: 'Habit deactivated' });
  } catch (err) {
    res.status(500).json({ error: 'Failed to delete habit' });
  }
});

module.exports = router;
