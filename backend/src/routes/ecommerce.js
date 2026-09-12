const express = require('express');
const router = express.Router();
// db.js exports { query, pool } - the raw pg Pool (with .connect()) lives at .pool,
// not at the top level. Requiring '../db' directly and calling .connect() on it
// (the old code did `const pool = require('../db')`) throws "pool.connect is not
// a function" on every request, since the module object has no .connect method.
const { pool } = require('../db');

// Real-DB schema drift found by testing this route against the live Supabase database:
// `users.tailoring_pipeline_status` didn't exist at all, and neither did the
// `user_measurements` table this webhook reads/writes — both queries below threw
// "column/relation does not exist" on every real call (rolled back cleanly, but the
// webhook could never actually succeed). Self-heal on module load, idempotent, mirroring
// the same pattern db.js already uses for `checkins`/`membership`.
if (pool && typeof pool.query === 'function') {
  pool.query(`
    ALTER TABLE users ADD COLUMN IF NOT EXISTS tailoring_pipeline_status VARCHAR(50) DEFAULT 'pending';
    CREATE TABLE IF NOT EXISTS user_measurements (
      id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
      user_id UUID REFERENCES users(id) ON DELETE CASCADE,
      order_number VARCHAR(255) UNIQUE NOT NULL,
      status VARCHAR(50),
      created_at TIMESTAMPTZ DEFAULT NOW(),
      updated_at TIMESTAMPTZ DEFAULT NOW()
    );
  `).catch((err) => {
    console.error('❌ Failed to ensure ecommerce webhook schema (tailoring_pipeline_status / user_measurements):', err.message);
  });
}

// Basic webhook verification middleware
const verifyWebhook = (req, res, next) => {
  const secret = req.headers['x-webhook-secret'];
  const expectedSecret = process.env.SHOPIFY_WEBHOOK_SECRET;
  if (expectedSecret) {
    // A secret is configured: always enforce it, in every environment.
    if (secret !== expectedSecret) {
      return res.status(401).json({ error: 'Unauthorized webhook call' });
    }
  } else if (process.env.NODE_ENV === 'production') {
    // No secret configured at all while running in production: fail closed
    // rather than silently accepting unauthenticated webhook calls.
    return res.status(401).json({ error: 'Unauthorized webhook call' });
  }
  next();
};

/**
 * POST /api/ecommerce/webhook
 * Receives physical garment tracking updates from n8n (Shopify).
 * Updates user tailoring pipeline and awards Gamified XP.
 */
router.post('/webhook', verifyWebhook, async (req, res) => {
  const { user_id, order_number, status, xp_reward = 0 } = req.body;

  if (!user_id || !order_number || !status) {
    return res.status(400).json({ error: 'Missing required fields' });
  }

  let client;
  try {
    client = await pool.connect();
  } catch (connErr) {
    // This previously ran outside any try/catch, so a connection failure here
    // was an unhandled promise rejection that never sent a response.
    console.error('Webhook DB connection error:', connErr);
    return res.status(500).json({ error: 'Server error processing webhook' });
  }

  try {
    await client.query('BEGIN');

    // 1. Update the user's tailoring pipeline status
    await client.query(
      `UPDATE users 
       SET tailoring_pipeline_status = $1, updated_at = NOW() 
       WHERE id = $2`,
      [status, user_id]
    );

    // 2. Update or insert the user measurement tracking status
    // Assuming n8n might pass measurements occasionally, but primarily updates status
    const measureCheck = await client.query(
      `SELECT id FROM user_measurements WHERE order_number = $1`,
      [order_number]
    );

    if (measureCheck.rows.length > 0) {
      await client.query(
        `UPDATE user_measurements SET status = $1, updated_at = NOW() WHERE order_number = $2`,
        [status, order_number]
      );
    } else {
      // If we don't have measurements yet, just create the tracker
      await client.query(
        `INSERT INTO user_measurements (user_id, order_number, status) VALUES ($1, $2, $3)`,
        [user_id, order_number, status]
      );
    }

    // 3. Award Gamification XP for the e-commerce purchase/update
    if (xp_reward > 0) {
      const userRes = await client.query(
        `UPDATE users 
         SET xp = xp + $1, 
             creation_score = creation_score + $1 
         WHERE id = $2 
         RETURNING xp, level`,
        [xp_reward, user_id]
      );

      const user = userRes.rows[0];
      if (user) {
        // Level up logic (every 1000 XP)
        const newLevel = Math.floor(user.xp / 1000) + 1;
        if (newLevel > user.level) {
          await client.query(
            `UPDATE users SET level = $1 WHERE id = $2`,
            [newLevel, user_id]
          );
        }
      }
    }

    await client.query('COMMIT');
    res.json({ success: true, message: `Tailoring pipeline updated to ${status} for user ${user_id}` });
  } catch (error) {
    await client.query('ROLLBACK');
    console.error('Webhook error:', error);
    res.status(500).json({ error: 'Server error processing webhook' });
  } finally {
    client.release();
  }
});

module.exports = router;
