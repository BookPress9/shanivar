const request = require('supertest');
const express = require('express');

describe('Ecommerce webhook', () => {
  let mockClient;
  const ORIGINAL_ENV = process.env;

  function buildApp(dbMock) {
    jest.doMock('../src/db', () => dbMock);
    const ecommerceRoutes = require('../src/routes/ecommerce');
    const app = express();
    app.use(express.json());
    app.use('/api/ecommerce', ecommerceRoutes);
    return app;
  }

  beforeEach(() => {
    jest.resetModules();
    process.env = { ...ORIGINAL_ENV };
    mockClient = {
      query: jest.fn().mockResolvedValue({ rows: [] }),
      release: jest.fn()
    };
  });

  afterAll(() => {
    process.env = ORIGINAL_ENV;
  });

  it('opens a pooled client via db.pool.connect() and commits the transaction', async () => {
    // Regression test: the route used to do `const pool = require('../db')` and then
    // `pool.connect()`, but db.js exports { query, pool } - not a Pool at the top
    // level - so `.connect` was undefined and every webhook call threw
    // "pool.connect is not a function" (outside any try/catch).
    mockClient.query.mockImplementation((sql) => {
      if (sql.includes('SELECT id FROM user_measurements')) return Promise.resolve({ rows: [] });
      if (sql.includes('RETURNING xp, level')) return Promise.resolve({ rows: [{ xp: 1200, level: 1 }] });
      return Promise.resolve({ rows: [] });
    });

    const dbMock = { query: jest.fn(), pool: { connect: jest.fn().mockResolvedValue(mockClient) } };
    const app = buildApp(dbMock);

    const res = await request(app)
      .post('/api/ecommerce/webhook')
      .send({ user_id: 'u1', order_number: 'ORD-1', status: 'shipped', xp_reward: 50 });

    expect(res.statusCode).toEqual(200);
    expect(res.body.success).toBe(true);
    expect(dbMock.pool.connect).toHaveBeenCalled();
    expect(mockClient.query).toHaveBeenCalledWith('BEGIN');
    expect(mockClient.query).toHaveBeenCalledWith('COMMIT');
    expect(mockClient.release).toHaveBeenCalled();
  });

  it('returns 500 instead of crashing when pool.connect() rejects', async () => {
    const dbMock = {
      query: jest.fn(),
      pool: { connect: jest.fn().mockRejectedValue(new Error('connection refused')) }
    };
    const app = buildApp(dbMock);

    const res = await request(app)
      .post('/api/ecommerce/webhook')
      .send({ user_id: 'u1', order_number: 'ORD-1', status: 'shipped' });

    expect(res.statusCode).toEqual(500);
  });

  it('rolls back and responds 500 (not throwing) when a query inside the transaction fails', async () => {
    mockClient.query.mockImplementation((sql) => {
      if (sql === 'BEGIN') return Promise.resolve();
      if (sql.includes('UPDATE users') && sql.includes('tailoring_pipeline_status')) {
        return Promise.reject(new Error('boom'));
      }
      return Promise.resolve({ rows: [] });
    });
    const dbMock = { query: jest.fn(), pool: { connect: jest.fn().mockResolvedValue(mockClient) } };
    const app = buildApp(dbMock);

    const res = await request(app)
      .post('/api/ecommerce/webhook')
      .send({ user_id: 'u1', order_number: 'ORD-1', status: 'shipped' });

    expect(res.statusCode).toEqual(500);
    expect(mockClient.query).toHaveBeenCalledWith('ROLLBACK');
    expect(mockClient.release).toHaveBeenCalled();
  });

  it('rejects a bad x-webhook-secret when SHOPIFY_WEBHOOK_SECRET is configured, regardless of NODE_ENV', async () => {
    // Regression test: verification used to be skipped entirely unless
    // NODE_ENV === 'production', so a staging deploy (or one where NODE_ENV
    // was simply never set) would accept any webhook call with no secret at all.
    process.env.SHOPIFY_WEBHOOK_SECRET = 'topsecret';
    process.env.NODE_ENV = 'development';

    const dbMock = { query: jest.fn(), pool: { connect: jest.fn().mockResolvedValue(mockClient) } };
    const app = buildApp(dbMock);

    const res = await request(app)
      .post('/api/ecommerce/webhook')
      .set('x-webhook-secret', 'wrong-secret')
      .send({ user_id: 'u1', order_number: 'ORD-1', status: 'shipped' });

    expect(res.statusCode).toEqual(401);
    expect(dbMock.pool.connect).not.toHaveBeenCalled();
  });

  it('accepts the call when the configured secret matches', async () => {
    process.env.SHOPIFY_WEBHOOK_SECRET = 'topsecret';
    process.env.NODE_ENV = 'production';

    const dbMock = { query: jest.fn(), pool: { connect: jest.fn().mockResolvedValue(mockClient) } };
    const app = buildApp(dbMock);

    const res = await request(app)
      .post('/api/ecommerce/webhook')
      .set('x-webhook-secret', 'topsecret')
      .send({ user_id: 'u1', order_number: 'ORD-1', status: 'shipped' });

    expect(res.statusCode).toEqual(200);
  });

  it('fails closed in production when no secret is configured at all', async () => {
    delete process.env.SHOPIFY_WEBHOOK_SECRET;
    process.env.NODE_ENV = 'production';

    const dbMock = { query: jest.fn(), pool: { connect: jest.fn().mockResolvedValue(mockClient) } };
    const app = buildApp(dbMock);

    const res = await request(app)
      .post('/api/ecommerce/webhook')
      .send({ user_id: 'u1', order_number: 'ORD-1', status: 'shipped' });

    expect(res.statusCode).toEqual(401);
  });
});
