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

jest.mock('../src/utils/GamificationEngine', () => ({
  addXp: jest.fn().mockResolvedValue({ xp: 10, level: 1, level_up: false }),
}));

const communityRoutes = require('../src/routes/community');
const app = express();
app.use(express.json());
app.use('/api/community', communityRoutes);

beforeEach(() => {
  mockDb.query.mockReset();
});

describe('Community content sanitization', () => {
  it('strips HTML/script tags from a post title and body before storing', async () => {
    mockDb.query.mockResolvedValueOnce({ rows: [{ id: 'p1' }] });

    await request(app)
      .post('/api/community/posts')
      .send({ title: '<script>alert(1)</script>Hello', body: '<b onclick="evil()">World</b>' });

    const [, params] = mockDb.query.mock.calls[0];
    expect(params[1]).not.toMatch(/<script|onclick/i);
    expect(params[2]).not.toMatch(/<b |onclick/i);
    expect(params[1]).toContain('Hello');
    expect(params[2]).toContain('World');
  });

  it('strips HTML/script tags from a reply body before storing', async () => {
    mockDb.query.mockResolvedValueOnce({ rows: [{ id: 'r1' }] });

    await request(app)
      .post('/api/community/posts/p1/reply')
      .send({ body: '<img src=x onerror=alert(1)>nice post' });

    const [, params] = mockDb.query.mock.calls[0];
    expect(params[2]).not.toMatch(/<img|onerror/i);
    expect(params[2]).toContain('nice post');
  });
});
