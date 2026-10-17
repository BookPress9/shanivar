const request = require('supertest');
const express = require('express');

// Mock authentication middleware (same pattern as chart.test.js)
jest.mock('../src/middleware/auth', () => ({
  authenticateToken: (req, res, next) => {
    req.user = { id: 'test-user-uuid' };
    next();
  }
}));

// Mock db so we can control responses per-query and inspect exactly what SQL/params
// each route sends, without touching a real database.
const mockQuery = jest.fn();
jest.mock('../src/db', () => ({
  query: (...args) => mockQuery(...args)
}));

const practicesRoutes = require('../src/routes/practices');

const app = express();
app.use(express.json());
app.use('/api/practices', practicesRoutes);

const PRACTICE = { id: 'p1', xp_reward: 25, category: 'breath' };

beforeEach(() => {
  mockQuery.mockReset();
});

describe('POST /api/practices/complete', () => {
  it('rejects a completion attempt when one was already recorded today (atomic check-and-insert)', async () => {
    mockQuery.mockImplementation((text) => {
      if (text.includes('SELECT * FROM practices WHERE id')) return Promise.resolve({ rows: [PRACTICE] });
      // Atomic INSERT ... WHERE NOT EXISTS returns no row when a completion already exists today
      if (text.includes('INSERT INTO practice_completions')) return Promise.resolve({ rows: [] });
      return Promise.resolve({ rows: [] });
    });

    const res = await request(app)
      .post('/api/practices/complete')
      .send({ practice_id: 'p1' });

    expect(res.statusCode).toEqual(400);
    expect(res.body.error).toMatch(/already completed/i);

    // Must not have gone on to touch streak/xp for a rejected completion
    const calledStreakUpdate = mockQuery.mock.calls.some(([text]) => text.includes('streak_days = CASE'));
    expect(calledStreakUpdate).toBe(false);
  });

  it('completes a practice exactly once and does not double-award XP when the insert succeeds', async () => {
    mockQuery.mockImplementation((text) => {
      if (text.includes('SELECT * FROM practices WHERE id')) return Promise.resolve({ rows: [PRACTICE] });
      if (text.includes('SELECT level FROM users WHERE id')) return Promise.resolve({ rows: [{ level: 1 }] });
      if (text.includes('INSERT INTO practice_completions')) return Promise.resolve({ rows: [{ id: 'comp-1' }] });
      if (text.includes('streak_days = CASE')) return Promise.resolve({ rows: [] });
      if (text.includes('xp = xp + $1')) {
        return Promise.resolve({ rows: [{ xp: 25, level: 1, current_stage: 1, streak_days: 1, awareness_score: 5 }] });
      }
      if (text.includes("condition_type = 'total_xp'")) return Promise.resolve({ rows: [] });
      if (text.includes("condition_type = 'streak_days'")) return Promise.resolve({ rows: [] });
      if (text.includes('COUNT(id) as count FROM practice_completions')) return Promise.resolve({ rows: [{ count: '1' }] });
      if (text.includes("condition_type = 'practices_completed'")) return Promise.resolve({ rows: [] });
      if (text.includes('COUNT(id) as count FROM community_posts')) return Promise.resolve({ rows: [{ count: '0' }] });
      if (text.includes("condition_type = 'community_posts'")) return Promise.resolve({ rows: [] });
      // Badges keyed off active habit count / journal entry count (e.g. "Habit Architect",
      // "Inner Scribe") — added after finding these condition_types existed in the live
      // badges table but were never queried here.
      if (text.includes('COUNT(id) as count FROM habits')) return Promise.resolve({ rows: [{ count: '0' }] });
      if (text.includes("condition_type = 'active_habits'")) return Promise.resolve({ rows: [] });
      if (text.includes('COUNT(id) as count FROM journal_entries')) return Promise.resolve({ rows: [{ count: '0' }] });
      if (text.includes("condition_type = 'journal_entries'")) return Promise.resolve({ rows: [] });
      if (text.includes('journey_stages WHERE stage_number')) return Promise.resolve({ rows: [] });
      if (text.includes('SELECT xp, level, focus_score')) return Promise.resolve({ rows: [{ xp: 25, level: 1 }] });
      return Promise.resolve({ rows: [] });
    });

    const res = await request(app)
      .post('/api/practices/complete')
      .send({ practice_id: 'p1', rating: 5 });

    expect(res.statusCode).toEqual(200);
    expect(res.body.xp_earned).toEqual(25);
    expect(res.body.level_up).toBe(false);

    // Exactly one insert into practice_completions for this request
    const insertCalls = mockQuery.mock.calls.filter(([text]) => text.includes('INSERT INTO practice_completions'));
    expect(insertCalls).toHaveLength(1);
  });

  it('reports level_up: true when the completion actually crosses a level threshold (regression guard: must compare pre-update level, not post-update level to itself)', async () => {
    mockQuery.mockImplementation((text) => {
      if (text.includes('SELECT * FROM practices WHERE id')) return Promise.resolve({ rows: [PRACTICE] });
      // User was level 1 before this completion...
      if (text.includes('SELECT level FROM users WHERE id')) return Promise.resolve({ rows: [{ level: 1 }] });
      if (text.includes('INSERT INTO practice_completions')) return Promise.resolve({ rows: [{ id: 'comp-1' }] });
      if (text.includes('streak_days = CASE')) return Promise.resolve({ rows: [] });
      // ...and the XP award pushes them to level 2.
      if (text.includes('xp = xp + $1')) {
        return Promise.resolve({ rows: [{ xp: 500, level: 2, current_stage: 1, streak_days: 1, awareness_score: 5 }] });
      }
      if (text.includes('COUNT(id) as count')) return Promise.resolve({ rows: [{ count: '1' }] });
      if (text.includes('SELECT xp, level, focus_score')) return Promise.resolve({ rows: [{ xp: 500, level: 2 }] });
      return Promise.resolve({ rows: [] });
    });

    const res = await request(app)
      .post('/api/practices/complete')
      .send({ practice_id: 'p1' });

    expect(res.statusCode).toEqual(200);
    expect(res.body.level_up).toBe(true);
  });

  it('rejects an out-of-range rating before touching the database', async () => {
    const res = await request(app)
      .post('/api/practices/complete')
      .send({ practice_id: 'p1', rating: 7 });

    expect(res.statusCode).toEqual(400);
    expect(mockQuery).not.toHaveBeenCalled();
  });

  it('rejects a negative rating', async () => {
    const res = await request(app)
      .post('/api/practices/complete')
      .send({ practice_id: 'p1', rating: -3 });

    expect(res.statusCode).toEqual(400);
  });

  it('requires practice_id', async () => {
    const res = await request(app).post('/api/practices/complete').send({});
    expect(res.statusCode).toEqual(400);
    expect(mockQuery).not.toHaveBeenCalled();
  });

  it('the streak-preserving UPDATE keeps a same-day-already-practiced branch (regression guard for the reset-to-1 bug)', async () => {
    let capturedStreakSql = null;
    mockQuery.mockImplementation((text) => {
      if (text.includes('SELECT * FROM practices WHERE id')) return Promise.resolve({ rows: [PRACTICE] });
      if (text.includes('SELECT level FROM users WHERE id')) return Promise.resolve({ rows: [{ level: 1 }] });
      if (text.includes('INSERT INTO practice_completions')) return Promise.resolve({ rows: [{ id: 'comp-1' }] });
      if (text.includes('streak_days = CASE')) {
        capturedStreakSql = text;
        return Promise.resolve({ rows: [] });
      }
      if (text.includes('xp = xp + $1')) {
        return Promise.resolve({ rows: [{ xp: 25, level: 1, current_stage: 1, streak_days: 5, awareness_score: 5 }] });
      }
      return Promise.resolve({ rows: [] });
    });

    await request(app).post('/api/practices/complete').send({ practice_id: 'p1' });

    expect(capturedStreakSql).not.toBeNull();
    // A user who already has last_practice_date = today (completing a 2nd practice same day)
    // must NOT fall through to the "reset to 1" branch.
    expect(capturedStreakSql).toMatch(/WHEN\s+last_practice_date\s*=\s*CURRENT_DATE\s+THEN\s+streak_days\b/i);
  });
});

describe('POST /api/practices/generate', () => {
  it('requires an intention', async () => {
    const res = await request(app)
      .post('/api/practices/generate')
      .send({ energy: 5, emotions: ['Joy'], body: ['Chest'], mind: ['Clear'] });
    expect(res.statusCode).toEqual(400);
  });

  it('rejects a malformed (non-array) mind/body/emotions field instead of crashing to a 500', async () => {
    mockQuery.mockResolvedValue({ rows: [{ checkins: [] }] });
    const res = await request(app)
      .post('/api/practices/generate')
      .send({ energy: 5, emotions: 'Joy', body: ['Chest'], mind: ['Clear'], intention: 'Release' });
    expect(res.statusCode).toEqual(400);
  });

  it('generates a protocol for a well-formed request', async () => {
    mockQuery.mockResolvedValue({ rows: [{ checkins: [] }] });
    const res = await request(app)
      .post('/api/practices/generate')
      .send({ energy: 5, emotions: ['Joy'], body: ['Chest'], mind: ['Clear'], intention: 'Release' });
    expect(res.statusCode).toEqual(200);
    expect(res.body).toHaveProperty('stages');
  });
});
