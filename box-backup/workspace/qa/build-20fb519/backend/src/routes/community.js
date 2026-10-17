const express = require('express');
const db = require('../db');
const { authenticateToken } = require('../middleware/auth');
const { sanitizeText } = require('../utils/sanitize');
const router = express.Router();

// List posts
router.get('/posts', async (req, res) => {
  try {
    const result = await db.query(
      `SELECT cp.*, u.username, u.display_name, u.avatar_url,
        (SELECT COUNT(*) FROM community_replies cr WHERE cr.post_id = cp.id) as reply_count,
        (SELECT json_agg(json_build_object('name', b.name, 'icon', b.icon_name, 'color', b.category)) 
         FROM user_badges ub JOIN badges b ON ub.badge_id = b.id WHERE ub.user_id = u.id) as badges
       FROM community_posts cp JOIN users u ON cp.user_id = u.id ORDER BY cp.created_at DESC LIMIT 50`
    );
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: 'Failed to get posts' });
  }
});

// Get single post with replies
router.get('/posts/:id', async (req, res) => {
  try {
    const post = await db.query(
      `SELECT cp.*, u.username, u.display_name, u.avatar_url,
        (SELECT json_agg(json_build_object('name', b.name, 'icon', b.icon_name, 'color', b.category)) 
         FROM user_badges ub JOIN badges b ON ub.badge_id = b.id WHERE ub.user_id = u.id) as badges
       FROM community_posts cp JOIN users u ON cp.user_id = u.id WHERE cp.id = $1`,
      [req.params.id]
    );
    if (post.rows.length === 0) return res.status(404).json({ error: 'Post not found' });
    const replies = await db.query(
      `SELECT cr.*, u.username, u.display_name, u.avatar_url,
        (SELECT json_agg(json_build_object('name', b.name, 'icon', b.icon_name, 'color', b.category)) 
         FROM user_badges ub JOIN badges b ON ub.badge_id = b.id WHERE ub.user_id = u.id) as badges
       FROM community_replies cr JOIN users u ON cr.user_id = u.id WHERE cr.post_id = $1 ORDER BY cr.created_at`,
      [req.params.id]
    );
    res.json({ post: post.rows[0], replies: replies.rows });
  } catch (err) {
    res.status(500).json({ error: 'Failed to get post' });
  }
});

// Create post
router.post('/posts', authenticateToken, async (req, res) => {
  try {
    const { title, body, category } = req.body;
    if (!body) return res.status(400).json({ error: 'Body is required' });
    const result = await db.query(
      'INSERT INTO community_posts (user_id, title, body, category) VALUES ($1,$2,$3,$4) RETURNING *',
      [req.user.id, sanitizeText(title), sanitizeText(body), category || 'general']
    );

    // Gamification Hook: Community posting
    const GamificationEngine = require('../utils/GamificationEngine');
    const gamifyStats = await GamificationEngine.addXp(req.user.id, 8, 'community');

    res.status(201).json({ post: result.rows[0], gamification: gamifyStats });
  } catch (err) {
    res.status(500).json({ error: 'Failed to create post' });
  }
});

// Reply to post
router.post('/posts/:id/reply', authenticateToken, async (req, res) => {
  try {
    const { body } = req.body;
    if (!body) return res.status(400).json({ error: 'Body is required' });
    const result = await db.query(
      'INSERT INTO community_replies (post_id, user_id, body) VALUES ($1,$2,$3) RETURNING *',
      [req.params.id, req.user.id, sanitizeText(body)]
    );
    
    // Gamification Hook: Community replying
    const GamificationEngine = require('../utils/GamificationEngine');
    const gamifyStats = await GamificationEngine.addXp(req.user.id, 3, 'community');

    res.status(201).json({ reply: result.rows[0], gamification: gamifyStats });
  } catch (err) {
    res.status(500).json({ error: 'Failed to reply' });
  }
});

module.exports = router;
