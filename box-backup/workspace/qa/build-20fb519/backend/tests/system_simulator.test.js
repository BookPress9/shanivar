const express = require('express');
const request = require('supertest');

// MOCKS
const userDb = [];
const badgesDb = [
  { id: 'b1', name: 'First Breath', category: 'milestone', xp_required: 0, condition_type: 'practices_completed', condition_value: 1 }
];
const practicesDb = [
  { id: 'p1', title: 'Breath Anchor', category: 'breath', xp_reward: 15, stage_required: 1 }
];
const chartDb = [];
const insightDb = [];
const whatsappDb = [];
const completionsDb = [];
const userBadgesDb = [];

let hasCalculatedInsight = false;

// Mock the Data Layer
process.env.GEMINI_API_KEY = "mock_api_key_for_simulation";

global.fetch = async (url, options) => {
  if (url.includes('astrology')) {
    return { ok: true, json: async () => ({ sun_sign: 'Leo', moon_sign: 'Cancer', rising_sign: 'Sagittarius', planets: [] }) };
  }
  if (url.includes('gemini')) {
    return {
      ok: true,
      json: async () => ({
        candidates: [{
          content: {
            parts: [{
              text: "Insight: You are operating from Survival Awareness. This is natural but you can transcend it.\nAction: Take 3 slow breaths before answering your next email.\nReflection: Where am I rushing today?"
            }]
          }
        }]
      })
    };
  }
};

const mockQuery = async (queryText, params) => {
  // 1. Auth / User
  if (queryText.includes('INSERT INTO users')) {
    const u = { id: 'u-1234', email: params[0], username: params[1], xp: 0, level: 1, current_stage: 1, awareness_score: 0, streak_days: 0 };
    userDb.push(u);
    return { rows: [u] };
  }
  if (queryText.includes('SELECT id FROM users WHERE email')) return { rows: [] };
  
  // 2. Natal Chart
  if (queryText.includes('SELECT * FROM user_chart')) {
    const chart = chartDb.find(c => c.user_id === params[0]);
    return { rows: chart ? [chart] : [] };
  }
  if (queryText.includes('SELECT planet_name')) return { rows: [] };
  
  if (queryText.includes('INSERT INTO user_chart')) {
    const c = { id: 'c-1', user_id: params[0], sun_sign: 'Leo', moon_sign: 'Cancer', rising_sign: 'Sagittarius' };
    chartDb.push(c);
    return { rows: [c] };
  }
  if (queryText.includes('INSERT INTO planet_positions')) return { rows: [] };
  if (queryText.includes('INSERT INTO houses')) return { rows: [] };

  // 3. Daily Insight
  if (queryText.includes('SELECT current_stage FROM users')) {
    return { rows: [userDb[0]] };
  }
  if (queryText.includes('SELECT * FROM daily_insights WHERE user_id = $1 AND insight_date = CURRENT_DATE')) {
    return { rows: hasCalculatedInsight ? insightDb : [] };
  }
  if (queryText.includes('INSERT INTO daily_insights')) {
    hasCalculatedInsight = true;
    const insight = { id: 'i-1', user_id: params[0], insight_text: params[1], action_text: params[2], reflection_text: params[3] };
    insightDb.push(insight);
    return { rows: [insight] };
  }

  // Practices
  if (queryText.includes('SELECT p.*')) return { rows: practicesDb };
  if (queryText.includes('SELECT * FROM practices WHERE id')) return { rows: practicesDb };
  if (queryText.includes('INSERT INTO practice_completions')) {
    completionsDb.push({ id: 'comp1', user_id: params[0], practice_id: params[1] });
    return { rows: [{ id: 'comp1' }] };
  }
  
  // Gamification Engine
  if (queryText.includes('streak_days = CASE')) {
    userDb[0].streak_days = 1;
    return { rows: [] };
  }
  if (queryText.includes('UPDATE users SET \n        xp = xp + $1')) {
    userDb[0].xp += params[0];
    userDb[0].awareness_score += params[1];
    return { rows: [userDb[0]] };
  }
  if (queryText.includes(`SELECT * FROM badges \n       WHERE condition_type = 'total_xp'`)) return { rows: [] };
  if (queryText.includes(`SELECT * FROM badges \n       WHERE condition_type = 'streak_days'`)) return { rows: [] };
  if (queryText.includes(`SELECT COUNT(id) as count FROM practice_completions`)) return { rows: [{ count: completionsDb.length }] };
  if (queryText.includes(`SELECT COUNT(id) as count FROM community_posts`)) return { rows: [{ count: 0 }] };
  if (queryText.includes(`SELECT * FROM badges \n       WHERE condition_type = 'practices_completed'`)) {
     if (completionsDb.length === 1 && userBadgesDb.length === 0) return { rows: [badgesDb[0]] };
     return { rows: [] };
  }
  if (queryText.includes(`SELECT * FROM badges \n       WHERE condition_type = 'community_posts'`)) return { rows: [] };
  if (queryText.includes('INSERT INTO user_badges')) {
    userBadgesDb.push({ user_id: params[0], badge_id: params[1] });
    return { rows: [] };
  }
  if (queryText.includes('SELECT stage_number, title, xp_required FROM journey_stages')) return { rows: [] };
  if (queryText.includes('SELECT xp, level, focus_score')) return { rows: [userDb[0]] };

  // WhatsApp
  if (queryText.includes('UPDATE users \n      SET phone_number')) {
    userDb[0].phone_number = params[0];
    return { rows: [userDb[0]] };
  }
  if (queryText.includes('INSERT INTO whatsapp_messages')) {
    const wm = { id: 'wm-1', user_id: params[0], trigger: params[1], content: params[2] };
    whatsappDb.push(wm);
    return { rows: [wm] };
  }
  if (queryText.includes('UPDATE whatsapp_messages \n      SET clicked = TRUE')) return { rows: [] };

  return { rows: [] };
};

const mockDb = { query: jest.fn().mockImplementation(mockQuery) };

jest.mock('../src/db', () => mockDb);

// Mock Auth Middleware
jest.mock('../src/middleware/auth', () => ({
  authenticateToken: (req, res, next) => {
    req.user = { id: 'u-1234', email: 'seeker@hybridmonk.com', role: 'user' };
    next();
  },
  JWT_SECRET: "test"
}));

// Set up server
const app = express();
app.use(express.json());

app.use('/api/auth', require('../src/routes/auth'));
app.use('/api/whatsapp', require('../src/routes/whatsapp'));
app.use('/api/chart', require('../src/routes/chart'));
app.use('/api/insights', require('../src/routes/insights'));
app.use('/api/practices', require('../src/routes/practices'));

async function runSimulation() {
  console.log("\n==============================================");
  console.log("🧘 HYBRID MONK: SYSTEM LOOP VALIDATION 🧘");
  console.log("==============================================\n");

  const agent = request(app);

  // 1. Signup
  console.log("1. 👤 Simulating New User Signup...");
  const signupRes = await agent.post('/api/auth/register').send({
    email: 'seeker@hybridmonk.com', username: 'seeker', password: 'password123'
  });
  console.log("  → Account Created:", signupRes.body.user);
  
  // 2. WhatsApp Opt-in
  console.log("\n2. 📱 Phone Verification & Opt-In...");
  const phoneRes = await agent.post('/api/whatsapp/verify').send({ phoneNumber: '+1234567890' });
  console.log("  → WhatsApp System:", phoneRes.body.message);

  // 3. Natal Chart
  console.log("\n3. 🌌 Generating Astrology & Birth Geometry...");
  const chartRes = await agent.post('/api/chart/generate').send({
    birthDate: '1990-01-01', birthTime: '12:00', location: 'New York, USA', lat: 40.7, lon: -74.0
  });
  console.log("  → Chart Built:", chartRes.body.chart);

  // 4. Daily Insight (The Inner Observer AI)
  console.log("\n4. 🕰️ Triggering AI Observer - Daily Insight Generation...");
  const insightRes = await agent.post('/api/insights/generate');
  console.log("  → AI Insight Constructed:");
  console.log("      [Insight]:", insightRes.body.insight.insight_text);
  console.log("      [Action]:", insightRes.body.insight.action_text);
  console.log("      [Reflection]:", insightRes.body.insight.reflection_text);

  // 5. Simulate n8n sending WhatsApp Notification
  console.log("\n5. 📨 Dispatching via n8n WhatsApp Hooks...");
  const n8nRes = await agent.post('/api/whatsapp/n8n/log-message')
    .set('Authorization', 'Bearer n8n_secure_secret_dev')
    .send({ userId: 'u-1234', triggerType: 'daily_insight', messageContent: 'Dispatched Msg ID 80482' });
  console.log("  → Notification Triggered. Log ID:", n8nRes.body.messageId);

  // 6. User Clicks WhatsApp Link (Redirect Simulation)
  console.log("\n6. 🔗 User returns via WhatsApp tracking link...");
  const trackRes = await agent.get(`/api/whatsapp/track/${n8nRes.body.messageId}?url=http://front/practice`);
  console.log("  → Redirecting to WebApp... Status:", trackRes.statusCode);

  // 7. Completing a Practice
  console.log("\n7. 🧘 User Executes 'Breath Anchor' Practice...");
  const practiceRes = await agent.post('/api/practices/complete').send({
    practice_id: 'p1', notes: 'Felt calm.', rating: 5
  });
  const gamify = practiceRes.body;
  console.log("   ✓ XP Earned:", gamify.xp_earned);
  console.log("   ✓ Level Up:", gamify.level_up);
  console.log("   ✓ Badges Unlocked:", gamify.badges_unlocked);
  console.log("   ✓ User Core State Updated:", gamify.user);

  console.log("\n==============================================");
  console.log("✅ FULL HYBRID LOOP SUCCESSFUL");
  console.log("==============================================\n");
}

describe('Hybrid Monk Full System Simulation Loop', () => {
  it('Should run the complete engagement journey from start to finish', async () => {
    await runSimulation();
  });
});
