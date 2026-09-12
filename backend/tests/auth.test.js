const request = require('supertest');
const express = require('express');
const bcrypt = require('bcryptjs');

// A real bcrypt hash of "correct-password" at cost 12, used to simulate an
// existing user row coming back from the DB.
const REAL_PASSWORD = 'correct-password';
const REAL_PASSWORD_HASH = '$2a$12$6BEZR0e/FyNuZWjtGLzPC..wMreQlkKo5MJRM3vAQNekye9o7dFWm';

// Mock the db module the same way the DB is not real (Supabase project is
// gone) — see tests/chart.test.js for the established pattern.
const mockQuery = jest.fn();
jest.mock('../src/db', () => ({
  query: (...args) => mockQuery(...args),
}));

// AnalyticsEngine.logEvent is invoked (fire-and-forget-ish, but awaited) from
// the register route. It internally calls db.query and swallows its own
// errors, so it works fine against the mocked db above; no separate mock
// needed, but we still let it run through the shared mockQuery.

const authRoutes = require('../src/routes/auth');

const app = express();
app.use(express.json());
app.use('/api/auth', authRoutes);

beforeEach(() => {
  mockQuery.mockReset();
});

describe('POST /api/auth/register', () => {
  it('hashes the password with a strong bcrypt cost factor (regression: was downgraded to 4)', async () => {
    mockQuery
      .mockResolvedValueOnce({ rows: [] }) // existing email/username check -> none found
      .mockResolvedValueOnce({
        rows: [{
          id: 'user-1',
          email: 'new@example.com',
          username: 'newuser',
          display_name: 'New User',
          role: 'user',
          xp: 0,
          level: 1,
          current_stage: 1,
        }],
      }) // INSERT INTO users RETURNING ...
      .mockResolvedValueOnce({ rows: [] }); // AnalyticsEngine logEvent insert

    const hashSpy = jest.spyOn(bcrypt, 'hash');

    const res = await request(app)
      .post('/api/auth/register')
      .send({
        email: 'new@example.com',
        username: 'newuser',
        password: 'somePassword123',
        phone: '+15551234567',
      });

    expect(res.statusCode).toEqual(201);
    expect(res.body).toHaveProperty('token');

    // The bug: a prior edit had dropped this to cost factor 4. It must be a
    // secure value (>= 10; the codebase standard is 12).
    expect(hashSpy).toHaveBeenCalled();
    const costFactor = hashSpy.mock.calls[0][1];
    expect(costFactor).toBeGreaterThanOrEqual(10);

    hashSpy.mockRestore();
  });

  it('rejects registration when email or username already exists', async () => {
    mockQuery.mockResolvedValueOnce({ rows: [{ id: 'existing-user' }] });

    const res = await request(app)
      .post('/api/auth/register')
      .send({
        email: 'dupe@example.com',
        username: 'dupeuser',
        password: 'somePassword123',
        phone: '+15551234567',
      });

    expect(res.statusCode).toEqual(409);
  });

  it('normalizes email case before checking/storing it, so Foo@x.com and foo@x.com are the same account', async () => {
    mockQuery
      .mockResolvedValueOnce({ rows: [] })
      .mockResolvedValueOnce({ rows: [{ id: 'user-2', email: 'foo@example.com', username: 'foouser', role: 'user', xp: 0, level: 1, current_stage: 1 }] })
      .mockResolvedValueOnce({ rows: [] });

    const res = await request(app)
      .post('/api/auth/register')
      .send({ email: 'Foo@Example.com', username: 'foouser', password: 'somePassword123', phone: '+15551234567' });

    expect(res.statusCode).toEqual(201);
    const [existsSql, existsParams] = mockQuery.mock.calls[0];
    expect(existsParams[0]).toEqual('foo@example.com');
    const [, insertParams] = mockQuery.mock.calls[1];
    expect(insertParams[0]).toEqual('foo@example.com');
  });

  it('returns 409 instead of a raw 500 when two concurrent requests race past the existence check', async () => {
    // Simulates the TOCTOU window: the pre-check finds nothing, but the INSERT itself
    // hits the DB's unique constraint because another request won the race.
    mockQuery.mockResolvedValueOnce({ rows: [] });
    const uniqueViolation = new Error('duplicate key value violates unique constraint');
    uniqueViolation.code = '23505';
    mockQuery.mockRejectedValueOnce(uniqueViolation);

    const res = await request(app)
      .post('/api/auth/register')
      .send({ email: 'race@example.com', username: 'raceuser', password: 'somePassword123', phone: '+15551234567' });

    expect(res.statusCode).toEqual(409);
  });
});

describe('POST /api/auth/login', () => {
  it('logs in with correct credentials and never returns password_hash', async () => {
    mockQuery.mockResolvedValueOnce({
      rows: [{
        id: 'user-1',
        email: 'user@example.com',
        username: 'someuser',
        role: 'user',
        password_hash: REAL_PASSWORD_HASH,
      }],
    });

    const res = await request(app)
      .post('/api/auth/login')
      .send({ email: 'user@example.com', password: REAL_PASSWORD });

    expect(res.statusCode).toEqual(200);
    expect(res.body).toHaveProperty('token');
    expect(res.body.user).not.toHaveProperty('password_hash');
  });

  it('rejects an existing user with the wrong password', async () => {
    mockQuery.mockResolvedValueOnce({
      rows: [{
        id: 'user-1',
        email: 'user@example.com',
        role: 'user',
        password_hash: REAL_PASSWORD_HASH,
      }],
    });

    const res = await request(app)
      .post('/api/auth/login')
      .send({ email: 'user@example.com', password: 'wrong-password' });

    expect(res.statusCode).toEqual(401);
    expect(res.body).toEqual({ error: 'Invalid credentials' });
  });

  it('rejects a non-existent email with the same error/shape as a wrong password, and still runs bcrypt.compare (timing-attack / user-enumeration guard)', async () => {
    mockQuery.mockResolvedValueOnce({ rows: [] }); // no user found

    const compareSpy = jest.spyOn(bcrypt, 'compare');

    const res = await request(app)
      .post('/api/auth/login')
      .send({ email: 'nobody@example.com', password: 'whatever' });

    expect(res.statusCode).toEqual(401);
    expect(res.body).toEqual({ error: 'Invalid credentials' });
    // Regression guard: login must not short-circuit before bcrypt.compare
    // when the user isn't found, otherwise response time leaks which emails
    // are registered.
    expect(compareSpy).toHaveBeenCalled();

    compareSpy.mockRestore();
  });

  it('requires both email and password', async () => {
    const res = await request(app).post('/api/auth/login').send({ email: 'user@example.com' });
    expect(res.statusCode).toEqual(400);
    expect(mockQuery).not.toHaveBeenCalled();
  });

  it('logs in successfully when the stored email is lowercase but the client submits mixed case', async () => {
    mockQuery.mockResolvedValueOnce({
      rows: [{ id: 'user-1', email: 'user@example.com', username: 'someuser', role: 'user', password_hash: REAL_PASSWORD_HASH }],
    });

    const res = await request(app)
      .post('/api/auth/login')
      .send({ email: 'User@Example.com', password: REAL_PASSWORD });

    expect(res.statusCode).toEqual(200);
    expect(mockQuery.mock.calls[0][1][0]).toEqual('user@example.com');
  });
});
