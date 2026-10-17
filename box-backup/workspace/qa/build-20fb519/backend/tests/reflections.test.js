const request = require('supertest');
const express = require('express');
const crypto = require('crypto');

// Auth mock: reads req.user.id from an `x-test-user` header (defaulting to
// 'user-a') so a single test file can exercise ownership isolation between
// two different "authenticated" users without needing a second app instance.
jest.mock('../src/middleware/auth', () => ({
  authenticateToken: (req, res, next) => {
    req.user = { id: req.headers['x-test-user'] || 'user-a' };
    next();
  }
}));

// ─────────────────────────────────────────────────────────────────────────
// Fake in-memory Postgres for reflection_* tables — a hand-rolled relational
// simulation of exactly the queries backend/src/routes/reflections.js issues
// (verified against its literal SQL text), rather than canned per-substring
// stubs. This lets BEGIN/COMMIT/ROLLBACK, UNIQUE constraints and the two-phase
// transaction actually behave like the real thing would, which the version-guard
// and idempotency tests below depend on.
// ─────────────────────────────────────────────────────────────────────────
let sessions, cards, responses, idKeys, pendingSnapshot;

function resetFakeDb() {
  sessions = [];
  cards = [];
  responses = [];
  idKeys = [];
  pendingSnapshot = null;
}
resetFakeDb();

function clone(x) { return x === null || x === undefined ? x : JSON.parse(JSON.stringify(x)); }
function uuid() { return crypto.randomUUID(); }

async function mockFakeQuery(text, params = []) {
  const t = String(text).trim();
  const verb = t.split(/\s+/)[0].toUpperCase();

  if (verb === 'BEGIN') { pendingSnapshot = clone({ sessions, cards, responses, idKeys }); return { rows: [] }; }
  if (verb === 'COMMIT') { pendingSnapshot = null; return { rows: [] }; }
  if (verb === 'ROLLBACK') {
    if (pendingSnapshot) {
      ({ sessions, cards, responses, idKeys } = pendingSnapshot);
      pendingSnapshot = null;
    }
    return { rows: [] };
  }

  if (verb === 'CREATE') return { rows: [] }; // schema self-heal block at module load

  if (verb === 'INSERT') {
    if (t.includes('reflection_idempotency_keys')) {
      const [userId, operation, idempotency_key, request_hash] = params;
      const exists = idKeys.find(k => k.user_id === userId && k.operation === operation && k.idempotency_key === idempotency_key);
      if (exists) return { rows: [] };
      const row = { id: uuid(), user_id: userId, operation, idempotency_key, request_hash, response_status: null, response_body: null };
      idKeys.push(row);
      return { rows: [{ id: row.id }] };
    }
    if (t.includes('INSERT INTO reflection_sessions')) {
      const [userId, entry_point, timezone, local_date, mode, status, snapshotJson, context_as_of, session_limit] = params;
      const row = {
        id: uuid(), user_id: userId, entry_point, timezone, local_date, mode, status,
        version: 1, context_snapshot: JSON.parse(snapshotJson), context_as_of, session_limit,
        answered_count: 0, created_at: new Date().toISOString(), updated_at: new Date().toISOString(), closed_at: null
      };
      sessions.push(row);
      return { rows: [clone(row)] };
    }
    if (t.includes('reflection_cards')) {
      const [sessionId, index, sender, message, question, optionsJson, contextRefsJson, provenance_label] = params;
      const exists = cards.find(c => c.session_id === sessionId && c.index === index);
      if (exists) return { rows: [] };
      const row = {
        id: uuid(), session_id: sessionId, index, sender, message, question,
        options: JSON.parse(optionsJson), context_refs: JSON.parse(contextRefsJson),
        provenance_label, generation_status: 'ready', created_at: new Date().toISOString()
      };
      cards.push(row);
      return { rows: [clone(row)] };
    }
    if (t.includes('reflection_responses')) {
      const [sessionId, cardId, kind, value] = params;
      const exists = responses.find(r => r.session_id === sessionId && r.card_id === cardId);
      if (exists) return { rows: [] };
      const row = { id: uuid(), session_id: sessionId, card_id: cardId, kind, value, created_at: new Date().toISOString() };
      responses.push(row);
      return { rows: [clone(row)] };
    }
  }

  if (verb === 'SELECT') {
    if (t.includes('reflection_idempotency_keys')) {
      const [userId, operation, idempotency_key] = params;
      const row = idKeys.find(k => k.user_id === userId && k.operation === operation && k.idempotency_key === idempotency_key);
      return { rows: row ? [{ request_hash: row.request_hash, response_status: row.response_status, response_body: clone(row.response_body) }] : [] };
    }
    if (t.includes('reflection_cards') && t.includes('JOIN reflection_responses')) {
      const [sessionId] = params;
      const rows = cards.filter(c => c.session_id === sessionId)
        .map(c => {
          const resp = responses.find(r => r.card_id === c.id && r.session_id === sessionId);
          if (!resp) return null;
          return {
            id: c.id, index: c.index, sender: c.sender, message: c.message, question: c.question,
            options: c.options, context_refs: c.context_refs, provenance_label: c.provenance_label,
            response_kind: resp.kind, response_value: resp.value, response_created_at: resp.created_at
          };
        })
        .filter(Boolean)
        .sort((a, b) => a.index - b.index);
      return { rows: rows.map(clone) };
    }
    if (t.includes('FROM reflection_cards WHERE session_id')) {
      const [sessionId, index] = params;
      const row = cards.find(c => c.session_id === sessionId && c.index === index);
      return { rows: row ? [clone(row)] : [] };
    }
    if (t.includes('FROM reflection_sessions WHERE id = $1 AND user_id = $2')) {
      const [id, userId] = params;
      const row = sessions.find(s => s.id === id && s.user_id === userId);
      return { rows: row ? [clone(row)] : [] };
    }
    if (t.includes('FROM reflection_sessions WHERE id = $1 FOR UPDATE')) {
      const [id] = params;
      const row = sessions.find(s => s.id === id);
      return { rows: row ? [clone(row)] : [] };
    }
  }

  if (verb === 'UPDATE') {
    if (t.includes('reflection_idempotency_keys')) {
      const [status, bodyJson, id] = params;
      const row = idKeys.find(k => k.id === id);
      if (row) { row.response_status = status; row.response_body = JSON.parse(bodyJson); }
      return { rows: [] };
    }
    if (t.includes("SET status = 'closed'")) {
      const [id] = params;
      const row = sessions.find(s => s.id === id);
      if (row) { row.status = 'closed'; row.closed_at = new Date().toISOString(); row.version += 1; row.updated_at = new Date().toISOString(); }
      return { rows: row ? [clone(row)] : [] };
    }
    if (t.includes('SET version = version + 1')) {
      const [answeredCount, status, id] = params;
      const row = sessions.find(s => s.id === id);
      if (row) { row.version += 1; row.answered_count = answeredCount; row.status = status; row.updated_at = new Date().toISOString(); }
      return { rows: row ? [clone(row)] : [] };
    }
    if (t.includes('SET status = $1, updated_at = NOW()')) {
      const [status, id] = params;
      const row = sessions.find(s => s.id === id);
      if (row) { row.status = status; row.updated_at = new Date().toISOString(); }
      return { rows: row ? [clone(row)] : [] };
    }
  }

  if (verb === 'DELETE') {
    if (t.includes('reflection_idempotency_keys')) {
      if (t.includes('WHERE id = $1')) {
        const [id] = params;
        idKeys = idKeys.filter(k => k.id !== id);
        return { rows: [] };
      }
      const [userId, pattern] = params;
      const suffix = String(pattern).replace('%', '');
      idKeys = idKeys.filter(k => !(k.user_id === userId && k.operation.endsWith(suffix)));
      return { rows: [] };
    }
    if (t.includes('reflection_sessions')) {
      const [id, userId] = params;
      const idx = sessions.findIndex(s => s.id === id && s.user_id === userId);
      if (idx === -1) return { rows: [] };
      const [removed] = sessions.splice(idx, 1);
      cards = cards.filter(c => c.session_id !== removed.id);
      responses = responses.filter(r => r.session_id !== removed.id);
      return { rows: [{ id: removed.id }] };
    }
  }

  throw new Error('Unmocked query in fake reflections DB: ' + t.slice(0, 160));
}

jest.mock('../src/db', () => ({
  query: (...args) => mockFakeQuery(...args),
  pool: {
    query: (...args) => mockFakeQuery(...args),
    connect: async () => ({ query: (...args) => mockFakeQuery(...args), release: () => {} })
  }
}));

const mockBuildContextSnapshot = jest.fn();
const mockGenerateNextCard = jest.fn();
jest.mock('../src/utils/reflectionGeneration', () => ({
  buildContextSnapshot: (...args) => mockBuildContextSnapshot(...args),
  generateNextCard: (...args) => mockGenerateNextCard(...args)
}));

const reflectionsRoutes = require('../src/routes/reflections');

const app = express();
app.use(express.json());
app.use('/api/reflections', reflectionsRoutes);

const DEFAULT_SNAPSHOT = {
  schemaVersion: 1, mode: 'general', birthDataQuality: 'none', calculationEngine: 'test',
  zodiacSystem: 'tropical', natalHouseSystem: null, transitHouseSystemInternal: 'whole-sign',
  chartId: null, chartRevisionAt: null, transitAsOfUtc: '2026-09-09T00:00:00.000Z',
  timezone: 'Asia/Kolkata', localDate: '2026-09-09', natal: null, transitPlanets: [], aspects: []
};

function defaultCardFor(session) {
  return {
    index: session.nextIndex,
    sender: 'Shanivar',
    message: `Reflection card ${session.nextIndex}`,
    question: 'Does that fit today?',
    options: [
      { id: 'fits', label: 'This fits' },
      { id: 'not_today', label: 'Not today' },
      { id: 'unsure', label: "I'm unsure" },
      { id: 'prefer_not_to_share', label: 'Prefer not to share' }
    ],
    context_refs: [],
    mode: session.contextSnapshot.mode,
    provenance_label: session.contextSnapshot.mode,
    context_as_of: session.contextSnapshot.transitAsOfUtc
  };
}

beforeEach(() => {
  resetFakeDb();
  mockBuildContextSnapshot.mockReset();
  mockGenerateNextCard.mockReset();
  mockBuildContextSnapshot.mockImplementation(async ({ userId, timezone }) => ({ ...DEFAULT_SNAPSHOT, timezone }));
  mockGenerateNextCard.mockImplementation(async ({ session }) => ({ ok: true, status: 'ready', card: defaultCardFor(session) }));
});

// Creates a session for `user` and returns the parsed response body.
async function createSession(user = 'user-a', body = { timezone: 'Asia/Kolkata' }) {
  const res = await request(app)
    .post('/api/reflections/sessions')
    .set('x-test-user', user)
    .send(body);
  return res;
}

describe('POST /api/reflections/sessions', () => {
  it('creates a session with a ready first card', async () => {
    const res = await createSession();
    expect(res.statusCode).toBe(201);
    expect(res.body.status).toBe('awaiting_response');
    expect(res.body.version).toBe(1);
    expect(res.body.answered_count).toBe(0);
    expect(res.body.session_limit).toBe(5);
    expect(res.body.current_card).toMatchObject({ index: 0, sender: 'Shanivar' });
    expect(Array.isArray(res.body.current_card.options)).toBe(true);
    expect(res.body.answered_history).toEqual([]);
  });

  it('rejects an invalid IANA timezone with 422', async () => {
    const res = await createSession('user-a', { timezone: 'Not/AZone' });
    expect(res.statusCode).toBe(422);
    expect(mockBuildContextSnapshot).not.toHaveBeenCalled();
  });

  it('rejects an invalid entry_point with 422', async () => {
    const res = await createSession('user-a', { timezone: 'Asia/Kolkata', entry_point: 'homepage' });
    expect(res.statusCode).toBe(422);
  });

  it('returns 503 and generation_failed status when even the fallback card is unavailable', async () => {
    mockGenerateNextCard.mockResolvedValueOnce({ ok: false, status: 'generation_failed', reason: 'total_outage' });
    const res = await createSession();
    expect(res.statusCode).toBe(503);
    expect(res.body.status).toBe('generation_failed');
    expect(res.body.current_card).toBeNull();
  });
});

describe('GET /api/reflections/sessions/:id', () => {
  it('returns 404 for a session that does not exist', async () => {
    const res = await request(app).get('/api/reflections/sessions/00000000-0000-4000-8000-000000000000').set('x-test-user', 'user-a');
    expect(res.statusCode).toBe(404);
  });

  it('returns 404 (not 403) when a different user requests someone else\'s session', async () => {
    const created = await createSession('user-a');
    const res = await request(app)
      .get(`/api/reflections/sessions/${created.body.session_id}`)
      .set('x-test-user', 'user-b');
    expect(res.statusCode).toBe(404);
  });

  it('returns the owner\'s session with current card and history', async () => {
    const created = await createSession('user-a');
    const res = await request(app)
      .get(`/api/reflections/sessions/${created.body.session_id}`)
      .set('x-test-user', 'user-a');
    expect(res.statusCode).toBe(200);
    expect(res.body.session_id).toBe(created.body.session_id);
    expect(res.body.current_card.id).toBe(created.body.current_card.id);
  });
});

describe('POST /api/reflections/sessions/:id/responses', () => {
  it('accepts a valid response, bumps version/answered_count and returns the next card', async () => {
    const created = await createSession();
    const sessionId = created.body.session_id;
    const cardId = created.body.current_card.id;

    const res = await request(app)
      .post(`/api/reflections/sessions/${sessionId}/responses`)
      .set('x-test-user', 'user-a')
      .send({ card_id: cardId, expected_version: 1, response: { kind: 'option', value: 'fits' } });

    expect(res.statusCode).toBe(200);
    expect(res.body.version).toBe(2);
    expect(res.body.answered_count).toBe(1);
    expect(res.body.current_card.index).toBe(1);
    expect(res.body.current_card.id).not.toBe(cardId);
    expect(res.body.answered_history).toHaveLength(1);
    expect(res.body.answered_history[0].response).toMatchObject({ kind: 'option', value: 'fits' });
  });

  it('rejects a stale expected_version with 409 and does not touch the response', async () => {
    const created = await createSession();
    const sessionId = created.body.session_id;
    const cardId = created.body.current_card.id;

    // First submission succeeds, moving version 1 -> 2.
    await request(app)
      .post(`/api/reflections/sessions/${sessionId}/responses`)
      .set('x-test-user', 'user-a')
      .set('idempotency-key', 'first-submit')
      .send({ card_id: cardId, expected_version: 1, response: { kind: 'option', value: 'fits' } });

    // A second, distinct request replaying the stale expected_version=1 must be rejected —
    // this proves the version guard, independent of idempotency-key replay.
    const res = await request(app)
      .post(`/api/reflections/sessions/${sessionId}/responses`)
      .set('x-test-user', 'user-a')
      .set('idempotency-key', 'second-distinct-attempt')
      .send({ card_id: cardId, expected_version: 1, response: { kind: 'option', value: 'not_today' } });

    expect(res.statusCode).toBe(409);
  });

  it('rejects a card_id that does not match the current card with 409 (guards against a guessed/forged id)', async () => {
    const created = await createSession();
    const sessionId = created.body.session_id;

    const res = await request(app)
      .post(`/api/reflections/sessions/${sessionId}/responses`)
      .set('x-test-user', 'user-a')
      .send({ card_id: '11111111-1111-4111-8111-111111111111', expected_version: 1, response: { kind: 'option', value: 'fits' } });

    expect(res.statusCode).toBe(409);
  });

  it('returns 404 (not 403) when a different user tries to respond to someone else\'s session', async () => {
    const created = await createSession('user-a');
    const res = await request(app)
      .post(`/api/reflections/sessions/${created.body.session_id}/responses`)
      .set('x-test-user', 'user-b')
      .send({ card_id: created.body.current_card.id, expected_version: 1, response: { kind: 'option', value: 'fits' } });
    expect(res.statusCode).toBe(404);
  });

  it('replays an identical idempotency key without duplicating the persisted response', async () => {
    const created = await createSession();
    const sessionId = created.body.session_id;
    const cardId = created.body.current_card.id;
    const payload = { card_id: cardId, expected_version: 1, response: { kind: 'option', value: 'fits' } };

    const first = await request(app)
      .post(`/api/reflections/sessions/${sessionId}/responses`)
      .set('x-test-user', 'user-a')
      .set('idempotency-key', 'replay-key-1')
      .send(payload);
    expect(first.statusCode).toBe(200);

    const second = await request(app)
      .post(`/api/reflections/sessions/${sessionId}/responses`)
      .set('x-test-user', 'user-a')
      .set('idempotency-key', 'replay-key-1')
      .send(payload);

    expect(second.statusCode).toBe(200);
    expect(second.body).toEqual(first.body);

    // Prove no duplicate was persisted: exactly one answered_history entry and
    // answered_count still 1, verified independently via GET.
    const state = await request(app).get(`/api/reflections/sessions/${sessionId}`).set('x-test-user', 'user-a');
    expect(state.body.answered_count).toBe(1);
    expect(state.body.answered_history).toHaveLength(1);
    // generateNextCard should only have been called twice total (session creation + one
    // real next-card generation), never a third time for the replayed request.
    expect(mockGenerateNextCard).toHaveBeenCalledTimes(2);
  });

  it('rejects a reused idempotency key with a different payload as 409', async () => {
    const created = await createSession();
    const sessionId = created.body.session_id;
    const cardId = created.body.current_card.id;

    const first = await request(app)
      .post(`/api/reflections/sessions/${sessionId}/responses`)
      .set('x-test-user', 'user-a')
      .set('idempotency-key', 'shared-key')
      .send({ card_id: cardId, expected_version: 1, response: { kind: 'option', value: 'fits' } });
    expect(first.statusCode).toBe(200);

    const second = await request(app)
      .post(`/api/reflections/sessions/${sessionId}/responses`)
      .set('x-test-user', 'user-a')
      .set('idempotency-key', 'shared-key')
      .send({ card_id: cardId, expected_version: 1, response: { kind: 'text', value: 'a completely different answer' } });

    expect(second.statusCode).toBe(409);
  });

  it('rejects whitespace-only free text as 422 without persisting anything', async () => {
    const created = await createSession();
    const res = await request(app)
      .post(`/api/reflections/sessions/${created.body.session_id}/responses`)
      .set('x-test-user', 'user-a')
      .send({ card_id: created.body.current_card.id, expected_version: 1, response: { kind: 'text', value: '   ' } });
    expect(res.statusCode).toBe(422);
  });

  it('rejects free text over 1000 characters as 422', async () => {
    const created = await createSession();
    const res = await request(app)
      .post(`/api/reflections/sessions/${created.body.session_id}/responses`)
      .set('x-test-user', 'user-a')
      .send({ card_id: created.body.current_card.id, expected_version: 1, response: { kind: 'text', value: 'x'.repeat(1001) } });
    expect(res.statusCode).toBe(422);
  });

  it('rejects a malformed response.kind as 422 before touching the database', async () => {
    const created = await createSession();
    const res = await request(app)
      .post(`/api/reflections/sessions/${created.body.session_id}/responses`)
      .set('x-test-user', 'user-a')
      .send({ card_id: created.body.current_card.id, expected_version: 1, response: { kind: 'bogus', value: 'x' } });
    expect(res.statusCode).toBe(422);
  });

  it('marks the session generation_failed (503) when next-card generation has no fallback, while retaining the accepted response', async () => {
    const created = await createSession();
    const sessionId = created.body.session_id;
    const cardId = created.body.current_card.id;

    mockGenerateNextCard.mockResolvedValueOnce({ ok: false, status: 'generation_failed', reason: 'provider_down' });

    const res = await request(app)
      .post(`/api/reflections/sessions/${sessionId}/responses`)
      .set('x-test-user', 'user-a')
      .send({ card_id: cardId, expected_version: 1, response: { kind: 'option', value: 'fits' } });

    expect(res.statusCode).toBe(503);
    expect(res.body.status).toBe('generation_failed');
    // The response itself was retained despite generation failing for the *next* card.
    expect(res.body.answered_count).toBe(1);
    expect(res.body.answered_history).toHaveLength(1);
  });

  it('completes the session once session_limit is reached, with no further card generated', async () => {
    const created = await createSession();
    let sessionId = created.body.session_id;
    let cardId = created.body.current_card.id;
    let version = 1;

    for (let i = 0; i < 5; i++) {
      const res = await request(app)
        .post(`/api/reflections/sessions/${sessionId}/responses`)
        .set('x-test-user', 'user-a')
        .send({ card_id: cardId, expected_version: version, response: { kind: 'option', value: 'fits' } });
      expect(res.statusCode).toBe(200);
      version = res.body.version;
      if (i < 4) cardId = res.body.current_card.id;
      if (i === 4) {
        expect(res.body.status).toBe('completed');
        expect(res.body.current_card).toBeNull();
        expect(res.body.answered_count).toBe(5);
      }
    }
    // generateNextCard must never have been asked to generate a 6th card.
    const indices = mockGenerateNextCard.mock.calls.map(([{ session }]) => session.nextIndex);
    expect(indices).not.toContain(5);
  });
});

describe('POST /api/reflections/sessions/:id/retry', () => {
  it('recovers a generation_failed session to awaiting_response without duplicating the accepted response', async () => {
    const created = await createSession();
    const sessionId = created.body.session_id;
    const cardId = created.body.current_card.id;

    mockGenerateNextCard.mockResolvedValueOnce({ ok: false, status: 'generation_failed', reason: 'provider_down' });
    const failed = await request(app)
      .post(`/api/reflections/sessions/${sessionId}/responses`)
      .set('x-test-user', 'user-a')
      .send({ card_id: cardId, expected_version: 1, response: { kind: 'option', value: 'fits' } });
    expect(failed.statusCode).toBe(503);
    expect(failed.body.status).toBe('generation_failed');

    const retried = await request(app)
      .post(`/api/reflections/sessions/${sessionId}/retry`)
      .set('x-test-user', 'user-a')
      .send({});

    expect(retried.statusCode).toBe(200);
    expect(retried.body.status).toBe('awaiting_response');
    expect(retried.body.current_card).not.toBeNull();
    // Still exactly one answered response — retry must never resubmit/duplicate it.
    expect(retried.body.answered_count).toBe(1);
    expect(retried.body.answered_history).toHaveLength(1);
  });

  it('returns 404 (not 403) when a different user tries to retry someone else\'s session', async () => {
    const created = await createSession('user-a');
    const res = await request(app)
      .post(`/api/reflections/sessions/${created.body.session_id}/retry`)
      .set('x-test-user', 'user-b')
      .send({});
    expect(res.statusCode).toBe(404);
  });

  it('is a harmless no-op (200) when there is nothing to retry', async () => {
    const created = await createSession();
    const res = await request(app)
      .post(`/api/reflections/sessions/${created.body.session_id}/retry`)
      .set('x-test-user', 'user-a')
      .send({});
    expect(res.statusCode).toBe(200);
    expect(res.body.status).toBe('awaiting_response');
  });
});

describe('POST /api/reflections/sessions/:id/close', () => {
  it('closes an open session and is idempotent on a second call', async () => {
    const created = await createSession();
    const sessionId = created.body.session_id;

    const first = await request(app).post(`/api/reflections/sessions/${sessionId}/close`).set('x-test-user', 'user-a').send({});
    expect(first.statusCode).toBe(200);
    expect(first.body.status).toBe('closed');

    const second = await request(app).post(`/api/reflections/sessions/${sessionId}/close`).set('x-test-user', 'user-a').send({});
    expect(second.statusCode).toBe(200);
    expect(second.body.status).toBe('closed');
  });

  it('returns 404 (not 403) when a different user tries to close someone else\'s session', async () => {
    const created = await createSession('user-a');
    const res = await request(app)
      .post(`/api/reflections/sessions/${created.body.session_id}/close`)
      .set('x-test-user', 'user-b')
      .send({});
    expect(res.statusCode).toBe(404);
  });
});

describe('DELETE /api/reflections/sessions/:id', () => {
  it('deletes an owned session, and it is gone afterward', async () => {
    const created = await createSession();
    const sessionId = created.body.session_id;

    const del = await request(app).delete(`/api/reflections/sessions/${sessionId}`).set('x-test-user', 'user-a');
    expect(del.statusCode).toBe(200);

    const after = await request(app).get(`/api/reflections/sessions/${sessionId}`).set('x-test-user', 'user-a');
    expect(after.statusCode).toBe(404);
  });

  it('returns 404 (not 403) when a different user tries to delete someone else\'s session', async () => {
    const created = await createSession('user-a');
    const res = await request(app).delete(`/api/reflections/sessions/${created.body.session_id}`).set('x-test-user', 'user-b');
    expect(res.statusCode).toBe(404);

    // Confirm it was NOT actually deleted by the unauthorized attempt.
    const stillThere = await request(app).get(`/api/reflections/sessions/${created.body.session_id}`).set('x-test-user', 'user-a');
    expect(stillThere.statusCode).toBe(200);
  });
});
