# VBT breathing and meditation catalogue for Shanivar

Date: 25 September 2026. Research and product specification; no implementation or clinical validation claimed.

## Direction

The founder requests breathing/meditation as Shanivar's third pillar alongside action-oriented alarms and focus protection. Present modern choices based on feelings, circumstances and the user's daily schedule; preserve traceable VBT origins. This is an authorized scope extension beyond the earlier library deferral. The full VBT inventory and the beginner app library are different sets.

Use **Breathe & reset** as the entry label, with breath, sound and attention practices identified honestly. Source details are available from every practice. Belief, Sanskrit knowledge and an account are not prerequisites. “Brain rot” is audience language, not a diagnosis or a claim that the app repairs the brain.

Build instructions: [Claude handoff](../android-app/CLAUDE_VBT_BREATHING_HANDOFF_2026-09-25.md). Exact starter scripts: [versioned catalogue](../shanivar-vbt/content/breathing-mvp-v1.json). Acceptance checks: [test guide](../android-app/VBT_BREATHING_TEST_GUIDE_2026-09-25.md).

## Sources, numbering and coverage

The repository contains 12 selected adaptations under `shanivar-vbt/content/`, an old assessment router and a citation report. It does not contain a complete locally identified VBT book edition. For this expansion, I read the English practice verses 24–138 in Christopher Wallis's current public [complete translation](https://tantra112.app/vijnana-bhairava-tantra), checked the breath-related closing material, and consulted his specialist commentaries below. This was an English-source audit, not an independent Sanskrit translation or a cover-to-cover review of Singh's commentary.

Use Sanskrit **verse/sloka** references, not a “technique number out of 112.” Grouping differs: the current Wallis resource groups 24–27 under one yukti. The old local filenames 01, 02, etc. are catalogue IDs, never verse numbers. The closing haṃsa passage is specifically labeled **156-A** in the current resource; it has manuscript/edition qualifications. Store that label as a string.

Source keys:

- **W:** [Wallis, complete public translation](https://tantra112.app/vijnana-bhairava-tantra), web edition accessed 2026-09-25; do not silently label new citations “2018” when using the revised web text.
- **F:** [Commentary on 24–27](https://hareesh.org/blog/2022/11/30/vijana-bhairava-tantra-verses-22-27-the-foundational-practice).
- **C:** [Commentary on 28](https://hareesh.org/blog/2022/12/3/vijana-bhairava-tantra-verse-28-the-central-channel).
- **S:** [Commentary on 38–39](https://hareesh.org/blog/2024/10/1/vijnaana-bhairava-tantra-verses-38-amp-39) and [40–41](https://hareesh.org/blog/2024/12/7/vijaana-bhairava-tantra-verses-40-amp-41).
- **U:** [Commentary on 42](https://hareesh.org/blog/2025/4/30/vijaana-bhairava-tantra-verse-42-advanced-uccaara).
- **H:** [Commentary on 49–51](https://hareesh.org/blog/2025/6/1/vijaana-bhairava-tantra-verses-49-51-the-heart-lotus-and-the-end-of-twelve).
- **D:** [Commentary on 55, including variant readings](https://hareesh.org/blog/2025/11/29/vijaana-bhairava-tantra-verse-55-freedom-in-the-dream-state).
- **J:** [Revised closing verses](https://hareesh.org/blog/2025/9/30/the-closing-verses-of-the-vijana-bhairava-tantra).

The local report's “12 of 12 clear to ship” is a historical citation-review conclusion. It is not a medical, accessibility, licensing or device-safety certification. Its broad two-source claims are stronger than its described spot-check method in several rows. Newly added entries below have been checked against W and the named commentaries where available; an independent second-edition check is not claimed.

## All identified breath passages and relevant boundary cases

This is the complete set identified by the above audit under these explicit inclusion rules: direct breath instructions; prāṇa/retention practices; voiced or internal utterance relevant to a breathing implementation; and nearby practices often confused with breathing. A broad “all prāṇa is breath” reading would incorrectly relabel much of the text. Research-only entries remain discoverable in this document, not selectable in the beginner app.

### Direct breath, retention and closing practice

| Verse | Source idea, briefly | App disposition | Source |
| --- | --- | --- | --- |
| 24 | Awareness at breath endpoints | Natural anchor and turn-awareness adaptations | F |
| 25 | Remaining at inner/outer endpoints | Combined with 24; never prolong a pause | F |
| 26 | Stillness of breath-energy | Research only; do not train respiratory arrest | F |
| 27 | Retention following inhalation/exhalation | Research only; exclude the old counted-hold script | F |
| 55 | Gross/subtle breath-energy; variant interpretations | Research only; no promise of dream control | D |
| 64 | Convergence of the two vital flows | Research only; no invented “balance your hormones” routine | W |
| 68 | Retention within an esoteric pleasure practice | Research only; no general-wellness simplification | W |
| 154–155 | Breath-energy and contemplative absorption | Contextual reference, not a separate beginner drill | J |
| 156-A; 156-B | Natural breath-mantra; traditional enumeration | Optional breath-and-word adaptation; no breath quota | J; W |

F describes a contemplative framework, not modern prescriptions for anxiety, work or circadian regulation. In D, the textual reading can change whether verse 55 concerns dreams at all. In J, 156-A is described as absent from many manuscripts, though historically attested. Preserve these qualifications in source notes. The traditional daily breath count is not an app target, a sensor estimate or a health norm.

### Breath-adjacent, sound and subtle-body material

The concise labels below identify scope; disposition is Shanivar's design judgment.

| Verse | Subject | Disposition |
| --- | --- | --- |
| 28 | Subtle energy visualization | Research only; C/W. Not anatomical airflow. |
| 29–31 | Centers, ascent, energetic restraint | Research only; W. No chakra or physiological measurement. |
| 35–37 | Central channel, sensory closure, bindu | Research only; W. Not a beginner breath timer. |
| 38 | Inner sound | Sound context only; S. Do not instruct users to seek tinnitus. |
| 39 | Sustained utterance and its ending | Modern gentle hum option; S. No forced exhale or delayed inhale. |
| 40 | Sound boundaries | Supports sound/attention adaptation; S. Not inherently breath control. |
| 41 | Fading instrumental sound | Listening alternative; S. No breathing instruction required. |
| 42 | Advanced internal mantra progression | Research only; U. Not a novice sound preset. |
| 49–51 | Heart/upper-center attention | Research only in this release; H. Breath-endpoint interpretation is commentary-dependent. |
| 67 | Restraining vital-energy streams | Research only; W. No sensory sealing, locks or strain. |
| 77 | Named mudrās | Boundary case; W. Not five ready-made breathing exercises. |
| 81 | Jaw/tongue posture and mental HA | Later research; W. Removing its specifics would need a separately labeled adaptation. |
| 90–91 | Vowel/visarga contemplation | Later sound research; W. Do not relabel as physiological sighing. |
| 114 | Closures and subtle sound | Research only; W. No beginner instructions for bodily locks. |
| 118 | Transitions, including sneezing | Boundary case; W. Never induce sneezing, fear or exhaustion. |
| 130; 144 | Recitation/meaning and japa context | Outside breath library; W. Not respiratory protocols. |
| 138 | Dissolution including vital energy | Concluding context; W. Not a breath-stopping exercise. |

Other reviewed passages are predominantly visual, spatial, sensory, emotional or contemplative rather than direct breathing instructions. This catalogue does not invent breath techniques to inflate the total to 112. In particular, box breathing, 4-7-8, alternate-nostril breathing, physiological sighing, “Tesla breathing,” and prescribed breaths/minute are not established as VBT prescriptions by this audit.

### Supporting practices for modern conditions

Reuse the local adaptations only after the corrections below. Verse 75 supplies sleep-threshold context; 101 emotion observation; 61–62 attention transitions; 60/84 spacious looking; 83 gentle rocking; 72 savoring. These are **not breath-control practices**. The MVP includes the first two plus the listening alternative from 41. A simple external grounding option has **no VBT attribution**, rather than attaching a misleading verse to it. Existing local sources: [content index](../shanivar-vbt/content/README.md), especially techniques 04, 09, 10 and 12.

## Repairs to the inherited material

| Finding | Required treatment in the new feature |
| --- | --- |
| Technique 01 says gaps occur “twice a minute” and implies a fixed half-second pause | There are turning points in each breathing cycle; their duration is variable and a perceptible pause is not required. Do not reproduce that wording. |
| Technique 02 teaches 4-4-4-4 retention | Keep as historical research only. Beginner release has no holds; a source citation does not validate that ratio for every user. |
| Technique 03 says not to inhale immediately after humming and to run out of breath | Stop the hum while comfortable; inhale whenever needed, including while listening. Silence is not apnea. |
| Old router calls breath the safest universal fallback and routes anxiety to breath | Replace with user preference/comfort and an external-attention fallback. Never infer that anxiety means a user should control their breathing. |
| Old router's opening still says content is empty | Historical scaffolding, not a current implementation contract. |
| Old player displays “App lock off” without reading actual protection | New player cannot claim protection status from practice state. |
| Old player keeps the screen awake and asks for text reflection | Bedtime variant lets the display sleep and requires no response. |
| Earlier brief proposes counted pacing | VBT MVP uses **unpaced awareness**. Any later paced exercise needs its own modern-source identity. |

The JSON is the sole starter-script source for this feature. Do not auto-import the old Markdown scripts or regenerate them into the new asset with `compile-content.cjs`.

## Modern options and condition mapping

These are usability hypotheses, not diagnosis-to-treatment rules. Users choose the feeling or goal and can always change the suggestion. The same underlying practice can serve multiple contexts; avoid presenting a new technique for every emotion.

| Option / ID | Use when | Schedule context | VBT relationship | Duration choices |
| --- | --- | --- | --- | --- |
| **Arrive with your breath** / `natural_anchor` | Scattered, before work, just awake | Any awake period | Gentle introductory adaptation of 24 | 1 / 3 / 5 min |
| **Notice the turn** / `breath_turn` | Steady enough for subtle attention; already familiar | Awake, seated and comfortable | 24–25; unforced transition awareness | 1 / 3 / 5 min |
| **Hum, then listen** / `hum_listen` | Tense; wants sound; private enough | Awake or winding down | Modern adaptation of 39–40 | 1 / 3 min |
| **One breath, one word** / `breath_word` | Scattered/repetitive thinking; prefers a mental cue | Awake or winding down | Modern nonreligious word substitution for 156-A | 1 / 3 / 5 min |
| **Make room for the feeling** / `emotion_space` | Angry, frustrated or caught in an urge | Any safe awake period | Attention adaptation of local v.101 content; not breathwork | 1 / 3 min |
| **Follow a fading sound** / `sound_listen` | Overstimulated; wants an external anchor; sound is comfortable | Awake or winding down | Sound adaptation of 41 | 1 / 3 min |
| **Let the day end** / `sleep_threshold` | Already sleepy and choosing sleep | User-selected sleep time, including daytime | Attention adaptation of 75; not breathwork | 1 / 3 / 5 min |
| **Notice the room** / `external_grounding` | Breath/sound attention unwelcome; unsure what to choose | Any safe stationary context | Modern fallback; no claimed VBT source | 1 / 3 min |

“Low energy” should suggest a short eyes-open arrival or room-orientation practice followed by the user's action, never stimulating hyperventilation. “Tense” should offer a choice between a comfortable anchor and making room for the feeling. Feeling overwhelmed is a reason to simplify, not to assign an advanced technique. Source notes belong behind “Origin & adaptation”; everyday language leads the card.

## Body-clock support: what can be engineered honestly

Circadian timing and sleep pressure are influenced by light, time awake and individual schedules. A clock-derived label does not measure circadian phase. The new feature is **schedule-aware**, with user-reported states overriding suggestions; it is not a circadian treatment. General support for this distinction: [NHLBI sleep/wake explanation](https://www.nhlbi.nih.gov/health/sleep/sleep-wake-cycle).

Use locally chosen wake and intended-sleep times, plus an explicit “Just woke up / Daytime / Winding down / Going to sleep” override. Never assume 07:00 is morning for a shift worker. `Rhythm.quietTime` is an existing quiet preference, not confirmed sleep onset; obtain a separate optional sleep preference. Do not infer cortisol, melatonin, vagal tone or sleep stage from time, mood, birth data or phone activity.

Near planned sleep, use dim visuals, no bright flashes, no closing bell, no streak animation and no required check-in. After waking, keep eyes-open practice available and offer the next action. A scheduled alarm does not prove the person woke up; an alarm-dismissal entry is a context hint, which the user can change. Unknown schedule means neutral suggestions, not fabricated personalization. Exact windows and precedence are specified in the handoff as product rules to test, not biological constants.

## Evidence and release interpretation

VBT gives contemplative origins. It does not establish the clinical effectiveness of these modern scripts or their emotion/time-of-day matching. Prior breathwork research is indirect: the [400-person coherent-breathing trial](https://www.nature.com/articles/s41598-023-49279-8) found no advantage over its active control on mental health/wellbeing. It did not test these VBT adaptations. [NHS guidance](https://www.nhs.uk/mental-health/self-help/guides-tools-and-activities/breathing-exercises-for-stress/) supports comfortable, unforced breathing, not compulsory counting or holds.

Keep scripts original and link the translation. Do not embed the modern translation, paid recordings, app artwork or teacher audio without appropriate permission. The author explicitly reserves reproduction rights on the closing commentary; free access is not a redistribution license. Sanskrit heritage and a modern translator's wording are distinct.

Claude can build the complete local feature now from the accompanying package. Before public release, record qualified content/comfort review and real-device results. An internal build can be complete while a release gate is still outstanding; do not describe one as the other.
