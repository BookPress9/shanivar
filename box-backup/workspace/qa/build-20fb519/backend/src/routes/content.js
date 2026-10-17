const express = require('express');
const db = require('../db');
const { authenticateToken, optionalAuth, requireAdmin } = require('../middleware/auth');
const router = express.Router();

const PREMIUM_MEMBERSHIPS = new Set(['observer', 'monk']);

// Server-derived access, never trust client-supplied stage/premium claims: looks up the
// caller's real current_stage/membership from the DB (stage 1 / non-premium if logged out).
async function resolveAccess(req) {
  if (!req.user) return { stage: 1, isPremium: false };
  const result = await db.query('SELECT current_stage, membership FROM users WHERE id = $1', [req.user.id]);
  const row = result.rows[0];
  if (!row) return { stage: 1, isPremium: false };
  return {
    stage: row.current_stage || 1,
    isPremium: PREMIUM_MEMBERSHIPS.has(row.membership),
  };
}

// Applies gating without hiding content entirely — locked items still show up (title,
// thumbnail, why it's locked) so progression/upgrade paths stay visible and inviting
// rather than the item just vanishing, which is friendlier UX than a blank 403/404.
function applyGate(item, access) {
  const stageLocked = item.stage_required > access.stage;
  const premiumLocked = item.is_premium && !access.isPremium;
  if (!stageLocked && !premiumLocked) {
    return { ...item, locked: false };
  }
  const { body, media_url, ...teaser } = item;
  return {
    ...teaser,
    body: null,
    media_url: null,
    locked: true,
    lock_reason: stageLocked ? 'stage' : 'premium',
  };
}

// List content
router.get('/', optionalAuth, async (req, res) => {
  try {
    const { type } = req.query;
    let query = 'SELECT * FROM content_items WHERE published = TRUE';
    const params = [];
    if (type) { params.push(type); query += ` AND content_type = $${params.length}`; }
    query += ' ORDER BY created_at DESC';
    const [result, access] = await Promise.all([db.query(query, params), resolveAccess(req)]);
    res.json(result.rows.map((item) => applyGate(item, access)));
  } catch (err) {
    res.status(500).json({ error: 'Failed to get content' });
  }
});

// Get single content item
router.get('/:id', optionalAuth, async (req, res) => {
  try {
    // Only published content is publicly visible (mirrors the /list filter above) -
    // previously this looked content up by id alone, leaking unpublished/draft items.
    const [result, access] = await Promise.all([
      db.query('SELECT * FROM content_items WHERE id = $1 AND published = TRUE', [req.params.id]),
      resolveAccess(req),
    ]);
    if (result.rows.length === 0) return res.status(404).json({ error: 'Content not found' });
    const gated = applyGate(result.rows[0], access);
    // Only count a view once the caller can actually see the real content.
    if (!gated.locked) {
      await db.query('UPDATE content_items SET view_count = view_count + 1 WHERE id = $1', [req.params.id]);
    }
    res.json(gated);
  } catch (err) {
    res.status(500).json({ error: 'Failed to get content' });
  }
});

// Admin: Create content
router.post('/', authenticateToken, requireAdmin, async (req, res) => {
  try {
    const { title, body, content_type, thumbnail_url, media_url, stage_required, is_premium, tags } = req.body;
    const result = await db.query(
      'INSERT INTO content_items (title, body, content_type, thumbnail_url, media_url, stage_required, is_premium, author_id, tags) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9) RETURNING *',
      [title, body, content_type, thumbnail_url, media_url, stage_required || 1, is_premium || false, req.user.id, tags || []]
    );
    res.status(201).json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: 'Failed to create content' });
  }
});

module.exports = router;
