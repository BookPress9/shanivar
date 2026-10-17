const { createClient } = require('@supabase/supabase-js');

const SUPABASE_URL = process.env.SUPABASE_URL;
const SUPABASE_SERVICE_KEY = process.env.SUPABASE_SERVICE_ROLE_KEY;

if (!SUPABASE_URL || !SUPABASE_SERVICE_KEY) {
  console.warn('⚠️  Supabase env vars not set. Client will be null.');
}

// Service-role client (bypasses RLS — for backend use only)
const supabase = SUPABASE_URL && SUPABASE_SERVICE_KEY
  ? createClient(SUPABASE_URL, SUPABASE_SERVICE_KEY, {
      auth: { autoRefreshToken: false, persistSession: false }
    })
  : null;

// Anon client (for operations that respect RLS)
const supabaseAnon = SUPABASE_URL && process.env.SUPABASE_ANON_KEY
  ? createClient(SUPABASE_URL, process.env.SUPABASE_ANON_KEY)
  : null;

module.exports = { supabase, supabaseAnon };
