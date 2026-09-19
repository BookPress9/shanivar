# Calendar package verification — 12 September 2026

Verified the local files and actual browser behaviour, not platform publication.

- `build.py` completed with its structural validations: 12 bilingual concepts, 16 edition slots and 30 daily Story pairs.
- CSV checks passed: unique IDs; 14 September–13 October dates; eight English/eight Hindi publications; 10 carousels/six Reels; four two-edition language pairs.
- All publication rows remain Draft with blank approval, media URL and platform post ID. All 16 media-manifest records remain Not produced.
- All 51 metrics rows remain blank for reach/saves/shares. BASE-01 preserves the supplied canonical permalink, with no invented publication date or analytics.
- Local links in the campaign Markdown files resolve.
- Headless Chrome/Playwright: 30 calendar cards displayed; week filter returned seven days; Carousel/Reel filters returned 10/six; Hindi returned eight. Expanding a draft revealed both copy editions without JavaScript errors.
- Mobile width 390×844: no horizontal overflow. Desktop 1365×960 and mobile screenshots were captured and visually inspected: readable hierarchy, consistent charcoal/bone/bronze palette and usable filters.
- `git diff --check` passed; Git emitted only existing CRLF conversion warnings.

Screenshots: [desktop](preview-desktop.png) and [mobile](preview-mobile.png). These show the editorial workspace, not finished social-media graphics.

Not verified: the first live post's actual creative/Insights, platform account permissions, upload acceptance, final artwork/video rendering, fluent-reader Hindi sign-off, live scheduling, audience response or acquisition. The source documents contain conflicting historical native-test descriptions; public copy therefore retains development/concept framing and does not repeat a stronger release claim.
