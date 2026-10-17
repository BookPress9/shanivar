const express = require('express');
const db = require('../db');
const { authenticateToken } = require('../middleware/auth');
const { sanitizeText } = require('../utils/sanitize');
const router = express.Router();

// Get journal entries
router.get('/', authenticateToken, async (req, res) => {
  try {
    const result = await db.query(
      'SELECT * FROM journal_entries WHERE user_id = $1 ORDER BY created_at DESC',
      [req.user.id]
    );
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: 'Failed to get journal entries' });
  }
});

// Create journal entry
router.post('/', authenticateToken, async (req, res) => {
  try {
    const { title, body, mood, tags } = req.body;
    if (!body) return res.status(400).json({ error: 'Body is required' });
    const result = await db.query(
      'INSERT INTO journal_entries (user_id, title, body, mood, tags) VALUES ($1,$2,$3,$4,$5) RETURNING *',
      [req.user.id, sanitizeText(title), sanitizeText(body), mood, tags || []]
    );

    // Gamification Hook: Journaling is categorized under 'reflection' / 'journal'.
    // XP is capped at one award per day (matching practices/habits) so entries stay
    // unlimited but can't be farmed for infinite XP — only the day's first entry pays out.
    const todayCount = await db.query(
      "SELECT COUNT(*)::int AS count FROM journal_entries WHERE user_id = $1 AND created_at::date = CURRENT_DATE",
      [req.user.id]
    );
    const xpEligible = todayCount.rows[0].count <= 1;
    let gamifyStats = null;
    if (xpEligible) {
      const GamificationEngine = require('../utils/GamificationEngine');
      gamifyStats = await GamificationEngine.addXp(req.user.id, 5, 'journal');
    }

    res.status(201).json({ entry: result.rows[0], gamification: gamifyStats, xp_awarded: xpEligible });
  } catch (err) {
    res.status(500).json({ error: 'Failed to create journal entry' });
  }
});

// Update journal entry
router.put('/:id', authenticateToken, async (req, res) => {
  try {
    const { title, body, mood, tags } = req.body;
    const result = await db.query(
      'UPDATE journal_entries SET title = COALESCE($1, title), body = COALESCE($2, body), mood = COALESCE($3, mood), tags = COALESCE($4, tags), updated_at = NOW() WHERE id = $5 AND user_id = $6 RETURNING *',
      [sanitizeText(title), sanitizeText(body), mood, tags, req.params.id, req.user.id]
    );
    if (result.rows.length === 0) return res.status(404).json({ error: 'Entry not found' });
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: 'Failed to update journal entry' });
  }
});

// Delete journal entry
router.delete('/:id', authenticateToken, async (req, res) => {
  try {
    await db.query('DELETE FROM journal_entries WHERE id = $1 AND user_id = $2', [req.params.id, req.user.id]);
    res.json({ message: 'Deleted' });
  } catch (err) {
    res.status(500).json({ error: 'Failed to delete journal entry' });
  }
});

module.exports = router;
