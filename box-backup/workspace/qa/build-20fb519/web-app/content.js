/* Content copied from shanivar-vbt/content/breathing-mvp-v1.json (catalogVersion vbt-breathe-2026-09-25.1, "implementation_seed_review_before_public_release")
   and android-app InitiationSitting.kt. Do not paraphrase; both need qualified human review before public release. */
(function (root) {
"use strict";
var PRACTICES = [
 {
  "id": "natural_anchor",
  "title": "Arrive with your breath",
  "allowed": [
   60,
   180,
   300
  ],
  "defaultSeconds": 180,
  "source": "Modern adaptation · VBT verse 24",
  "sleep": false,
  "stages": {
   "arrival": "Sit or settle somewhere safe. Keep your eyes open or let your gaze soften.",
   "instruction": "Notice one ordinary breath coming in and going out. Leave its size and speed alone.",
   "practice": "Whenever attention wanders, return to an easy part of breathing. Nothing needs to become deeper or slower.",
   "closing": "Look around when you are ready. Choose the next small thing you want to do."
  }
 },
 {
  "id": "breath_turn",
  "title": "Notice the turn",
  "allowed": [
   60,
   180,
   300
  ],
  "defaultSeconds": 180,
  "source": "Modern adaptation · VBT verse 24, 25",
  "sleep": false,
  "stages": {
   "arrival": "Settle comfortably. Let breathing continue by itself.",
   "instruction": "Notice a breath changing direction. There may be no noticeable pause. Do not create one or wait to breathe.",
   "practice": "Let the next breath arrive whenever it needs to. If looking for a turn feels effortful, simply notice ordinary breathing.",
   "closing": "Leave the practice whenever you wish. Bring attention back to the room."
  }
 },
 {
  "id": "hum_listen",
  "title": "Hum, then listen",
  "allowed": [
   60,
   180
  ],
  "defaultSeconds": 180,
  "source": "Modern adaptation · VBT verse 39, 40",
  "sleep": false,
  "stages": {
   "arrival": "Choose a place where making a quiet sound feels comfortable.",
   "instruction": "On an easy out-breath, try a gentle mmm. Stop the sound before you need more air. Inhale whenever you need to.",
   "practice": "Listen after the hum while breathing normally. Repeat only if comfortable; otherwise simply listen to the room.",
   "closing": "Let the sound go. Notice whether you want to rest or return to your next action."
  }
 },
 {
  "id": "breath_word",
  "title": "One breath, one word",
  "allowed": [
   60,
   180,
   300
  ],
  "defaultSeconds": 180,
  "source": "Modern adaptation · VBT verse 156-A",
  "sleep": false,
  "stages": {
   "arrival": "Settle into an easy position and let your breath find its own rhythm.",
   "instruction": "As a breath comes in, silently think in. As it goes out, think out. Let the word follow the breath.",
   "practice": "If the words become distracting, drop them and notice ordinary breathing. There is no count to finish.",
   "closing": "Release the words. Choose what you want to give your attention to next."
  }
 },
 {
  "id": "emotion_space",
  "title": "Make room for the feeling",
  "allowed": [
   60,
   180
  ],
  "defaultSeconds": 180,
  "source": "Modern adaptation · VBT verse 101",
  "sleep": false,
  "stages": {
   "arrival": "Pause somewhere safe. Notice the support of your chair or the floor.",
   "instruction": "Name the feeling quietly if that helps. You do not have to make it disappear or act on it right now.",
   "practice": "Let your breathing take care of itself. Stay with the support beneath you; you can look around or leave at any time.",
   "closing": "The feeling may still be here. Choose one next action that matters to you."
  }
 },
 {
  "id": "sound_listen",
  "title": "Follow a fading sound",
  "allowed": [
   60,
   180
  ],
  "defaultSeconds": 180,
  "source": "Modern adaptation · VBT verse 41",
  "sleep": false,
  "stages": {
   "arrival": "Set the sound to a comfortable low volume, or choose Notice the room.",
   "instruction": "When the soft tone plays, listen as it becomes quieter. Your breathing can continue normally.",
   "practice": "When it fades, notice the sounds already around you. There is nothing you need to hear.",
   "closing": "Let listening widen to the room. End when you are ready."
  }
 },
 {
  "id": "sleep_threshold",
  "title": "Let the day end",
  "allowed": [
   60,
   180,
   300
  ],
  "defaultSeconds": 180,
  "source": "Modern adaptation · VBT verse 75",
  "sleep": true,
  "stages": {
   "arrival": "Use this when you are choosing sleep, somewhere you can rest safely.",
   "instruction": "Let the bed or chair support you. You can put the screen down. Breathing needs no special rhythm.",
   "practice": "If you notice yourself drifting, there is nothing to hold onto. You may fall asleep or simply rest. Stop trying if this makes you more alert.",
   "closing": ""
  }
 },
 {
  "id": "external_grounding",
  "title": "Notice the room",
  "allowed": [
   60,
   180
  ],
  "defaultSeconds": 180,
  "source": null,
  "sleep": false,
  "stages": {
   "arrival": "Settle somewhere safe. No breathing exercise is needed.",
   "instruction": "Notice one ordinary thing around you, or the support beneath you. Choose whichever feels easiest.",
   "practice": "Let your attention return to that simple detail when you want to. You can change the detail or stop.",
   "closing": "Choose your next action, or finish here."
  }
 }
];
var COPY = {
  internalNotice: "Internal test. The body-scan script is a draft and has not been reviewed.",
  globalGuidance: "Keep breathing comfortable. Stop if you feel dizzy, short of breath or more distressed. You can choose Notice the room instead.",
  safeSetting: "Please don't use this while driving, in water, or doing anything that needs your attention.",
  safeChip: "I'm somewhere safe and still",
  entryTitle: "Your first sitting",
  entryBody: "A six-step body-scan sitting, about five minutes: three minutes of attention, then one line about where you want to reach.",
  stop: { title: "Stop.", lines: ["Find a safe place for three minutes.", "You can end this at any time."], primary: "Begin", endLabel: "Not now" },
  settle: { title: "Settle.", lines: ["Close your eyes if that's comfortable.", "Notice how your body is held up."] },
  scan: { title: "Scan, top to bottom.", lines: ["Head, face, jaw, neck, shoulders, chest, stomach, hands, legs, feet.", "Notice where there's sensation, and where there's none."] },
  stay: { title: "Stay with one spot.", lines: ["Pick the strongest sensation.", "No need to fix or explain it. Let it be there."] },
  stayOptional: "Optional: on the exhale, you may quietly say \"I allow\". Or leave this out.",
  askTitle: "Where do you want to reach?",
  askHelp: "Your own words. One line, up to 60 characters.",
  lineHint: "your words",
  linePrivacy: "It stays in this browser on this device. Nobody scores it. Edit or clear it whenever you like.",
  closeTitle: "The time has passed.",
  stoppedTitle: "You stopped there.",
  interruptedTitle: "That was interrupted.",
  endedHelp: "However that felt, it's information. Nothing to fix.",
  readyLead: "Where you want to reach:"
};
var api = { PRACTICES: PRACTICES, COPY: COPY, SITTING_ID: "body_scan_sitting", SITTING_SECONDS: 180, SCAN_AT: 30, STAY_AT: 120 };
if (typeof module !== "undefined" && module.exports) module.exports = api; else root.SHContent = api;
})(typeof self !== "undefined" ? self : this);
