// Local UI fixture only: no database, messages, payments, or production credentials.
const http = require('node:http');
const port = Number(process.env.PORT || 5101);
const token = 'shanivar-local-fixture';
let preferences = {
  id: 'pref-1', wake_time: '07:00:00', quiet_time: '21:30:00', focus_minutes: 30,
  preferred_language: 'en', dismissal_method: 'movement', movement_destination: 'morning_light',
  step_target: 18, scene_confirmation: false, whatsapp_time: '08:00:00', whatsapp_tone: 'direct',
  daily_note_enabled: true, block_package_list: ['com.instagram.android']
};
let focus = null;
const readBody = async (req) => {
  let raw = '';
  for await (const chunk of req) { raw += chunk; if (raw.length > 32768) throw new Error('Request too large'); }
  return raw ? JSON.parse(raw) : {};
};
http.createServer(async (req, res) => {
  res.setHeader('Content-Type', 'application/json');
  const send = (status, body) => { res.writeHead(status); res.end(JSON.stringify(body)); };
  if (req.url === '/api/health') return send(200, {status: 'ok', mode: 'frontend-fixture'});
  if (req.method === 'POST' && req.url === '/api/auth/login') {
    let body;
    try { body = await readBody(req); } catch { return send(400, {error: 'Invalid JSON'}); }
    if (body.email !== 'observer@example.com' || body.password !== 'local-practice') return send(401, {error: 'Invalid credentials'});
    return send(200, {token});
  }
  if (req.method === 'POST' && req.url === '/api/auth/register') {
    let body;
    try { body = await readBody(req); } catch { return send(400, {error: 'Invalid JSON'}); }
    if (!body.email || !body.password) return send(400, {error: 'Email and password are required'});
    return send(201, {token, user: {id: 'local-observer', display_name: body.display_name || 'Observer'}});
  }
  if (req.headers.authorization !== 'Bearer ' + token) return send(401, {error: 'Access token required'});
  if (await require('./mock-reflections.cjs')(req, send)) return;
  if (req.url === '/api/users/me') return send(200, {id: 'local-observer', display_name: 'Local Observer', streak_days: 8});
  if (req.url === '/api/insights/today') return send(200, {insight: {
    id: '00000000-0000-0000-0000-000000000000',
    insight_text: 'Local mock note: make room for one quiet moment.',
    action_text: 'Take three comfortable breaths.',
    reflection_text: 'What deserves your attention today?',
    insight_date: new Date().toLocaleDateString('en-CA')
  }});
  if (req.url === '/api/discipline/preferences' && req.method === 'GET') return send(200, preferences);
  if (req.url === '/api/discipline/preferences' && req.method === 'PUT') {
    try { preferences = {...preferences, ...(await readBody(req))}; return send(200, preferences); }
    catch { return send(400, {error: 'Invalid preferences'}); }
  }
  if (req.url === '/api/discipline/focus/active' && req.method === 'GET') return send(200, {active: focus});
  if (req.url === '/api/discipline/focus/start' && req.method === 'POST') {
    const body = await readBody(req);
    focus = {id: '33333333-3333-3333-3333-333333333333', requested_duration_minutes: body.duration_minutes,
      block_package_list: body.block_package_list, started_at: new Date().toISOString(), status: 'active'};
    return send(201, focus);
  }
  if (/^\/api\/discipline\/focus\/[0-9a-f-]+\/release$/.test(req.url) && req.method === 'POST') {
    const released = {...focus, status: 'released'}; focus = null; return send(200, released);
  }
  if (req.url === '/api/discipline/alarms' && req.method === 'POST') {
    const body = await readBody(req);
    return send(201, {id: '11111111-1111-1111-1111-111111111111', ...body, next_confirmed_occurrence: null});
  }
  if (/^\/api\/discipline\/alarms\/[0-9a-f-]+\/confirm-scheduled$/.test(req.url) && req.method === 'POST') {
    const body = await readBody(req);
    return send(200, {id: '11111111-1111-1111-1111-111111111111', next_confirmed_occurrence: body.next_occurrence});
  }
  send(404, {error: 'Route not included in frontend fixture'});
}).listen(port, '127.0.0.1', () => console.log('Shanivar fixture: http://127.0.0.1:' + port + '/api — observer@example.com / local-practice'));

