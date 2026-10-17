const request = require('supertest');
const express = require('express');

jest.mock('../src/middleware/auth', () => ({
  authenticateToken: (req, res, next) => {
    req.user = { id: 'test-user-uuid' };
    next();
  }
}));

const mockQuery = jest.fn();
jest.mock('../src/db', () => ({
  query: (...args) => mockQuery(...args)
}));

const habitsRoutes = require('../src/routes/habits');

const app = express();
app.use(express.json());
app.use('/api/habits', habitsRoutes);

beforeEach(() => {
  mockQuery.mockReset();
});

describe('POST /api/habits/:id/log', () => {
  it('refuses to log a habit that does not belong to the requesting user', async () => {
    mockQuery.mockImplementation((text) => {
      if (text.includes('SELECT id FROM habits WHERE id')) return Promise.resolve({ rows: [] }); // not owned / not found
      return Promise.resolve({ rows: [{ id: 'log-1' }] });
    });

    const res = await request(app).post('/api/habits/someone-elses-habit/log').send({ notes: 'hi' });

    expect(res.statusCode).toEqual(404);
    // The insert into habit_logs must never be reached once ownership check fails
    const insertCalls = mockQuery.mock.calls.filter(([text]) => text.includes('INSERT INTO habit_logs'));
    expect(insertCalls).toHaveLength(0);
  });

  it('logs completion for a habit the user owns', async () => {
    mockQuery.mockImplementation((text) => {
      if (text.includes('SELECT id FROM habits WHERE id')) return Promise.resolve({ rows: [{ id: 'h1' }] });
      if (text.includes('INSERT INTO habit_logs')) return Promise.resolve({ rows: [{ id: 'log-1', habit_id: 'h1' }] });
      return Promise.resolve({ rows: [] });
    });

    const res = await request(app).post('/api/habits/h1/log').send({ notes: 'done' });
    expect(res.statusCode).toEqual(200);
    expect(res.body.id).toEqual('log-1');
  });

  it('reports already logged today when the ON CONFLICT DO NOTHING yields no row', async () => {
    mockQuery.mockImplementation((text) => {
      if (text.includes('SELECT id FROM habits WHERE id')) return Promise.resolve({ rows: [{ id: 'h1' }] });
      if (text.includes('INSERT INTO habit_logs')) return Promise.resolve({ rows: [] });
      return Promise.resolve({ rows: [] });
    });

    const res = await request(app).post('/api/habits/h1/log').send({});
    expect(res.statusCode).toEqual(400);
  });
});

describe('POST /api/habits', () => {
  it('requires a name', async () => {
    const res = await request(app).post('/api/habits').send({});
    expect(res.statusCode).toEqual(400);
  });

  it('rejects an invalid frequency', async () => {
    const res = await request(app).post('/api/habits').send({ name: 'Cold shower', frequency: 'hourly' });
    expect(res.statusCode).toEqual(400);
  });

  it('rejects a non-positive target_count', async () => {
    const res = await request(app).post('/api/habits').send({ name: 'Cold shower', target_count: 0 });
    expect(res.statusCode).toEqual(400);
  });

  it('rejects a negative target_count', async () => {
    const res = await request(app).post('/api/habits').send({ name: 'Cold shower', target_count: -2 });
    expect(res.statusCode).toEqual(400);
  });

  it('creates a habit for a valid payload', async () => {
    mockQuery.mockResolvedValue({ rows: [{ id: 'h1', name: 'Cold shower' }] });
    const res = await request(app).post('/api/habits').send({ name: 'Cold shower', frequency: 'daily', target_count: 1 });
    expect(res.statusCode).toEqual(201);
  });
});
