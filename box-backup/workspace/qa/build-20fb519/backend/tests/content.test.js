const request = require('supertest');
const express = require('express');

// authenticateToken/requireAdmin stay fixed (admin create-content tests, if any, rely on
// them), but optionalAuth is exercised per-test via a mutable holder so we can simulate
// both anonymous requests and requests from a real logged-in user.
let mockCurrentUser = null;
jest.mock('../src/middleware/auth', () => ({
  authenticateToken: (req, res, next) => {
    req.user = { id: 'admin-1', role: 'admin' };
    next();
  },
  optionalAuth: (req, res, next) => {
    if (mockCurrentUser) req.user = mockCurrentUser;
    next();
  },
  requireAdmin: (req, res, next) => next()
}));

const mockDb = { query: jest.fn() };
jest.mock('../src/db', () => mockDb);

const contentRoutes = require('../src/routes/content');
const app = express();
app.use(express.json());
app.use('/api/content', contentRoutes);

describe('Content single-item fetch', () => {
  beforeEach(() => {
    mockDb.query.mockReset();
    mockCurrentUser = null;
  });

  it('returns 404 for unpublished/draft content instead of leaking it by id', async () => {
    // Regression test: GET /:id used to run `SELECT * FROM content_items WHERE id = $1`
    // with no published check at all, unlike the list endpoint - any draft/unpublished
    // item was fully readable to anyone who knew or guessed its id.
    mockDb.query.mockResolvedValueOnce({ rows: [] });

    const res = await request(app).get('/api/content/draft-id');

    expect(res.statusCode).toEqual(404);
    expect(mockDb.query.mock.calls[0][0]).toMatch(/published\s*=\s*TRUE/i);
    // view_count must not be incremented for content that isn't found/published
    expect(mockDb.query).toHaveBeenCalledTimes(1);
  });

  it('returns published, unlocked content and increments its view_count', async () => {
    mockDb.query.mockResolvedValueOnce({
      rows: [{ id: 'c1', title: 'Hello', body: 'Full body', published: true, stage_required: 1, is_premium: false }],
    });
    mockDb.query.mockResolvedValueOnce({ rows: [] }); // view_count UPDATE

    const res = await request(app).get('/api/content/c1');

    expect(res.statusCode).toEqual(200);
    expect(res.body.id).toEqual('c1');
    expect(res.body.locked).toBe(false);
    expect(res.body.body).toEqual('Full body');
    expect(mockDb.query).toHaveBeenCalledTimes(2);
    expect(mockDb.query.mock.calls[1][0]).toMatch(/UPDATE content_items SET view_count/i);
  });

  it('a client-supplied ?stage= query param no longer controls access (server derives it instead)', async () => {
    // Regression guard for the flagged security gap: stage/premium gating used to be
    // driven entirely by a client-supplied query param, so a client could just omit or
    // change it to see everything. Access must now come from the caller's real DB row.
    mockDb.query.mockResolvedValueOnce({ rows: [] });

    const res = await request(app).get('/api/content?stage=99');

    expect(res.statusCode).toEqual(200);
    const [sql, params] = mockDb.query.mock.calls[0];
    expect(sql).not.toMatch(/stage_required/i);
    expect(params).toEqual([]);
  });

  it('serves a teaser (no body/media_url) for stage-locked content to a logged-in user below the required stage', async () => {
    mockCurrentUser = { id: 'user-1', role: 'user' };
    mockDb.query.mockResolvedValueOnce({
      rows: [{ id: 'c2', title: 'Advanced Practice', body: 'Secret body', media_url: 'https://x/y.mp4', published: true, stage_required: 5, is_premium: false }],
    });
    mockDb.query.mockResolvedValueOnce({ rows: [{ current_stage: 1, membership: 'trial' }] }); // resolveAccess lookup

    const res = await request(app).get('/api/content/c2');

    expect(res.statusCode).toEqual(200);
    expect(res.body.locked).toBe(true);
    expect(res.body.lock_reason).toEqual('stage');
    expect(res.body.body).toBeNull();
    expect(res.body.media_url).toBeNull();
    // A locked item must not count as a view.
    expect(mockDb.query).toHaveBeenCalledTimes(2);
  });

  it('serves a teaser for premium content to a non-premium member, and the full item to a premium member', async () => {
    mockCurrentUser = { id: 'user-2', role: 'user' };
    mockDb.query.mockResolvedValueOnce({
      rows: [{ id: 'c3', title: 'Premium Guide', body: 'Paywalled body', published: true, stage_required: 1, is_premium: true }],
    });
    mockDb.query.mockResolvedValueOnce({ rows: [{ current_stage: 1, membership: 'trial' }] });

    const lockedRes = await request(app).get('/api/content/c3');
    expect(lockedRes.body.locked).toBe(true);
    expect(lockedRes.body.lock_reason).toEqual('premium');
    expect(lockedRes.body.body).toBeNull();

    mockDb.query.mockReset();
    mockDb.query.mockResolvedValueOnce({
      rows: [{ id: 'c3', title: 'Premium Guide', body: 'Paywalled body', published: true, stage_required: 1, is_premium: true }],
    });
    mockDb.query.mockResolvedValueOnce({ rows: [{ current_stage: 1, membership: 'monk' }] });
    mockDb.query.mockResolvedValueOnce({ rows: [] }); // view_count UPDATE, since it's unlocked now

    const unlockedRes = await request(app).get('/api/content/c3');
    expect(unlockedRes.body.locked).toBe(false);
    expect(unlockedRes.body.body).toEqual('Paywalled body');
  });

  it('ignores a non-numeric ?stage= param instead of sending NaN to the database', async () => {
    mockDb.query.mockResolvedValueOnce({ rows: [] });

    const res = await request(app).get('/api/content?stage=not-a-number');

    expect(res.statusCode).toEqual(200);
    const [sql, params] = mockDb.query.mock.calls[0];
    expect(sql).not.toMatch(/stage_required/i);
    expect(params).toEqual([]);
  });

  it('list endpoint marks locked items rather than omitting them, so progression/upgrade paths stay visible', async () => {
    mockCurrentUser = { id: 'user-3', role: 'user' };
    mockDb.query.mockResolvedValueOnce({
      rows: [
        { id: 'a', title: 'Free', body: 'x', published: true, stage_required: 1, is_premium: false },
        { id: 'b', title: 'Locked Stage', body: 'y', published: true, stage_required: 9, is_premium: false },
      ],
    });
    mockDb.query.mockResolvedValueOnce({ rows: [{ current_stage: 1, membership: 'trial' }] });

    const res = await request(app).get('/api/content');

    expect(res.statusCode).toEqual(200);
    expect(res.body.find((i) => i.id === 'a').locked).toBe(false);
    expect(res.body.find((i) => i.id === 'b').locked).toBe(true);
    expect(res.body.find((i) => i.id === 'b').body).toBeNull();
  });
});
