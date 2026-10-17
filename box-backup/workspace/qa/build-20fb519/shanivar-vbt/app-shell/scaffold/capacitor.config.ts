import type { CapacitorConfig } from '@capacitor/cli';

// REFERENCE ONLY — see ./README.md. The architecture decision reconciled to
// fully-native (../architecture-decision.md), and a separate agent (Codex)
// owns the real frontend build. This file is a superseded sketch from the
// earlier hybrid-leaning analysis pass — do not build on it.
const config: CapacitorConfig = {
  appId: 'com.shanivar.app',
  appName: 'Shanivar',
  webDir: 'www', // unused placeholder dir in remote-URL mode; Capacitor CLI requires the key to exist
  server: {
    // TODO: point at the real production/staging host before first build.
    // Using the live web deployment means shell code changes are NOT required
    // to ship content/UX fixes during the pre-cohort period.
    url: 'https://shanivar.com',
    cleartext: false,
  },
  android: {
    // Discipline-layer native components (LockoutAccessibilityService,
    // AlarmActivity, AlarmReceiver) are declared directly in
    // android/app/src/main/AndroidManifest.xml — they run independent of
    // this WebView's lifecycle by design. See architecture-decision.md §5.
    allowMixedContent: false,
  },
};

export default config;
