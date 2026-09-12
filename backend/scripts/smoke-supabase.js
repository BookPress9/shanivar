/**
 * End-to-end API smoke test against the configured Supabase database.
 * Requires the backend to be running on API_BASE_URL (defaults to localhost:5000/api).
 * Creates one uniquely named account and removes it in finally; user-owned rows cascade.
 */
require('dotenv').config();
const crypto = require('crypto');
const db = require('../src/db');

const base = process.env.API_BASE_URL || 'http://127.0.0.1:5000/api';
const suffix = crypto.randomUUID().replace(/-/g, '').slice(0, 12);
const email = `android-smoke-${suffix}@example.invalid`;
let token = '';

async function call(path, { method = 'GET', body, expected = [200] } = {}) {
  const response = await fetch(base + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    ...(body ? { body: JSON.stringify(body) } : {})
  });
  const json = await response.json().catch(() => ({}));
  if (!expected.includes(response.status)) {
    throw new Error(`${method} ${path}: expected ${expected.join('/')}, got ${response.status}: ${json.error || 'unknown error'}`);
  }
  return json;
}

async function main() {
  const results = [];
  try {
    const health = await call('/health');
    if (health.database !== 'ok') throw new Error('health did not confirm the database');
    results.push('health + Supabase');

    const registered = await call('/auth/register', {
      method: 'POST', expected: [201],
      body: { email, password: `Smoke-${suffix}!`, display_name: 'Android Smoke', preferred_language: 'hi' }
    });
    token = registered.token;
    if (!token) throw new Error('registration returned no token');
    results.push('simple registration');

    await call('/users/me', { method: 'PUT', body: {
      preferred_language: 'hi', birth_date: '1990-01-01', birth_time: '07:30', birth_location: 'Delhi, India'
    }});
    results.push('birth context + language');

    const preferences = await call('/discipline/preferences', { method: 'PUT', body: {
      wake_time: '06:45', quiet_time: '21:30', focus_minutes: 30, preferred_language: 'hi',
      dismissal_method: 'movement', movement_destination: 'morning_light', step_target: 18,
      scene_confirmation: false, whatsapp_time: '08:00', whatsapp_tone: 'direct',
      daily_note_enabled: true, block_package_list: ['com.instagram.android']
    }});
    if (preferences.movement_destination !== 'morning_light' || preferences.step_target !== 18) {
      throw new Error('movement preferences did not round-trip');
    }
    results.push('rhythm round-trip');

    const focus = await call('/discipline/focus/start', { method: 'POST', expected: [201], body: {
      duration_minutes: 30, block_package_list: ['com.instagram.android']
    }});
    await call(`/discipline/focus/${focus.id}/release`, { method: 'POST' });
    results.push('focus start + release');

    const alarm = await call('/discipline/alarms', { method: 'POST', expected: [201], body: {
      local_time: '06:45', dismissal_method: 'movement', movement_destination: 'morning_light', step_target: 18
    }});
    const next = new Date(Date.now() + 86_400_000).toISOString();
    await call(`/discipline/alarms/${alarm.id}/confirm-scheduled`, { method: 'POST', body: { next_occurrence: next } });
    await call('/discipline/alarms/dismiss', { method: 'POST', expected: [201], body: {
      alarm_schedule_id: alarm.id, method: 'movement', verified: true,
      verification_detail: { steps: 18, sensor: 'integration_fixture' }
    }});
    results.push('alarm create + confirm + movement record');

    await call('/insights/today');
    results.push('Today response');
    console.log(`Supabase integration smoke passed: ${results.join('; ')}`);
  } finally {
    await db.query('DELETE FROM users WHERE email = $1', [email]).catch(() => {});
    if (db.pool) await db.pool.end().catch(() => {});
  }
}

main().catch((error) => { console.error(error.message); process.exitCode = 1; });
