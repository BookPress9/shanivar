const express = require('express');
const router = express.Router();
const pool = require('../db');
const { authenticateToken } = require('../middleware/auth');
const { computeCurrentTransits, summarizeTransitsForPrompt } = require('../utils/astrology');

const GEMINI_API_KEY = process.env.GEMINI_API_KEY;
// gemini-2.0-flash was the old default here and is now deprecated — the live API 404s on
// it and names gemini-3.6-flash as the replacement. Matches reflectionGeneration.js.
const GEMINI_MODEL = process.env.GEMINI_MODEL || 'gemini-3.6-flash';
const GEMINI_API_URL = `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent?key=`;
const GEMINI_TIMEOUT_MS = 15000;
// NOTE: ASTROLOGY_SERVICE_URL previously lived here but was unused dead code — no such
// microservice exists anywhere in this repo. Flagged to the founder rather than guessing
// whether a real transit-data service is planned; see squad report. Removed to avoid
// implying an integration that doesn't exist.

const SYSTEM_INSTRUCTION = `You are a minimalist consciousness guide. You do not use mystical fluff or generic astrology tropes. 
Your tone is direct, calm, and grounded.

You are given a user's natal chart, current planetary transits, and their current consciousness level (1-6) on "The Hybrid Monk" platform.

Analyze planetary transits against their natal chart to identify exactly ONE primary theme to focus on today. 
Themes include: focus, relationships, career discipline, family energy, or self-awareness.

Map this astrological theme into practical Hybrid Monk teachings.

You MUST reply with EXACTLY three labeled sections:
Insight: (1 paragraph identifying the energy and theme)
Action: (1 sentence giving a direct instruction)
Reflection: (1 sentence posing a question for the user to contemplate today)

Do NOT add any other conversational text or markdown formatting outside of these sections.`;

function getCurrentUTCTime() {
  const now = new Date();
  const dateStr = `${now.getUTCFullYear()}/${String(now.getUTCMonth() + 1).padStart(2, '0')}/${String(now.getUTCDate()).padStart(2, '0')}`;
  const timeStr = `${String(now.getUTCHours()).padStart(2, '0')}:${String(now.getUTCMinutes()).padStart(2, '0')}`;
  return { dateStr, timeStr };
}

// POST /generate and GET /today are defined below after the shared generator function


async function generateInsightForUser(userId) {
  const userProm = pool.query('SELECT current_stage, username, display_name, streak_days FROM users WHERE id = $1', [userId]);
  const chartProm = pool.query('SELECT * FROM user_chart WHERE user_id = $1', [userId]);
  const [{ rows: userRows }, { rows: chartRows }] = await Promise.all([userProm, chartProm]);

  const user = userRows[0] || {};
  const currentStage = user.current_stage || 1;
  const stageName = ['', 'Survival Awareness', 'Identity Awareness', 'Emotional Mastery', 'Attention Discipline', 'Flow State', 'Cosmic Perspective'][currentStage];
  const today = new Date().toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long' });

  let chartContext = 'No natal chart available — derive purely from consciousness stage and date energy.';

  if (chartRows.length > 0) {
    const natal = chartRows[0];
    const { rows: natalPlanets } = await pool.query(
      'SELECT planet_name, zodiac_sign, degree, house FROM planet_positions WHERE chart_id = $1', [natal.id]
    );
    chartContext = `Sun: ${natal.sun_sign} | Moon: ${natal.moon_sign} | Rising: ${natal.rising_sign}\nPlanets: ${natalPlanets.map(p => `${p.planet_name} in ${p.zodiac_sign} (House ${p.house})`).join(', ')}`;
  }

  // Real computed transit positions for today (Moshier ephemeris via
  // backend/src/utils/astrology.js) — ground truth for Gemini's interpretive layer,
  // replacing the previous vague "consider the astrological transits for today"
  // instruction that gave it no actual astronomical data to work from.
  const transits = computeCurrentTransits();
  const transitContext = `Today's planetary transits (${today}): ${summarizeTransitsForPrompt(transits)}.`;

  const aiPrompt = `Today is ${today}.
User: ${user.display_name || user.username || 'Seeker'} | Streak: ${user.streak_days || 0} days
Stage ${currentStage} — ${stageName}

Natal Chart:
${chartContext}

${transitContext}

Provide today's Hybrid Monk daily note. Be precise, personal, and grounded. No mystical fluff.`;

  const timeoutController = new AbortController();
  const timeoutHandle = setTimeout(() => timeoutController.abort(), GEMINI_TIMEOUT_MS);
  let geminiRes;
  try {
    geminiRes = await fetch(`${GEMINI_API_URL}${GEMINI_API_KEY}`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        system_instruction: { parts: [{ text: SYSTEM_INSTRUCTION }] },
        contents: [{ role: 'user', parts: [{ text: aiPrompt }] }],
        // maxOutputTokens must cover BOTH the model's internal "thinking" tokens and the
        // visible answer — verified live that gemini-3.6-flash can spend 300+ tokens on
        // invisible reasoning before writing a single visible word, silently truncating
        // the labeled Insight/Action/Reflection output well before "Action:" ever appears
        // (finishReason: MAX_TOKENS, candidatesTokenCount in the single digits). A
        // thinkingConfig.thinkingBudget override was tried to suppress this but this model
        // rejects that field with 400 INVALID_ARGUMENT, so the budget is raised instead.
        generationConfig: { temperature: 0.72, maxOutputTokens: 2000 }
      }),
      signal: timeoutController.signal
    });
  } catch (fetchErr) {
    if (fetchErr.name === 'AbortError') {
      throw new Error(`Gemini generation timed out after ${GEMINI_TIMEOUT_MS}ms`);
    }
    throw fetchErr;
  } finally {
    clearTimeout(timeoutHandle);
  }

  if (!geminiRes.ok) throw new Error('Gemini generation failed');

  const data = await geminiRes.json();
  const aiText = data.candidates?.[0]?.content?.parts?.[0]?.text || '';

  const insightMatch = aiText.match(/Insight:\s*([\s\S]*?)(?=Action:|$)/i);
  const actionMatch  = aiText.match(/Action:\s*([\s\S]*?)(?=Reflection:|$)/i);
  const reflMatch    = aiText.match(/Reflection:\s*([\s\S]*?)$/i);

  const insightText    = insightMatch ? insightMatch[1].trim() : aiText;
  const actionText     = actionMatch  ? actionMatch[1].trim()  : 'Return to the breath for two minutes.';
  const reflectionText = reflMatch    ? reflMatch[1].trim()    : 'Where did you lose yourself today?';

  const { rows } = await pool.query(
    `INSERT INTO daily_insights (user_id, insight_text, action_text, reflection_text)
     VALUES ($1, $2, $3, $4) RETURNING *`,
    [userId, insightText, actionText, reflectionText]
  );
  await require('../utils/AnalyticsEngine').logEvent(userId, 'insight_generated_success', { stage: currentStage });
  return rows[0];
}

// ─── GET /today — returns today's insight, auto-generates if needed ────────────
router.get('/today', authenticateToken, async (req, res) => {
  try {
    const { rows } = await pool.query(
      'SELECT * FROM daily_insights WHERE user_id = $1 AND insight_date = CURRENT_DATE ORDER BY created_at DESC LIMIT 1',
      [req.user.id]
    );
    if (rows.length > 0) return res.json({ insight: rows[0] });

    // None yet today — auto-generate
    if (!GEMINI_API_KEY) {
      return res.json({ insight: getFallbackInsight() });
    }
    const insight = await generateInsightForUser(req.user.id);
    res.json({ insight });
  } catch (error) {
    console.error('GET /insights/today error, returning fallback:', error.message);
    res.json({ insight: getFallbackInsight() });
  }
});

// ─── POST /generate — explicit regeneration ────────────────────────────────────
router.post('/generate', authenticateToken, async (req, res) => {
  const userId = req.user.id;
  try {
    const { rows: existing } = await pool.query(
      'SELECT * FROM daily_insights WHERE user_id = $1 AND insight_date = CURRENT_DATE', [userId]
    );
    if (existing.length > 0) return res.json({ message: 'Insight already generated today', insight: existing[0] });
    if (!GEMINI_API_KEY) {
      return res.status(201).json({ message: 'Daily insight generated (fallback mode)', insight: getFallbackInsight() });
    }
    const insight = await generateInsightForUser(userId);
    res.status(201).json({ message: 'Daily insight generated', insight });
  } catch (error) {
    console.error('POST /insights/generate error, returning fallback:', error.message);
    await require('../utils/AnalyticsEngine').logEvent(userId, 'insight_generated_error', { error: error.message });
    res.status(201).json({ message: 'Daily insight generated (fallback mode)', insight: getFallbackInsight() });
  }
});

function getFallbackInsight() {
  return {
    id: '00000000-0000-0000-0000-000000000000',
    insight_text: "The Inner Observer is quiet today. Look past the chatter of the mind and notice the vast space of awareness that holds it all.",
    action_text: "Take three slow, conscious breaths before responding to any external trigger.",
    reflection_text: "What is the primary distraction drawing you away from presence today?",
    insight_date: new Date().toISOString().split('T')[0],
    created_at: new Date().toISOString()
  };
}

module.exports = router;

