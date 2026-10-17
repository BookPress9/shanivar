# Android device distribution

## Immediate local build

The backend is deployed and no longer needs to run on this computer - the current test APK is
built against the production API at `https://hybridmonk-api.onrender.com/api` directly, so a
device only needs internet, not the same Wi-Fi network as this computer. Only the APK download
itself uses the local network, and only because there is no hosted distribution channel set up yet
(see "Recommended hosted test path" below for one that would remove even that requirement):

`http://192.168.1.6:8088/app-debug.apk`

Serve it (already running as of this build):

```powershell
python -m http.server 8088 --bind 0.0.0.0 --directory android-app/app/build/outputs/apk/debug
```

The Android device must be on the same Wi-Fi network as this computer to reach that URL, and keep
this computer + that server running while installing. Android will ask to allow installs from the
browser because this is a debug APK. Supports Android 8.0/API 26 and newer.

Verified build, 25 September 2026:

- Size: `34,445,969` bytes
- SHA-256: `5DCB45BB733E88D5053654EAE9B8013F99D4071A3BBAE50C452D7FDF1FE71737`
- API health: `https://hybridmonk-api.onrender.com/api/health` → `200 {"status":"ok","database":"ok"}`
  in 22.6s (free-tier cold start)
- Android JVM/Robolectric: 160/160 tests, 0 skipped
- Android lint: 0 errors
- Adds the breathing third pillar (see
  [decision 0011](../docs/decisions/0011-vbt-breathing-third-pillar.md) and
  [the release evidence](VBT_BREATHING_RELEASE_EVIDENCE_2026-09-25.md), which lists what is still
  NOT RUN). No backend change, so `backend/` was not rebuilt or retested for this build; its last
  verified figure is 305/305 on 19 September 2026.
- Not verified: anything requiring physical hardware (no emulator/managed-device task runs on
  this machine) - see the eight-check protocol in `ENFORCEMENT_STANCE_2026-09-12.md`. For the
  breathing feature this specifically covers the tone actually stopping, lock-screen behaviour,
  process death and alarm-during-practice.

Rebuild against production (no LAN IP needed for the API anymore):

```powershell
./android-app/gradlew.bat -p android-app assembleDebug "-PshanivarApiUrl=https://hybridmonk-api.onrender.com/api"
```

This local route is for hands-on testing only. It depends on the computer and local network for
the *download* only; the app itself, once installed, depends only on internet.

## Recommended hosted test path

Use **Firebase App Distribution** for APK delivery. It is currently a no-cost Firebase product,
tracks invitations and downloads, accepts signed debug or release APKs, and keeps each release for
150 days. Register the exact Android package `com.shanivar.nativeapp.dev` for debug builds or
`com.shanivar.nativeapp` for release builds; Firebase package names cannot be changed afterward.

Console flow:

1. Create or select the Shanivar Firebase project.
2. Register the package name for the build variant.
3. Open App Distribution and upload `app/build/outputs/apk/debug/app-debug.apk`.
4. Add testers or a tester group and distribute the release.

No Firebase runtime SDK is required merely to upload and distribute an APK.

## API hosting decision

Supabase now owns persistent data. The Express API still performs authentication, ownership checks,
astrology calculations, idempotency, AI orchestration and discipline-state writes, so it needs an
always-reachable Node.js host. Never ship `SUPABASE_SERVICE_ROLE_KEY` or `DATABASE_URL` in the APK.

1. **Use the existing Hostinger plan if it is Business Web Hosting or Cloud Hosting.** Those plans
   support managed Node.js deployment, GitHub deployment, TLS and process management. The marginal
   cost is zero when the plan is already paid. Set the application root to `backend`, start command
   to `npm start`, health path to `/api/health`, and copy the backend environment variables in hPanel.
2. **Use Railway Hobby when the current Hostinger plan cannot run Node.js.** It has a USD 5 monthly
   minimum that includes USD 5 of usage, keeps the small API available, and supports the committed
   `backend/Dockerfile`, `backend/railway.json`, health checks and restarts.
3. **Use Render Free only for disposable previews.** Its free web services sleep after 15 idle
   minutes and can take about a minute to wake, which makes the first mobile request feel broken.

After deployment, point `api.thehybridmonk.com` to the provider, enable managed TLS, and verify:

```text
https://api.thehybridmonk.com/api/health
```

The Android release build and the existing web production environment both use
`https://api.thehybridmonk.com/api`. At the time of this setup the hostname did not resolve from the
development machine, so public-device testing still requires the DNS/hosting step.

Supabase Edge Functions could later replace individual Express routes, but moving this complete API
would be an application rewrite because it currently uses Node/Express, `pg`, custom JWTs and several
longer orchestration flows.
