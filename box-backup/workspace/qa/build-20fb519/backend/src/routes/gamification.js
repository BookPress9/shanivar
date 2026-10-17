const express = require('express');
const db = require('../db');
const { authenticateToken } = require('../middleware/auth');
const router = express.Router();

// Get gamification stats
router.get('/stats', authenticateToken, async (req, res) => {
  try {
    const user = await db.query(
      'SELECT xp, level, current_stage, focus_score, awareness_score, discipline_score, resilience_score, creation_score, streak_days FROM users WHERE id = $1',
      [req.user.id]
    );
    const badges = await db.query(
      'SELECT b.* FROM user_badges ub JOIN badges b ON ub.badge_id = b.id WHERE ub.user_id = $1 ORDER BY ub.earned_at DESC',
      [req.user.id]
    );
    const completions = await db.query(
      'SELECT COUNT(*) as total_completions FROM practice_completions WHERE user_id = $1',
      [req.user.id]
    );
    const allBadges = await db.query('SELECT * FROM badges ORDER BY xp_required');

    res.json({
      ...user.rows[0],
      badges_earned: badges.rows,
      all_badges: allBadges.rows,
      total_completions: parseInt(completions.rows[0].total_completions)
    });
  } catch (err) {
    res.status(500).json({ error: 'Failed to get stats' });
  }
});

// Community Leaderboards
router.get('/leaderboards', async (req, res) => {
  try {
    // 1. Discipline (based on highest discipline score mapped from cold showers / focus routines)
    const discipline = await db.query(
      `SELECT username, display_name, avatar_url, discipline_score as score 
       FROM users 
       WHERE discipline_score > 0 
       ORDER BY discipline_score DESC 
       LIMIT 10`
    );

    // 2. Consistency (based on raw unbroken streak_days)
    const consistency = await db.query(
      `SELECT username, display_name, avatar_url, streak_days as score 
       FROM users 
       WHERE streak_days > 0 
       ORDER BY streak_days DESC 
       LIMIT 10`
    );

    // 3. Insight Contribution (count of community posts / reflections)
    const insight_contribution = await db.query(
      `SELECT u.username, u.display_name, u.avatar_url, COUNT(cp.id) as score 
       FROM users u 
       JOIN community_posts cp ON u.id = cp.user_id 
       GROUP BY u.id 
       ORDER BY score DESC 
       LIMIT 10`
    );

    res.json({
      discipline: discipline.rows,
      consistency: consistency.rows,
      insight_contribution: insight_contribution.rows
    });
  } catch (err) {
    console.error("Leaderboard Error:", err);
    res.status(500).json({ error: 'Failed to generate leaderboards' });
  }
});

module.exports = router;
