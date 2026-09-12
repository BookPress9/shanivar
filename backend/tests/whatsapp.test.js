const request = require('supertest');
const express = require('express');

describe('WhatsApp routes', () => {
  const ORIGINAL_ENV = process.env;
  let dbMock;

  function buildApp() {
    jest.doMock('../src/db', () => dbMock);
    jest.doMock('../src/middleware/auth', () => ({
      authenticateToken: (req, res, next) => {
        req.user = { id: 'user-1' };
        next();
      }
    }));
    jest.doMock('../src/utils/AnalyticsEngine', () => ({ logEvent: jest.fn().mockResolvedValue() }));
    const whatsappRoutes = require('../src/routes/whatsapp');
    const app = express();
    app.use(express.json());
    app.use('/api/whatsapp', whatsappRoutes);
    return app;
  }

  beforeEach(() => {
    jest.resetModules();
    process.env = { ...ORIGINAL_ENV, FRONTEND_URL: 'https://shanivar.com' };
    dbMock = { query: jest.fn().mockResolvedValue({ rows: [{ user_id: 'user-1' }] }) };
  });

  afterAll(() => {
    process.env = ORIGINAL_ENV;
  });

  describe('GET /track/:messageId (open redirect fix)', () => {
    it('redirects to the requested url when its host is an allowed frontend host', async () => {
      const app = buildApp();
      const res = await request(app).get('/api/whatsapp/track/msg-1?url=https://shanivar.com/dashboard/insight/1');

      expect(res.statusCode).toEqual(302);
      expect(res.headers.location).toEqual('https://shanivar.com/dashboard/insight/1');
    });

    it('does NOT redirect to an attacker-controlled host - falls back to the safe frontend url', async () => {
      // Regression test: the route used to do
      // `res.redirect(req.query.url || <fallback>)` with zero validation, so a link
      // like .../track/123?url=https://evil-phish.example.com was a working open
      // redirect off a trusted domain - a classic phishing primitive.
      const app = buildApp();
      const res = await request(app).get(
        '/api/whatsapp/track/msg-1?url=https://evil-phish.example.com/login'
      );

      expect(res.statusCode).toEqual(302);
      expect(res.headers.location).not.toContain('evil-phish.example.com');
      expect(res.headers.location).toEqual('https://shanivar.com');
    });

    it('allows a same-site relative path', async () => {
      const app = buildApp();
      const res = await request(app).get('/api/whatsapp/track/msg-1?url=%2Fdashboard%2Ftoday');

      expect(res.statusCode).toEqual(302);
      expect(res.headers.location).toEqual('/dashboard/today');
    });

    it('falls back to the frontend url when no url query param is given', async () => {
      const app = buildApp();
      const res = await request(app).get('/api/whatsapp/track/msg-1');

      expect(res.statusCode).toEqual(302);
      expect(res.headers.location).toEqual('https://shanivar.com');
    });
  });

  describe('n8n auth (no hardcoded default secret)', () => {
    it('rejects the old hardcoded default secret once N8N_SECRET is configured', async () => {
      // Regression test: authorizeN8n used to fall back to the literal string
      // 'n8n_secure_secret_dev' baked into source whenever N8N_SECRET wasn't set,
      // so any deployment that forgot to configure it accepted that well-known value.
      process.env.N8N_SECRET = 'real-production-secret';
      const app = buildApp();

      const res = await request(app)
        .get('/api/whatsapp/n8n/users/daily-insight')
        .set('Authorization', 'Bearer n8n_secure_secret_dev');

      expect(res.statusCode).toEqual(401);
    });

    it('accepts the correctly configured secret', async () => {
      process.env.N8N_SECRET = 'real-production-secret';
      const app = buildApp();

      const res = await request(app)
        .get('/api/whatsapp/n8n/users/daily-insight')
        .set('Authorization', 'Bearer real-production-secret');

      expect(res.statusCode).toEqual(200);
    });

    it('fails closed in production when no N8N_SECRET is configured at all', async () => {
      process.env.NODE_ENV = 'production';
      delete process.env.N8N_SECRET;
      const app = buildApp();

      const res = await request(app).get('/api/whatsapp/n8n/users/daily-insight');

      expect(res.statusCode).toEqual(401);
    });

    it('allows requests outside production when no N8N_SECRET is configured (dev convenience)', async () => {
      process.env.NODE_ENV = 'development';
      delete process.env.N8N_SECRET;
      const app = buildApp();

      const res = await request(app).get('/api/whatsapp/n8n/users/daily-insight');

      expect(res.statusCode).toEqual(200);
    });
  });
});
