const express = require('express');
const db = require('../db');
const { authenticateToken, requireAdmin } = require('../middleware/auth');
const router = express.Router();

// Admin: Get analytics dashboard data
router.get('/', authenticateToken, requireAdmin, async (req, res) => {
  try {
    const totalUsers = await db.query('SELECT COUNT(*) FROM users');
    const activeToday = await db.query(
      "SELECT COUNT(DISTINCT user_id) FROM practice_completions WHERE completed_at::date = CURRENT_DATE"
    );
    const totalCompletions = await db.query('SELECT COUNT(*) FROM practice_completions');
    const avgXP = await db.query('SELECT ROUND(AVG(xp)) as avg_xp FROM users');
    const topPractices = await db.query(
      `SELECT p.title, p.category, COUNT(pc.id) as completions 
       FROM practice_completions pc JOIN practices p ON pc.practice_id = p.id 
       GROUP BY p.id, p.title, p.category ORDER BY completions DESC LIMIT 5`
    );
    const stageDistribution = await db.query(
      'SELECT current_stage, COUNT(*) as count FROM users GROUP BY current_stage ORDER BY current_stage'
    );
    const weeklyActivity = await db.query(
      `SELECT DATE(completed_at) as date, COUNT(*) as completions 
       FROM practice_completions 
       WHERE completed_at > NOW() - INTERVAL '7 days' 
       GROUP BY DATE(completed_at) ORDER BY date`
    );

    res.json({
      total_users: parseInt(totalUsers.rows[0].count),
      active_today: parseInt(activeToday.rows[0].count),
      total_completions: parseInt(totalCompletions.rows[0].count),
      avg_xp: parseInt(avgXP.rows[0].avg_xp || 0),
      top_practices: topPractices.rows,
      stage_distribution: stageDistribution.rows,
      weekly_activity: weeklyActivity.rows
    });
  } catch (err) {
    console.error('Analytics error:', err);
    res.status(500).json({ error: 'Failed to get analytics' });
  }
});

module.exports = router;
