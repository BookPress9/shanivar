const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const dist = path.join(root, "dist");
const envPath = path.join(root, ".env.production.local");
const config = {};

for (const line of fs.readFileSync(envPath, "utf8").replace(/^\uFEFF/, "").split(/\r?\n/)) {
  const match = line.match(/^([^=]+)=(.*)$/);
  if (match) config[match[1]] = match[2];
}
if (!/^https:\/\/[a-z0-9]+\.supabase\.co$/.test(config.SUPABASE_URL || "")) throw new Error("Valid SUPABASE_URL required");
if ((config.SUPABASE_ANON_KEY || "").length < 100) throw new Error("SUPABASE_ANON_KEY required");

fs.rmSync(dist, { recursive: true, force: true });
fs.mkdirSync(dist, { recursive: true });
for (const name of ["index.html", "style.css", "onboarding.css", "auth.css", "content.js", "logic.js", "sync.js", "app.js", "sw.js", "manifest.webmanifest", "icon.svg", "netlify.toml"]) {
  fs.copyFileSync(path.join(root, name), path.join(dist, name));
}
const publicConfig = {
  supabaseUrl: config.SUPABASE_URL,
  supabaseAnonKey: config.SUPABASE_ANON_KEY,
  siteUrl: config.SITE_URL || "https://app.shanivar.com",
  googleEnabled: config.GOOGLE_AUTH_ENABLED === "true"
};
fs.writeFileSync(path.join(dist, "config.js"), `window.SHANIVAR_CONFIG=${JSON.stringify(publicConfig)};\n`);
console.log("Production bundle created in web-app/dist (public client configuration included).");
