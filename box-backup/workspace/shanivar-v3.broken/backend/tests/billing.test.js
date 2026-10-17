const request = require('supertest');
const express = require('express');

jest.mock('../src/middleware/auth', () => ({
  authenticateToken: (req, res, next) => {
    req.user = { id: 'user-1' };
    next();
  }
}));

const mockDb = { query: jest.fn() };
jest.mock('../src/db', () => mockDb);

const billingRoutes = require('../src/routes/billing');
const app = express();
app.use(express.json());
app.use('/api/billing', billingRoutes);

describe('Billing refund', () => {
  beforeEach(() => {
    mockDb.query.mockReset();
  });

  it('binds the authenticated user id to the UPDATE\'s only placeholder ($1)', async () => {
    // Regression test: the route used to write `WHERE id = $2` while only
    // passing a single bound parameter, so this call threw a Postgres
    // bind-parameter-count error on every real request.
    mockDb.query
      .mockResolvedValueOnce({ rows: [{ membership: 'monk' }] }) // SELECT membership
      .mockResolvedValueOnce({ rows: [] }); // UPDATE

    const res = await request(app).post('/api/billing/refund');

    expect(res.statusCode).toEqual(200);
    expect(res.body.success).toBe(true);

    const [updateSql, updateParams] = mockDb.query.mock.calls[1];
    expect(updateSql).toContain('$1');
    expect(updateSql).not.toContain('$2');
    expect(updateParams).toEqual(['user-1']);
  });

  it('returns 404 when the user no longer exists', async () => {
    mockDb.query.mockResolvedValueOnce({ rows: [] });

    const res = await request(app).post('/api/billing/refund');

    expect(res.statusCode).toEqual(404);
  });
});
