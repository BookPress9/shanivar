require('dotenv').config();
const express = require('express');
const cors = require('cors');
const rateLimit = require('express-rate-limit');

const authRoutes = require('./routes/auth');
const userRoutes = require('./routes/users');
const journeyRoutes = require('./routes/journey');
const practiceRoutes = require('./routes/practices');
const gamificationRoutes = require('./routes/gamification');
const contentRoutes = require('./routes/content');
const communityRoutes = require('./routes/community');
const journalRoutes = require('./routes/journal');
const aiRoutes = require('./routes/ai');
const chartRoutes = require('./routes/chart');
const insightsRoutes = require('./routes/insights');
const analyticsRoutes = require('./routes/analytics');
const ecommerceRoutes = require('./routes/ecommerce');
const billingRoutes = require('./routes/billing');
const habitsRoutes = require('./routes/habits');
const reflectionsRoutes = require('./routes/reflections');
const disciplineRoutes = require('./routes/discipline');
const db = require('./db');

const app = express();
const PORT = process.env.PORT || 5000;

// Render (and any PaaS) puts a reverse proxy in front of this app, so req.ip is the
// proxy's address unless we opt into X-Forwarded-For. express-rate-limit keys on req.ip,
// so without this EVERY request shares one bucket — the 20-per-15min limiter on
// login/register in routes/auth.js would then be a platform-wide lockout rather than a
// per-attacker throttle, and normal concurrent sign-ins would trip it. `1` = trust only
// the single proxy hop we actually have; `true` would let a client spoof its own IP by
// setting X-Forwarded-For and walk straight past the rate limiter.
app.set('trust proxy', 1);

// Middleware
const allowedOrigins = [
  process.env.FRONTEND_URL || 'http://localhost:3000',
  'http://localhost:3000',
  'https://shanivar.com',
  'https://www.shanivar.com',
  'http://shanivar.com',
  'http://www.shanivar.com',
  'https://thehybridmonk.com',
  'https://www.thehybridmonk.com'
].filter(Boolean);

app.use(cors({
  origin: function (origin, callback) {
    // Allow requests with no origin (mobile apps, curl, etc.)
    if (!origin || allowedOrigins.includes(origin)) {
      callback(null, true);
    } else {
      callback(new Error('Not allowed by CORS'));
    }
  },
  credentials: true
}));
app.use(express.json({ limit: '10mb' }));

// Rate limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 10000,
  standardHeaders: true,
  legacyHeaders: false
});
app.use('/api/', limiter);

// Routes
app.use('/api/auth', authRoutes);
app.use('/api/users', userRoutes);
app.use('/api/journey', journeyRoutes);
app.use('/api/practices', practiceRoutes);
app.use('/api/gamification', gamificationRoutes);
app.use('/api/content', contentRoutes);
app.use('/api/community', communityRoutes);
app.use('/api/journal', journalRoutes);
app.use('/api/ai', aiRoutes);
app.use('/api/chart', chartRoutes);
app.use('/api/insights', insightsRoutes);
app.use('/api/analytics', analyticsRoutes);
app.use('/api/ecommerce', ecommerceRoutes);
app.use('/api/billing', billingRoutes);
app.use('/api/habits', habitsRoutes);
app.use('/api/reflections', reflectionsRoutes);
app.use('/api/discipline', disciplineRoutes);
app.use('/api/whatsapp', require('./routes/whatsapp'));

// Health check. This is the host's health-check target, so it has to actually mean
// something: db.js sets its pool to null (no throw, no exit) when DATABASE_URL is missing
// or still a placeholder, which previously let this endpoint report a cheerful 'ok' from a
// deploy where every data route 500s — observed live on the deployed service before this
// change. A real SELECT is the only way this answers the question the host is asking.
app.get('/api/health', async (req, res) => {
  const body = { status: 'ok', timestamp: new Date().toISOString(), version: '1.0.0', database: 'ok' };
  try {
    await db.query('SELECT 1');
  } catch (err) {
    console.error('Health check: database unreachable:', err.message);
    return res.status(503).json({ ...body, status: 'degraded', database: 'unreachable' });
  }
  res.json(body);
});

// Error handler
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({ error: 'Internal server error' });
});

app.listen(PORT, () => {
  console.log(`🧘 Shanivar API running on port ${PORT}`);
});

module.exports = app;
