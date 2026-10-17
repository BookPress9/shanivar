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

// Mock the DB module used by ai.js for user/chart/practice context lookups
const mockQuery = jest.fn();
jest.mock('../src/db', () => ({ query: mockQuery }));

const mockFetch = jest.fn();
global.fetch = mockFetch;

process.env.GEMINI_API_KEY = 'mock_key_for_ai_test';

const aiRoutes = require('../src/routes/ai');
const app = express();
app.use(express.json());
app.use('/api/ai', aiRoutes);

describe('Inner Observer chat endpoint', () => {
  beforeEach(() => {
    mockFetch.mockReset();
    mockQuery.mockReset();
    mockQuery.mockImplementation((queryText) => {
      if (queryText.includes('SELECT current_stage FROM users')) {
        return Promise.resolve({ rows: [{ current_stage: 2 }] });
      }
      if (queryText.includes('SELECT sun_sign')) {
        return Promise.resolve({ rows: [] });
      }
      if (queryText.includes('SELECT title, category FROM practices')) {
        return Promise.resolve({ rows: [] });
      }
      return Promise.resolve({ rows: [] });
    });
  });

  it('does not leave a dangling unanswered user turn in history after a Gemini failure', async () => {
    // First call: Gemini fails outright.
    mockFetch.mockRejectedValueOnce(new Error('network down'));

    const failRes = await request(app)
      .post('/api/ai/chat')
      .send({ message: 'first message that will fail' });

    expect(failRes.statusCode).toBe(200);
    // Regression guard: a failed Gemini call used to return this canned fallback tagged
    // identically to a real reply ('The Inner Observer'), with no signal to the client that
    // the model was never actually reached. Must now be distinguishable, matching the
    // existing no-API-key convention.
    expect(failRes.body.source).toBe('The Inner Observer (offline mode)');
    expect(failRes.body.note).toMatch(/temporarily unavailable/i);

    // Second call: Gemini succeeds. Capture what we actually send it.
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve({
        candidates: [{ content: { parts: [{ text: 'A grounded reply.' }] } }]
      })
    });

    const okRes = await request(app)
      .post('/api/ai/chat')
      .send({ message: 'second message' });

    expect(okRes.statusCode).toBe(200);
    expect(okRes.body.response).toBe('A grounded reply.');

    // Inspect the request body sent on the second (successful) call.
    const secondCallArgs = mockFetch.mock.calls[0]; // only one call reached fetch successfully in this test's second request
    const sentBody = JSON.parse(mockFetch.mock.calls[mockFetch.mock.calls.length - 1][1].body);

    // Only the second message's user turn should be present — the first (failed)
    // call's user turn must have been rolled back, otherwise Gemini would see two
    // consecutive 'user' turns with no 'model' turn between them.
    expect(sentBody.contents).toHaveLength(1);
    expect(sentBody.contents[0]).toEqual({ role: 'user', parts: [{ text: 'second message' }] });
  });

  it('passes an AbortController signal so a hung Gemini call times out instead of hanging forever', async () => {
    // Confirms an AbortSignal is wired into the fetch call and that an AbortError is
    // handled as a clean fallback rather than hanging or crashing. The abort is
    // simulated directly rather than waiting out the real timeout duration.
    mockFetch.mockImplementationOnce((url, options) => {
      expect(options.signal).toBeDefined();
      const err = new Error('aborted');
      err.name = 'AbortError';
      return Promise.reject(err);
    });

    const res = await request(app).post('/api/ai/chat').send({ message: 'hello' });

    expect(res.statusCode).toBe(200);
    // Falls back gracefully rather than hanging/crashing, and is tagged distinctly from a
    // real reply (see regression guard above).
    expect(res.body.source).toBe('The Inner Observer (offline mode)');
  });
});
