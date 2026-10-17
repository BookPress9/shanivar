/**
 * Hybrid Monk — Supabase Schema Setup Script
 * 
 * Uses Supabase REST API with service role key to:
 * 1. Test API connection
 * 2. Check which tables exist
 * 3. Run schema.sql via pg connection (if DATABASE_URL is set)
 * 
 * Run: node scripts/setup-supabase.js
 */
require('dotenv').config();
const fs = require('fs');
const path = require('path');

const SUPABASE_URL = process.env.SUPABASE_URL;
const SERVICE_KEY = process.env.SUPABASE_SERVICE_ROLE_KEY;
const DATABASE_URL = process.env.DATABASE_URL;

console.log('\n🧘 Hybrid Monk — Supabase Setup\n');
console.log('SUPABASE_URL :', SUPABASE_URL || '❌ NOT SET');
console.log('SERVICE_KEY  :', SERVICE_KEY ? SERVICE_KEY.substring(0, 20) + '...' : '❌ NOT SET');
console.log('DATABASE_URL :', DATABASE_URL
  ? DATABASE_URL.replace(/:([^:@]+)@/, ':***@')
  : '❌ NOT SET');
console.log('');

async function testRestAPI() {
  console.log('Step 1: Testing Supabase REST API connection...');
  try {
    const response = await fetch(`${SUPABASE_URL}/rest/v1/`, {
      headers: {
        'apikey': SERVICE_KEY,
        'Authorization': `Bearer ${SERVICE_KEY}`
      }
    });
    if (response.ok || response.status === 404) {
      console.log('✅ Supabase REST API reachable\n');
      return true;
    }
    console.log(`⚠️  API responded with status: ${response.status}\n`);
    return false;
  } catch (err) {
    console.log('❌ REST API unreachable:', err.message, '\n');
    return false;
  }
}

async function checkExistingTables() {
  console.log('Step 2: Checking existing tables via REST API...');
  const CORE_TABLES = ['users', 'practices', 'journey_stages', 'badges', 'analytics_events', 'daily_insights'];
  const existing = [];

  for (const table of CORE_TABLES) {
    try {
      const res = await fetch(`${SUPABASE_URL}/rest/v1/${table}?limit=1`, {
        headers: {
          'apikey': SERVICE_KEY,
          'Authorization': `Bearer ${SERVICE_KEY}`
        }
      });
      if (res.status === 200) {
        existing.push(table);
        console.log(`  ✅ ${table}`);
      } else {
        console.log(`  ❌ ${table} (not found — status ${res.status})`);
      }
    } catch (err) {
      console.log(`  ❌ ${table} (error: ${err.message})`);
    }
  }
  console.log('');
  return existing;
}

async function runSchemaViaPg() {
  console.log('Step 3: Running schema via direct PostgreSQL connection...');
  
  if (!DATABASE_URL || DATABASE_URL.includes('[YOUR-DB-PASSWORD]')) {
    console.log('⏭️  Skipping — DATABASE_URL not configured with real password\n');
    return false;
  }

  const { Pool } = require('pg');
  const pool = new Pool({
    connectionString: DATABASE_URL,
    ssl: { rejectUnauthorized: false },
    connectionTimeoutMillis: 10000
  });

  try {
    const client = await pool.connect();
    console.log('  ✅ Connected!\n');

    // Run schema
    const schemaPath = path.join(__dirname, '../../database/schema.sql');
    const seedPath = path.join(__dirname, '../../database/seed.sql');

    if (fs.existsSync(schemaPath)) {
      console.log('  Running schema.sql...');
      const schema = fs.readFileSync(schemaPath, 'utf8');
      await client.query(schema);
      console.log('  ✅ schema.sql executed\n');
    }

    if (fs.existsSync(seedPath)) {
      console.log('  Running seed.sql...');
      const seed = fs.readFileSync(seedPath, 'utf8');
      await client.query(seed);
      console.log('  ✅ seed.sql executed\n');
    }

    // Verify
    const tableCheck = await client.query(`
      SELECT table_name FROM information_schema.tables
      WHERE table_schema = 'public' ORDER BY table_name
    `);
    console.log('  Tables in database:', tableCheck.rows.map(r => r.table_name).join(', '));

    client.release();
    await pool.end();
    return true;
  } catch (err) {
    console.log('  ❌ Schema run failed:', err.message);
    await pool.end();
    return false;
  }
}

async function main() {
  await testRestAPI();
  const tables = await checkExistingTables();
  
  if (tables.length === 0) {
    console.log('⚠️  No tables found. You need to run the schema.');
    const pgResult = await runSchemaViaPg();
    
    if (!pgResult) {
      console.log('─── MANUAL SETUP REQUIRED ─────────────────────────────');
      console.log('1. Go to: https://app.supabase.com/project/zpukheowjaxvocbplfmp/sql');
      console.log('2. Paste and run: database/schema.sql');
      console.log('3. Paste and run: database/seed.sql');
      console.log('4. Go to: Settings → Database → Connection string → URI');
      console.log('5. Copy the URI and replace DATABASE_URL in backend/.env');
      console.log('────────────────────────────────────────────────────────\n');
    }
  } else {
    console.log(`✅ ${tables.length}/6 core tables found. Schema looks good!`);
    
    if (tables.length < 6) {
      console.log('⚠️  Some tables are missing. Run the full schema.sql in Supabase SQL Editor.');
    }
  }
}

main().catch(err => {
  console.error('Setup failed:', err.message);
  process.exit(1);
});
