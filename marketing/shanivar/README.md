# Current creative revision

Social production now follows the [engagement and Reels playbook](engagement-reels-playbook.md). The Social Media Manager may activate the [Creative Writer](../../ai_org/agents/creative_writer.md), with accepted changes recorded in the [creative learning log](creative-learning-log.md).

Publishing and acquisition automation follows the [official-API strategy](../../automation/social/SOCIAL_AUTOMATION_STRATEGY.md). The prepared Instagram workflow and local setup guide are under [`automation/social/`](../../automation/social/README.md); account authorization, public media hosting and final approval are still required before anything can be scheduled live.

The current creative is [Week 01, revision 3: A question worth staying with](week-01-calendar.md). It contains seven new captions, three 30-second Reel scripts, image/cover briefs and draft accessibility descriptions reflecting the founder's updated audience, astrology, AI teacher and text-scroller direction. Copy is ready for review; matching v3 media has not yet been produced.

[Revision 2 media preview](week-01-v2/index.html) and all media under `week-01/` are historical packages, not previews of the current calendar. The [previous calendar](week-01/calendar-v2-before-positioning.md) is preserved. The media specifications and QA below describe the original package only.

---

# Shanivar week-one media package

Open week-01/index.html in a browser to review all seven posts and play the videos. Full captions and proposed timing are in week-01-calendar.md; evidence and audience assumptions are in audience-research.md.

Prepared 9 September 2026. Proposed dates: 14–20 September, 19:30 Asia/Kolkata. Instagram-first English-language India pilot; this positioning requires validation against actual audience responses.

Deliverables: four AI-generated portrait PNGs (1122 x 1402), three original silent 1080 x 1920 H.264 MP4 motion graphics (24 fps; 16, 15 and 16 seconds), three Reel covers, captions, source links and alt text. AI artwork depicts illustrative scenes, not customers or testimonials. No music, voiceover or filmed footage is included.

QA: image copy and composition visually reviewed; all MP4s fully decoded successfully; every encoded scene sampled in week-01/video-review.png. Technical checks do not establish platform upload acceptance, which has not been tested. Source-generation storyboard thumbnails are also included as day-XX-qa-N.png.

Rebuild videos: install Pillow and imageio-ffmpeg in your Python environment, then run week-01/render-reels.py. The script uses Arial from C:/Windows/Fonts; adapt fontdir on other operating systems. The workspace .tools dependency folder is ignored by Git.

Status: ready for review. No live posts, scheduled posts, account connections, messages or ad spend. Publishing approval has not been requested because the current task is research and asset creation.
