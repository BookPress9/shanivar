import datetime as dt
from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.utils import get_column_letter

D = lambda m, d: dt.date(2026, m, d)
COLS = ["Week","Date","Day","Post time (IST)","Format","Pillar","Concept","3-sec hook","Shot list (1 person, phone)","On-screen text","Caption draft","Hashtags","Audio / vibe","Audience value"]

SH_TAGS = "#mindfulness #breathwork #meditation #hybridmonk #shanivar"
MH_TAGS = "#solan #himachal #oachghat #hillcafe #homestay"

shanivar = [
# W1
(D(10,19),"7:30 AM","Reel","Aura / Teach","The 60-second reset: physiological sigh, done like it's nothing.","\"Your nervous system has a reset button. Here it is.\"",
 "1) Tripod wide: slow walk into frame at sunrise, sit. 2) Close-up: double inhale through nose. 3) Side profile: long exhale. 4) Slow-mo shoulders dropping. 5) Eyes open, small nod to camera.",
 "Double inhale (nose) → Long exhale (mouth) → Repeat 3x → That's the reset.",
 "Stress doesn't need an hour. It needs one honest exhale. Try 3 rounds now, before you scroll on. Save this for your next 3pm spiral.",
 SH_TAGS+" #physiologicalsigh #stressrelief #calmmind","Original audio: real breath sounds + low ambient drone. Calm, unhurried.","A science-backed stress downshift that takes 60 seconds, anywhere."),
(D(10,21),"8:00 PM","Carousel","Teach","\"Why you can't sit still\" — 5 reasons + 1 fix each.","Slide 1: \"You're not bad at meditation. You're doing it wrong.\"",
 "Text carousel, 7 slides, earthy palette. Optional slide 1 photo: you seated, back to camera, window light.",
 "1 Hook / 2 Too long too soon → start with 2 min / 3 Wrong time → after a walk / 4 Fighting thoughts → label them 'thinking' / 5 No anchor → count breaths to 10 / 6 Phone nearby → other room / 7 Save + try tonight",
 "Restlessness isn't failure. It's feedback. Pick ONE fix from this list and try it tonight. Which one is you? 👇",
 SH_TAGS+" #meditationtips #beginnermeditation #mentalhealth","N/A (carousel). Optional: soft acoustic if posted with music.","Removes the #1 reason beginners quit: thinking they're 'bad' at it."),
(D(10,23),"7:30 PM","Reel","Aura / Trend","Upside Down trend: doomscroll (camera upside down) → flip → seated, calm, candle lit.","Upside-down frame + text: \"POV: you swap 10 min of scrolling for this.\"",
 "1) Phone flipped: you on sofa, scrolling, harsh blue light. 2) Hand covers lens (transition). 3) Upright: same spot, phone face-down, candle, eyes closed. 4) Slow push-in on calm face.",
 "Before: 10 min of scrolling / After: 10 min of nothing (best decision today)",
 "Same 10 minutes. Very different nervous system. Swap one scroll session today and tell me how you sleep.",
 SH_TAGS+" #digitaldetox #upsidedowntrend #screentime","Trending 'Upside Down' audio if your account type allows; else slow lo-fi beat drop at the flip.","Makes a phone-free habit feel easy and desirable, not preachy."),
(D(10,24),"9:00 AM","Story","Community","Shanivar Sit #1 (weekly Saturday ritual): 5-min guided sit-along.","\"It's Shanivar. Sit with me for 5 minutes.\"",
 "4 frames: 1) Selfie video, invite. 2) Poll: 'How's your mind today? Calm / Cloudy'. 3) 5-min countdown sticker + 'Close your eyes when the timer starts'. 4) Link sticker to app.",
 "Shanivar Sit #1 · 5 min · Breath only",
 "(Story) Every Saturday. Same time. Five minutes. Show up.",
 "—","Your voice, soft room tone.","A tiny weekly ritual that builds consistency + community."),
(D(10,25),"7:00 PM","Reel","Aura / Ritual","Dussehra week: burn one habit. Write it, fold it, let it go (safely, in a bowl).","\"Ravan burned this week. What are you letting go of?\"",
 "1) Overhead: pen writing one word on paper. 2) Fold slowly. 3) Match strike close-up. 4) Paper in metal bowl (safe). 5) Wide: you sitting with it, breathing, unbothered.",
 "Write one habit · Fold it · Let it go · Breathe 3x",
 "Victory over the inner Ravan isn't loud. It's one quiet decision. Comment the one word you're releasing.",
 SH_TAGS+" #dussehra #lettinggo #innerwork #spiritualliving","Deep tanpura drone or slow flute. Cinematic, minimal.","Turns a festival into a usable reflection ritual."),
# W2
(D(10,26),"7:30 AM","Reel","Aura / Trend","Morning Reset Loading (GRWM progress-bar trend): monk-style morning in 6 cuts.","\"Morning reset loading… 0%\" (you, half-awake, phone in hand)",
 "1) 0% bed, phone glow. 2) 20% glass of water. 3) 40% balcony, sunlight on face. 4) 60% seated breath. 5) 80% one-line journal. 6) 100% walk out the door, calm stride.",
 "0% phone · 20% water · 40% sunlight · 60% breathe 4 min · 80% one line journal · 100% ready",
 "No 5am club. No ice bath. Just 6 small steps in 15 minutes. Steal this routine.",
 SH_TAGS+" #morningroutine #grwm #slowmorning","'Daily GRWM loading' style: sped-up trending sound if allowed, else upbeat lo-fi.","A realistic 15-min morning routine they can copy tomorrow."),
(D(10,28),"8:00 PM","Carousel","Teach / Trend","Halloween myth-busting: 'Meditation ghost stories' (sheet-ghost format).","Slide 1: sheet-ghost graphic + \"Scary meditation myths 👻 (busted)\"",
 "7 slides. Optional: one photo of a bedsheet ghost sitting cross-legged (funny, shareable).",
 "Myth: you must empty your mind → Truth: notice it / Myth: needs an hour → 2 min counts / Myth: it's religious → it's training attention / Myth: must sit cross-legged → chair is fine / Myth: it should feel blissful → it should feel honest",
 "The only scary thing about meditation is how long we wait to start. Send this to the friend who says 'I can't meditate.'",
 SH_TAGS+" #halloween #meditationmyths #mindfulliving","N/A","Clears misconceptions; highly shareable."),
(D(10,30),"7:30 PM","Reel","Aura / Teach","Flow State: box breathing before a 25-min deep work block. Calm competence over hustle.","\"Monks don't multitask. Here's the 25-min version.\"",
 "1) Messy desk, 12 tabs (screen record). 2) Close laptop lid halfway, box breath 4-4-4-4 (tight shot). 3) Timer set to 25. 4) Top-down timelapse of focused work. 5) Timer ends, you stretch, tiny smile.",
 "Box breath 4-4-4-4 · One tab · 25 min · Then rest",
 "Focus isn't a personality. It's a protocol. Breathe, single-task, rest. Repeat twice today.",
 SH_TAGS+" #deepwork #focus #flowstate #productivity","Minimal piano or ticking-clock ambient.","Gives working professionals a calm focus method."),
(D(10,31),"9:00 AM","Story","Community","Shanivar Sit #2 — Halloween edition: fear check-in + 4-7-8 breath.","\"What's scaring you lately? (Anonymous.)\"",
 "1) Selfie video invite. 2) Question box: 'What are you afraid of lately?' 3) Text frames: 4-7-8 breath x4. 4) Repost 2-3 anonymous answers with a one-line reframe. 5) App link.",
 "Shanivar Sit #2 · Inhale 4 · Hold 7 · Exhale 8",
 "(Story) Name the fear. Then breathe it smaller.",
 "—","Soft voice, no music.","Safe space for anxiety + a calming technique."),
(D(11,1),"7:00 PM","Reel","Trend / Habit","'Message From A' callout format: anonymous texts calling out daily habits.","Phone screen: \"Checked your phone before your eyes fully opened? –A\"",
 "1) Screen-recorded fake texts (Notes/Messages mock). 2) Cut to you reacting deadpan. 3) 3-4 texts: skipped lunch, 'quick scroll' for 40 min, 'I'll breathe later'. 4) Final text: 'Breathe now. –A'. 5) You close eyes, exhale.",
 "Message from A: 'Breathe now.'",
 "A knows. A always knows. Which text got you? 😅 Start with one breath right now.",
 SH_TAGS+" #messagefroma #habits #selfawareness","Trending 'Message From A' suspense sound if allowed; else tense synth → calm pad.","Humorous self-awareness that nudges micro-habits."),
# W3
(D(11,2),"7:30 AM","Reel","Teach / Seasonal","Diwali-week survival breath: 3 techniques for festive overwhelm (crowds, family, noise).","\"Diwali week will test your peace. Carry this.\"",
 "1) Busy market/kitchen B-roll (chaos). 2) You in the middle, still, eyes calm. 3) Technique 1 close-up: long exhale. 4) Technique 2: hand on chest, belly breath. 5) Technique 3: 5-4-3-2-1 senses, eyes scanning. 6) Walk on smiling.",
 "Crowd: long exhale · Family stress: hand on chest · Overwhelm: 5-4-3-2-1 senses",
 "Festivals are joy AND noise. Keep your calm portable. Save this for the week ahead.",
 SH_TAGS+" #diwali #festivestress #calm","Festive dhol muffled → silence → soft flute.","Practical tools for a stressful, timely week."),
(D(11,4),"8:00 PM","Carousel","Teach / Seasonal","Clean your inner home before Diwali: mental declutter checklist.","Slide 1: \"You cleaned the house. Did you clean your head?\"",
 "8 slides, warm diya-toned design. Slide 1 photo: broom + diya flat-lay.",
 "1 Unfollow 5 draining accounts / 2 Close one open loop / 3 Forgive one small thing / 4 Delete one app for the week / 5 Write 3 things you're grateful for / 6 One 5-min sit daily / 7 Save + share",
 "Lakshmi enters a clean home. Peace enters a clean mind. Which one are you doing today?",
 SH_TAGS+" #diwalicleaning #mentaldeclutter #spiritualliving","N/A","Ties a cultural ritual to an actionable mental-health habit."),
(D(11,6),"7:30 PM","Reel","Aura / Seasonal","Dhanteras: 'real wealth is attention' — slow, aesthetic, confident.","\"Buy gold today. But invest in this too.\"",
 "1) Macro: diya flame. 2) Hands pouring chai. 3) You seated, still, gold-hour light. 4) Slow head turn to camera. 5) Text reveal over flame.",
 "Real wealth: attention · rest · breath · people you love",
 "Gold appreciates. So does presence. Invest 5 minutes in yourself today.",
 SH_TAGS+" #dhanteras #presence #mindfulliving","Slow sitar or ambient tanpura. Warm, cinematic.","Reframes wealth; shareable festival message."),
(D(11,7),"9:00 AM","Story","Community","Shanivar Sit #3: 3-min diya gazing (trataka) sit-along.","\"Grab a diya. 3 minutes. Just look.\"",
 "1) Selfie invite. 2) Locked shot of diya, 3-min timer sticker. 3) Emoji slider 'How focused did you feel?' 4) App link.",
 "Shanivar Sit #3 · Trataka · 3 min",
 "(Story) Let the flame hold your attention so you don't have to.",
 "—","Room tone + faint crackle.","Introduces a traditional focus practice, perfectly timed."),
(D(11,8),"6:30 PM","Reel","Aura / Ritual","Diwali: light one diya mindfully — a 30-sec intention ritual.","\"This Diwali, light one diya like a monk.\"",
 "1) Close-up hands, matchstick. 2) Flame catches (slow-mo). 3) Eyes close, one breath. 4) Whisper an intention (no audio needed). 5) Wide: rows of diyas, you seated, calm.",
 "One diya · One breath · One intention",
 "Shubh Deepavali 🪔 Before the crackers, one quiet flame. What's your intention for the year ahead?",
 SH_TAGS+" #diwali #happydiwali #intention","Soft bells + flute. Sacred, unhurried.","A meaningful micro-ritual for the biggest festival of the year."),
# W4
(D(11,9),"7:30 AM","Reel","Teach","Post-Diwali downshift: 5-min reset for sugar, noise and sleep debt.","\"Festival hangover? Don't detox. Downshift.\"",
 "1) Messy room, sweets boxes. 2) Cold water splash on face. 3) 10 min sunlight walk (POV feet). 4) Physiological sigh x3. 5) Glass of water + early-night alarm set.",
 "Cold water · Sunlight walk · 3 sighs · Water · Early night",
 "No guilt. No juice cleanse. Just help your system come back to baseline.",
 SH_TAGS+" #postdiwali #reset #nervoussystem","Gentle morning acoustic.","Guilt-free recovery routine after festive excess."),
(D(11,11),"8:00 PM","Carousel","Teach / Product","The Hybrid Monk week: 7 micro-rituals for people with jobs (one per day).","Slide 1: \"Monk energy. Office calendar.\"",
 "9 slides, one ritual per day, clean typographic design. Last slide: app screenshot.",
 "Mon: 2-min sit · Tue: walk without phone · Wed: box breath before meetings · Thu: gratitude text · Fri: tech sunset 9pm · Sat: Shanivar Sit · Sun: plan + rest",
 "You don't need a monastery. You need a structure. Save this and try one week. All rituals are inside the app — link in bio.",
 SH_TAGS+" #worklifebalance #rituals #modernspirituality","N/A","A saveable weekly system + soft app CTA."),
(D(11,13),"7:30 PM","Reel","Aura / Trend","'Some people call me…' trend → 2am racing-mind protocol.","\"Some people call me the Hybrid Monk. You can call me at 2am when your mind won't shut up.\"",
 "1) Direct-to-camera, calm smirk (the line). 2) Dark room, you in bed, phone face-down. 3) Hand on belly, 4-7-8 breath. 4) Body scan: slow pan feet → head. 5) Lights out.",
 "2am protocol: phone face-down · 4-7-8 x4 · Scan feet to head · Let thoughts pass",
 "Racing mind at night? Don't fight it. Slow the body, and the mind follows. Send this to your 2am friend.",
 SH_TAGS+" #sleep #insomnia #overthinking #somepeoplecallme","Trending 'Some people call me' audio for the opener, then silence/soft rain.","A sleep technique for overthinkers — high save/share value."),
(D(11,14),"9:00 AM","Story","Community","Shanivar Sit #4 + Children's Day: 'breathe like a kid' belly breathing + month recap.","\"Kids breathe right. We forgot. Let's relearn.\"",
 "1) Selfie invite. 2) Belly breathing demo (hand on belly). 3) Poll: 'Did you do any Shanivar Sit this month?' 4) Question box: 'What should next month's sits cover?' 5) App link.",
 "Shanivar Sit #4 · Belly breath · 5 min",
 "(Story) One month of Saturdays. Thank you for sitting with me.",
 "—","Soft voice.","Simple technique + collects content ideas from the audience."),
(D(11,15),"7:00 PM","Reel","Story / Trend","'Take that chance' trend: the Hybrid Monk origin in quick cuts (use your real story beats).","\"I didn't quit my life. I learned to breathe inside it.\"",
 "Quick cuts synced to beat: 1) Old photo/clip of the busy-life era. 2) A low moment (empty desk, night). 3) First sit. 4) Practice montage. 5) Building Shanivar (laptop, app screens). 6) You today, walking calm toward camera.",
 "Word-by-word: take · that · chance · cause · you · might · grow",
 "This app started as one breath on a bad day. If you're waiting for the right time to start, this is it. Link in bio.",
 SH_TAGS+" #founderstory #takethatchance #growth","'What You Waiting For?' trending audio if allowed; else rising cinematic beat.","Builds trust in the founder and brand; inspires the first step."),
]

maahee = [
# W1
(D(10,20),"7:00 PM","Reel","Aura / Place","No-signal arrival: the walk from car to chai, unbothered.","\"Network: gone. Peace: found. 📍Oachghat\"",
 "1) Phone screen: signal bars dropping (or 'No Service' mock). 2) Car door shuts, feet on gravel path. 3) Slow walk-in shot from behind. 4) Chai steam close-up. 5) Wide reveal of valley/view, you sitting, back to camera.",
 "Oachghat, Solan · Quiet is the main amenity",
 "Some places don't need Wi-Fi to feel connected. A short drive past Solan, and the noise just… stops. Tag who you're bringing.",
 MH_TAGS+" #solandiaries #himachaltourism #offbeathimachal","Business-safe 'Autumn Colors' (LuLuMusic) or birdsong + soft guitar.","Shows travelers the core promise: real quiet, close to Solan."),
(D(10,21),"11:00 AM","Story","Locals","Today at Maahee: weather + chai of the day + poll.","\"Today in Oachghat: 18°C and very slow.\"",
 "1) Window/terrace shot with weather sticker. 2) Chai pour close-up. 3) Poll: 'Chai or coffee?' 4) Location sticker + 'Open till __'.",
 "Today: [chai of the day] · Open till [time]",
 "(Story) Come sit for an hour. No rush.",
 "—","Ambient cafe sound.","Gives locals a daily reason to drop by."),
(D(10,22),"7:00 PM","Carousel","Travel guide","48 hours near Solan, slow: a weekend itinerary with Maahee as base.","Slide 1: \"The weekend Himachal trip nobody's posting about.\"",
 "8 slides: your own photos of room, cafe, view + 1 map graphic. Scrapbook-style layout (rising carousel aesthetic).",
 "Day 1: arrive, chai on the terrace, sunset, bonfire dinner / Day 2: Karol Tibba trek or Shoolini Mata temple, Solan market lunch, slow evening at Maahee / Day 3: breakfast with the view, drive out by noon / Save for your next long weekend",
 "Skip the Shimla traffic. This is the slow version of Himachal. Save this, send it to your trip group, and DM us for dates.",
 MH_TAGS+" #weekendgetaway #chandigarhtohimachal #itinerary #slowtravel","N/A","A ready-made, saveable trip plan that removes planning effort."),
(D(10,23),"6:30 PM","Reel","Aura / Seasonal","Autumn in the hills: golden-hour cafe montage (sweater, book, pine light).","\"October in the hills hits different when it's this quiet.\"",
 "1) Sun flare through pines. 2) Hands in sweater around a mug. 3) Book page turning, wind. 4) Slow pan across terrace tables. 5) Locked wide, sun dipping behind hills.",
 "October · Oachghat · No crowd",
 "This is your sign to leave Friday evening. Weekend tables and rooms open — DM to book.",
 MH_TAGS+" #autumnvibes #octoberinhimachal #goldenhour","'Autumn Colors' by LuLuMusic (business-safe, trending).","Seasonal FOMO + direct booking nudge right before the weekend."),
(D(10,25),"9:00 AM","Reel","Aura / Stay","Room loading 0% → 100% (GRWM progress-bar trend): curtains open on the view.","\"Your Sunday morning view could be… loading 0%\"",
 "1) 0% dark room, curtains closed. 2) 25% kettle on. 3) 50% curtains open. 4) 75% blanket + chai at window. 5) 100% balcony/view wide.",
 "Room loading… 0% → 100% · Maahee Homestay",
 "Wake up to this instead of an alarm. Weekday stays are even quieter. Link in bio / DM 'STAY'.",
 MH_TAGS+" #roomtour #homestayhimachal #morningview","Trending 'Windows in Brazil (Sped Up)' if allowed; else soft upbeat acoustic.","Lets travelers see exactly what the room experience is."),
# W2
(D(10,27),"7:00 PM","Reel","Trend / Workcation","Upside Down trend: city WFH desk (flipped) → hill desk at Maahee.","Upside-down city desk + \"Your WFH desk vs. mine.\"",
 "1) Flipped camera: cramped desk, traffic noise. 2) Hand-over-lens transition. 3) Upright: laptop on terrace, chai, view. 4) Screen-record a real Wi-Fi speed test (only if it's good). 5) You leaning back, calm.",
 "Same meetings. Better view. Wi-Fi: [real speed] Mbps",
 "Workcation-ready: Wi-Fi, chai on tap, and a view for your 4pm slump. Weekly stay rates — DM 'WORK'.",
 MH_TAGS+" #workcation #workfromhills #remotework #digitalnomadindia","'Upside Down' trending audio if allowed; else beat drop on flip.","Answers the remote worker's #1 question: can I actually work here?"),
(D(10,28),"11:00 AM","Story","Locals","Weekday locals' special + 'guess the dish' quiz.","\"Locals, this one's for you.\"",
 "1) Dish close-up with blur. 2) Quiz sticker: 'Guess today's special'. 3) Reveal. 4) Weekday offer text + reminder sticker.",
 "Weekday special: [dish] · [offer, e.g. free chai refill Mon–Thu]",
 "(Story) Solan regulars, weekdays are yours.",
 "—","Kitchen sizzle sound.","Gives locals a recurring weekday reason to visit."),
(D(10,29),"7:00 PM","Carousel","Travel guide","What to pack for Solan in November (and what to skip).","Slide 1: \"Packing for the hills in November? Don't make mistake #3.\"",
 "7 slides: flat-lay photos of jacket, shoes, thermos on a Maahee table.",
 "1 Layers (days ~20°C, nights cold) / 2 Proper walking shoes / 3 Mistake: no warm sleepwear / 4 Torch for evening walks / 5 Cash backup / 6 Skip: heavy luggage / 7 Save + DM for dates",
 "Pack light, pack warm, arrive slow. Save this before your trip.",
 MH_TAGS+" #packinglist #himachaltravel #winterinhimachal","N/A","Practical trip prep; positions Maahee as the helpful local expert."),
(D(10,30),"7:00 PM","Reel","Trend / Seasonal","'Things you should definitely ghost' (Halloween week) → this weekend's escape.","\"Things you should definitely ghost this weekend 👻\"",
 "1) Clip: traffic jam (stock/own). 2) Crowded mall road. 3) Phone notifications pinging. 4) Cut: quiet Maahee path. 5) You with chai, unbothered smirk at camera.",
 "Ghost: Shimla traffic · Ghost: crowded mall roads · Ghost: notifications · Keep: this",
 "Ghost the crowd, not your weekend. Rooms open for Sat — DM 'QUIET'.",
 MH_TAGS+" #halloween #weekendvibes #offbeathimachal","Playful spooky synth → calm acoustic at the reveal. Or voiceover.","Humorous, timely reason to pick a quiet spot this weekend."),
(D(11,1),"9:00 AM","Reel","Food / ASMR","Kitchen ASMR: hill breakfast being made (swap in your real signature dish).","Sound-first: butter sizzling on tawa (no music for the first 3 sec).",
 "1) Tight: butter hits tawa. 2) Dough/paratha flip. 3) Chai boiling over. 4) Plating on the terrace. 5) First bite, view behind.",
 "Sunday breakfast at Maahee · [dish name]",
 "Sundays taste like this here. Open for breakfast from [time]. Locals, bring the family.",
 MH_TAGS+" #pahadifood #breakfast #foodasmr #solanfood","Original ASMR audio only. Warm, homely.","Shows the food, drives locals for Sunday breakfast."),
# W3
(D(11,3),"7:00 PM","Reel","Trend / Humor","'My Reaction To' ladder: city noise levels → Oachghat silence.","Deadpan face + \"My reaction to: 10 cars honking\"",
 "Same framing each clip, reaction escalates: 1) 10 horns: blank. 2) 5 horns: slight frown. 3) 1 horn: eyebrow. 4) 0 horns, only birds: slow, huge smile, leans back into chair at Maahee.",
 "10 horns 😐 · 5 horns 😑 · 1 horn 🤨 · 0 horns, just birds 😌",
 "Diwali is coming. The noise is coming. Plan your quiet now — few rooms left.",
 MH_TAGS+" #myreactionto #relatable #peacefulplaces","Original/voiceover (no dominant audio for this trend).","Funny, relatable, and plants the 'quiet Diwali' idea."),
(D(11,4),"11:00 AM","Story","Booking","Diwali weekend at Maahee: rooms left + countdown.","\"Quiet Diwali in the hills? [X] rooms left.\"",
 "1) Room shot with diya. 2) Countdown sticker to Nov 7. 3) Text: what's included (diyas, dinner, bonfire — only if true). 4) 'DM DIWALI' + link sticker.",
 "Diwali at Maahee · [X] rooms left · DM 'DIWALI'",
 "(Story) Celebrate with lights, not smoke.",
 "—","Soft festive flute.","Urgency + clear booking path."),
(D(11,5),"7:00 PM","Carousel","Seasonal","A quiet Diwali in the hills: 6 reasons to celebrate away from smoke and noise.","Slide 1: \"What if this Diwali was quiet?\"",
 "7 slides: diyas on terrace, valley at night, sweets on a table, family at bonfire (your photos).",
 "1 Cleaner air / 2 Valley lights instead of crackers / 3 Home-style festive dinner / 4 Pets & elders stay calm / 5 Real time with family / 6 Rooms left — DM",
 "Same festival, fresher air. Celebrate Diwali the slow way at Maahee. Save + send to family.",
 MH_TAGS+" #diwali2026 #greendiwali #diwaligetaway","N/A","Gives families a clear, appealing alternative plan."),
(D(11,6),"6:30 PM","Reel","Aura / Seasonal","Dhanteras: decorating Maahee with diyas at dusk (timelapse + slow-mo).","\"Getting Maahee ready for Diwali 🪔\"",
 "1) Timelapse (tripod): lighting diyas along the path. 2) Slow-mo: wick catching. 3) Fairy lights switching on. 4) Wide dusk shot, valley lights below. 5) Calm walk past the diyas.",
 "Dhanteras at Maahee · Oachghat, Solan",
 "Lights are up. Chai's on. Shubh Dhanteras from all of us at Maahee 🪔",
 MH_TAGS+" #dhanteras #diwalidecor #himachal","Business-safe soft festive instrumental.","Atmospheric, shareable; last-minute weekend bookings."),
(D(11,8),"8:00 PM","Story","Community","Diwali night live: diyas, sweets, guests (with permission).","\"Happy Diwali from the hills 🪔\"",
 "4-6 quick frames: diyas, rangoli, dinner table, valley lights, guest smiles (ask first), team wish.",
 "Shubh Deepavali · Maahee family",
 "(Story) Thank you for celebrating with us.",
 "—","Real ambient sound.","Social proof + warm brand moment."),
# W4
(D(11,10),"7:00 PM","Reel","Aura / Place","Post-Diwali escape: 10 seconds of pure Oachghat morning sound.","Black screen text: \"Turn sound ON. This is Oachghat at 7am.\"",
 "1) Locked tripod: mist over hills. 2) Birds in a tree (zoom). 3) Dew on a leaf (macro). 4) Steam from a cup. 5) Wide, nobody in frame.",
 "After 5 days of crackers, your ears deserve this.",
 "No filter on the sound. Weekday stays this month are the quietest of the year. DM 'QUIET'.",
 MH_TAGS+" #naturesounds #morningvibes #peace","Original audio: birds + wind only.","Sensory proof of the quiet; perfect post-festival timing."),
(D(11,11),"11:00 AM","Story","Locals / UGC","Meet the regulars + repost tagged guest stories.","\"Our regulars have opinions 👀\"",
 "1) 15-sec selfie chat with a regular (permission). 2) Repost 2-3 guest stories. 3) Question box: 'What should we add to the menu?' 4) 'Tag @[handle] to be featured'.",
 "Regulars' table · Tag us to be featured",
 "(Story) This place is built by the people who keep coming back.",
 "—","Cafe ambience.","Community + social proof for locals."),
(D(11,12),"7:00 PM","Carousel","Brand story","The story of Maahee: scrapbook-style 'how this place came to be'.","Slide 1: \"We built a cafe where nobody was looking.\"",
 "8 slides, scrapbook aesthetic: old/build photos, hand notes, the first cup, the team, today.",
 "1 Hook / 2 Why Oachghat / 3 The first day / 4 The name 'Maahee' / 5 What we serve / 6 The people / 7 What's next / 8 Come say hi",
 "Every quiet place has a loud backstory. Here's ours. Thank you for being part of it.",
 MH_TAGS+" #smallbusiness #ourstory #himachalcafe","N/A","Builds emotional connection and trust."),
(D(11,13),"7:00 PM","Reel","Aura / Seasonal","Winter weekends start now: bonfire, blanket, maggi, stars (feature only what you offer).","Fire crackle close-up + \"Winter weekends at Maahee start now.\"",
 "1) Match lighting bonfire. 2) Blanket over shoulders. 3) Maggi/soup steam close-up. 4) Low-angle sky (night mode). 5) Wide: friends around fire, silhouettes.",
 "Bonfire · Blankets · Stars · Oachghat",
 "Cold nights, warm fire, zero hurry. Book your winter weekend — DM 'FIRE'.",
 MH_TAGS+" #bonfirenight #winterinhimachal #weekendplans","Acoustic guitar + fire crackle.","Seeds winter demand and repeat weekend trips."),
(D(11,15),"6:30 PM","Reel","Aura / Trend","'Some people call me…' trend: \"Some people call it the middle of nowhere. We call it Maahee.\"","\"Some people call it the middle of nowhere…\"",
 "1) Drone-like high shot from a slope (handheld, steady). 2) Empty road leading in. 3) Cut: cafe lights on. 4) Host to camera, calm smile: '…we call it Maahee.' 5) Door opens, warm light.",
 "Middle of nowhere → Maahee Cafe & Homestay · Oachghat, Solan",
 "Middle of nowhere. Center of calm. Cafe open daily, rooms on weekdays & weekends. DM to book.",
 MH_TAGS+" #somepeoplecallme #hiddengem #solan","Trending 'Some people call me' audio for the open, then soft acoustic.","Memorable brand line + clear invite for locals and travelers."),
]

wb = Workbook()
thin = Side(style="thin", color="D9D9D9")
fills = {"Reel":"FCE4D6","Carousel":"DDEBF7","Story":"E2EFDA"}
widths = [6,11,6,10,10,13,32,32,48,36,44,30,30,30]

def sheet(ws, rows, title_color):
    ws.append(COLS)
    for c in ws[1]:
        c.font = Font(bold=True, color="FFFFFF"); c.fill = PatternFill("solid", fgColor=title_color)
        c.alignment = Alignment(wrap_text=True, vertical="center")
    start = dt.date(2026,10,19)
    for r in rows:
        d,t,fmt,pillar,concept,hook,shots,ost,cap,tags,audio,val = r
        wk = (d-start).days//7+1
        ws.append([wk, d, d.strftime("%a"), t, fmt, pillar, concept, hook, shots, ost, cap, tags, audio, val])
        row = ws.max_row
        ws.cell(row,2).number_format = "ddd dd mmm yyyy"
        for c in ws[row]:
            c.alignment = Alignment(wrap_text=True, vertical="top")
            c.border = Border(top=thin,bottom=thin,left=thin,right=thin)
        ws.cell(row,5).fill = PatternFill("solid", fgColor=fills[fmt])
        ws.cell(row,5).font = Font(bold=True)
    for i,w in enumerate(widths,1):
        ws.column_dimensions[get_column_letter(i)].width = w
    ws.freeze_panes = "H2"
    ws.auto_filter.ref = ws.dimensions

ws1 = wb.active; ws1.title = "Shanivar (@app.shanivar)"
sheet(ws1, shanivar, "5B4A8B")
ws2 = wb.create_sheet("Maahee Cafe & Homestay")
sheet(ws2, maahee, "2E6B4F")

g = wb.create_sheet("Guide")
guide = [
 ("Topic","Guidance"),
 ("Window","4 weeks: Mon 19 Oct – Sun 15 Nov 2026. 5 posts/week per brand (3 reels, 1 carousel, 1 story for Shanivar; Maahee mixes 2–3 reels, 1 carousel, 1–2 stories)."),
 ("Aura farming, our version","'Effortlessly cool without trying': slow movement, steady framing, minimal text, one confident beat (a look, nod or smirk to camera). Never chase it. Every aura reel still carries one useful takeaway, so it earns saves and shares, not just views."),
 ("Trend tie-ins (Oct 2026)","Upside Down transition, Daily GRWM 'loading' progress bar, Message From A callouts, My Reaction To ladder, 'Things you should definitely ghost', Some People Call Me, Take That Chance, Flow State, scrapbook carousels, Halloween sheet-ghost myth-busting. Check each in-app before posting; trends move weekly."),
 ("Audio rule","Business accounts often can't use licensed/trending songs. Use the in-app 'commercially available' sounds, original audio, or voiceover. 'Autumn Colors' (LuLuMusic) is reported as business-safe and trending. Original breath/nature/ASMR audio fits both brands anyway."),
 ("Festival hooks","Dussehra week (Oct 19–25), Halloween (Sat 31 Oct), Dhanteras (Fri 6 Nov), Diwali (Sun 8 Nov), Children's Day (Sat 14 Nov)."),
 ("Post times","Times in the sheets are IST (India audience). IST is 3.5 h ahead of Berlin until Sat 24 Oct, then 4.5 h ahead from Sun 25 Oct (CET). Example: 7:00 PM IST = 3:30 PM Berlin (to 24 Oct) / 2:30 PM Berlin (from 25 Oct)."),
 ("Solo shooting kit","Phone + small tripod/clamp, natural window or golden-hour light, clip-on mic (optional). Shoot 4K/30, 9:16, 3–5 s per clip, then edit in Edits/CapCut/IG editor."),
 ("Placeholders","Anything in [brackets] (dishes, offers, room counts, Wi-Fi speed, open hours, founder story beats) needs real details. Don't claim amenities you don't offer."),
 ("KPIs to watch","Reels: 3-s hold rate, avg watch %, shares, saves. Carousels: saves, swipe-through rate. Stories: poll/quiz taps, link clicks. Maahee: DMs with keywords (STAY/QUIET/DIWALI/WORK/FIRE). Shanivar: link-in-bio app clicks, Shanivar Sit replies."),
]
for r in guide: g.append(list(r))
for c in g[1]: c.font = Font(bold=True)
g.column_dimensions["A"].width = 24; g.column_dimensions["B"].width = 120
for row in g.iter_rows():
    for c in row: c.alignment = Alignment(wrap_text=True, vertical="top")

wb.save("/workspace/content-calendar.xlsx")
print(len(shanivar), len(maahee))
