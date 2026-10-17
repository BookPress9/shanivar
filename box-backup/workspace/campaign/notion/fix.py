import re,glob
def rep(f,pairs):
    s=open(f).read()
    for a,b in pairs:
        n=s.count(a)
        assert n>=1,(f,a[:60])
        s=s.replace(a,b)
    open(f,'w').write(s)
# label internal everywhere
for f in glob.glob('0[7-9]*.md')+['10-reels-10-12.md']:
    rep(f,[("**Source and interpretation:**","**Source and interpretation (INTERNAL: not for public copy):**")])
# R03
rep('07-reels-01-03.md',[
("- 20–30: *30 seconds before you reply. Phone down. Watch what the anger wants to type.*",
 "- 20–30: *Dwell on it and it grows: wanting, then anger. 30 seconds before you reply. Phone down. Watch what the anger wants to type.*"),
("> Tradition ties Shani to consequences. Not punishment: consequences.\n> The one you control is the next message.",
 "> Tradition ties Shani to consequences. Not punishment: consequences.\n> Dwell on a thing and it grows: into wanting, then into anger. The pause is where the chain breaks.\n> The consequence you control is the next message."),
("- Attributed ✔ (\"tradition ties…\", no fabricated quote).","- Attributed ✔ (\"tradition ties…\", no fabricated quote; the dwelling → anger chain is paraphrased in our voice, no text named)."),
])
# R05
rep('08-reels-04-06.md',[
("- **0:00–0:03 (A).** ON CAMERA: \"In the Vishnu Purana, Saturn's mother is a shadow.\"",
 "- **0:00–0:03 (A).** ON CAMERA: \"In the old story, Saturn's mother is a shadow.\""),
("- Respectful about the Purana: no smirk on the story itself;","- Respectful about the story: no smirk on the story itself;"),
("- 13–28: *Vishnu Purana III.2* · *Chhāyā = \"shadow\"* · *Our reading: the part of you not yet watched.*",
 "- 13–28: *An old story* · *Chhāyā = \"shadow\"* · *Our reading: the part of you not yet watched.*"),
("> In the Vishnu Purana (III.2, Wilson's translation), Saturn is born to Chhāyā, whose name means \"shadow\".\n>\n> Our reading, not scripture:",
 "> In the old story, Saturn is born to Chhāyā, whose name means \"shadow\".\n>\n> Our reading, not the story's:"),
("> #Shanivar #Saturn #Shani #VishnuPurana #Habits","> #Shanivar #Saturn #Shani #Shadow #Habits"),
("- **Carousel:** \"Four layers\" format for the story: text (VP III.2), tradition, psychology, our reading.",
 "- **Carousel:** \"Four layers\" format for the story: the old story, tradition, psychology, our reading (no text or translator named on slides)."),
("- Saturn link ✔ (a real Puranic passage).\n- Attributed ✔ (VP III.2, Wilson; our reading labelled).",
 "- Saturn link ✔ (a real traditional story).\n- Attributed ✔ internally (VP III.2, Wilson); public copy says \"the old story\" and labels our reading."),
])
# R06
rep('08-reels-04-06.md',[
("\"Here's what I found. The Gita line everyone quotes: 'Your business is with action alone; not by any means with fruit.' That's about doing, not getting. Tradition links Shani to karma,",
 "\"Here's what I found. The old advice is simple: the doing is yours, the result isn't. It's about doing, not getting. Tradition links Shani to karma,"),
("- Deliver the Gita line plainly, without a \"spiritual voice\".","- Deliver \"the doing is yours, the result isn't\" plainly, without a \"spiritual voice\". Never present it as a quotation."),
("- 13–27: *Gita 2.47: \"Your business is with action alone; not by any means with fruit.\" (tr. Telang)* · *karma = action*",
 "- 13–27: *The doing is yours. The result isn't.* · *karma = action*"),
("> Gita 2.47 (Telang's translation): \"Your business is with action alone; not by any means with fruit.\"\n> Karma means action.",
 "> The old advice: the doing is yours, the result isn't.\n> Karma means action."),
("- Attributed ✔ (Gita 2.47, Telang; reading labelled).","- Attributed ✔ internally (Gita 2.47, Telang); public copy paraphrases in our voice and names no text; reading labelled."),
])
# R07
rep('09-reels-07-09.md',[
("\"The Yoga Sutra says practice gets grounded over a long time, without interruption. A habit study I read says one missed day doesn't seriously set you back. I'm keeping both. Long time. And come back.\"",
 "\"Practice takes root slowly, over a long time. Separately, habit researchers found that missing the occasional day didn't seriously set people back. That's a study about habits, not about my app. I'm keeping both. Long time. And come back.\""),
("- 13–25: *Yoga Sutra 1.14: practice grounded over a long time.* / *One missed day doesn't undo a habit (Gardner, Lally & Wardle 2012).*",
 "- 13–25: *Practice takes root slowly.* / *Habit research, not a Shanivar result: an occasional missed day didn't seriously set habit-building back (Gardner, Lally & Wardle 2012).*"),
("> Yoga Sutra 1.14 says practice becomes grounded over a long time, without interruption. A habit study says one missed day doesn't seriously set you back. I'm keeping both.",
 "> Practice takes root slowly, over a long time.\n> Separately, a habit study (Gardner, Lally & Wardle 2012; nothing to do with Shanivar) found that missing the occasional day didn't seriously set habit-building back. I'm keeping both."),
("- Attributed ✔ (YS 1.14 BonGiovanni; Gardner et al.).",
 "- Attributed ✔ internally (YS 1.14 BonGiovanni); public copy paraphrases in our voice and names no text. The Gardner line is framed as third-party habit research, never as a Shanivar outcome; it is kept apart from the product line at 0:35."),
])
# R09
rep('09-reels-07-09.md',[
("So fine. Not a scam. Just hard.\"","So fine. Not a scam. Just hard. A restless mind is normal. It steadies with practice, slowly.\""),
("- 14–24: *Wilson et al., Science 2014: many chose a shock over 6–15 min of just thinking.*",
 "- 14–24: *Wilson et al., Science 2014: many chose a shock over 6–15 min of just thinking.* / *A restless mind steadies with practice. Slowly.*"),
("- **0:32–0:35 (E).** VO (founder): \"Shanivar starts with one deliberate minute.\"",
 "- **0:32–0:35 (E).** VO (founder): \"Shanivar is being built to begin with one deliberate minute.\""),
("> Not a scam. Just hard. Try 60 seconds and comment your number.\n>\n> Shanivar starts with one deliberate minute. In development for Android.",
 "> Not a scam. Just hard. A restless mind is normal; it steadies with practice, slowly.\n> Try 60 seconds and comment your number.\n>\n> Shanivar is being built to begin with one deliberate minute. In development for Android."),
("- Saturn link ✔ (light; Gita 6.35 in the caption pack).",
 "- Saturn link ✔ (light). The Gita 6.35 idea appears only as our paraphrase (\"a restless mind steadies with practice, slowly\"); no text named."),
])
# R10
rep('10-reels-10-12.md',[
("VO over the test build: \"You choose one boundary. 'Make room for one uninterrupted hour.' You pick the apps that can wait.",
 "VO over the test build: \"You choose one protected hour. You pick the apps that step back during it."),
("   - the \"Your first boundary\" screen (\"Make room for one uninterrupted hour.\"),",
 "   - the \"Your first boundary\" screen, framed on the time picker (do not hold on or caption the line \"Make room for one uninterrupted hour.\"; it promises more than the app controls),"),
])
# R11
rep('10-reels-10-12.md',[
("- **Shanivar reading:** you choose what reaches you, and when.",
 "- **Shanivar reading:** you choose which apps can wait, and when. (Not \"what reaches you\": Shanivar does not control notifications.)"),
("Shanivar is built on one idea from it: you choose what reaches you, and when.\"",
 "Shanivar is being built on one idea from it: you choose which apps can wait, and when.\""),
("- Insight: you choose what reaches you.","- Insight: you choose which apps can wait."),
("**Active-passive moment:** A notification storm, then a phone vibrating on steel that nobody touches.",
 "**Active-passive moment:** A notification storm, then a phone vibrating on steel that nobody touches. (The person chooses not to touch it; never imply Shanivar silenced or filtered the notifications.)"),
])
