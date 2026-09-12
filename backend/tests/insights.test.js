const request = require('supertest');
const express = require('express');

// Mock authentication middleware
jest.mock('../src/middleware/auth', () => {
  return {
    authenticateToken: (req, res, next) => {
      req.user = { id: 'test-user-uuid' };
      next();
    }
  };
});

// Mock the pool module BEFORE requiring routes
const mockQuery = jest.fn();
jest.mock('../src/db', () => {
  return {
    query: mockQuery
  };
});

const mockFetch = jest.fn();
global.fetch = mockFetch;

const insightsRoutes = require('../src/routes/insights');
const app = express();
app.use(express.json());
app.use('/api/insights', insightsRoutes);

describe('Daily Insight Engine', () => {
  
  let hasCalculatedInsight = false;

  beforeEach(() => {
    mockFetch.mockReset();
    mockQuery.mockReset();
    hasCalculatedInsight = false;

    // Setup DB mock logic inside beforeEach so it resets properly
    mockQuery.mockImplementation((queryText, params) => {
      if (queryText.includes('SELECT * FROM daily_insights WHERE user_id = $1 AND insight_date = CURRENT_DATE')) {
        if (hasCalculatedInsight) {
           return Promise.resolve({
             rows: [{
               id: 'mock-insight-id',
               insight_text: 'The stars align for focus.',
               action_text: 'Work deeply for 2 hours.',
               reflection_text: 'What distracts you most?'
             }]
           });
        }
        return Promise.resolve({ rows: [] });
      }

      if (queryText.includes('SELECT current_stage FROM users')) {
        return Promise.resolve({ rows: [{ current_stage: 3 }] });
      }

      if (queryText.includes('SELECT * FROM user_chart')) {
        return Promise.resolve({ rows: [{ 
          id: 'mock-chart-id', 
          sun_sign: 'Leo', 
          moon_sign: 'Cancer', 
          rising_sign: 'Sagittarius',
          latitude: '40.7128',
          longitude: '-74.0060'
        }]});
      }

      if (queryText.includes('SELECT planet_name')) {
        return Promise.resolve({ rows: [
          { planet_name: 'Sun', zodiac_sign: 'Leo', house: 9 }
        ]});
      }

      if (queryText.includes('INSERT INTO daily_insights')) {
        hasCalculatedInsight = true; // Mark as generated
        return Promise.resolve({
          rows: [{
            id: 'mock-insight-id',
            insight_text: params[1],
            action_text: params[2],
            reflection_text: params[3]
          }]
        });
      }

      return Promise.resolve({ rows: [] });
    });
  });

  it('Generates a new daily insight using natal and transit data', async () => {

    // insights.js makes exactly ONE fetch call — to Gemini. (There is no separate
    // "astrology service" call in this codebase; ASTROLOGY_SERVICE_URL was dead,
    // unused config that has since been removed — see squad report.) Gemini's real
    // generateContent response wraps the model's text inside candidates[0].content
    // .parts[0].text, which is what gets regex-parsed into Insight/Action/Reflection.
    mockFetch.mockReturnValueOnce(Promise.resolve({
      ok: true,
      json: () => Promise.resolve({
        candidates: [{
          content: {
            parts: [{
              text: "Insight: Mars energy is driving deep focus today. Action: Channel this drive into your most difficult task. Reflection: What resistance are you burning through?"
            }]
          }
        }]
      })
    }));

    // Requires GEMINI_API_KEY environment var to be set technically, but fetch is mocked
    process.env.GEMINI_API_KEY = "mock_key";

    const res = await request(app).post('/api/insights/generate');

    expect(res.statusCode).toEqual(201);
    expect(res.body).toHaveProperty('insight');
    expect(res.body.insight.insight_text).toEqual('Mars energy is driving deep focus today.');
    expect(res.body.insight.action_text).toEqual('Channel this drive into your most difficult task.');
    expect(res.body.insight.reflection_text).toEqual('What resistance are you burning through?');
    expect(mockFetch).toHaveBeenCalledTimes(1);

    // The Gemini prompt must now carry real computed transit data (planet/sign pairs
    // from backend/src/utils/astrology.js), not the old vague "consider the
    // astrological transits for today" instruction that gave it nothing to work from.
    const requestBody = JSON.parse(mockFetch.mock.calls[0][1].body);
    const promptText = requestBody.contents[0].parts[0].text;
    expect(promptText).toMatch(/Today's planetary transits/);
    expect(promptText).toMatch(/Sun in \w+/);
    expect(promptText).not.toMatch(/Consider the astrological transits/);
  });

  it('Retrieves the already generated insight for today', async () => {
    // Simulate that an insight was already generated earlier today, so the DB mock's
    // "SELECT * FROM daily_insights ... CURRENT_DATE" branch returns the cached row
    // instead of falling through to auto-generation.
    hasCalculatedInsight = true;

    // Should hit the DB mock for already generated insight
    const res = await request(app).get('/api/insights/today');

    expect(res.statusCode).toEqual(200);
    expect(res.body).toHaveProperty('insight');
    expect(res.body.insight.insight_text).toEqual('The stars align for focus.');
    expect(mockFetch).not.toHaveBeenCalled(); // cached insight found — no Gemini call needed
  });

  it('Falls back gracefully (not hanging) when the Gemini call times out', async () => {
    process.env.GEMINI_API_KEY = "mock_key";
    // Confirms an AbortSignal is wired into the fetch call and that an AbortError
    // is handled as a clean fallback rather than hanging or crashing. The abort is
    // simulated directly rather than waiting out the real timeout duration.
    mockFetch.mockImplementationOnce((url, options) => {
      expect(options.signal).toBeDefined();
      const err = new Error('aborted');
      err.name = 'AbortError';
      return Promise.reject(err);
    });

    const res = await request(app).post('/api/insights/generate');

    expect(res.statusCode).toEqual(201);
    expect(res.body.message).toBe('Daily insight generated (fallback mode)');
  });
});
