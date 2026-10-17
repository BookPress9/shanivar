# Shanivar web application

Responsive, installable web client for Shanivar Routine and Sadhana. It uses the same Supabase authentication, RLS tables, outbox timestamps, tombstones and routine revision endpoint as the Android client. Routine planning and practice remain usable offline; queued changes sync after sign-in and reconnection.

## Run and test

No build step or third-party package is required.

```text
cd web-app
npx serve .
node --test test/*.test.js
```

Any static HTTPS host can serve the directory. The service worker provides the offline application shell and the manifest makes it installable.

## Connect the existing Supabase project

Copy `config.example.js` to `config.js` in the deployment output and set:

- `supabaseUrl`: project API URL
- `supabaseAnonKey`: public client anon key
- `siteUrl`: deployed web URL, also registered as a Supabase Auth redirect URL

The anon key is public client configuration. Never put a service-role key in this directory. `config.js` currently contains blank values, so a fresh checkout runs in local-only mode instead of contacting an unknown backend.

Supported account paths are Supabase anonymous guest, email OTP/magic link, and Google OAuth when enabled in the existing project. Tokens stay in browser local storage; production hosting must use HTTPS and a restrictive CSP.

## Product parity

- Today view, current capability invitation and Living Orbit
- Routine anchors, recurring personal activities and recurring protection-window intent
- Five currently released Sadhana definitions, foreground active-time timer, pause, anchor change and immediate end
- Factual attempts, active minutes and independent-practice records
- Local-first outbox with authenticated Supabase writes
- Responsive phone/desktop navigation and offline PWA shell

Web browsers cannot block other installed applications or guarantee exact background alarms. The UI records and syncs the desired Routine without claiming that native enforcement occurred. Those device capabilities remain in the Android client.

## Backend contract

- Writes never send `user_id`; Supabase derives ownership from the bearer token and RLS.
- Row writes add `client_updated_at` and upsert on `user_id,id`.
- Deletes become `tombstones` upserts.
- Routine uses `put_routine` with the last server revision and an idempotency key.
- If `put_routine` is not deployed, Routine stays in the local outbox and the Settings screen explains the pending state.

The repository currently documents that its previously stored project reference no longer resolves. A working replacement URL and anon key are therefore required before live end-to-end sync can be certified.
