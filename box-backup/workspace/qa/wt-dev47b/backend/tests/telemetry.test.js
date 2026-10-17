const request = require('supertest');
const express = require('express');

jest.mock('../src/middleware/auth', () => ({
  authenticateToken: (req, res, next) => {
    req.user = { id: 'test-user-uuid' };
    next();
  },
  requireAdmin: (req, res, next) => next(),
}));

const mockQuery = jest.fn();
jest.mock('../src/db', () => ({
  query: (...args) => mockQuery(...args),
  pool: null, // module-load-time schema self-heal is a no-op when pool is falsy
}));

const telemetryRoutes = require('../src/routes/telemetry');

const app = express();
app.use(express.json());
app.use('/api/telemetry', telemetryRoutes);

const OWNED_ALARM_ID = '11111111-1111-1111-1111-111111111111';
const CLIENT_EVENT_ID = 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee';

beforeEach(() => {
  mockQuery.mockReset();
});

describe('POST /api/telemetry/alarm-fired', () => {
  test('records a fire event with device metadata', async () => {
    mockQuery.mockResolvedValueOnce({ rows: [{ id: 'fire-1', device_manufacturer: 'Xiaomi' }] });

    const res = await request(app).post('/api/telemetry/alarm-fired').send({
      delivery_path: 'full_screen_intent',
      device_manufacturer: 'Xiaomi',
      device_model: 'Redmi Note 12',
      android_sdk: 34,
      app_version: '0.1.0',
    });

    expect(res.status).toBe(201);
    expect(res.body.device_manufacturer).toBe('Xiaomi');
    const [sql, params] = mockQuery.mock.calls[0];
    expect(sql).toContain('INSERT INTO alarm_fire_events');
    expect(params).toContain('Xiaomi');
    expect(params).toContain(34);
  });

  test('rejects an unknown delivery_path rather than storing a value the OEM report cannot group', async () => {
    const res = await request(app).post('/api/telemetry/alarm-fired').send({ delivery_path: 'carrier_pigeon' });
    expect(res.status).toBe(400);
    expect(mockQuery).not.toHaveBeenCalled();
  });

  test('rejects a malformed scheduled_for instead of silently recording null', async () => {
    const res = await request(app).post('/api/telemetry/alarm-fired').send({ scheduled_for: 'tomorrow-ish' });
    expect(res.status).toBe(400);
    expect(mockQuery).not.toHaveBeenCalled();
  });

  test('rejects an alarm_schedule_id belonging to someone else', async () => {
    mockQuery.mockResolvedValueOnce({ rows: [] }); // ownership lookup finds nothing
    const res = await request(app)
      .post('/api/telemetry/alarm-fired')
      .send({ alarm_schedule_id: OWNED_ALARM_ID });
    expect(res.status).toBe(404);
  });

  test('a retried report of the same alarm is idempotent, not a second fire', async () => {
    // A device that fires offline retries when it reconnects. Counting that twice would
    // overstate alarm reliability, which is the one number this table exists to produce.
    const duplicate = Object.assign(new Error('duplicate key'), { code: '23505' });
    mockQuery.mockRejectedValueOnce(duplicate);
    mockQuery.mockResolvedValueOnce({ rows: [{ id: 'fire-1', client_event_id: CLIENT_EVENT_ID }] });

    const res = await request(app)
      .post('/api/telemetry/alarm-fired')
      .send({ client_event_id: CLIENT_EVENT_ID });

    expect(res.status).toBe(200);
    expect(res.body.id).toBe('fire-1');
  });

  test('rejects a non-UUID client_event_id before touching the database', async () => {
    const res = await request(app).post('/api/telemetry/alarm-fired').send({ client_event_id: 'not-a-uuid' });
    expect(res.status).toBe(400);
    expect(mockQuery).not.toHaveBeenCalled();
  });
});

describe('GET /api/telemetry/alarm-reliability', () => {
  test('returns verified-dismissal rate grouped by manufacturer', async () => {
    mockQuery.mockResolvedValueOnce({
      rows: [
        { manufacturer: 'Xiaomi', fires: 40, users: 4, verified_dismissals: 22, verified_rate_percent: '55.0' },
        { manufacturer: 'Google', fires: 20, users: 2, verified_dismissals: 19, verified_rate_percent: '95.0' },
      ],
    });

    const res = await request(app).get('/api/telemetry/alarm-reliability?days=14');

    expect(res.status).toBe(200);
    expect(res.body.window_days).toBe(14);
    expect(res.body.by_manufacturer[0].manufacturer).toBe('Xiaomi');
    expect(mockQuery.mock.calls[0][1]).toEqual([14]);
  });

  test('clamps an absurd window instead of scanning all history', async () => {
    mockQuery.mockResolvedValueOnce({ rows: [] });
    await request(app).get('/api/telemetry/alarm-reliability?days=99999');
    expect(mockQuery.mock.calls[0][1]).toEqual([365]);
  });
});
