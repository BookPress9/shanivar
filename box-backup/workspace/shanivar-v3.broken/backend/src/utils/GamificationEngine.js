const db = require('../db');

// Centralize gamification mechanics here so they can be triggered from practices, journals, or community
class GamificationEngine {
  
  static async addXp(userId, amount, category = 'awareness') {
    // Defensive guard: this engine is a shared entry point for granting XP (practices, journal,
    // and potentially future callers). Never trust a caller-supplied amount to be a sane
    // positive number — a negative/NaN value here would silently reduce a user's XP while
    // still bumping their category stat (statBump below floors at 1 regardless of sign).
    if (typeof amount !== 'number' || !Number.isFinite(amount) || amount < 0) {
      throw new Error('addXp: amount must be a non-negative finite number');
    }

    // 1. Update basic XP and dynamically calculate Level (Logarithmic scale)
    const scoreField = this.getScoreField(category);
    
    // Using a logarithmic scale for levels to create a steeper progression curve
    const updateQuery = `
      UPDATE users SET 
        xp = xp + $1, 
        level = GREATEST(1, FLOOR(LOG(2, GREATEST(1, xp + $1) / 100.0 + 1)) + 1),
        ${scoreField} = ${scoreField} + $2,
        updated_at = NOW()
      WHERE id = $3
      RETURNING xp, level, current_stage, streak_days, ${scoreField}
    `;
    
    // We give a small bump to the specific category stat
    const statBump = Math.max(1, Math.floor(amount / 5)); 
    const result = await db.query(updateQuery, [amount, statBump, userId]);
    
    const userState = result.rows[0];

    // Log XP Event
    await require('./AnalyticsEngine').logEvent(userId, 'xp_awarded', { amount, category });

    // 2. Check for newly earned badges
    const newBadges = await this.checkAndAwardBadges(userId, userState);

    // 3. Check for Stage Promotion Eligibility
    const promotionEligibility = await this.checkStageEligibility(userState.xp, userState.current_stage);

    return {
      xp_added: amount,
      new_total_xp: userState.xp,
      level: userState.level,
      category_leveled: category,
      new_badges: newBadges,
      promotion_ready: promotionEligibility
    };
  }

  static getScoreField(category) {
    const map = {
      breath: 'awareness_score',
      focus: 'focus_score',
      reflection: 'awareness_score',
      discipline: 'discipline_score',
      creation: 'creation_score',
      journal: 'awareness_score',
      community: 'resilience_score'
    };
    return map[category.toLowerCase()] || 'awareness_score';
  }

  static async checkAndAwardBadges(userId, userState) {
    const newlyEarned = [];

    // Check Total XP badges
    const xpBadgesRes = await db.query(
      `SELECT * FROM badges 
       WHERE condition_type = 'total_xp' AND condition_value <= $1
       AND id NOT IN (SELECT badge_id FROM user_badges WHERE user_id = $2)`,
      [userState.xp, userId]
    );

    // Check Streak badges
    const streakBadgesRes = await db.query(
      `SELECT * FROM badges 
       WHERE condition_type = 'streak_days' AND condition_value <= $1
       AND id NOT IN (SELECT badge_id FROM user_badges WHERE user_id = $2)`,
      [userState.streak_days, userId]
    );

    // Check query-based dynamic counts
    const practiceCountRes = await db.query('SELECT COUNT(id) as count FROM practice_completions WHERE user_id = $1', [userId]);
    const practiceBadgesRes = await db.query(
      `SELECT * FROM badges 
       WHERE condition_type = 'practices_completed' AND condition_value <= $1
       AND id NOT IN (SELECT badge_id FROM user_badges WHERE user_id = $2)`,
      [parseInt(practiceCountRes.rows[0].count), userId]
    );

    const postCountRes = await db.query('SELECT COUNT(id) as count FROM community_posts WHERE user_id = $1', [userId]);
    const postBadgesRes = await db.query(
      `SELECT * FROM badges
       WHERE condition_type = 'community_posts' AND condition_value <= $1
       AND id NOT IN (SELECT badge_id FROM user_badges WHERE user_id = $2)`,
      [parseInt(postCountRes.rows[0].count), userId]
    );

    // Bug found against the live badges table: 'active_habits' (e.g. "Habit Architect") and
    // 'journal_entries' (e.g. "Inner Scribe") condition_type rows exist in production but were
    // never checked here — those badges were permanently unreachable regardless of how many
    // habits/journal entries a user had. Confirmed live: gave a test user 5 active habits and
    // re-ran this method — no badge awarded until this fix.
    const habitCountRes = await db.query('SELECT COUNT(id) as count FROM habits WHERE user_id = $1 AND is_active = TRUE', [userId]);
    const habitBadgesRes = await db.query(
      `SELECT * FROM badges
       WHERE condition_type = 'active_habits' AND condition_value <= $1
       AND id NOT IN (SELECT badge_id FROM user_badges WHERE user_id = $2)`,
      [parseInt(habitCountRes.rows[0].count), userId]
    );

    const journalCountRes = await db.query('SELECT COUNT(id) as count FROM journal_entries WHERE user_id = $1', [userId]);
    const journalBadgesRes = await db.query(
      `SELECT * FROM badges
       WHERE condition_type = 'journal_entries' AND condition_value <= $1
       AND id NOT IN (SELECT badge_id FROM user_badges WHERE user_id = $2)`,
      [parseInt(journalCountRes.rows[0].count), userId]
    );

    // Combine all discovered badges
    let newlyEarnedList = [
      ...xpBadgesRes.rows,
      ...streakBadgesRes.rows,
      ...practiceBadgesRes.rows,
      ...postBadgesRes.rows,
      ...habitBadgesRes.rows,
      ...journalBadgesRes.rows
    ];

    for (let badge of newlyEarnedList) {
      await this.awardBadge(userId, badge.id);
      newlyEarned.push(badge);
    }

    return newlyEarned;
  }

  static async awardBadge(userId, badgeId) {
    try {
      await db.query(
        `INSERT INTO user_badges (user_id, badge_id) VALUES ($1, $2) ON CONFLICT DO NOTHING`,
        [userId, badgeId]
      );
      await require('./AnalyticsEngine').logEvent(userId, 'badge_awarded', { badgeId });
    } catch(e) {
      console.error("Error awarding badge", e);
    }
  }

  static async checkStageEligibility(currentXp, currentStage) {
    const nextStage = await db.query(
      `SELECT stage_number, title, xp_required FROM journey_stages WHERE stage_number = $1`, 
      [currentStage + 1]
    );

    if (nextStage.rows.length > 0) {
      const stage = nextStage.rows[0];
      if (currentXp >= stage.xp_required) {
        return {
          eligible: true,
          next_stage: stage.stage_number,
          stage_name: stage.title
        };
      }
    }
    return { eligible: false };
  }
}

module.exports = GamificationEngine;
