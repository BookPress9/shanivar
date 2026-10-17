const express = require('express');
const db = require('../db');
const { authenticateToken } = require('../middleware/auth');
const router = express.Router();

// Get all stages
router.get('/stages', async (req, res) => {
  try {
    const result = await db.query('SELECT * FROM journey_stages ORDER BY stage_number');
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: 'Failed to get stages' });
  }
});

// Unlock a stage
router.post('/unlock', authenticateToken, async (req, res) => {
  try {
    const user = await db.query('SELECT xp, current_stage FROM users WHERE id = $1', [req.user.id]);
    if (user.rows.length === 0) return res.status(404).json({ error: 'User not found' });

    const { xp, current_stage } = user.rows[0];
    const nextStage = current_stage + 1;
    const stage = await db.query('SELECT * FROM journey_stages WHERE stage_number = $1', [nextStage]);

    if (stage.rows.length === 0) return res.status(400).json({ error: 'No more stages to unlock' });
    if (xp < stage.rows[0].xp_required) {
      return res.status(400).json({ error: 'Not enough XP', required: stage.rows[0].xp_required, current: xp });
    }

    await db.query('UPDATE users SET current_stage = $1, updated_at = NOW() WHERE id = $2', [nextStage, req.user.id]);
    
    // Check if there is a badge for reaching this stage and grant it
    const badgeRes = await db.query(
      `SELECT id FROM badges WHERE condition_type = 'stage_reached' AND condition_value = $1`,
      [nextStage]
    );

    let newBadge = null;
    if (badgeRes.rows.length > 0) {
      try {
        await db.query(`INSERT INTO user_badges (user_id, badge_id) VALUES ($1, $2) ON CONFLICT DO NOTHING`, [req.user.id, badgeRes.rows[0].id]);
        const b = await db.query(`SELECT * FROM badges WHERE id = $1`, [badgeRes.rows[0].id]);
        newBadge = b.rows[0];
      } catch(e) {}
    }

    res.json({ message: 'Stage unlocked!', stage: stage.rows[0], unlocked_badge: newBadge });
  } catch (err) {
    res.status(500).json({ error: 'Failed to unlock stage' });
  }
});

module.exports = router;
