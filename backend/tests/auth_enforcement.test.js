/**
 * Auth-enforcement coverage.
 *
 * Before this file, every existing test (chart.test.js, insights.test.js,
 * system_simulator.test.js) mocked `../src/middleware/auth` to always inject
 * a fake authenticated user — which is correct for exercising route
 * business logic, but means NOTHING in the suite ever ran the *real*
 * authenticateToken / requireAdmin middleware against a real request. A
 * route that forgot to attach `authenticateToken` at all would not have
 * failed a single test.
 *
 * This file mounts the REAL middleware (no jest.mock of ../src/middleware/auth)
 * against every route module in src/routes/, and a mocked ../src/db (so no
 * network/DB access happens — auth rejection happens before any handler
 * body runs), and asserts:
 *   1. Every route that calls authenticateToken rejects a request with no
 *      Authorization header (401) and a syntactically-invalid token (403).
 *   2. Every route that additionally calls requireAdmin rejects a valid,
 *      non-admin token (403).
 *   3. A valid, well-formed JWT is accepted by the middleware (i.e. the
 *      request is NOT stopped at 401/403 — whatever happens past that point
 *      is that route's own business logic, out of scope here).
 *   4. Routes that are intentionally public (no authenticateToken) still
 *      respond without a token, i.e. they were not accidentally locked down
 *      in a way that would show up as an unexpected 401 (a cheap regression
 *      guard the other direction).
 *
 * This does not require a live database: the mocked ../src/db resolves
 * every query as empty, and for protected routes the assertions only care
 * about the 401/403 boundary, which is decided by the middleware before any
 * db call happens.
 */
const request = require('supertest');
const express = require('express');
const jwt = require('jsonwebtoken');

// Generic DB mock shared by every router under test. Auth rejection happens
// before a handler ever touches this, so its shape only matters for the
// "valid token" assertions, where we only check we didn't get 401/403.
const mockClient = {
  query: jest.fn().mockResolvedValue({ rows: [] }),
  release: jest.fn()
};
jest.mock('../src/db', () => ({
  query: jest.fn().mockResolvedValue({ rows: [] }),
  connect: jest.fn().mockResolvedValue(mockClient)
}));

// Several routes (ai.js, chart.js, insights.js) call an external Gemini/
// astrology API past the auth boundary. This suite only cares about the
// 401/403 boundary itself, so stub global.fetch to fail fast and locally
// instead of letting valid-token requests reach a real external service.
global.fetch = jest.fn().mockRejectedValue(new Error('network calls disabled in auth_enforcement.test.js'));

// Use the REAL middleware (not mocked) so we exercise actual JWT verification.
const { JWT_SECRET } = require('../src/middleware/auth');

function tokenFor(payload) {
  return jwt.sign(payload, JWT_SECRET, { expiresIn: '1h' });
}

const validUserToken = tokenFor({ id: 'test-user-uuid', email: 'seeker@test.com', role: 'user' });
const validAdminToken = tokenFor({ id: 'admin-uuid', email: 'admin@test.com', role: 'admin' });
const malformedToken = 'this.is.not.a.valid.jwt';

const app = express();
app.use(express.json());
app.use('/api/auth', require('../src/routes/auth'));
app.use('/api/users', require('../src/routes/users'));
app.use('/api/journey', require('../src/routes/journey'));
app.use('/api/practices', require('../src/routes/practices'));
app.use('/api/gamification', require('../src/routes/gamification'));
app.use('/api/content', require('../src/routes/content'));
app.use('/api/community', require('../src/routes/community'));
app.use('/api/journal', require('../src/routes/journal'));
app.use('/api/ai', require('../src/routes/ai'));
app.use('/api/chart', require('../src/routes/chart'));
app.use('/api/insights', require('../src/routes/insights'));
app.use('/api/analytics', require('../src/routes/analytics'));
app.use('/api/billing', require('../src/routes/billing'));
app.use('/api/habits', require('../src/routes/habits'));
app.use('/api/whatsapp', require('../src/routes/whatsapp'));

// Routes that call authenticateToken (and nothing more).
const protectedRoutes = [
  ['post', '/api/ai/chat'],
  ['post', '/api/ai/reset'],
  ['get', '/api/billing/status'],
  ['post', '/api/billing/subscribe'],
  ['post', '/api/billing/refund'],
  ['post', '/api/chart/generate'],
  ['get', '/api/chart/my-chart'],
  ['post', '/api/community/posts'],
  ['post', '/api/community/posts/some-id/reply'],
  ['get', '/api/gamification/stats'],
  ['get', '/api/habits/'],
  ['post', '/api/habits/'],
  ['post', '/api/habits/some-id/log'],
  ['delete', '/api/habits/some-id'],
  ['get', '/api/insights/today'],
  ['post', '/api/insights/generate'],
  ['get', '/api/journal/'],
  ['post', '/api/journal/'],
  ['put', '/api/journal/some-id'],
  ['delete', '/api/journal/some-id'],
  ['post', '/api/journey/unlock'],
  ['get', '/api/practices/daily'],
  ['post', '/api/practices/complete'],
  ['post', '/api/practices/generate'],
  ['get', '/api/users/me'],
  ['put', '/api/users/me'],
  ['post', '/api/whatsapp/verify'],
  ['post', '/api/whatsapp/opt-out'],
];

// Routes that call authenticateToken AND requireAdmin.
const adminRoutes = [
  ['get', '/api/analytics/'],
  ['post', '/api/content/'],
];

// Routes that are intentionally public (no authenticateToken at all).
const publicRoutes = [
  ['get', '/api/community/posts'],
  ['get', '/api/community/posts/some-id'],
  ['get', '/api/content/'],
  ['get', '/api/content/some-id'],
  ['get', '/api/gamification/leaderboards'],
  ['get', '/api/journey/stages'],
  ['post', '/api/auth/register'],
  ['post', '/api/auth/login'],
];

describe('authenticateToken middleware — real middleware, no mocking', () => {
  describe.each(protectedRoutes)('%s %s', (method, path) => {
    it('rejects a request with no Authorization header (401)', async () => {
      const res = await request(app)[method](path);
      expect(res.statusCode).toEqual(401);
      expect(res.body).toHaveProperty('error');
    });

    it('rejects a syntactically invalid token (403)', async () => {
      const res = await request(app)[method](path).set('Authorization', `Bearer ${malformedToken}`);
      expect(res.statusCode).toEqual(403);
    });

    it('does not stop a well-formed, validly-signed token at the auth boundary', async () => {
      const res = await request(app)[method](path).set('Authorization', `Bearer ${validUserToken}`);
      // Whatever the route does past authentication (200, 400, 500 from the
      // mocked DB shape, etc.) is that route's own business logic and out of
      // scope here — we only assert auth itself didn't block a valid token.
      expect(res.statusCode).not.toEqual(401);
      expect(res.statusCode).not.toEqual(403);
    });
  });
});

describe('requireAdmin middleware — real middleware, no mocking', () => {
  describe.each(adminRoutes)('%s %s', (method, path) => {
    it('rejects an unauthenticated request (401)', async () => {
      const res = await request(app)[method](path);
      expect(res.statusCode).toEqual(401);
    });

    it('rejects a valid token belonging to a non-admin user (403)', async () => {
      const res = await request(app)[method](path).set('Authorization', `Bearer ${validUserToken}`);
      expect(res.statusCode).toEqual(403);
    });

    it('admits a valid token belonging to an admin user', async () => {
      const res = await request(app)[method](path).set('Authorization', `Bearer ${validAdminToken}`);
      expect(res.statusCode).not.toEqual(401);
      expect(res.statusCode).not.toEqual(403);
    });
  });
});

describe('intentionally public routes — regression guard', () => {
  describe.each(publicRoutes)('%s %s', (method, path) => {
    it('does not require a token (no 401/403 from missing auth)', async () => {
      const res = await request(app)[method](path);
      expect(res.statusCode).not.toEqual(401);
      expect(res.statusCode).not.toEqual(403);
    });
  });
});
