// Unit tests for GamificationEngine, mocking db directly (no HTTP layer needed).
const mockQuery = jest.fn();
jest.mock('../src/db', () => ({
  query: (...args) => mockQuery(...args)
}));

const GamificationEngine = require('../src/utils/GamificationEngine');

beforeEach(() => {
  mockQuery.mockReset();
});

describe('GamificationEngine.addXp', () => {
  it('rejects a negative XP amount instead of silently decrementing a user\'s XP', async () => {
    await expect(GamificationEngine.addXp('u1', -50, 'breath')).rejects.toThrow(/non-negative/i);
    expect(mockQuery).not.toHaveBeenCalled();
  });

  it('rejects a non-numeric XP amount', async () => {
    await expect(GamificationEngine.addXp('u1', 'lots', 'breath')).rejects.toThrow();
    expect(mockQuery).not.toHaveBeenCalled();
  });

  it('passes the user\'s real streak_days through to the streak-badge lookup (regression guard: RETURNING must include streak_days)', async () => {
    mockQuery.mockImplementation((text, params) => {
      if (text.includes('xp = xp + $1')) {
        // Simulate the DB returning the row, including streak_days, per the RETURNING clause
        return Promise.resolve({ rows: [{ xp: 120, level: 2, current_stage: 1, streak_days: 7, awareness_score: 10 }] });
      }
      if (text.includes("condition_type = 'streak_days'")) {
        // Assert the value bound for condition_value <= $1 is the real streak (7), not undefined/null
        expect(params[0]).toEqual(7);
        return Promise.resolve({ rows: [] });
      }
      if (text.includes('COUNT(id) as count')) return Promise.resolve({ rows: [{ count: '0' }] });
      return Promise.resolve({ rows: [] });
    });

    await GamificationEngine.addXp('u1', 20, 'breath');

    const streakBadgeCall = mockQuery.mock.calls.find(([text]) => text.includes("condition_type = 'streak_days'"));
    expect(streakBadgeCall).toBeDefined();
    expect(streakBadgeCall[1][0]).toEqual(7);
  });

  it('returns the xp/level/promotion summary for a valid award', async () => {
    mockQuery.mockImplementation((text) => {
      if (text.includes('xp = xp + $1')) {
        return Promise.resolve({ rows: [{ xp: 100, level: 2, current_stage: 1, streak_days: 3, awareness_score: 5 }] });
      }
      if (text.includes('journey_stages WHERE stage_number')) {
        return Promise.resolve({ rows: [{ stage_number: 2, title: 'Social Identity', xp_required: 500 }] });
      }
      if (text.includes('COUNT(id) as count')) return Promise.resolve({ rows: [{ count: '0' }] });
      return Promise.resolve({ rows: [] });
    });

    const result = await GamificationEngine.addXp('u1', 20, 'breath');
    expect(result.new_total_xp).toEqual(100);
    expect(result.level).toEqual(2);
    expect(result.promotion_ready).toEqual({ eligible: false });
  });
});
