# Shanivar Web and Social Team

Created at the founder's request. These roles extend the existing organization; the original five-role WhatsApp cohort remains a separate track.

| Agent | Owns | Coordinates with |
|---|---|---|
| [Web Developer](agents/web_developer.md) | Responsive website, web app screens, campaign pages, accessibility and browser verification | Codex UI/UX lead; Claude backend |
| [Social Media Manager](agents/social_media_manager.md) | Editorial planning, social drafts, asset briefs, community reply drafts and performance review | Founder; Content Designer; Web Developer |
| [Creative Writer](agents/creative_writer.md) | Campaign narratives, deep copy, carousel progression, Reel scripts and evolving voice craft | Social Media Manager; Content Designer |

## Ownership
Codex leads UI/UX and native Android frontend work. Claude owns backend/native service contracts and maintains [CODEX_HANDOFF.md](../shanivar-vbt/app-shell/CODEX_HANDOFF.md). Read that document before work crossing those boundaries; do not overwrite it.

## How to use
Assign a bounded task to a role using its linked Markdown as the role prompt. These files define reusable agents; they do not start background workers, connect social accounts or schedule posts.

Example: "Use the Web Developer agent to improve the mobile Shanivar landing page and verify the signup flow locally."

Example: "Use the Social Media Manager agent to draft next week's Instagram calendar from verified features."

Example: "Have the Social Media Manager activate the Creative Writer for two Reel scripts, using the latest approved learning log and verified product state."

The Creative Writer is a downstream specialist, not an independent publishing role. The Social Media Manager supplies its brief, reviews its claims and records accepted learning. The founder has authorized requests for the latest available Astra model only for image, carousel-artwork and video production or review; actual availability still depends on the active runtime and media tools.

## Shared handoff

Current assignment: [the 30-day continuation campaign](../marketing/shanivar/month-02/README.md), with [weekly review operations](../marketing/shanivar/month-02/operations.md). The Social Media Manager owns a rolling month and its actual publication ledger; the Creative Writer receives one bounded, evidence-backed craft proposal per review. The first post is founder-reported; blocked access must not be turned into invented performance analysis. Role updates are instructions for future runs, not persistent workers.

Social briefs specify audience, objective, approved claim, CTA and destination. Web returns preview instructions, implemented behavior and test evidence. Social then reconciles copy with what the destination actually supports.

Report completed work, reviewable files, verification and remaining dependencies. Keep mock evidence distinct from live-service verification. Public messages, live scheduling and ad spend require explicit user authorization; local drafting and implementation proceed within the assigned scope.
