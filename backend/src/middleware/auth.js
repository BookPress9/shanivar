const jwt = require('jsonwebtoken');

function resolveJwtSecret() {
  if (process.env.JWT_SECRET) return process.env.JWT_SECRET;
  if (process.env.NODE_ENV === 'production') {
    // Refuse to boot with a guessable, publicly-known secret in production — a forgeable
    // JWT_SECRET means anyone can mint tokens for any user/role.
    throw new Error('JWT_SECRET must be set in production (refusing to start with an insecure default).');
  }
  console.warn('[auth] JWT_SECRET not set — using an insecure development-only fallback. Do not use this outside local dev.');
  return 'dev-only-insecure-fallback-secret';
}

const JWT_SECRET = resolveJwtSecret();

function authenticateToken(req, res, next) {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];
  if (!token) return res.status(401).json({ error: 'Access token required' });

  jwt.verify(token, JWT_SECRET, (err, user) => {
    if (err) return res.status(403).json({ error: 'Invalid or expired token' });
    req.user = user;
    next();
  });
}

// Populates req.user when a valid token is present, but never rejects the request —
// for routes that are publicly browsable but personalize/gate content when logged in
// (e.g. content listing, which needs the caller's real stage/membership when available).
function optionalAuth(req, res, next) {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];
  if (!token) return next();

  jwt.verify(token, JWT_SECRET, (err, user) => {
    if (!err) req.user = user;
    next();
  });
}

function requireAdmin(req, res, next) {
  if (req.user.role !== 'admin') {
    return res.status(403).json({ error: 'Admin access required' });
  }
  next();
}

module.exports = { authenticateToken, optionalAuth, requireAdmin, JWT_SECRET };
