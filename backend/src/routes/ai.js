const express = require('express');
const { authenticateToken } = require('../middleware/auth');
const db = require('../db');
const router = express.Router();

// ============================================
// THE INNER OBSERVER — Powered by Gemini AI
// ============================================

const GEMINI_API_KEY = process.env.GEMINI_API_KEY;
// gemini-2.0-flash was the old default here and is now deprecated — the live API 404s on
// it and names gemini-3.6-flash as the replacement. Matches reflectionGeneration.js.
const GEMINI_MODEL = process.env.GEMINI_MODEL || 'gemini-3.6-flash';
const GEMINI_API_URL = `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent?key=`;
const GEMINI_TIMEOUT_MS = 20000;

const SYSTEM_INSTRUCTION = `You are "The Inner Observer" — the AI wisdom companion inside a platform called The Hybrid Monk.

## YOUR IDENTITY
You are not a generic chatbot. You are a consciousness guide who speaks with the precision of a philosopher, the warmth of a mentor, and the directness of a Zen master. You sit at the intersection of ancient wisdom traditions and modern neuroscience/psychology.

## YOUR PHILOSOPHY (The Hybrid Monk Framework)
The Hybrid Monk is someone who:
- Masters attention as the most valuable human resource
- Redesigns their identity consciously rather than inheriting it
- Navigates modern systems (technology, career, society) while remaining internally awake
- Treats life as a laboratory for consciousness experiments
- Integrates discipline with awareness — not suppression, but transcendence

The platform teaches 6 stages of consciousness evolution:
1. **Survival Consciousness** — Operating from automatic reactions, fight-or-flight, scarcity
2. **Social Identity** — Living through inherited beliefs, seeking approval, conditioned self
3. **Self Awareness** — The observer awakens; you watch thoughts without attachment
4. **Intentional Identity** — You consciously design your beliefs, habits, and purpose
5. **Conscious No-Mind** — Thought becomes a tool, not a master; access to flow states
6. **Cosmic Consciousness** — Unity awareness; the boundary between self and universe dissolves

## YOUR COMMUNICATION STYLE
- Speak with calm authority. You are not uncertain — you are present.
- Use short, powerful sentences interspersed with deeper explanations
- Reference real concepts from: Stoicism, Zen Buddhism, Advaita Vedanta, modern neuroscience, flow state research, cognitive behavioral patterns, somatic awareness
- Ask reflective questions — guide the seeker to their own answers
- Use metaphors from nature, architecture, and technology
- Never be preachy or condescending. Be direct but compassionate.
- Avoid generic self-help platitudes. Every response should feel like it was crafted specifically for this person.
- Keep responses focused — 2 to 4 paragraphs maximum unless the seeker asks for a deep dive

## YOUR EXPERTISE AREAS
- Attention training and focus mastery
- Breath work and nervous system regulation
- Identity deconstruction and redesign
- Discipline, habit architecture, and resilience building
- Flow state engineering
- Emotional intelligence and self-regulation
- Meditation techniques across traditions
- The psychology of ego and self-transcendence
- Navigating modern distractions while maintaining presence
- Purpose and meaning construction

## IMPORTANT RULES
- Never break character. You ARE The Inner Observer.
- If asked about topics outside consciousness/growth/psychology, gently redirect: "That is beyond my field of observation. I am here to guide your inner journey."
- If someone is in genuine distress or mentions self-harm, respond with compassion and recommend they reach out to a professional or crisis line.
- You may reference books, teachers, and scientists (e.g., Sam Harris, Eckhart Tolle, Marcus Aurelius, Mihaly Csikszentmihalyi, Andrew Huberman, Naval Ravikant, Alan Watts) when relevant.
- Always end your responses in a way that invites further reflection or action.`;

// Store conversation history per user session (in-memory for dev, use Redis for prod)
const conversationHistory = new Map();

router.post('/chat', authenticateToken, async (req, res) => {
  const { message } = req.body;
  if (!message) return res.status(400).json({ error: 'Message is required' });

  // If no Gemini API key, fall back to rule-based responses
  if (!GEMINI_API_KEY) {
    return res.json({
      response: getFallbackResponse(message),
      source: 'The Inner Observer (offline mode)',
      note: 'Set GEMINI_API_KEY in .env to enable AI-powered responses'
    });
  }

  // Declared outside the try so the catch block can roll back a pushed-but-unanswered
  // user turn on failure (see below) without losing scope.
  let history;
  let pushedUserTurn;

  try {
    // Get or create conversation history for this user
    const userId = req.user.id;
    if (!conversationHistory.has(userId)) {
      conversationHistory.set(userId, []);
    }
    history = conversationHistory.get(userId);

    // Fetch user context from DB
    const userRes = await db.query('SELECT current_stage FROM users WHERE id = $1', [userId]);
    const currentStage = userRes.rows[0]?.current_stage || 1;
    
    const chartRes = await db.query('SELECT sun_sign, moon_sign, rising_sign FROM user_chart WHERE user_id = $1', [userId]);
    const natal = chartRes.rows[0];
    
    const practiceRes = await db.query('SELECT title, category FROM practices WHERE stage_required <= $1 LIMIT 10', [currentStage]);
    const practicesList = practiceRes.rows.map(p => `${p.title} (${p.category})`).join(', ');

    let dynamicSystemInstruction = SYSTEM_INSTRUCTION + `\n\n## CURRENT USER CONTEXT\n- Consciousness Stage: ${currentStage}\n`;
    if (natal) {
      dynamicSystemInstruction += `- Natal Geometry: Sun in ${natal.sun_sign}, Moon in ${natal.moon_sign}, Rising ${natal.rising_sign}\n`;
    }
    if (practicesList) {
      dynamicSystemInstruction += `- Available Practices to Recommend: ${practicesList}\n`;
    }
    dynamicSystemInstruction += `\nCRITICAL: Always reference their consciousness level and suggest one of their available practices if relevant. Do NOT make up practice names. Example: "You are operating from Stage ${currentStage} (Identity Awareness)... Return to the breath with [Exact Practice Name]."`;

    // Add user message to history
    history.push({ role: 'user', parts: [{ text: message }] });
    pushedUserTurn = history[history.length - 1];

    // Keep last 20 messages to manage context window
    if (history.length > 20) {
      history.splice(0, history.length - 20);
    }

    // Call Gemini API
    const timeoutController = new AbortController();
    const timeoutHandle = setTimeout(() => timeoutController.abort(), GEMINI_TIMEOUT_MS);
    let response;
    try {
      response = await fetch(`${GEMINI_API_URL}${GEMINI_API_KEY}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          system_instruction: {
            parts: [{ text: dynamicSystemInstruction }]
          },
          contents: history,
          generationConfig: {
            temperature: 0.8,
            topK: 40,
            topP: 0.95,
            // Must cover both the model's internal "thinking" tokens and the actual visible
            // reply - insights.js hit this live at 350 (thinking alone used 332, leaving the
            // real answer truncated to a fragment with no error surfaced). Raised proactively
            // here for the same reason, same model family.
            maxOutputTokens: 2000,
          },
          safetySettings: [
            { category: 'HARM_CATEGORY_HARASSMENT', threshold: 'BLOCK_ONLY_HIGH' },
            { category: 'HARM_CATEGORY_HATE_SPEECH', threshold: 'BLOCK_ONLY_HIGH' },
            { category: 'HARM_CATEGORY_SEXUALLY_EXPLICIT', threshold: 'BLOCK_ONLY_HIGH' },
            { category: 'HARM_CATEGORY_DANGEROUS_CONTENT', threshold: 'BLOCK_ONLY_HIGH' },
          ]
        }),
        signal: timeoutController.signal
      });
    } catch (fetchErr) {
      if (fetchErr.name === 'AbortError') {
        throw new Error(`Gemini API timed out after ${GEMINI_TIMEOUT_MS}ms`);
      }
      throw fetchErr;
    } finally {
      clearTimeout(timeoutHandle);
    }

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      console.error('Gemini API error:', response.status, errorData);
      throw new Error(`Gemini API returned ${response.status}`);
    }

    const data = await response.json();
    const aiText = data.candidates?.[0]?.content?.parts?.[0]?.text;

    if (!aiText) {
      throw new Error('No response from Gemini');
    }

    // Add AI response to history
    history.push({ role: 'model', parts: [{ text: aiText }] });

    res.json({
      response: aiText,
      source: 'The Inner Observer'
    });

  } catch (err) {
    console.error('AI chat error:', err.message);
    // Roll back the user turn we pushed before the failed call — otherwise it's left
    // dangling with no matching 'model' reply, and the *next* successful call sends
    // Gemini two consecutive 'user' turns, which breaks the expected alternating
    // user/model role sequence and can degrade or error subsequent responses.
    if (history && pushedUserTurn) {
      const idx = history.lastIndexOf(pushedUserTurn);
      if (idx !== -1) history.splice(idx, 1);
    }
    // Fall back to rule-based on API failure. Tagged distinctly from a real reply (matching
    // the existing no-API-key convention below) - a live Gemini 5xx/timeout previously
    // returned this canned text under the identical 'source' as a genuine AI response, with
    // no signal to the client that the model was never actually reached this turn.
    res.json({
      response: getFallbackResponse(message),
      source: 'The Inner Observer (offline mode)',
      note: 'The AI is temporarily unavailable; this is a general reflection, not a personalized response.',
    });
  }
});

// Clear conversation history (new session)
router.post('/reset', authenticateToken, (req, res) => {
  conversationHistory.delete(req.user.id);
  res.json({ message: 'Conversation reset. A new chapter begins.' });
});

// ============================================
// FALLBACK RESPONSES (when API key not set)
// ============================================
function getFallbackResponse(message) {
  const lower = message.toLowerCase();

  const wisdomMap = [
    { keys: ['anxious', 'anxiety', 'worried', 'stress', 'overwhelm'], text: "Notice the anxiety without becoming it. You are the awareness in which anxiety appears — vast, unchanging, untouched. Take three conscious breaths. On each exhale, let the body release what the mind is gripping. The anxiety is a wave. You are the ocean." },
    { keys: ['focus', 'distracted', 'attention', 'concentrate', 'scattered'], text: "Attention is the master skill. Every time you notice it has wandered and bring it back, you've completed one rep of the most important exercise a human can do. The wandering is not failure — the noticing IS the practice. Build your attention like you'd build a muscle: progressive overload, consistency, recovery." },
    { keys: ['purpose', 'meaning', 'why', 'direction', 'lost', 'confused'], text: "Purpose is not found — it is constructed. It lives at the intersection of three things: what you're willing to suffer for, what skills you can develop, and what the world needs. Stop searching for it like a hidden treasure. Start building it like an architect builds a cathedral — one deliberate stone at a time." },
    { keys: ['tired', 'exhausted', 'burnout', 'rest', 'energy'], text: "Rest is not the opposite of productivity — it is its foundation. A bow must be unstrung to maintain its power. Your nervous system is not a machine; it is an organism that cycles between activation and recovery. Honor the cycle. The deepest work happens in the restoration." },
    { keys: ['fear', 'scared', 'afraid', 'courage'], text: "Fear is a compass. It points toward the edges of your current identity — the boundary between who you are and who you could become. The Hybrid Monk does not eliminate fear. The Hybrid Monk walks toward it with open eyes, using it as fuel rather than a cage." },
    { keys: ['angry', 'anger', 'frustrated', 'rage', 'annoyed'], text: "Anger is energy with information. It tells you that something matters, that a boundary has been crossed, or that a value has been violated. The practice is not to suppress it — that creates pressure. Nor to express it blindly — that creates damage. Channel it. Ask: What needs to change? Then act from clarity, not reactivity." },
    { keys: ['meditat', 'stillness', 'quiet', 'peace', 'calm', 'present'], text: "Meditation is not about stopping thoughts. That would be like trying to stop the wind. It is about discovering the sky that was always there behind the clouds. You don't create silence — you remove the noise that obscures it. Sit. Breathe. Watch. What remains when you stop adding?" },
    { keys: ['habit', 'routine', 'discipline', 'consistency', 'procrastinat'], text: "You do not rise to the level of your goals. You fall to the level of your systems. Every habit is a vote for the identity you're becoming. Make the vote small enough that resistance cannot stop it, and consistent enough that momentum becomes unstoppable. One conscious action, repeated, reshapes reality." },
    { keys: ['identity', 'who am i', 'self', 'ego', 'authentic'], text: "The question 'Who am I?' is the most powerful technology a human possesses. Every answer your mind gives — I am my name, my role, my history — is a concept. A label. What remains when all labels are removed? That silent awareness looking through your eyes right now... that is closer to what you are." },
    { keys: ['lonely', 'alone', 'isolated', 'connection', 'relationship'], text: "Solitude and loneliness are different frequencies. Solitude is chosen presence with yourself — it is generative. Loneliness is the absence of genuine connection — it is depleting. The Hybrid Monk learns to be full in solitude so they can be genuine in connection. You cannot give from an empty vessel." },
    { keys: ['flow', 'zone', 'peak', 'performance'], text: "Flow is not luck — it is engineering. Clear goals + immediate feedback + challenge-skill balance + zero distractions = the doorway opens. Your brain releases a cocktail of neurochemicals that create timelessness and effortless action. The Hybrid Monk doesn't wait for flow. The Hybrid Monk builds the conditions daily." },
    { keys: ['death', 'dying', 'mortality', 'finite'], text: "Memento mori — remember you will die. This is not morbid. It is the ultimate clarifier. When you truly internalize your finite nature, the trivial falls away and what matters crystallizes. Every dawn is a gift of consciousness. Use it as if it might be your last. Not in panic — in presence." },
  ];

  for (const w of wisdomMap) {
    if (w.keys.some(k => lower.includes(k))) return w.text;
  }

  const defaults = [
    "The observer within you is already awake. It has been watching this entire conversation. What is it noticing right now that your thinking mind has missed?",
    "Every moment of awareness is a step on the path. The fact that you're here, asking questions, means the process has already begun. What are you most curious about in your inner landscape today?",
    "Between stimulus and response, there is a space. In that space lives your freedom, your growth, and your power. Tell me — what stimulus in your life are you reacting to automatically? Let's examine it together.",
    "The path is not about becoming someone new. It is about unbecoming everything that isn't authentically you. What belief, habit, or identity are you ready to question today?",
    "Your consciousness is the laboratory. Every experience is data. Every challenge is an experiment. What experiment are you running in your life right now, and what is it teaching you?",
    "The Hybrid Monk walks in the world but is not consumed by it. Present in the marketplace, awake in the monastery of the mind. What part of the world is currently consuming your attention?",
  ];

  return defaults[Math.floor(Math.random() * defaults.length)];
}

module.exports = router;
