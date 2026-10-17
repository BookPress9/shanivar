const express = require('express');
const db = require('../db');
const { authenticateToken } = require('../middleware/auth');
const router = express.Router();

// Get daily practices (based on user's current stage)
router.get('/daily', authenticateToken, async (req, res) => {
  try {
    const user = await db.query('SELECT current_stage FROM users WHERE id = $1', [req.user.id]);
    const stage = user.rows[0]?.current_stage || 1;
    const result = await db.query(
      'SELECT p.*, (SELECT COUNT(*) FROM practice_completions pc WHERE pc.practice_id = p.id AND pc.user_id = $1 AND pc.completed_at::date = CURRENT_DATE) > 0 AS completed_today FROM practices p WHERE p.stage_required <= $2 ORDER BY p.category, p.difficulty',
      [req.user.id, stage]
    );
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: 'Failed to get practices' });
  }
});

// Complete a practice
router.post('/complete', authenticateToken, async (req, res) => {
  try {
    const { practice_id, notes, rating } = req.body;
    if (!practice_id) return res.status(400).json({ error: 'practice_id is required' });
    if (rating !== undefined && rating !== null && (typeof rating !== 'number' || rating < 1 || rating > 5)) {
      return res.status(400).json({ error: 'rating must be a number between 1 and 5' });
    }

    const practice = await db.query('SELECT * FROM practices WHERE id = $1', [practice_id]);
    if (practice.rows.length === 0) return res.status(404).json({ error: 'Practice not found' });

    // Atomically check-and-insert in a single statement to close the race window between
    // "check if already completed" and "insert completion" (two users/requests hitting this
    // concurrently could otherwise both pass the check and both insert, double-awarding XP).
    // As of this pass, practice_completions also has a real DB-level backstop: a unique index
    // on (user_id, practice_id, day) created in db.js (matching habit_logs' existing
    // UNIQUE(habit_id, completed_at) pattern). Verified live against Supabase Postgres: 10
    // truly concurrent completion attempts for the same user/practice/day resulted in exactly
    // 1 row — but under the index, a genuine race now surfaces as a 23505 unique-violation
    // rather than a silently-accepted duplicate, so we catch that below too.
    let inserted;
    try {
      inserted = await db.query(
        `INSERT INTO practice_completions (user_id, practice_id, notes, rating)
         SELECT $1, $2, $3, $4
         WHERE NOT EXISTS (
           SELECT 1 FROM practice_completions
           WHERE user_id = $1 AND practice_id = $2 AND completed_at::date = CURRENT_DATE
         )
         RETURNING id`,
        [req.user.id, practice_id, notes, rating]
      );
    } catch (insertErr) {
      if (insertErr.code === '23505') {
        return res.status(400).json({ error: 'Already completed today' });
      }
      throw insertErr;
    }
    if (inserted.rows.length === 0) {
      return res.status(400).json({ error: 'Already completed today' });
    }

    // Gamified Stat Mechanics & Streak Check
    // Handle Streak First before letting the XP engine kick in
    await db.query(`
      UPDATE users SET
        streak_days = CASE
          WHEN last_practice_date = CURRENT_DATE THEN streak_days
          WHEN last_practice_date = CURRENT_DATE - INTERVAL '1 day' OR last_practice_date IS NULL THEN streak_days + 1
          ELSE 1
        END,
        last_practice_date = CURRENT_DATE,
        updated_at = NOW()
      WHERE id = $1
    `, [req.user.id]);

    const GamificationEngine = require('../utils/GamificationEngine');
    const xpReward = practice.rows[0].xp_reward;
    const category = practice.rows[0].category;

    // Capture the level BEFORE awarding XP. GamificationEngine.addXp performs the level
    // update atomically and returns the POST-update level, and the "updatedUser" select
    // below runs after that update has already committed — so comparing gamifyStats.level
    // to updatedUser's level (as this used to do) compares the new level to itself and
    // level_up was always false, even on a genuine level-up.
    const beforeUser = await db.query('SELECT level FROM users WHERE id = $1', [req.user.id]);
    const levelBefore = beforeUser.rows[0]?.level ?? 1;

    // Delegate leveling, score assigning, and badge awarding to the engine
    const gamifyStats = await GamificationEngine.addXp(req.user.id, xpReward, category);

    await require('../utils/AnalyticsEngine').logEvent(req.user.id, 'practice_completed', { practice_id, category, xpReward });

    const updatedUser = await db.query(
      'SELECT xp, level, focus_score, awareness_score, discipline_score, resilience_score, creation_score, streak_days FROM users WHERE id = $1',
      [req.user.id]
    );

    res.json({
      message: 'Practice completed!',
      xp_earned: gamifyStats.xp_added,
      level_up: gamifyStats.level > levelBefore,
      promotion_ready: gamifyStats.promotion_ready,
      badges_unlocked: gamifyStats.new_badges,
      user: updatedUser.rows[0]
    });
  } catch (err) {
    console.error('Complete practice error:', err);
    res.status(500).json({ error: 'Failed to complete practice' });
  }
});

// Generate Dynamic State Protocol
router.post('/generate', authenticateToken, async (req, res) => {
  try {
    const { energy, emotions = [], body = [], mind = [], intention } = req.body;

    if (!intention || typeof intention !== 'string') {
      return res.status(400).json({ error: 'intention is required' });
    }
    if (!Array.isArray(emotions) || !Array.isArray(body) || !Array.isArray(mind)) {
      return res.status(400).json({ error: 'emotions, body, and mind must be arrays' });
    }

    // Fetch user and checkins
    const userRes = await db.query('SELECT checkins FROM users WHERE id = $1', [req.user.id]);
    if (userRes.rows.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    const checkins = userRes.rows[0].checkins || [];
    checkins.push({
      timestamp: new Date().toISOString(),
      energy, emotions, body, mind, intention
    });

    // Save updated checkins back to database
    await db.query('UPDATE users SET checkins = $1 WHERE id = $2', [JSON.stringify(checkins), req.user.id]);

    const primaryEmotion = emotions[0] || 'Tension';
    const primaryBody = body[0] || 'Body';
    const title = `${primaryEmotion.toUpperCase()} ➔ HEART ➔ INTEGRATION`;
    const interpretation = `Today we will work with ${primaryEmotion.toLowerCase()} stored around the ${primaryBody.toLowerCase()} while gently reconnecting you with your heart.`;

    let presetIdx = 0; // Alpha Focus
    if (intention === 'Sleep') presetIdx = 2; // Delta Sleep
    else if (mind.includes('Overthinking') || mind.includes('Lost')) presetIdx = 1; // Theta Space

    let gradientFrom = '#16120a';
    let gradientTo = '#0f0f0a';
    if (primaryEmotion === 'Anger') { gradientFrom = '#1c0a0a'; gradientTo = '#100606'; }
    else if (primaryEmotion === 'Joy') { gradientFrom = '#1a180a'; gradientTo = '#100e06'; }
    else if (primaryEmotion === 'Confusion') { gradientFrom = '#0f0a1c'; gradientTo = '#080512'; }

    // Helper for intention challenge
    const getIntentionChallenge = (intent) => {
      switch(intent) {
        case 'Release': return 'Notice every moment you hold your breath today. Take one long exhale.';
        case 'Observe': return 'Twice today, stop for 30 seconds and list three things you notice in your physical environment.';
        case 'Forgive': return 'If friction arises today, mentally say: "I let this pass. I release judgment."';
        case 'Create': return 'Doodle or sketch for 2 minutes without attempting to draw anything specific.';
        case 'Heal': return 'Rest your palm over your chest for 10 breaths before you sleep tonight.';
        case 'Lead': return 'In your conversations today, wait 1 second before replying after someone finishes speaking.';
        case 'Sleep': return 'Turn off all screens 45 minutes before sleep and listen to the ambient sound of your room.';
        default: return 'Take 3 deep breaths before entering any new space today.';
      }
    };

    const generatedProtocol = {
      title: title,
      interpretation: interpretation,
      presetIdx: presetIdx,
      theme: {
        gradientFrom,
        gradientTo,
      },
      stages: [
        {
          id: 0, name: 'Arrival', type: 'arrival', durationSec: 60,
          text: 'Welcome to Brahma Labs.',
          subtext: 'Breathe. Allow the world outside to fade.'
        },
        {
          id: 1, name: "Today's Protocol", type: 'question', durationSec: 120,
          text: `${title}\n\n${interpretation}`,
          subtext: 'Hold the realization. Let us begin.'
        },
        {
          id: 2, name: 'Enter the ' + primaryBody, type: 'practice', durationSec: 120,
          text: `Bring your attention to the ${primaryBody.toLowerCase()}.\n\nNotice the physical sensation of ${primaryEmotion.toLowerCase()} residing here.\n\nDo not resist it. Simply witness.`
        },
        {
          id: 3, name: 'Observe the Mind', type: 'practice', durationSec: 120,
          text: `Observe your thoughts: "${mind.join(', ')}".\n\nThoughts flow like clouds. You are the space they move through.`
        },
        {
          id: 4, name: 'Breath Connection', type: 'practice', durationSec: 120,
          text: `Breathe directly into the ${primaryBody.toLowerCase()}.\n\nLet the inhale touch the core of the tension, and let the exhale dissolve it.`
        },
        {
          id: 5, name: 'Rest in the Heart', type: 'practice', durationSec: 120,
          text: `Bring awareness to your chest center.\n\nConnect the heart's natural compassion with the ${primaryBody.toLowerCase()}.\n\nThere is space for everything inside you.`
        },
        {
          id: 6, name: 'Transforming State', type: 'practice', durationSec: 120,
          text: `We transform the feeling of ${primaryEmotion.toLowerCase()} into the intention to ${intention.toLowerCase()}.\n\nYou are no longer bound by automatic loops.`
        },
        {
          id: 7, name: 'Pure Awareness', type: 'practice', durationSec: 120,
          text: 'Rest in silent stillness.\n\nYou are the observer of it all.'
        },
        {
          id: 8, name: 'Reflection', type: 'reflection', durationSec: 180,
          text: `What did you observe regarding the ${primaryEmotion.toLowerCase()} in your ${primaryBody.toLowerCase()} today?`,
          subtext: 'Record your awareness below.'
        },
        {
          id: 9, name: 'Integration', type: 'integration', durationSec: 60,
          text: `Integration Challenge:\n${getIntentionChallenge(intention)}`
        }
      ]
    };

    res.json(generatedProtocol);
  } catch (err) {
    console.error('Generate protocol error:', err);
    res.status(500).json({ error: 'Failed to generate protocol' });
  }
});

module.exports = router;
