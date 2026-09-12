const express = require('express');
const db = require('../db');
const { authenticateToken } = require('../middleware/auth');
const router = express.Router();

// Get billing status
router.get('/status', authenticateToken, async (req, res) => {
  try {
    const result = await db.query(
      'SELECT membership, membership_since, xp FROM users WHERE id = $1',
      [req.user.id]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }
    const user = result.rows[0];
    res.json({
      membership: user.membership || 'trial',
      membership_since: user.membership_since || null,
      experiments_used: user.xp ? Math.floor(user.xp / 25) : 0
    });
  } catch (err) {
    console.error('Get billing status error:', err);
    res.status(500).json({ error: 'Failed to retrieve billing status' });
  }
});

// Subscribe to a plan (Observer or Monk)
router.post('/subscribe', authenticateToken, async (req, res) => {
  try {
    const { plan } = req.body; // 'observer' or 'monk'
    if (!plan || !['observer', 'monk'].includes(plan)) {
      return res.status(400).json({ error: 'Invalid plan selected' });
    }

    await db.query(
      'UPDATE users SET membership = $1, membership_since = NOW(), updated_at = NOW() WHERE id = $2',
      [plan, req.user.id]
    );

    res.json({
      success: true,
      plan,
      message: `Welcome to ${plan === 'monk' ? 'Monk Membership' : 'Observer Membership'}.`
    });
  } catch (err) {
    console.error('Subscribe error:', err);
    res.status(500).json({ error: 'Failed to process subscription' });
  }
});

// Refund (set to free)
router.post('/refund', authenticateToken, async (req, res) => {
  try {
    const userRes = await db.query('SELECT membership FROM users WHERE id = $1', [req.user.id]);
    if (userRes.rows.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }
    const prevPlan = userRes.rows[0].membership;

    await db.query(
      "UPDATE users SET membership = 'free', membership_since = NULL, updated_at = NOW() WHERE id = $1",
      [req.user.id]
    );

    res.json({
      success: true,
      message: `Refund processed. Previous plan was: ${prevPlan || 'none'}`
    });
  } catch (err) {
    console.error('Refund error:', err);
    res.status(500).json({ error: 'Failed to process refund' });
  }
});

module.exports = router;
