# A little more of your life, returned

Next 30-day campaign for **@app.shanivar**, prepared 12 September 2026. Proposed dates: **14 September–13 October 2026**, Asia/Kolkata. The folder label means the next campaign after the first post; no full prior published month is assumed.

Open [the interactive editorial calendar](index.html). Filter by week, format or feed language; expand any day for captions and slide/scene copy. It opens as a local HTML file without a server. This is a planning preview, not rendered Instagram artwork.

- [30-day calendar](calendar.md) and [calendar CSV](calendar.csv): 16 feed slots, 30 optional daily Stories and review dates.
- [Complete bilingual production copy](production-copy.md): 12 original concepts with English/Hindi captions, 48 original carousel slides per language, four five-scene Reel scripts per language, visual briefs, CTA and accessibility drafts.
- [Publishing drafts CSV](publishing-drafts.csv): eight English/eight Hindi proposed editions. Status Draft; approval, media URL and platform ID intentionally blank.
- [First-post analysis](first-post-analysis.md): founder-supplied URL recorded; actual creative and Insights unavailable because the fetch was throttled.
- [Operations](operations.md): experiment design, roles, review cadence, metric definitions and acquisition dependencies.
- [Metrics intake](metrics.csv): 51 blank observation rows for the first post and 16 proposed posts at 24h/7d/28d. Blank is unknown, not zero.
- [Asset manifest](asset-manifest.json): 16 production entries, all unrendered and unapproved.
- [Verification record](QA.md): export checks and desktop/mobile browser checks, with screenshots.

The sequence is morning attention → rest after work → conversation → a flexible दिनचर्या. Four EN/HI pairs explore accessibility; they do not establish a causal reach gain. All copy is original editorial work, not scripture or an individual chart reading. Product mentions use development/concept labels.

**Completed:** calendar, exact bilingual copy, timed scripts, visual briefs, accessibility drafts, interactive planning preview, exports and the agent learning procedure. **Still needed for publication:** render and inspect matching artwork/footage, fluent Hindi review, approved media/copy/timing and account access. No new post has been scheduled, published or pinned. No background worker has been activated.

Source of truth: edit `campaign.json`, then run `python marketing/shanivar/month-02/build.py` from the repository root. The builder validates counts and exports the preview/calendar/copy/manifest. Existing `metrics.csv` is preserved. The script uses Python's IANA timezone database; on a Windows installation without it, install the `tzdata` package before running.

The asset manifest is a generated planning skeleton: once actual production starts, move signed-off records to the publishing system or a separate production manifest before regenerating. The builder does not preserve manual edits to derived editorial exports.
