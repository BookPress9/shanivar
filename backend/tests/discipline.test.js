const request = require('supertest');
const express = require('express');

jest.mock('../src/middleware/auth', () => ({
  authenticateToken: (req, res, next) => {
    req.user = { id: 'test-user-uuid' };
    next();
  }
}));

const mockQuery = jest.fn();
jest.mock('../src/db', () => ({
  query: (...args) => mockQuery(...args),
  pool: null // module-load-time schema self-heal is a no-op when pool is falsy
}));

const disciplineRoutes = require('../src/routes/discipline');

const app = express();
app.use(express.json());
app.use('/api/discipline', disciplineRoutes);

const OWNED_ALARM_ID = '11111111-1111-1111-1111-111111111111';
const OTHER_ALARM_ID = '22222222-2222-2222-2222-222222222222';
const OWNED_SESSION_ID = '33333333-3333-3333-3333-333333333333';

beforeEach(() => {
  mockQuery.mockReset();
});

// ─────────────────────────────────────────────────────────────────────────
// Preferences
// ─────────────────────────────────────────────────────────────────────────
describe('GET /api/discipline/preferences', () => {
  it('returns existing preferences without inserting a new row', async () => {
    mockQuery.mockImplementation((text) => {
      if (text.includes('SELECT * FROM discipline_preferences')) {
        return Promise.resolve({ rows: [{ id: 'p1', user_id: 'test-user-uuid', wake_time: '06:00:00' }] });
      }
      return Promise.resolve({ rows: [] });
    });
    const res = await request(app).get('/api/discipline/preferences');
    expect(res.statusCode).toEqual(200);
    expect(res.body.id).toEqual('p1');
    const insertCalls = mockQuery.mock.calls.filter(([text]) => text.includes('INSERT INTO discipline_preferences'));
    expect(insertCalls).toHaveLength(0);
  });

  it('creates default preferences on first read for a user with none yet', async () => {
    let selectCount = 0;
    mockQuery.mockImplementation((text) => {
      if (text.includes('INSERT INTO discipline_preferences')) {
        return Promise.resolve({ rows: [{ id: 'p1', user_id: 'test-user-uuid' }] });
      }
      if (text.includes('SELECT * FROM discipline_preferences')) {
        selectCount += 1;
        return Promise.resolve({ rows: [] });
      }
      return Promise.resolve({ rows: [] });
    });
    const res = await request(app).get('/api/discipline/preferences');
    expect(res.statusCode).toEqual(200);
    expect(res.body.id).toEqual('p1');
    expect(selectCount).toEqual(1);
  });
});

describe('PUT /api/discipline/preferences', () => {
  it('rejects an invalid wake_time', async () => {
    const res = await request(app).put('/api/discipline/preferences').send({ wake_time: '25:99' });
    expect(res.statusCode).toEqual(400);
  });

  it('rejects an invalid dismissal_method', async () => {
    const res = await request(app).put('/api/discipline/preferences').send({ dismissal_method: 'puzzle' });
    expect(res.statusCode).toEqual(400);
  });

  it('rejects a malformed days_of_week', async () => {
    const res = await request(app).put('/api/discipline/preferences').send({ days_of_week: [0, 9] });
    expect(res.statusCode).toEqual(400);
  });

  it('rejects an oversized block_package_list', async () => {
    const res = await request(app)
      .put('/api/discipline/preferences')
      .send({ block_package_list: Array.from({ length: 51 }, (_, i) => `pkg.${i}`) });
    expect(res.statusCode).toEqual(400);
  });

  it('accepts a valid partial update', async () => {
    mockQuery.mockImplementation((text) => {
      if (text.includes('UPDATE discipline_preferences')) {
        return Promise.resolve({ rows: [{ id: 'p1', dismissal_method: 'photo_motion' }] });
      }
      return Promise.resolve({ rows: [] });
    });
    const res = await request(app).put('/api/discipline/preferences').send({ dismissal_method: 'photo_motion' });
    expect(res.statusCode).toEqual(200);
    expect(res.body.dismissal_method).toEqual('photo_motion');
  });

  it('accepts the movement-first rhythm fields', async () => {
    mockQuery.mockImplementation((text) => {
      if (text.includes('UPDATE discipline_preferences')) {
        return Promise.resolve({ rows: [{ id: 'p1', dismissal_method: 'movement', movement_destination: 'kitchen', step_target: 16 }] });
      }
      return Promise.resolve({ rows: [] });
    });
    const res = await request(app).put('/api/discipline/preferences').send({
      dismissal_method: 'movement', movement_destination: 'kitchen', step_target: 16,
      scene_confirmation: true, quiet_time: '21:30', focus_minutes: 45,
      preferred_language: 'hi', whatsapp_time: '08:00', whatsapp_tone: 'gentle', daily_note_enabled: true
    });
    expect(res.statusCode).toEqual(200);
    expect(res.body.movement_destination).toEqual('kitchen');
  });
});

// ─────────────────────────────────────────────────────────────────────────
// Focus sessions
// ─────────────────────────────────────────────────────────────────────────
describe('POST /api/discipline/focus/start', () => {
  it('rejects a duration below the minimum', async () => {
    const res = await request(app).post('/api/discipline/focus/start').send({ duration_minutes: 1 });
    expect(res.statusCode).toEqual(400);
  });

  it('rejects a duration above the maximum', async () => {
    const res = await request(app).post('/api/discipline/focus/start').send({ duration_minutes: 10000 });
    expect(res.statusCode).toEqual(400);
  });

  it('rejects a non-integer duration', async () => {
    const res = await request(app).post('/api/discipline/focus/start').send({ duration_minutes: 'soon' });
    expect(res.statusCode).toEqual(400);
  });

  it('refuses to start a second session while one is already active', async () => {
    mockQuery.mockImplementation((text) => {
      if (text.includes("status = 'active'") && text.includes('SELECT')) {
        return Promise.resolve({ rows: [{ id: OWNED_SESSION_ID, status: 'active' }] });
      }
      return Promise.resolve({ rows: [] });
    });
    const res = await request(app).post('/api/discipline/focus/start').send({ duration_minutes: 30 });
    expect(res.statusCode).toEqual(409);
    const insertCalls = mockQuery.mock.calls.filter(([text]) => text.includes('INSERT INTO focus_sessions'));
    expect(insertCalls).toHaveLength(0);
  });

  it('blocks starting a session for a user with membership=free (not entitled)', async () => {
    mockQuery.mockImplementation((text) => {
      if (text.includes("status = 'active'") && text.includes('SELECT')) return Promise.resolve({ rows: [] });
      if (text.includes('SELECT membership FROM users')) return Promise.resolve({ rows: [{ membership: 'free' }] });
      return Promise.resolve({ rows: [] });
    });
    const res = await request(app).post('/api/discipline/focus/start').send({ duration_minutes: 30 });
    expect(res.statusCode).toEqual(402);
    const insertCalls = mockQuery.mock.calls.filter(([text]) => text.includes('INSERT INTO focus_sessions'));
    expect(insertCalls).toHaveLength(0);
  });

  it('starts a session for an entitled (trial) user, defaulting to the block list from preferences', async () => {
    mockQuery.mockImplementation((text) => {
      if (text.includes("status = 'active'") && text.includes('SELECT')) return Promise.resolve({ rows: [] });
      if (text.includes('SELECT membership FROM users')) return Promise.resolve({ rows: [{ membership: 'trial' }] });
      if (text.includes('SELECT block_package_list FROM discipline_preferences')) {
        return Promise.resolve({ rows: [{ block_package_list: ['com.instagram.android'] }] });
      }
      if (text.includes('INSERT INTO focus_sessions')) {
        return Promise.resolve({
          rows: [{ id: OWNED_SESSION_ID, status: 'active', entitlement_check: true, block_package_list: ['com.instagram.android'] }]
        });
      }
      return Promise.resolve({ rows: [] });
    });
    const res = await request(app).post('/api/discipline/focus/start').send({ duration_minutes: 30 });
    expect(res.statusCode).toEqual(201);
    expect(res.body.id).toEqual(OWNED_SESSION_ID);
    expect(res.body.entitlement_check).toEqual(true);
  });
});

describe('POST /api/discipline/focus/:id/release', () => {
  it('404s for a session id that is not a valid UUID', async () => {
    const res = await request(app).post('/api/discipline/focus/not-a-uuid/release');
    expect(res.statusCode).toEqual(404);
  });

  it('404s for a session that does not belong to the requesting user', async () => {
    mockQuery.mockImplementation((text) => {
      if (text.includes('SELECT * FROM focus_sessions WHERE id')) return Promise.resolve({ rows: [] });
      return Promise.resolve({ rows: [] });
    });
    const res = await request(app).post(`/api/discipline/focus/${OWNED_SESSION_ID}/release`);
    expect(res.statusCode).toEqual(404);
  });

  it('releases an active session', async () => {
    mockQuery.mockImplementation((text) => {
      if (text.includes('SELECT * FROM focus_sessions WHERE id')) {
        return Promise.resolve({ rows: [{ id: OWNED_SESSION_ID, status: 'active' }] });
      }
      if (text.includes("SET status = 'released'")) {
        return Promise.resolve({ rows: [{ id: OWNED_SESSION_ID, status: 'released' }] });
      }
      return Promise.resolve({ rows: [] });
    });
    const res = await request(app).post(`/api/discipline/focus/${OWNED_SESSION_ID}/release`);
    expect(res.statusCode).toEqual(200);
    expect(res.body.status).toEqual('released');
  });

  it('is idempotent: releasing an already-released session returns 200, not an error', async () => {
    mockQuery.mockImplementation((text) => {
      if (text.includes('SELECT * FROM focus_sessions WHERE id')) {
        return Promise.resolve({ rows: [{ id: OWNED_SESSION_ID, status: 'released' }] });
      }
      return Promise.resolve({ rows: [] });
    });
    const res = await request(app).post(`/api/discipline/focus/${OWNED_SESSION_ID}/release`);
    expect(res.statusCode).toEqual(200);
    expect(res.body.status).toEqual('released');
    const updateCalls = mockQuery.mock.calls.filter(([text]) => text.includes("SET status = 'released'"));
    expect(updateCalls).toHaveLength(0);
  });
});

describe('GET /api/discipline/focus/active', () => {
  it('returns active: null when there is no active session', async () => {
    mockQuery.mockResolvedValue({ rows: [] });
    const res = await request(app).get('/api/discipline/focus/active');
    expect(res.statusCode).toEqual(200);
    expect(res.body.active).toBeNull();
  });

  it('sweeps stale sessions before reporting active status', async () => {
    mockQuery.mockImplementation((text) => {
      if (text.includes("SELECT * FROM focus_sessions WHERE user_id = \$1 AND status = 'active'")) {
        return Promise.resolve({ rows: [{ id: OWNED_SESSION_ID, status: 'active' }] });
      }
      return Promise.resolve({ rows: [] });
    });
    const res = await request(app).get('/api/discipline/focus/active');
    expect(res.statusCode).toEqual(200);
    expect(res.body.active.id).toEqual(OWNED_SESSION_ID);
    const sweepCalls = mockQuery.mock.calls.filter(([text]) => text.includes("SET status = 'expired'"));
    expect(sweepCalls.length).toBeGreaterThan(0);
  });
});

// ─────────────────────────────────────────────────────────────────────────
// Alarms
// ─────────────────────────────────────────────────────────────────────────
describe('POST /api/discipline/alarms', () => {
  it('rejects a missing/invalid local_time', async () => {
    const res = await request(app).post('/api/discipline/alarms').send({ local_time: 'not-a-time' });
    expect(res.statusCode).toEqual(400);
  });

  it('rejects an invalid dismissal_method', async () => {
    const res = await request(app).post('/api/discipline/alarms').send({ local_time: '06:30', dismissal_method: 'nope' });
    expect(res.statusCode).toEqual(400);
  });

  it('creates an alarm with valid input', async () => {
    mockQuery.mockResolvedValue({ rows: [{ id: OWNED_ALARM_ID, local_time: '06:30:00', dismissal_method: 'qr' }] });
    const res = await request(app).post('/api/discipline/alarms').send({ local_time: '06:30', dismissal_method: 'qr' });
    expect(res.statusCode).toEqual(201);
    expect(res.body.id).toEqual(OWNED_ALARM_ID);
  });

  it('creates a movement alarm with a destination and step target', async () => {
    mockQuery.mockResolvedValue({ rows: [{ id: OWNED_ALARM_ID, dismissal_method: 'movement', movement_destination: 'morning_light', step_target: 18 }] });
    const res = await request(app).post('/api/discipline/alarms').send({
      local_time: '06:30', dismissal_method: 'movement', movement_destination: 'morning_light', step_target: 18
    });
    expect(res.statusCode).toEqual(201);
    expect(res.body.dismissal_method).toEqual('movement');
  });
});

describe('PUT /api/discipline/alarms/:id', () => {
  it('404s when the alarm does not belong to the requesting user', async () => {
    mockQuery.mockImplementation((text) => {
      if (text.includes('SELECT * FROM alarm_schedules WHERE id')) return Promise.resolve({ rows: [] });
      return Promise.resolve({ rows: [] });
    });
    const res = await request(app).put(`/api/discipline/alarms/${OTHER_ALARM_ID}`).send({ local_time: '07:00' });
    expect(res.statusCode).toEqual(404);
    const updateCalls = mockQuery.mock.calls.filter(([text]) => text.includes('UPDATE alarm_schedules'));
    expect(updateCalls).toHaveLength(0);
  });

  it('updates an owned alarm and resets next_confirmed_occurrence', async () => {
    mockQuery.mockImplementation((text) => {
      if (text.includes('SELECT * FROM alarm_schedules WHERE id')) {
        return Promise.resolve({ rows: [{ id: OWNED_ALARM_ID, user_id: 'test-user-uuid' }] });
      }
      if (text.includes('UPDATE alarm_schedules')) {
        expect(text).toContain('next_confirmed_occurrence = NULL');
        return Promise.resolve({ rows: [{ id: OWNED_ALARM_ID, local_time: '07:00:00', next_confirmed_occurrence: null }] });
      }
      return Promise.resolve({ rows: [] });
    });
    const res = await request(app).put(`/api/discipline/alarms/${OWNED_ALARM_ID}`).send({ local_time: '07:00' });
    expect(res.statusCode).toEqual(200);
    expect(res.body.next_confirmed_occurrence).toBeNull();
  });
});

describe('DELETE /api/discipline/alarms/:id', () => {
  it('404s when nothing was deleted (not owned / not found)', async () => {
    mockQuery.mockResolvedValue({ rows: [] });
    const res = await request(app).delete(`/api/discipline/alarms/${OTHER_ALARM_ID}`);
    expect(res.statusCode).toEqual(404);
  });

  it('deletes an owned alarm', async () => {
    mockQuery.mockResolvedValue({ rows: [{ id: OWNED_ALARM_ID }] });
    const res = await request(app).delete(`/api/discipline/alarms/${OWNED_ALARM_ID}`);
    expect(res.statusCode).toEqual(200);
  });
});

describe('POST /api/discipline/alarms/:id/confirm-scheduled', () => {
  it('404s for an alarm not owned by the requesting user', async () => {
    mockQuery.mockImplementation((text) => {
      if (text.includes('SELECT * FROM alarm_schedules WHERE id')) return Promise.resolve({ rows: [] });
      return Promise.resolve({ rows: [] });
    });
    const res = await request(app)
      .post(`/api/discipline/alarms/${OTHER_ALARM_ID}/confirm-scheduled`)
      .send({ next_occurrence: new Date().toISOString() });
    expect(res.statusCode).toEqual(404);
  });

  it('rejects a missing/invalid next_occurrence', async () => {
    mockQuery.mockImplementation((text) => {
      if (text.includes('SELECT * FROM alarm_schedules WHERE id')) {
        return Promise.resolve({ rows: [{ id: OWNED_ALARM_ID, user_id: 'test-user-uuid' }] });
      }
      return Promise.resolve({ rows: [] });
    });
    const res = await request(app)
      .post(`/api/discipline/alarms/${OWNED_ALARM_ID}/confirm-scheduled`)
      .send({ next_occurrence: 'not-a-date' });
    expect(res.statusCode).toEqual(400);
  });

  it('sets next_confirmed_occurrence for an owned alarm', async () => {
    const iso = new Date(Date.now() + 3600000).toISOString();
    mockQuery.mockImplementation((text) => {
      if (text.includes('SELECT * FROM alarm_schedules WHERE id')) {
        return Promise.resolve({ rows: [{ id: OWNED_ALARM_ID, user_id: 'test-user-uuid' }] });
      }
      if (text.includes('SET next_confirmed_occurrence')) {
        return Promise.resolve({ rows: [{ id: OWNED_ALARM_ID, next_confirmed_occurrence: iso }] });
      }
      return Promise.resolve({ rows: [] });
    });
    const res = await request(app).post(`/api/discipline/alarms/${OWNED_ALARM_ID}/confirm-scheduled`).send({ next_occurrence: iso });
    expect(res.statusCode).toEqual(200);
    expect(res.body.next_confirmed_occurrence).toEqual(iso);
  });
});

describe('POST /api/discipline/alarms/dismiss', () => {
  it('rejects an invalid method', async () => {
    const res = await request(app).post('/api/discipline/alarms/dismiss').send({ method: 'shout', verified: true });
    expect(res.statusCode).toEqual(400);
  });

  it('rejects a non-boolean verified', async () => {
    const res = await request(app).post('/api/discipline/alarms/dismiss').send({ method: 'qr', verified: 'yes' });
    expect(res.statusCode).toEqual(400);
  });

  it('rejects an oversized verification_detail', async () => {
    const res = await request(app)
      .post('/api/discipline/alarms/dismiss')
      .send({ method: 'photo_motion', verified: true, verification_detail: { blob: 'x'.repeat(3000) } });
    expect(res.statusCode).toEqual(400);
  });

  it('404s when alarm_schedule_id is supplied but not owned by the requester', async () => {
    mockQuery.mockImplementation((text) => {
      if (text.includes('SELECT * FROM alarm_schedules WHERE id')) return Promise.resolve({ rows: [] });
      return Promise.resolve({ rows: [] });
    });
    const res = await request(app)
      .post('/api/discipline/alarms/dismiss')
      .send({ alarm_schedule_id: OTHER_ALARM_ID, method: 'qr', verified: true });
    expect(res.statusCode).toEqual(404);
  });

  it('records a dismissal event without an alarm_schedule_id (manual test dismissal)', async () => {
    mockQuery.mockImplementation((text) => {
      if (text.includes('INSERT INTO alarm_dismissal_events')) {
        return Promise.resolve({ rows: [{ id: 'evt-1', method: 'photo_motion', verified: true }] });
      }
      return Promise.resolve({ rows: [] });
    });
    const res = await request(app)
      .post('/api/discipline/alarms/dismiss')
      .send({ method: 'photo_motion', verified: true, verification_detail: { motion_magnitude: 12.4 } });
    expect(res.statusCode).toEqual(201);
    expect(res.body.id).toEqual('evt-1');
  });
});
