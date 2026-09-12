const express = require('express');
const crypto = require('crypto');
const rateLimit = require('express-rate-limit');
const moment = require('moment-timezone');
const router = express.Router();

const db = require('../db');
const { pool } = require('../db');
const { authenticateToken } = require('../middleware/auth');
const { sanitizeText } = require('../utils/sanitize');
// Real interface built by the Generation Service Agent (Agent 3). buildContextSnapshot
// computes real chart/transit facts once at session creation; generateNextCard produces
// (and schema-validates) each card, falling back to a clearly-labeled general reflection
// on provider failure rather than ever leaving the session without a usable next card.
// See backend/src/utils/reflectionGeneration.js's module header for the exact contract.
const { buildContextSnapshot, generateNextCard } = require('../utils/reflectionGeneration');

// ─────────────────────────────────────────────────────────────────────────
// Self-healing schema (idempotent inline migration), same convention as
// backend/src/db.js's pool.on('connect', ...) block and
// backend/src/routes/ecommerce.js's schema block: CREATE TABLE IF NOT EXISTS /
// CREATE UNIQUE INDEX IF NOT EXISTS rather than a separate migration runner,
// since none exists in this codebase. Runs once at module load against the
// live Supabase Postgres pool.
//
// Schema shape follows the handoff doc's "Persistence and concurrency" section:
//   - reflection_sessions: owner, context-snapshot (pinned at creation), generation
//     status, version, timestamps.
//   - reflection_cards: unique (session_id, index).
//   - reflection_responses: unique (session_id, card_id).
//   - reflection_idempotency_keys: scoped to (authenticated owner, operation),
//     retains the request hash + persisted result. response_status/response_body
//     are nullable so a request can atomically "claim" a key (INSERT ... ON
//     CONFLICT DO NOTHING) before doing any work, then fill in the result —
//     see withIdempotency() below for why this matters for concurrent replays.
// ─────────────────────────────────────────────────────────────────────────
if (pool && typeof pool.query === 'function') {
  pool.query(`
    CREATE TABLE IF NOT EXISTS reflection_sessions (
      id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
      user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      entry_point VARCHAR(20) NOT NULL DEFAULT 'today' CHECK (entry_point IN ('today', 'blocked_app')),
      timezone VARCHAR(64) NOT NULL,
      local_date DATE NOT NULL,
      mode VARCHAR(20) NOT NULL DEFAULT 'general' CHECK (mode IN ('personalized', 'limited_chart', 'general')),
      status VARCHAR(20) NOT NULL DEFAULT 'awaiting_response' CHECK (status IN ('awaiting_response', 'generating', 'generation_failed', 'completed', 'closed')),
      version INTEGER NOT NULL DEFAULT 1,
      context_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
      context_as_of TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      session_limit INTEGER NOT NULL DEFAULT 5,
      answered_count INTEGER NOT NULL DEFAULT 0,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      closed_at TIMESTAMPTZ
    );
    CREATE INDEX IF NOT EXISTS reflection_sessions_user_id_idx ON reflection_sessions (user_id);

    CREATE TABLE IF NOT EXISTS reflection_cards (
      id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
      session_id UUID NOT NULL REFERENCES reflection_sessions(id) ON DELETE CASCADE,
      index INTEGER NOT NULL,
      sender VARCHAR(50) NOT NULL DEFAULT 'Shanivar',
      message TEXT NOT NULL,
      question TEXT NOT NULL,
      options JSONB NOT NULL DEFAULT '[]'::jsonb,
      context_refs JSONB NOT NULL DEFAULT '[]'::jsonb,
      provenance_label VARCHAR(100) NOT NULL DEFAULT 'general',
      generation_status VARCHAR(20) NOT NULL DEFAULT 'ready' CHECK (generation_status IN ('ready', 'failed')),
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
    CREATE UNIQUE INDEX IF NOT EXISTS reflection_cards_session_index_key ON reflection_cards (session_id, index);

    CREATE TABLE IF NOT EXISTS reflection_responses (
      id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
      session_id UUID NOT NULL REFERENCES reflection_sessions(id) ON DELETE CASCADE,
      card_id UUID NOT NULL REFERENCES reflection_cards(id) ON DELETE CASCADE,
      kind VARCHAR(10) NOT NULL CHECK (kind IN ('option', 'text')),
      value TEXT NOT NULL,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
    CREATE UNIQUE INDEX IF NOT EXISTS reflection_responses_session_card_key ON reflection_responses (session_id, card_id);
    CREATE INDEX IF NOT EXISTS reflection_responses_session_id_idx ON reflection_responses (session_id);

    CREATE TABLE IF NOT EXISTS reflection_idempotency_keys (
      id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
      user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      operation VARCHAR(150) NOT NULL,
      idempotency_key VARCHAR(255) NOT NULL,
      request_hash VARCHAR(128) NOT NULL,
      response_status INTEGER,
      response_body JSONB,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
    CREATE UNIQUE INDEX IF NOT EXISTS reflection_idempotency_owner_op_key
      ON reflection_idempotency_keys (user_id, operation, idempotency_key);
  `).catch((err) => {
    console.error('❌ Failed to ensure reflection_* schema:', err.message);
  });
}

// ─────────────────────────────────────────────────────────────────────────
// Small helpers
// ─────────────────────────────────────────────────────────────────────────

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function hashRequest(payload) {
  return crypto.createHash('sha256').update(JSON.stringify(payload || {})).digest('hex');
}

function getIdempotencyKey(req) {
  const headerKey = req.headers['idempotency-key'];
  const bodyKey = req.body && req.body.idempotency_key;
  const raw = headerKey || bodyKey;
  if (typeof raw !== 'string') return null;
  const trimmed = raw.trim();
  return trimmed.length > 0 && trimmed.length <= 255 ? trimmed : null;
}

// Postgres/connection error codes that mean "the database itself is temporarily
// unreachable/overloaded," as opposed to a genuine application bug. Mapped to the
// handoff doc's 503 "temporary unavailability before acceptance; retain the draft"
// semantics rather than a generic 500, since the client should retry the same
// request (optionally with the same idempotency key) instead of treating it as fatal.
const TRANSIENT_DB_ERROR_CODES = new Set([
  '53300', // too_many_connections
  '57P03', // cannot_connect_now
  '08000', '08001', '08003', '08004', '08006', '08007', // connection_exception family
  'ECONNREFUSED', 'ETIMEDOUT', 'ENOTFOUND', 'ECONNRESET'
]);
function isTransientDbError(err) {
  return !!(err && TRANSIENT_DB_ERROR_CODES.has(err.code));
}

// Runs `handler` under an idempotency guard scoped to (authenticated owner, operation).
// Concurrency-safe: the INSERT ... ON CONFLICT DO NOTHING atomically "claims" the key
// before any work happens, so two truly-concurrent requests with the same key can never
// both execute handler() — the loser sees response_status IS NULL (still being processed)
// and gets back a 503 telling it to retry/poll rather than re-running side effects.
// A reused key with a different request payload (different request_hash) is a 409 per
// the handoff doc. On handler failure the claim row is deleted so the same key can be
// retried later rather than getting stuck behind a permanently-pending claim.
async function withIdempotency({ userId, operation, idempotencyKey, requestPayload, handler }) {
  if (!idempotencyKey) {
    // No key supplied: caller gets no replay protection, but the underlying
    // version-guard/unique-constraint logic in each route still prevents duplicate
    // persistence — idempotency keys are the fast-path convenience, not the only guard.
    return handler();
  }

  const requestHash = hashRequest(requestPayload);

  const claimed = await db.query(
    `INSERT INTO reflection_idempotency_keys (user_id, operation, idempotency_key, request_hash)
     VALUES ($1, $2, $3, $4)
     ON CONFLICT (user_id, operation, idempotency_key) DO NOTHING
     RETURNING id`,
    [userId, operation, idempotencyKey, requestHash]
  );

  if (claimed.rows.length === 0) {
    // Someone already claimed this exact (user, operation, key) — either a completed
    // prior call (replay it) or a call still in flight (tell the client to wait/retry).
    const existing = await db.query(
      `SELECT request_hash, response_status, response_body FROM reflection_idempotency_keys
       WHERE user_id = $1 AND operation = $2 AND idempotency_key = $3`,
      [userId, operation, idempotencyKey]
    );
    const row = existing.rows[0];
    if (!row) {
      // Vanishingly unlikely (claim deleted between our INSERT and this SELECT) —
      // fall through and let the caller retry from scratch.
      return handler();
    }
    if (row.request_hash !== requestHash) {
      const err = new Error('Idempotency key reused with a different request');
      err.statusCode = 409;
      err.body = { error: 'This idempotency key was already used with a different request payload.' };
      throw err;
    }
    if (row.response_status === null) {
      const err = new Error('Idempotent request still processing');
      err.statusCode = 503;
      err.body = { error: 'The original request with this idempotency key is still being processed. Retry shortly.' };
      throw err;
    }
    const err = new Error('__idempotent_replay__');
    err.statusCode = row.response_status;
    err.body = row.response_body;
    err.isReplay = true;
    throw err;
  }

  const claimId = claimed.rows[0].id;
  let result;
  try {
    result = await handler();
  } catch (err) {
    // Release the claim so the client can retry with the same key after a genuine failure
    // instead of being stuck behind a stale pending claim forever. Note: if the failure
    // happened AFTER a response was already durably persisted (see the two-phase design
    // in POST /sessions/:id/responses), the persisted response itself is untouched by
    // this rollback of the *idempotency cache entry* — recovery for that case is GET
    // (poll session state) or POST /retry, per the handoff doc's documented recovery path.
    await db.query('DELETE FROM reflection_idempotency_keys WHERE id = $1', [claimId]).catch(() => {});
    throw err;
  }

  await db.query(
    `UPDATE reflection_idempotency_keys SET response_status = $1, response_body = $2 WHERE id = $3`,
    [result.status, JSON.stringify(result.body), claimId]
  ).catch((err) => {
    console.error('Failed to persist idempotency result (result already sent to client):', err.message);
  });

  return result;
}

// Sends whatever withIdempotency ultimately produced/threw, in one place, so every
// route below has identical replay/409/503 handling instead of repeating try/catch.
async function runIdempotent(req, res, { operation, requestPayload, handler }) {
  const idempotencyKey = getIdempotencyKey(req);
  try {
    const result = await withIdempotency({
      userId: req.user.id,
      operation,
      idempotencyKey,
      requestPayload,
      handler
    });
    return res.status(result.status).json(result.body);
  } catch (err) {
    if (err.statusCode && err.body) {
      return res.status(err.statusCode).json(err.body);
    }
    if (isTransientDbError(err)) {
      return res.status(503).json({ error: 'Temporarily unavailable, please retry.' });
    }
    console.error(`Reflections error (${operation}):`, err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

function isValidTimezone(tz) {
  return typeof tz === 'string' && tz.length > 0 && tz.length <= 64 && !!moment.tz.zone(tz);
}

// ─────────────────────────────────────────────────────────────────────────
// Envelope shaping — exactly the fields in the handoff doc's "Session envelope"
// and "Card" descriptions. Never leak internal columns (created_at on cards,
// raw context_snapshot, etc.) beyond what the contract specifies.
// ─────────────────────────────────────────────────────────────────────────
function cardEnvelope(card) {
  if (!card) return null;
  return {
    id: card.id,
    index: card.index,
    sender: card.sender,
    message: card.message,
    question: card.question,
    options: card.options,
    context_refs: card.context_refs,
    provenance_label: card.provenance_label
  };
}

function sessionEnvelope(session, currentCard, answeredHistory) {
  return {
    session_id: session.id,
    version: session.version,
    status: session.status,
    mode: session.mode,
    context_as_of: session.context_as_of,
    local_date: session.local_date,
    timezone: session.timezone,
    answered_count: session.answered_count,
    session_limit: session.session_limit,
    current_card: ['awaiting_response', 'generation_failed'].includes(session.status) ? cardEnvelope(currentCard) : null,
    answered_history: (answeredHistory || []).map(h => ({
      card: cardEnvelope(h.card),
      response: { kind: h.response_kind, value: h.response_value, created_at: h.response_created_at }
    }))
  };
}

async function loadAnsweredHistory(queryable, sessionId) {
  const res = await queryable.query(
    `SELECT c.id, c.index, c.sender, c.message, c.question, c.options, c.context_refs, c.provenance_label,
            r.kind AS response_kind, r.value AS response_value, r.created_at AS response_created_at
     FROM reflection_cards c
     JOIN reflection_responses r ON r.card_id = c.id AND r.session_id = c.session_id
     WHERE c.session_id = $1
     ORDER BY c.index ASC`,
    [sessionId]
  );
  return res.rows.map(row => ({
    card: row,
    response_kind: row.response_kind,
    response_value: row.response_value,
    response_created_at: row.response_created_at
  }));
}

// Shapes answered history into exactly what generateNextCard's `previousResponses`
// parameter expects: [{ index, question, response: { kind, value } }], oldest first.
function toGenerationHistory(historyRows) {
  return (historyRows || []).map(h => ({
    index: h.card.index,
    question: h.card.question,
    response: { kind: h.response_kind, value: h.response_value }
  }));
}

// Runs the generation-service interface and returns the card to persist — either the
// real generated card, or (on ok:false) the clearly-labeled fallback it always supplies.
// Only returns null in the defensive case where even the fallback is missing, which
// should not happen given reflectionGeneration.js's contract but is not assumed here.
async function resolveNextCard({ contextSnapshot, nextIndex, sessionLimit, previousResponses }) {
  const result = await generateNextCard({
    session: { contextSnapshot, nextIndex, sessionLimit },
    previousResponses
  });
  if (result && result.ok && result.card) return result.card;
  if (result && result.fallback) return result.fallback;
  return null;
}

// Ownership-scoped session lookup. Returns null for both "doesn't exist" and
// "belongs to another user" — callers must turn null into 404, never 403, matching
// the existing convention in habits.js (ownership check returns the same 404 either
// way rather than confirming a session ID exists for someone else).
async function findOwnedSession(client, sessionId, userId, { forUpdate = false } = {}) {
  if (!UUID_RE.test(sessionId)) return null;
  const res = await client.query(
    `SELECT * FROM reflection_sessions WHERE id = $1 AND user_id = $2${forUpdate ? ' FOR UPDATE' : ''}`,
    [sessionId, userId]
  );
  return res.rows[0] || null;
}

async function findCurrentCard(client, sessionId, index) {
  const res = await client.query(
    'SELECT * FROM reflection_cards WHERE session_id = $1 AND index = $2',
    [sessionId, index]
  );
  return res.rows[0] || null;
}

async function insertCardIfAbsent(client, { sessionId, index, card }) {
  const insert = await client.query(
    `INSERT INTO reflection_cards (session_id, index, sender, message, question, options, context_refs, provenance_label, generation_status)
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8, 'ready')
     ON CONFLICT (session_id, index) DO NOTHING
     RETURNING *`,
    [
      sessionId, index, card.sender || 'Shanivar', card.message, card.question,
      JSON.stringify(card.options || []), JSON.stringify(card.context_refs || []),
      card.provenance_label || 'general'
    ]
  );
  if (insert.rows.length > 0) return insert.rows[0];
  // Unique constraint already satisfied by a concurrent retry/duplicate job —
  // converge on whatever card actually won that slot instead of erroring.
  return findCurrentCard(client, sessionId, index);
}

// ─────────────────────────────────────────────────────────────────────────
// Rate limiting — the handoff doc's status-code contract requires 429 with retry
// guidance to be a real, reachable outcome, not just documented. Scoped to this
// router only (index.js already applies a much looser blanket /api/ limiter).
// ─────────────────────────────────────────────────────────────────────────
const reflectionsLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 300,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many reflection requests, please slow down and retry shortly.' }
});
router.use(reflectionsLimiter);

router.use(authenticateToken);

// ─────────────────────────────────────────────────────────────────────────
// POST /sessions
// ─────────────────────────────────────────────────────────────────────────
router.post('/sessions', async (req, res) => {
  const { entry_point, timezone } = req.body || {};
  const userId = req.user.id;

  const resolvedEntryPoint = entry_point === undefined ? 'today' : entry_point;
  if (!['today', 'blocked_app'].includes(resolvedEntryPoint)) {
    return res.status(422).json({ error: 'entry_point must be "today" or "blocked_app".' });
  }
  if (!isValidTimezone(timezone)) {
    return res.status(422).json({ error: 'timezone must be a valid IANA timezone string.' });
  }

  await runIdempotent(req, res, {
    operation: 'create_session',
    requestPayload: { entry_point: resolvedEntryPoint, timezone },
    handler: async () => {
      const contextSnapshot = await buildContextSnapshot({ userId, timezone });
      const sessionLimit = 5;

      // Generated OUTSIDE the database transaction, per the handoff doc's "Persistence
      // and concurrency" section — the Generation Service Agent's real implementation
      // makes a live Gemini call here, which must never run inside an open transaction.
      const firstCard = await resolveNextCard({
        contextSnapshot, nextIndex: 0, sessionLimit, previousResponses: []
      });

      const client = await pool.connect();
      try {
        await client.query('BEGIN');
        const sessionInsert = await client.query(
          `INSERT INTO reflection_sessions
             (user_id, entry_point, timezone, local_date, mode, status, context_snapshot, context_as_of, session_limit, answered_count)
           VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, 0)
           RETURNING *`,
          [
            userId, resolvedEntryPoint, timezone, contextSnapshot.localDate, contextSnapshot.mode,
            firstCard ? 'awaiting_response' : 'generation_failed',
            JSON.stringify(contextSnapshot), contextSnapshot.transitAsOfUtc, sessionLimit
          ]
        );
        const session = sessionInsert.rows[0];

        const cardRow = firstCard
          ? await insertCardIfAbsent(client, { sessionId: session.id, index: 0, card: firstCard })
          : null;

        await client.query('COMMIT');
        const body = sessionEnvelope(session, cardRow, []);
        return { status: firstCard ? 201 : 503, body };
      } catch (err) {
        await client.query('ROLLBACK').catch(() => {});
        throw err;
      } finally {
        client.release();
      }
    }
  });
});

// ─────────────────────────────────────────────────────────────────────────
// GET /sessions/:id
// ─────────────────────────────────────────────────────────────────────────
router.get('/sessions/:id', async (req, res) => {
  try {
    const session = await findOwnedSession(db, req.params.id, req.user.id);
    if (!session) return res.status(404).json({ error: 'Session not found' });

    const currentCard = ['awaiting_response', 'generation_failed'].includes(session.status)
      ? await findCurrentCard(db, session.id, session.answered_count)
      : null;
    const answeredHistory = await loadAnsweredHistory(db, session.id);

    res.json(sessionEnvelope(session, currentCard, answeredHistory));
  } catch (err) {
    console.error('GET /reflections/sessions/:id error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// ─────────────────────────────────────────────────────────────────────────
// POST /sessions/:id/responses — the core concurrency-sensitive route.
//
// Two-phase transaction design, per the handoff doc: "In a transaction, validate
// current card/version, persist the response and enqueue/reserve the next generation
// once. Generate outside the database transaction; persist exactly one next card under
// a unique constraint and version guard."
//   TXN 1: lock the session row (SELECT ... FOR UPDATE), validate version/card, insert
//          the response, bump version, mark status='generating'. Commit — this releases
//          the lock quickly and durably records that generation for the next index is
//          owed, so a crash here can be resumed later via POST /retry (status='generating'
//          or 'generation_failed' both mean "index `answered_count` still needs a card").
//   (generate outside any transaction)
//   TXN 2: re-lock the session, insert the next card under the UNIQUE (session_id, index)
//          constraint (defends against a concurrent duplicate retry/job winning the same
//          slot twice), then flip status to awaiting_response/completed, or
//          generation_failed only in the defensive case where even the generation
//          service's fallback card is unavailable.
// ─────────────────────────────────────────────────────────────────────────
router.post('/sessions/:id/responses', async (req, res) => {
  const { card_id, expected_version, response } = req.body || {};
  const userId = req.user.id;
  const sessionId = req.params.id;

  // Validate shape before touching the DB or the idempotency table at all.
  if (typeof card_id !== 'string' || !UUID_RE.test(card_id)) {
    return res.status(422).json({ error: 'card_id must be a valid card id.' });
  }
  if (!Number.isInteger(expected_version)) {
    return res.status(422).json({ error: 'expected_version must be an integer.' });
  }
  if (!response || typeof response !== 'object' || !['option', 'text'].includes(response.kind)) {
    return res.status(422).json({ error: 'response.kind must be "option" or "text".' });
  }
  let responseValue = response.value;
  if (response.kind === 'text') {
    if (typeof responseValue !== 'string') {
      return res.status(422).json({ error: 'response.value must be a string for kind "text".' });
    }
    responseValue = sanitizeText(responseValue).trim();
    if (responseValue.length < 1 || responseValue.length > 1000) {
      return res.status(422).json({ error: 'Free text response must be 1-1000 characters after trimming.' });
    }
  } else {
    if (typeof responseValue !== 'string' || responseValue.length === 0 || responseValue.length > 100) {
      return res.status(422).json({ error: 'response.value must be a non-empty option id.' });
    }
  }

  await runIdempotent(req, res, {
    operation: `submit_response:${sessionId}`,
    requestPayload: { card_id, expected_version, response: { kind: response.kind, value: responseValue } },
    handler: async () => {
      // ── TXN 1: validate + persist response + reserve the next generation slot ──
      const client = await pool.connect();
      let session, card, previousHistory;
      try {
        await client.query('BEGIN');
        session = await findOwnedSession(client, sessionId, userId, { forUpdate: true });
        if (!session) {
          await client.query('ROLLBACK');
          return { status: 404, body: { error: 'Session not found' } };
        }
        if (session.status === 'closed' || session.status === 'completed') {
          await client.query('ROLLBACK');
          return { status: 409, body: { error: `Session is ${session.status} and no longer accepts responses.` } };
        }
        if (session.status === 'generating' || session.status === 'generation_failed') {
          await client.query('ROLLBACK');
          return {
            status: 409,
            body: { error: 'No current card is ready to respond to. Fetch session state or call POST /retry.' }
          };
        }
        if (expected_version !== session.version) {
          await client.query('ROLLBACK');
          return { status: 409, body: { error: 'Stale version. Fetch the current session state and retry.' } };
        }

        card = await findCurrentCard(client, session.id, session.answered_count);
        if (!card || card.id !== card_id) {
          await client.query('ROLLBACK');
          return { status: 409, body: { error: 'card_id does not match the current card for this session.' } };
        }

        const insertResp = await client.query(
          `INSERT INTO reflection_responses (session_id, card_id, kind, value)
           VALUES ($1, $2, $3, $4)
           ON CONFLICT (session_id, card_id) DO NOTHING
           RETURNING *`,
          [session.id, card.id, response.kind, responseValue]
        );
        if (insertResp.rows.length === 0) {
          // Should be unreachable while we hold the session row lock for the whole
          // transaction — kept as defense-in-depth per the concurrency requirements.
          await client.query('ROLLBACK');
          return { status: 409, body: { error: 'This card was already answered.' } };
        }

        const newAnsweredCount = session.answered_count + 1;
        const reachedLimit = newAnsweredCount >= session.session_limit;
        const updated = await client.query(
          `UPDATE reflection_sessions
           SET version = version + 1, answered_count = $1, status = $2, updated_at = NOW()
           WHERE id = $3
           RETURNING *`,
          [newAnsweredCount, reachedLimit ? 'completed' : 'generating', session.id]
        );
        session = updated.rows[0];
        previousHistory = await loadAnsweredHistory(client, session.id);

        await client.query('COMMIT');
      } catch (err) {
        await client.query('ROLLBACK').catch(() => {});
        throw err;
      } finally {
        client.release();
      }

      if (session.status === 'completed') {
        // Session limit reached — no next card to generate. Nothing left to do.
        return { status: 200, body: sessionEnvelope(session, null, previousHistory) };
      }

      // ── generate OUTSIDE any transaction ──
      const nextCard = await resolveNextCard({
        contextSnapshot: session.context_snapshot,
        nextIndex: session.answered_count,
        sessionLimit: session.session_limit,
        previousResponses: toGenerationHistory(previousHistory)
      });

      // ── TXN 2: reserve/persist exactly one next card, then flip status ──
      const client2 = await pool.connect();
      try {
        await client2.query('BEGIN');
        const lockedRes = await client2.query('SELECT * FROM reflection_sessions WHERE id = $1 FOR UPDATE', [session.id]);
        session = lockedRes.rows[0];

        const cardRow = nextCard
          ? await insertCardIfAbsent(client2, { sessionId: session.id, index: session.answered_count, card: nextCard })
          : null;

        const finalStatus = nextCard ? 'awaiting_response' : 'generation_failed';
        const updated2 = await client2.query(
          `UPDATE reflection_sessions SET status = $1, updated_at = NOW() WHERE id = $2 RETURNING *`,
          [finalStatus, session.id]
        );
        session = updated2.rows[0];
        await client2.query('COMMIT');

        return {
          status: nextCard ? 200 : 503,
          body: sessionEnvelope(session, cardRow, previousHistory)
        };
      } catch (err) {
        await client2.query('ROLLBACK').catch(() => {});
        throw err;
      } finally {
        client2.release();
      }
    }
  });
});

// ─────────────────────────────────────────────────────────────────────────
// POST /sessions/:id/retry — re-attempt next-card generation/persistence after
// generation_failed, or resume a 'generating' session left stuck by a crash between
// TXN1 and TXN2 above (this backend has no separate async worker, so 'generating' can
// only mean "a prior request's TXN2 never completed" — safe to resume synchronously
// here). Never resubmits or duplicates the already-accepted response.
// ─────────────────────────────────────────────────────────────────────────
router.post('/sessions/:id/retry', async (req, res) => {
  const userId = req.user.id;
  const sessionId = req.params.id;

  await runIdempotent(req, res, {
    operation: `retry:${sessionId}`,
    requestPayload: {},
    handler: async () => {
      let session = await findOwnedSession(db, sessionId, userId);
      if (!session) return { status: 404, body: { error: 'Session not found' } };

      if (!['generation_failed', 'generating'].includes(session.status)) {
        // Nothing to retry — converge on current state rather than duplicating work.
        const currentCard = session.status === 'awaiting_response'
          ? await findCurrentCard(db, session.id, session.answered_count)
          : null;
        const history = await loadAnsweredHistory(db, session.id);
        return { status: 200, body: sessionEnvelope(session, currentCard, history) };
      }

      const previousHistory = await loadAnsweredHistory(db, session.id);
      const nextCard = await resolveNextCard({
        contextSnapshot: session.context_snapshot,
        nextIndex: session.answered_count,
        sessionLimit: session.session_limit,
        previousResponses: toGenerationHistory(previousHistory)
      });

      const client = await pool.connect();
      try {
        await client.query('BEGIN');
        const lockedRes = await client.query('SELECT * FROM reflection_sessions WHERE id = $1 AND user_id = $2 FOR UPDATE', [sessionId, userId]);
        session = lockedRes.rows[0];
        if (!session) {
          await client.query('ROLLBACK');
          return { status: 404, body: { error: 'Session not found' } };
        }
        if (!['generation_failed', 'generating'].includes(session.status)) {
          // State moved on (e.g. another retry already succeeded) while we generated —
          // converge on current state instead of overwriting it.
          await client.query('ROLLBACK');
          const currentCard = session.status === 'awaiting_response'
            ? await findCurrentCard(db, session.id, session.answered_count)
            : null;
          return { status: 200, body: sessionEnvelope(session, currentCard, previousHistory) };
        }

        const cardRow = nextCard
          ? await insertCardIfAbsent(client, { sessionId: session.id, index: session.answered_count, card: nextCard })
          : null;
        const finalStatus = nextCard ? 'awaiting_response' : 'generation_failed';
        const updated = await client.query(
          `UPDATE reflection_sessions SET status = $1, updated_at = NOW() WHERE id = $2 RETURNING *`,
          [finalStatus, session.id]
        );
        session = updated.rows[0];
        await client.query('COMMIT');
        return { status: nextCard ? 200 : 503, body: sessionEnvelope(session, cardRow, previousHistory) };
      } catch (err) {
        await client.query('ROLLBACK').catch(() => {});
        throw err;
      } finally {
        client.release();
      }
    }
  });
});

// ─────────────────────────────────────────────────────────────────────────
// POST /sessions/:id/close — end without answering; never touches app locks.
// ─────────────────────────────────────────────────────────────────────────
router.post('/sessions/:id/close', async (req, res) => {
  const userId = req.user.id;
  const sessionId = req.params.id;

  await runIdempotent(req, res, {
    operation: `close:${sessionId}`,
    requestPayload: {},
    handler: async () => {
      const client = await pool.connect();
      try {
        await client.query('BEGIN');
        let session = await findOwnedSession(client, sessionId, userId, { forUpdate: true });
        if (!session) {
          await client.query('ROLLBACK');
          return { status: 404, body: { error: 'Session not found' } };
        }
        if (session.status === 'closed' || session.status === 'completed') {
          // Idempotent no-op: closing an already-finished session is a harmless success.
          await client.query('COMMIT');
          const history = await loadAnsweredHistory(db, session.id);
          return { status: 200, body: sessionEnvelope(session, null, history) };
        }
        const updated = await client.query(
          `UPDATE reflection_sessions SET status = 'closed', closed_at = NOW(), version = version + 1, updated_at = NOW()
           WHERE id = $1 RETURNING *`,
          [session.id]
        );
        session = updated.rows[0];
        await client.query('COMMIT');
        const history = await loadAnsweredHistory(db, session.id);
        return { status: 200, body: sessionEnvelope(session, null, history) };
      } catch (err) {
        await client.query('ROLLBACK').catch(() => {});
        throw err;
      } finally {
        client.release();
      }
    }
  });
});

// ─────────────────────────────────────────────────────────────────────────
// DELETE /sessions/:id — removes owned reflection content/responses (cascades via
// FK ON DELETE CASCADE) and invalidates any cached idempotent results referencing it.
// ─────────────────────────────────────────────────────────────────────────
router.delete('/sessions/:id', async (req, res) => {
  try {
    if (!UUID_RE.test(req.params.id)) return res.status(404).json({ error: 'Session not found' });
    const result = await db.query(
      'DELETE FROM reflection_sessions WHERE id = $1 AND user_id = $2 RETURNING id',
      [req.params.id, req.user.id]
    );
    if (result.rows.length === 0) return res.status(404).json({ error: 'Session not found' });

    // Best-effort: stop a stale cached idempotent result (e.g. a cached responses/retry
    // reply) from ever being replayed against content that no longer exists.
    await db.query(
      `DELETE FROM reflection_idempotency_keys WHERE user_id = $1 AND operation LIKE $2`,
      [req.user.id, `%:${req.params.id}`]
    ).catch(() => {});

    res.json({ message: 'Reflection session deleted' });
  } catch (err) {
    console.error('DELETE /reflections/sessions/:id error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

module.exports = router;
