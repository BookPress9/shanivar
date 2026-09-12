/**
 * Pure unit tests for src/middleware/auth.js in isolation — no Express app,
 * no DB, no HTTP. This was previously untested: every other test file mocks
 * this module away entirely, so authenticateToken/requireAdmin themselves
 * had zero direct coverage before this file.
 */
const jwt = require('jsonwebtoken');
const { authenticateToken, requireAdmin, JWT_SECRET } = require('../src/middleware/auth');

function mockRes() {
  const res = {};
  res.status = jest.fn().mockReturnValue(res);
  res.json = jest.fn().mockReturnValue(res);
  return res;
}

describe('authenticateToken', () => {
  it('responds 401 when there is no Authorization header', () => {
    const req = { headers: {} };
    const res = mockRes();
    const next = jest.fn();

    authenticateToken(req, res, next);

    expect(res.status).toHaveBeenCalledWith(401);
    expect(res.json).toHaveBeenCalledWith({ error: 'Access token required' });
    expect(next).not.toHaveBeenCalled();
  });

  it('responds 401 when the Authorization header has no token after "Bearer"', () => {
    const req = { headers: { authorization: 'Bearer' } };
    const res = mockRes();
    const next = jest.fn();

    authenticateToken(req, res, next);

    expect(res.status).toHaveBeenCalledWith(401);
    expect(next).not.toHaveBeenCalled();
  });

  it('responds 403 for a token signed with the wrong secret', () => {
    const badToken = jwt.sign({ id: 'u1' }, 'a-completely-different-secret');
    const req = { headers: { authorization: `Bearer ${badToken}` } };
    const res = mockRes();
    const next = jest.fn();

    authenticateToken(req, res, next);

    expect(res.status).toHaveBeenCalledWith(403);
    expect(res.json).toHaveBeenCalledWith({ error: 'Invalid or expired token' });
    expect(next).not.toHaveBeenCalled();
  });

  it('responds 403 for an expired token', () => {
    const expiredToken = jwt.sign({ id: 'u1' }, JWT_SECRET, { expiresIn: -10 });
    const req = { headers: { authorization: `Bearer ${expiredToken}` } };
    const res = mockRes();
    const next = jest.fn();

    authenticateToken(req, res, next);

    expect(res.status).toHaveBeenCalledWith(403);
    expect(next).not.toHaveBeenCalled();
  });

  it('calls next() and attaches req.user for a valid token', () => {
    const validToken = jwt.sign({ id: 'u1', email: 'a@b.com', role: 'user' }, JWT_SECRET, { expiresIn: '1h' });
    const req = { headers: { authorization: `Bearer ${validToken}` } };
    const res = mockRes();
    const next = jest.fn();

    authenticateToken(req, res, next);

    expect(next).toHaveBeenCalledTimes(1);
    expect(res.status).not.toHaveBeenCalled();
    expect(req.user).toMatchObject({ id: 'u1', email: 'a@b.com', role: 'user' });
  });
});

describe('requireAdmin', () => {
  it('calls next() when req.user.role is admin', () => {
    const req = { user: { role: 'admin' } };
    const res = mockRes();
    const next = jest.fn();

    requireAdmin(req, res, next);

    expect(next).toHaveBeenCalledTimes(1);
    expect(res.status).not.toHaveBeenCalled();
  });

  it('responds 403 when req.user.role is not admin', () => {
    const req = { user: { role: 'user' } };
    const res = mockRes();
    const next = jest.fn();

    requireAdmin(req, res, next);

    expect(res.status).toHaveBeenCalledWith(403);
    expect(res.json).toHaveBeenCalledWith({ error: 'Admin access required' });
    expect(next).not.toHaveBeenCalled();
  });
});

describe('JWT_SECRET resolution', () => {
  // Regression guard: JWT_SECRET used to silently fall back to a hardcoded, publicly-known
  // string whenever the env var was unset, in every environment including production —
  // meaning anyone could forge a valid, signed token for any user/role. It must now refuse
  // to boot in production without a real secret, and only fall back (with a warning) in dev.
  const ORIGINAL_ENV = process.env.NODE_ENV;
  const ORIGINAL_SECRET = process.env.JWT_SECRET;

  afterEach(() => {
    process.env.NODE_ENV = ORIGINAL_ENV;
    if (ORIGINAL_SECRET === undefined) delete process.env.JWT_SECRET;
    else process.env.JWT_SECRET = ORIGINAL_SECRET;
  });

  it('throws at load time in production when JWT_SECRET is unset', () => {
    jest.resetModules();
    process.env.NODE_ENV = 'production';
    delete process.env.JWT_SECRET;

    expect(() => require('../src/middleware/auth')).toThrow(/JWT_SECRET must be set in production/);
  });

  it('falls back to a dev-only secret outside production when JWT_SECRET is unset', () => {
    jest.resetModules();
    process.env.NODE_ENV = 'development';
    delete process.env.JWT_SECRET;

    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    const { JWT_SECRET: devSecret } = require('../src/middleware/auth');

    expect(typeof devSecret).toBe('string');
    expect(devSecret.length).toBeGreaterThan(0);
    expect(warnSpy).toHaveBeenCalled();
    warnSpy.mockRestore();
  });
});
