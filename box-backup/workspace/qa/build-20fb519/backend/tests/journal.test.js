const request = require('supertest');
const express = require('express');

jest.mock('../src/middleware/auth', () => ({
  authenticateToken: (req, res, next) => {
    req.user = { id: 'user-1', role: 'user' };
    next();
  },
}));

const mockDb = { query: jest.fn() };
jest.mock('../src/db', () => mockDb);

const mockAddXp = jest.fn().mockResolvedValue({ xp: 105, level: 2, level_up: false });
jest.mock('../src/utils/GamificationEngine', () => ({ addXp: (...args) => mockAddXp(...args) }));

const journalRoutes = require('../src/routes/journal');
const app = express();
app.use(express.json());
app.use('/api/journal', journalRoutes);

beforeEach(() => {
  mockDb.query.mockReset();
  mockAddXp.mockClear();
});

describe('POST /api/journal', () => {
  it('awards XP for the first journal entry of the day', async () => {
    mockDb.query
      .mockResolvedValueOnce({ rows: [{ id: 'e1', title: 'T', body: 'B' }] }) // INSERT
      .mockResolvedValueOnce({ rows: [{ count: 1 }] }); // today count, including the one just inserted

    const res = await request(app).post('/api/journal').send({ title: 'T', body: 'B' });

    expect(res.statusCode).toEqual(201);
    expect(res.body.xp_awarded).toBe(true);
    expect(mockAddXp).toHaveBeenCalledWith('user-1', 5, 'journal');
  });

  it('does not award XP for a second journal entry on the same day (regression: was unlimited XP farming)', async () => {
    mockDb.query
      .mockResolvedValueOnce({ rows: [{ id: 'e2', title: 'T2', body: 'B2' }] }) // INSERT
      .mockResolvedValueOnce({ rows: [{ count: 2 }] }); // this is the second entry today

    const res = await request(app).post('/api/journal').send({ title: 'T2', body: 'B2' });

    expect(res.statusCode).toEqual(201);
    expect(res.body.xp_awarded).toBe(false);
    expect(res.body.gamification).toBeNull();
    expect(mockAddXp).not.toHaveBeenCalled();
  });

  it('strips HTML/script content from title and body before storing (backend-side sanitization)', async () => {
    mockDb.query
      .mockResolvedValueOnce({ rows: [{ id: 'e3', title: 'alert', body: 'hello' }] })
      .mockResolvedValueOnce({ rows: [{ count: 1 }] });

    await request(app)
      .post('/api/journal')
      .send({ title: '<script>alert(1)</script>alert', body: '<img src=x onerror=alert(1)>hello' });

    const insertParams = mockDb.query.mock.calls[0][1];
    expect(insertParams[1]).not.toMatch(/<script|<img/i);
    expect(insertParams[2]).not.toMatch(/<script|<img/i);
  });

  it('requires a body', async () => {
    const res = await request(app).post('/api/journal').send({ title: 'No body' });
    expect(res.statusCode).toEqual(400);
    expect(mockDb.query).not.toHaveBeenCalled();
  });
});
