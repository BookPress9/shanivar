# Android device distribution

## Immediate local build

The current test APK is built against the Supabase-backed API running on this computer at
`http://192.168.1.11:5000/api` and is being served on the local Wi-Fi network:

`http://192.168.1.11:8088/app-debug.apk`

The Android device must be on the same Wi-Fi network. Keep this computer and both local servers
running. Android may ask the user to allow installs from the browser because this is a debug APK.
The build supports Android 8.0/API 26 and newer.

Verified build, 12 September 2026:

- Size: `34,211,845` bytes
- SHA-256: `4047D36D0E872952D31C8B77AFFE874F7B9AD11DE90246465F989974D8C4E985`
- API health: Supabase database connected
- Android 14 managed Pixel: 5/5 instrumentation tests
- Android JVM/Robolectric: 71/71 tests
- Backend: 296/296 tests plus the live Supabase disposable-account smoke
- Android lint: passed

Rebuild when the computer's local IP changes:

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA/Android/Sdk"
./android-app/gradlew.bat -p android-app assembleDebug "-PshanivarApiUrl=http://YOUR_LAN_IP:5000/api"
```

Serve the resulting directory:

```powershell
python -m http.server 8088 --bind 0.0.0.0 --directory android-app/app/build/outputs/apk/debug
```

This local route is for hands-on testing only. It depends on the computer and local network.

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
