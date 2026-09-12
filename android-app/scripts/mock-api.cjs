// Local UI fixture only: no database, messages, payments, or production credentials.
const http = require('node:http');
const port = Number(process.env.PORT || 5101);
const token = 'shanivar-local-fixture';
http.createServer(async (req, res) => {
  res.setHeader('Content-Type', 'application/json');
  const send = (status, body) => { res.writeHead(status); res.end(JSON.stringify(body)); };
  if (req.url === '/api/health') return send(200, {status: 'ok', mode: 'frontend-fixture'});
  if (req.method === 'POST' && req.url === '/api/auth/login') {
    let raw = '';
    for await (const chunk of req) { raw += chunk; if (raw.length > 8192) return send(413, {error: 'Request too large'}); }
    let body;
    try { body = JSON.parse(raw); } catch { return send(400, {error: 'Invalid JSON'}); }
    if (body.email !== 'observer@example.com' || body.password !== 'local-practice') return send(401, {error: 'Invalid credentials'});
    return send(200, {token});
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
  send(404, {error: 'Route not included in frontend fixture'});
}).listen(port, '127.0.0.1', () => console.log('Shanivar fixture: http://127.0.0.1:' + port + '/api — observer@example.com / local-practice'));

