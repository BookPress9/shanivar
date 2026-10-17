# Browser verification

Verified 30 September 2026 with Chromium through Playwright. This verifies the browser wireframe only; it does not test Android alarms, app blocking, accessibility services, persistence, backend behavior or physical devices.

- PASS: Onboarding rejects identical wake/sleep times and proceeds without an account gate.
- PASS: Protection requires apps plus a valid same-day interval and does not claim readiness after permission denial.
- PASS: Practice needs stationary-setting confirmation; source navigation pauses; discomfort prevents celebratory escalation.
- PASS: The next challenge is voluntary and independent practice is recorded separately from timed attempts.
- PASS: 21 phone states captured without horizontal content overflow.
- PASS: Primary mobile routes fit a 375px viewport.
- PASS: No browser JavaScript errors occurred in exercised paths.

## Manual visual review

The latest exported phone states and desktop/mobile workspaces were inspected for hierarchy, text clipping, control reachability and contrast. Browser automation does not establish WCAG conformance, screen-reader quality or usability. Native implementation still requires 200% text, TalkBack, landscape, small-device and physical-device review.
