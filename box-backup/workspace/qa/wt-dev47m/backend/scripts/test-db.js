/**
 * Database Connection & Schema Validation Script
 * Run: node scripts/test-db.js
 */
require('dotenv').config();
const { Pool } = require('pg');

const DATABASE_URL = process.env.DATABASE_URL;
console.log('\n🧘 Hybrid Monk — Database Diagnostics\n');
console.log('DATABASE_URL:', DATABASE_URL ? DATABASE_URL.replace(/:([^:@]+)@/, ':***@') : '❌ NOT SET');

const useSSL = DATABASE_URL && DATABASE_URL.includes('supabase');
const pool = new Pool({
  connectionString: DATABASE_URL || 'postgresql://postgres:postgres@localhost:5432/hybridmonk',
  ssl: useSSL ? { rejectUnauthorized: false } : false,
  connectionTimeoutMillis: 5000
});

const REQUIRED_TABLES = [
  'users', 'journey_stages', 'practices', 'practice_completions',
  'badges', 'user_badges', 'content_items', 'community_posts',
  'community_replies', 'journal_entries', 'analytics_events',
  'user_chart', 'planet_positions', 'houses', 'daily_insights', 'whatsapp_messages'
];

async function run() {
  let client;
  try {
    client = await pool.connect();
    console.log('✅ Database connection successful!\n');

    // Check timestamp
    const timeRes = await client.query('SELECT NOW() as server_time');
    console.log('🕐 Server time:', timeRes.rows[0].server_time);

    // Check tables
    const tableRes = await client.query(`
      SELECT table_name FROM information_schema.tables
      WHERE table_schema = 'public'
      ORDER BY table_name
    `);
    const existingTables = tableRes.rows.map(r => r.table_name);
    console.log('\n📋 Table Validation:');
    let missing = [];
    for (const t of REQUIRED_TABLES) {
      const exists = existingTables.includes(t);
      console.log(`  ${exists ? '✅' : '❌'} ${t}`);
      if (!exists) missing.push(t);
    }

    if (missing.length > 0) {
      console.log(`\n⚠️  Missing tables: ${missing.join(', ')}`);
      console.log('👉 Run the schema SQL in your Supabase SQL Editor:');
      console.log('   database/schema.sql');
      console.log('   database/seed.sql');
    } else {
      console.log('\n✅ All required tables exist!');

      // Row counts
      console.log('\n📊 Row Counts:');
      for (const t of ['users', 'practices', 'journey_stages', 'badges']) {
        const r = await client.query(`SELECT COUNT(*) FROM ${t}`);
        console.log(`  ${t}: ${r.rows[0].count}`);
      }
    }

  } catch (err) {
    console.error('\n❌ Connection failed:', err.message);
    if (err.message.includes('ECONNREFUSED')) {
      console.log('\n💡 Fix: Your DATABASE_URL is pointing to localhost.');
      console.log('   Update backend/.env with your Supabase connection string.');
    }
    if (err.message.includes('SSL')) {
      console.log('\n💡 Fix: SSL error — ensure db.js uses { rejectUnauthorized: false }');
    }
  } finally {
    if (client) client.release();
    await pool.end();
    console.log('\n─────────────────────────────────\n');
  }
}

run();
