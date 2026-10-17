/**
 * Mock Backend Server for E2E Testing
 * Simulates all API endpoints without a real database
 */
const express = require('express');
const cors = require('cors');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');

const app = express();
const PORT = 5000;
const JWT_SECRET = 'test-secret-key';

// In-memory store
const users = [];
const practices = [
  { id: '1', title: 'Morning Breath Work', category: 'breath', description: 'Start your day with 10 conscious breaths', difficulty: 1, xp_reward: 25, duration_minutes: 5, stage_required: 1 },
  { id: '2', title: 'Focus Block', category: 'focus', description: '25 minutes of undistracted attention', difficulty: 2, xp_reward: 50, duration_minutes: 25, stage_required: 1 },
  { id: '3', title: 'Evening Reflection', category: 'reflection', description: 'Write 3 things you noticed today', difficulty: 1, xp_reward: 30, duration_minutes: 10, stage_required: 1 },
  { id: '4', title: 'Cold Exposure', category: 'discipline', description: '2 minutes cold shower', difficulty: 3, xp_reward: 75, duration_minutes: 2, stage_required: 2 },
  { id: '5', title: 'Creative Expression', category: 'creation', description: 'Create something without judgement', difficulty: 2, xp_reward: 40, duration_minutes: 15, stage_required: 1 },
  { id: 'chakra', title: 'Chakra Meditation', category: 'focus', description: 'Align your 7 energy centers through vocal chanting and color visualization', difficulty: 2, xp_reward: 50, duration_minutes: 14, stage_required: 1 },
  { id: 'vipassana', title: 'Vipassana Awareness', category: 'reflection', description: 'Pure silent observation of bodily sensations and breath flow', difficulty: 3, xp_reward: 60, duration_minutes: 15, stage_required: 1 },
  { id: 'protocol', title: 'Daily Dynamic Protocol', category: 'breath', description: 'Your AI generated consciousness protocol for today', difficulty: 2, xp_reward: 50, duration_minutes: 14, stage_required: 1 },
];
const stages = [
  { stage_number: 1, name: 'Survival', xp_required: 0, description: 'Operate beyond automatic reactions' },
  { stage_number: 2, name: 'Social Identity', xp_required: 500, description: 'Transcend inherited conditioning' },
  { stage_number: 3, name: 'Self Aware', xp_required: 1500, description: 'Awaken the inner observer' },
  { stage_number: 4, name: 'Intentional', xp_required: 3000, description: 'Consciously design your identity' },
  { stage_number: 5, name: 'No-Mind', xp_required: 6000, description: 'Access flow states at will' },
  { stage_number: 6, name: 'Cosmic', xp_required: 10000, description: 'Unity awareness' },
];

app.use(cors({ origin: 'http://localhost:3000', credentials: true }));
app.use(express.json({ limit: '10mb' }));

// Auth middleware
function auth(req, res, next) {
  const h = req.headers['authorization'];
  const token = h && h.split(' ')[1];
  if (!token) return res.status(401).json({ error: 'Access token required' });
  try {
    req.user = jwt.verify(token, JWT_SECRET);
    next();
  } catch { return res.status(403).json({ error: 'Invalid token' }); }
}

// Health
app.get('/api/health', (_, res) => res.json({ status: 'ok', timestamp: new Date().toISOString(), version: '1.0.0-mock' }));

// Register
app.post('/api/auth/register', async (req, res) => {
  const { email, username, password, phone, birth_date, birth_time, birth_location } = req.body;
  if (!email || !username || !password || !phone) {
    return res.status(400).json({ error: 'Email, username, password, and WhatsApp number are required' });
  }
  if (users.find(u => u.email === email || u.username === username)) {
    return res.status(409).json({ error: 'Email or username already exists' });
  }
  const password_hash = await bcrypt.hash(password, 4);
  const user = {
    id: String(users.length + 1),
    email, username, password_hash,
    display_name: username,
    role: 'user',
    xp: 0, level: 1, current_stage: 1,
    focus_score: 5, awareness_score: 3, discipline_score: 2, resilience_score: 4, creation_score: 1,
    streak_days: 0,
    phone_number: phone,
    birth_date, birth_time, birth_location,
    membership: 'trial',
    membership_since: new Date().toISOString()
  };
  users.push(user);
  const token = jwt.sign({ id: user.id, email: user.email, role: user.role }, JWT_SECRET, { expiresIn: '7d' });
  const { password_hash: _, ...safeUser } = user;
  console.log(`[MOCK] Registered user: ${email}`);
  res.status(201).json({ user: safeUser, token });
});

// Login
app.post('/api/auth/login', async (req, res) => {
  const { email, password } = req.body;
  if (!email || !password) return res.status(400).json({ error: 'Email and password are required' });
  const user = users.find(u => u.email === email);
  if (!user) return res.status(401).json({ error: 'Invalid credentials' });
  const valid = await bcrypt.compare(password, user.password_hash);
  if (!valid) return res.status(401).json({ error: 'Invalid credentials' });
  const token = jwt.sign({ id: user.id, email: user.email, role: user.role }, JWT_SECRET, { expiresIn: '7d' });
  const { password_hash: _, ...safeUser } = user;
  console.log(`[MOCK] Login: ${email}`);
  res.json({ user: safeUser, token });
});

// Get current user
app.get('/api/users/me', auth, (req, res) => {
  const user = users.find(u => u.id === req.user.id);
  if (!user) return res.status(404).json({ error: 'User not found' });
  const { password_hash: _, ...safeUser } = user;
  res.json(safeUser);
});

// Update user
app.put('/api/users/me', auth, (req, res) => {
  const user = users.find(u => u.id === req.user.id);
  if (!user) return res.status(404).json({ error: 'User not found' });
  if (req.body.display_name) user.display_name = req.body.display_name;
  if (req.body.avatar_url) user.avatar_url = req.body.avatar_url;
  const { password_hash: _, ...safeUser } = user;
  res.json(safeUser);
});

// Journey stages
app.get('/api/journey/stages', (_, res) => res.json(stages));
app.post('/api/journey/unlock', auth, (req, res) => {
  const user = users.find(u => u.id === req.user.id);
  if (!user) return res.status(404).json({ error: 'User not found' });
  const nextStage = stages.find(s => s.stage_number === user.current_stage + 1);
  if (!nextStage) return res.status(400).json({ error: 'Already at max stage' });
  if (user.xp < nextStage.xp_required) return res.status(400).json({ error: 'Not enough XP' });
  user.current_stage++;
  res.json({ message: `Unlocked Stage ${user.current_stage}`, stage: nextStage });
});

// Daily practices
app.get('/api/practices/daily', auth, (req, res) => {
  const user = users.find(u => u.id === req.user.id);
  const available = practices.filter(p => p.stage_required <= (user?.current_stage || 1));
  res.json(available.map(p => ({ ...p, completed_today: false })));
});

// Helper for dynamic integration challenges based on intention
function getIntentionChallenge(intention) {
  switch(intention) {
    case 'Release': return 'Notice every moment you hold your breath today. Take one long exhale.';
    case 'Observe': return 'Twice today, stop for 30 seconds and list three things you notice in your physical environment.';
    case 'Forgive': return 'If friction arises today, mentally say: "I let this pass. I release judgment."';
    case 'Create': return 'Doodle or sketch for 2 minutes without attempting to draw anything specific.';
    case 'Heal': return 'Rest your palm over your chest for 10 breaths before you sleep tonight.';
    case 'Lead': return 'In your conversations today, wait 1 second before replying after someone finishes speaking.';
    case 'Sleep': return 'Turn off all screens 45 minutes before sleep and listen to the ambient sound of your room.';
    default: return 'Take 3 deep breaths before entering any new space today.';
  }
}

// Generate Dynamic State Protocol
app.post('/api/practices/generate', auth, (req, res) => {
  const user = users.find(u => u.id === req.user.id);
  if (!user) return res.status(404).json({ error: 'User not found' });

  const { energy, emotions = [], body = [], mind = [], intention } = req.body;

  // Track check-in history to populate pattern memory logs
  if (!user.checkins) user.checkins = [];
  user.checkins.push({
    timestamp: new Date().toISOString(),
    energy, emotions, body, mind, intention
  });

  const primaryEmotion = emotions[0] || 'Tension';
  const primaryBody = body[0] || 'Body';
  const title = `${primaryEmotion.toUpperCase()} ➔ HEART ➔ INTEGRATION`;
  const interpretation = `Today we will work with ${primaryEmotion.toLowerCase()} stored around the ${primaryBody.toLowerCase()} while gently reconnecting you with your heart.`;

  // Visual/Audio mappings based on user state
  let presetIdx = 0; // Alpha Focus
  if (intention === 'Sleep') presetIdx = 2; // Delta Sleep
  else if (mind.includes('Overthinking') || mind.includes('Lost')) presetIdx = 1; // Theta Space

  let gradientFrom = '#16120a';
  let gradientTo = '#0f0f0a';
  if (primaryEmotion === 'Anger') { gradientFrom = '#1c0a0a'; gradientTo = '#100606'; }
  else if (primaryEmotion === 'Joy') { gradientFrom = '#1a180a'; gradientTo = '#100e06'; }
  else if (primaryEmotion === 'Confusion') { gradientFrom = '#0f0a1c'; gradientTo = '#080512'; }

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
        text: `Bring awareness to your chest center.\n\nConnect the heart\'s natural compassion with the ${primaryBody.toLowerCase()}.\n\nThere is space for everything inside you.`
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
});

app.post('/api/practices/complete', auth, (req, res) => {
  const user = users.find(u => u.id === req.user.id);
  if (!user) return res.status(404).json({ error: 'User not found' });
  const practice = practices.find(p => p.id === req.body.practice_id);
  if (!practice) return res.status(404).json({ error: 'Practice not found' });
  user.xp += practice.xp_reward;
  user.streak_days += 1;
  res.json({ message: 'Practice completed', xp_earned: practice.xp_reward, total_xp: user.xp });
});

// Gamification
app.get('/api/gamification/stats', auth, (req, res) => {
  const user = users.find(u => u.id === req.user.id);
  res.json({ xp: user?.xp || 0, level: user?.level || 1, streak_days: user?.streak_days || 0, achievements: [] });
});

// Content
app.get('/api/content', (_, res) => res.json([
  { id: '1', title: 'The Nature of Attention', type: 'article', description: 'Why attention is your most valuable resource', duration: '5 min read' },
  { id: '2', title: 'Morning Stillness', type: 'meditation', description: 'A guided meditation for clarity', duration: '10 min' },
]));
app.get('/api/content/:id', (req, res) => res.json({ id: req.params.id, title: 'The Nature of Attention', type: 'article', body: 'Full content here...' }));

// Community
app.get('/api/community/posts', (_, res) => res.json([
  { id: '1', title: 'Day 7 reflection', body: 'Noticing patterns in my reactions...', username: 'SeekingMonk', display_name: 'Seeking Monk', category: 'consciousness', likes: 3, reply_count: 1, created_at: new Date().toISOString() }
]));
app.post('/api/community/posts', auth, (req, res) => res.status(201).json({ id: '99', ...req.body, author: 'TestMonk', created_at: new Date().toISOString() }));
app.get('/api/community/posts/:id', (req, res) => res.json({ id: req.params.id, title: 'Day 7 reflection', body: 'Full post...', replies: [] }));
app.post('/api/community/posts/:id/reply', auth, (req, res) => res.status(201).json({ id: '1', body: req.body.body }));

// Journal
app.get('/api/journal', auth, (_, res) => res.json([]));
app.post('/api/journal', auth, (req, res) => res.status(201).json({ id: '1', ...req.body, created_at: new Date().toISOString() }));
app.delete('/api/journal/:id', auth, (_, res) => res.json({ message: 'Deleted' }));

// Habits
app.get('/api/habits', auth, (_, res) => res.json([]));
app.post('/api/habits', auth, (req, res) => res.status(201).json({ id: '1', ...req.body, streak: 0 }));
app.post('/api/habits/:id/log', auth, (_, res) => res.json({ message: 'Logged' }));
app.delete('/api/habits/:id', auth, (_, res) => res.json({ message: 'Deleted' }));

// AI Observer
app.post('/api/ai/chat', auth, (req, res) => {
  res.json({
    response: `You asked: "${req.body.message}"\n\nThe Inner Observer reflects: Every question carries its own answer. Notice what arose in you as you typed those words — that impulse, that curiosity, that uncertainty. That is the doorway.\n\nWhat do you notice right now, seeker?`,
    source: 'The Inner Observer (mock)'
  });
});
app.post('/api/ai/reset', auth, (_, res) => res.json({ message: 'Conversation reset. A new chapter begins.' }));

// Insights
app.get('/api/insights/today', auth, (_, res) => {
  res.json({
    insight: {
      insight_text: 'Today the cosmos invites you to observe your automatic reactions. Notice, but do not engage. You are the sky, not the weather.',
      action_text: 'Before your first interaction today, take 3 conscious breaths. Let each exhale carry away one assumption.',
      reflection_text: 'What belief are you carrying today that you did not consciously choose?',
      insight_date: new Date().toISOString().split('T')[0]
    }
  });
});

// Analytics
app.get('/api/analytics', auth, (_, res) => res.json({ total_users: 5, active_today: 2, practices_completed: 42 }));

// Chart
app.get('/api/chart', auth, (_, res) => res.json({ sun_sign: 'Leo', moon_sign: 'Scorpio', rising_sign: 'Aries' }));

// Billing
app.post('/api/billing/subscribe', auth, (req, res) => {
  const user = users.find(u => u.id === req.user.id);
  if (!user) return res.status(404).json({ error: 'User not found' });
  const { plan } = req.body; // 'observer' or 'monk'
  user.membership = plan;
  user.membership_since = new Date().toISOString();
  console.log(`[MOCK] ${user.email} subscribed to ${plan}`);
  res.json({ success: true, plan, message: `Welcome to ${plan === 'monk' ? 'Monk Membership' : 'Observer'}.` });
});

app.post('/api/billing/refund', auth, (req, res) => {
  const user = users.find(u => u.id === req.user.id);
  if (!user) return res.status(404).json({ error: 'User not found' });
  const prevPlan = user.membership;
  user.membership = 'free';
  console.log(`[MOCK] Refund processed for ${user.email} (was: ${prevPlan})`);
  res.json({ success: true, message: 'Refund processed. Your consciousness data is preserved.' });
});

app.get('/api/billing/status', auth, (req, res) => {
  const user = users.find(u => u.id === req.user.id);
  res.json({
    membership: user?.membership || 'trial',
    membership_since: user?.membership_since || null,
    experiments_used: user?.xp ? Math.floor(user.xp / 25) : 0
  });
});

app.listen(PORT, () => console.log(`[MOCK] Hybrid Monk Test API on port ${PORT}`));
