// Jest global setup — runs once per test FILE, before that file's module tree
// is required (see "setupFiles" in package.json's jest config).
//
// Several route modules (e.g. src/routes/chart.js, src/routes/insights.js)
// read process.env.GEMINI_API_KEY at module-load time:
//   const GEMINI_API_KEY = process.env.GEMINI_API_KEY;
// In normal operation src/index.js calls require('dotenv').config() before
// anything else is required, so this is populated from .env. Test files
// require route modules directly and never import index.js, so without this
// setup file GEMINI_API_KEY (and any other .env-only value) is undefined at
// the moment those route modules are first required — producing false
// failures ("GEMINI_API_KEY is not configured") that have nothing to do with
// the route logic under test.
//
// Loading dotenv here, before any test module is required, fixes that at the
// infrastructure level instead of requiring every test file to remember to
// do it (or to set process.env.GEMINI_API_KEY itself before requiring the
// route — which only works if that particular test file is careful about
// ordering).
require('dotenv').config();
