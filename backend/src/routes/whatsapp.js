const express = require('express');
const router = express.Router();
const pool = require('../db');
const { authenticateToken } = require('../middleware/auth');

// 1. User verification & Opt-In
router.post('/verify', authenticateToken, async (req, res) => {
  const { phoneNumber } = req.body;
  const userId = req.user.id;

  if (!phoneNumber) {
    return res.status(400).json({ error: "Phone number is required." });
  }

  // Simulated Twilio verify hook
  try {
    const query = `
      UPDATE users 
      SET phone_number = $1, phone_verified = TRUE, whatsapp_opt_in = TRUE, updated_at = NOW() 
      WHERE id = $2 RETURNING id, phone_number, whatsapp_opt_in
    `;
    const result = await pool.query(query, [phoneNumber, userId]);

    if(result.rows.length === 0) {
      return res.status(404).json({ error: "User not found" });
    }

    res.json({ message: "WhatsApp verified and opted in.", user: result.rows[0] });

  } catch (err) {
    console.error("WhatsApp verify error:", err);
    if (err.code === '23505') { // unique_violation
      return res.status(400).json({ error: "Phone number already in use." });
    }
    res.status(500).json({ error: "Failed to verify phone number" });
  }
});

router.post('/opt-out', authenticateToken, async (req, res) => {
  try {
    await pool.query('UPDATE users SET whatsapp_opt_in = FALSE, updated_at = NOW() WHERE id = $1', [req.user.id]);
    res.json({ message: "Successfully opted out of WhatsApp messages." });
  } catch (err) {
    res.status(500).json({ error: "Failed to opt out" });
  }
});

// 2. n8n Internal Hooks
// No hardcoded fallback secret - a well-known default ('n8n_secure_secret_dev')
// baked into the source would let anyone authenticate as n8n on any deployment
// that forgot to set N8N_SECRET.
const N8N_SECRET = process.env.N8N_SECRET;

function authorizeN8n(req, res, next) {
  if (!N8N_SECRET) {
    // Fail closed in production if no secret has been configured at all.
    if (process.env.NODE_ENV === 'production') {
      return res.status(401).json({ error: "Unauthorized n8n request" });
    }
    // Dev/test convenience only: no secret configured outside production.
    return next();
  }
  const authHeader = req.headers.authorization;
  if (!authHeader || authHeader !== `Bearer ${N8N_SECRET}`) {
    return res.status(401).json({ error: "Unauthorized n8n request" });
  }
  next();
}

router.get('/n8n/users/daily-insight', authorizeN8n, async (req, res) => {
  try {
    const query = `
      SELECT u.id, u.phone_number, u.current_stage
      FROM users u
      LEFT JOIN whatsapp_messages w ON u.id = w.user_id 
        AND w.trigger_type = 'daily_insight' 
        AND DATE(w.sent_at) = CURRENT_DATE
      WHERE u.whatsapp_opt_in = TRUE 
        AND u.phone_verified = TRUE
        AND w.id IS NULL
    `;
    const { rows } = await pool.query(query);
    res.json({ users: rows });
  } catch (err) {
    res.status(500).json({ error: "Query failed" });
  }
});

router.post('/n8n/log-message', authorizeN8n, async (req, res) => {
  const { userId, triggerType, messageContent, status } = req.body;
  
  try {
    const query = `
      INSERT INTO whatsapp_messages (user_id, trigger_type, message_content, status)
      VALUES ($1, $2, $3, $4) RETURNING id
    `;
    const { rows } = await pool.query(query, [userId, triggerType, messageContent, status || 'sent']);

    await require('../utils/AnalyticsEngine').logEvent(userId, 'whatsapp_message_delivered', { triggerType });

    res.status(201).json({ messageId: rows[0].id });
  } catch(err) {
    res.status(500).json({ error: "Failed to log message" });
  }
});

// Hosts we allow redirecting to. Anything else (or an unparsable url) falls
// back to our own frontend, so this endpoint can't be used as an open redirect
// for phishing (e.g. .../track/123?url=https://evil.example.com).
const ALLOWED_REDIRECT_HOSTS = new Set(
  [
    process.env.FRONTEND_URL,
    'http://localhost:3000',
    'https://shanivar.com',
    'https://www.shanivar.com',
    'https://thehybridmonk.com',
    'https://www.thehybridmonk.com'
  ]
    .filter(Boolean)
    .map((u) => {
      try {
        return new URL(u).host;
      } catch {
        return null;
      }
    })
    .filter(Boolean)
);

function resolveSafeRedirect(rawUrl) {
  const fallback = process.env.FRONTEND_URL || 'http://localhost:3000/dashboard';
  if (!rawUrl) return fallback;
  // A same-site relative path is always safe.
  if (rawUrl.startsWith('/')) return rawUrl;
  try {
    const parsed = new URL(rawUrl);
    if (ALLOWED_REDIRECT_HOSTS.has(parsed.host)) return rawUrl;
  } catch {
    // Not a valid absolute URL - fall through to the safe default.
  }
  return fallback;
}

router.get('/track/:messageId', async (req, res) => {
  const { messageId } = req.params;
  const redirectUrl = resolveSafeRedirect(req.query.url);

  try {
    const query = `
      UPDATE whatsapp_messages 
      SET clicked = TRUE, status = 'clicked', updated_at = NOW() 
      WHERE id = $1 RETURNING user_id
    `;
    const clickRes = await pool.query(query, [messageId]);
    
    if (clickRes.rows.length > 0) {
      await require('../utils/AnalyticsEngine').logEvent(clickRes.rows[0].user_id, 'whatsapp_link_click', { messageId });
    }

    res.redirect(redirectUrl);
  } catch (err) {
    console.error(err);
    res.redirect(redirectUrl);
  }
});

module.exports = router;
