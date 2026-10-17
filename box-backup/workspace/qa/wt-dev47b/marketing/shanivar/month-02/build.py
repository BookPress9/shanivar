"""Build review artifacts from campaign.json. Does not connect to Instagram or publish.

Run: python marketing/shanivar/month-02/build.py
Only derived editorial exports are overwritten; entered metrics remain untouched.
"""
import csv
import html
import json
from datetime import date, datetime, time, timedelta
from pathlib import Path
from zoneinfo import ZoneInfo

ROOT = Path(__file__).resolve().parent
DATA = json.loads((ROOT / 'campaign.json').read_text(encoding='utf-8'))
POSTS = {p['id']: p for p in DATA['posts']}
START = date.fromisoformat(DATA['start_date'])
assert len(POSTS) == len(DATA['posts']) == 12
assert len(DATA['stories']) == DATA['days'] == 30
assert len(DATA['slots']) == 16
assert all(1 <= s['day'] <= DATA['days'] for s in DATA['slots'])
assert len({s['day'] for s in DATA['slots']}) == 16
assert len({(s['post'], s['language']) for s in DATA['slots']}) == 16
for post in POSTS.values():
    for lang in ('en', 'hi'):
        edition = post[lang]
        assert all(edition.get(key) for key in ('title', 'slides', 'caption', 'cta', 'alt_scene'))
        assert len(edition['slides']) == (5 if post['format'] == 'Reel' else 6)
        assert not any('�' in text for text in [*edition['slides'], edition['caption']])

def write_csv(path, rows, fields):
    with path.open('w', encoding='utf-8-sig', newline='') as out:
        writer = csv.DictWriter(out, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)

def stamp(day, clock):
    return datetime.combine(day, time.fromisoformat(clock), ZoneInfo(DATA['timezone']))

def safe(value):
    return html.escape(str(value))

def para(value):
    return '<p>' + safe(value).replace('\n\n', '</p><p>').replace('\n', '<br>') + '</p>'

rows, feed, manifest, cards = [], [], [], []
calendar = [
    '# Shanivar — 30 days after the introduction', '',
    '**14 September–13 October 2026 · proposed dates · Asia/Kolkata.**', '',
    'This new draft supersedes the unposted week-one schedule only; the founder-reported first post is retained as BASE-01. Start date is a planning assumption, not a confirmed publishing date. Do not publish both calendars.', '',
    '16 proposed feed slots: 12 original concepts plus four companion language editions; 10 carousel publications and six Reel publications. Eight English and eight Hindi editions. Stories are optional, one frame a day in the selected language, not both languages squeezed onto one image.', '',
    'Feed: 19:30 IST / 16:00 Berlin. Stories: 12:30 IST / 09:00 Berlin. These are consistent planning slots, not researched peak times. Current dates are entirely before Berlin’s autumn clock change; exports use named timezones.', '',
    'Use [production-copy.md](production-copy.md) for every caption, slide, timed Reel scene, visual brief and accessibility draft. Use [operations.md](operations.md) for review decisions, publication gates and metrics definitions.', '',
    '| Day | Date | Feed at 19:30 IST | Story at 12:30 IST |',
    '| --- | --- | --- | --- |'
]
by_day = {s['day']: s for s in DATA['slots']}
for number in range(1, DATA['days'] + 1):
    day = START + timedelta(days=number - 1)
    slot = by_day.get(number)
    story_en, story_hi = DATA['stories'][number - 1]
    week = min((number - 1) // 7 + 1, 5)
    identifier, title, fmt, lang = '', 'Story / editorial review', '', ''
    if slot:
        post = POSTS[slot['post']]
        lang = slot['language']
        edition = post[lang]
        identifier = 'M02-' + post['id'] + '-' + lang
        title, fmt = edition['title'], post['format']
        feed.append(dict(content_id=identifier, concept_id=post['id'], channel='Instagram',
                         proposed_at=stamp(day, DATA['feed_time']).isoformat(),
                         berlin_at=stamp(day, DATA['feed_time']).astimezone(ZoneInfo('Europe/Berlin')).isoformat(),
                         format=fmt, language=lang, title=title, caption=edition['caption'],
                         cta=edition['cta'], audience=post['audience'], objective=post['objective'],
                         source=post['source'], pair_id=slot['pair'], status='Draft',
                         approval_reference='', media_url='', platform_post_id='',
                         slides_json=json.dumps(edition['slides'], ensure_ascii=False)))
        manifest.append(dict(content_id=identifier, concept_id=post['id'], language=lang,
                             format=fmt, width=1080, height=1920 if fmt == 'Reel' else 1350,
                             duration_seconds=27 if fmt == 'Reel' else None,
                             slide_count=None if fmt == 'Reel' else 6,
                             filename=None, public_url=None, renderer=None, model=None,
                             prompt_version=None, source_assets=[], rights_review='Pending',
                             disclosure_review='Pending', typography_review='Pending',
                             hindi_review='Pending' if lang == 'hi' else 'Not applicable',
                             visual_review='Pending', approval_reference='', status='Not produced'))
    rows.append(dict(day=number, date=day.isoformat(), week=week, content_id=identifier,
                     format=fmt, language=lang, title=title,
                     feed_time_ist=DATA['feed_time'] if slot else '',
                     story_time_ist=DATA['story_time'], story_en=story_en, story_hi=story_hi,
                     status='Draft', approval_reference=''))
    feed_label = f'{identifier} · {fmt} · {title}' if slot else 'No feed post'
    calendar.append(f'| {number} | {day.strftime("%a %d %b")} | {feed_label} | {story_en.replace("|", "/")} |')
    body = ''
    if slot:
        body += f'<p class="tag">{safe(identifier)} · {safe(fmt)} · {safe(lang.upper())} · 19:30 IST</p><h3>{safe(title)}</h3>'
        body += '<details><summary>Caption and production copy</summary>'
        for version in ('en', 'hi'):
            ed = post[version]
            body += f'<section lang="{version}"><h4>{version.upper()} edition</h4>' + para(ed['caption'])
            body += '<ol>' + ''.join('<li>' + safe(s) + '</li>' for s in ed['slides']) + '</ol></section>'
        body += '<p><b>Visual:</b> ' + safe(post['visual']) + '</p><p><b>Source:</b> ' + safe(post['source']) + '</p></details>'
    else:
        body += '<h3>A little room between posts</h3><p>No feed publication proposed today.</p>'
    body += '<details><summary>Optional Story · 12:30 IST</summary>' + para(story_en) + '<div lang="hi">' + para(story_hi) + '</div></details>'
    cards.append(f'<article data-week="{week}" data-kind="{safe(fmt or "Story")}" data-lang="{lang}"><p class="tag">DAY {number:02} · {day.strftime("%a %d %b")}</p>{body}<p class="status">DRAFT · media not produced · not scheduled</p></article>')

calendar += ['', '## Daily Stories — exact language editions', '']
for row in rows:
    calendar += [f'### Day {row["day"]} · {row["date"]}', '', '**EN:** ' + row['story_en'], '', '**HI:** ' + row['story_hi'], '']
calendar += ['## Planned editorial reviews', '',
             '- 20 Sep: inspect first-post intake and early snapshots; no language winner yet.',
             '- 27 Sep: first mature seven-day comparisons; hold if evidence is missing.',
             '- 4 Oct: review the first two language pairs after equal exposure.',
             '- 11 Oct: review weeks 1–3; week 4 is still maturing.',
             '- 13 Oct: month editorial review using only mature snapshots.',
             '- 17 Oct: last planned feed post reaches seven days, if published on time.',
             '- 7 Nov: last planned feed post reaches 28 days, if published on time.', '']
(ROOT / 'calendar.md').write_text('\n'.join(calendar), encoding='utf-8')
write_csv(ROOT / 'calendar.csv', rows, list(rows[0]))
write_csv(ROOT / 'publishing-drafts.csv', feed, list(feed[0]))
(ROOT / 'asset-manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

copy = ['# Shanivar — complete bilingual production copy', '',
        'Copy drafts and production briefs. No new campaign images or videos have been rendered. The HTML preview is an editorial calendar, not final Instagram artwork.', '',
        'Only the language editions in calendar.csv are proposed for publication. Other editions are alternatives, not extra scheduled posts. Do not publish both caption languages on every post.', '',
        'House visual system: charcoal #171918, bone #F1EADD, bronze #AD8865. Human detail leads; orbital marks are quiet decoration. Carousels 1080×1350; Reels/Stories 1080×1920. At master size, start with headings around 72 px and body at least 38 px, then check real phone crops and Hindi line breaks. Give every Reel a separately composed cover using its title. Do not promise platform acceptance from these house dimensions.', '',
        'Reel master: five shots at 0–4, 4–9, 9–15, 15–21 and 21–27 seconds. Use original footage and room tone by default. Optional voiceover reads the exact screen text in the edition language. Record and time the actual read; extend a shot if needed and log any duration difference in pair analysis. Essential meaning remains readable with sound off. Do not add forced looping or accelerated captions.', '',
        'Carousel alt text below combines a proposed scene description and exact slide copy. It is provisional: replace descriptions after rendering so each matches that actual slide. Reel visual summaries supplement visible captions; do not assume Instagram provides a Reel alt-text field.', '']
times = ['0–4s', '4–9s', '9–15s', '15–21s', '21–27s']
for p in POSTS.values():
    copy += [f'## {p["id"]} · {p["format"]}', '', '**Audience:** ' + p['audience'], '',
             '**Objective:** ' + p['objective'], '', '**Visual brief:** ' + p['visual'], '',
             '**Source / claim boundary:** ' + p['source'], '']
    for lang in ('en', 'hi'):
        ed = p[lang]
        copy += [f'### {lang.upper()} · {ed["title"]}', '', '**Caption — paste only after review:**', '', ed['caption'], '', '**Slide / screen copy:**', '']
        for i, text in enumerate(ed['slides']):
            label = times[i] if p['format'] == 'Reel' else f'Slide {i+1}'
            copy.append(f'{i+1}. **{label}:** {text}')
        copy += ['', '**CTA:** ' + ed['cta'], '', '**Accessibility draft:**', '']
        if p['format'] == 'Carousel':
            for i, text in enumerate(ed['slides']):
                copy.append(f'{i+1}. {ed["alt_scene"]} Text: “{text}”')
        else:
            copy += [ed['alt_scene'], 'All spoken words must also appear as readable captions.']
        copy += ['']
(ROOT / 'production-copy.md').write_text('\n'.join(copy), encoding='utf-8')

metrics_path = ROOT / 'metrics.csv'
if not metrics_path.exists():
    fields = ['content_id','post_url','publication_source','published_at','snapshot_at',
              'window_hours','format','language','distribution','reach','views','likes',
              'saves','shares','comments','profile_visits','follows','non_follower_reach',
              'average_watch_seconds','duration_seconds','link_clicks','signups',
              'activated_users','metric_source','notes']
    entries = []
    for item in [dict(content_id='BASE-01', format='', language=''), *feed]:
        for hours in (24,168,672):
            row = dict.fromkeys(fields, '')
            row.update(content_id=item['content_id'], window_hours=hours,
                       format=item['format'], language=item['language'])
            if item['content_id'] == 'BASE-01':
                row.update(post_url=DATA['baseline']['url'], publication_source='Founder report',
                           notes='Published date, creative and Insights unavailable; leave metrics blank.')
            entries.append(row)
    write_csv(metrics_path, entries, fields)

page = '''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Shanivar · 30-day editorial calendar</title>
<style>:root{color-scheme:dark}*{box-sizing:border-box}body{margin:0;background:#171918;color:#f1eadd;font:17px/1.65 system-ui,"Nirmala UI",sans-serif}main{max-width:1150px;margin:auto;padding:48px 24px}h1{font:clamp(38px,7vw,76px)/1.08 Georgia,serif;max-width:900px}h2,h3{line-height:1.3}h3{font-size:25px}.tag{color:#d8b89a;letter-spacing:.08em;font-size:12px}.intro{max-width:750px;color:#d9d1c5}a{color:#e7bd95}nav{display:flex;gap:12px;flex-wrap:wrap;margin:24px 0}label{display:block}select{font:inherit;background:#262923;color:#f1eadd;padding:8px;border:1px solid #ad8865;border-radius:6px}#grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,320px),1fr));gap:20px}article{border:1px solid #544a40;padding:24px;border-radius:12px;background:#1d201c}article[hidden]{display:none}summary{cursor:pointer;color:#e7bd95;padding:12px 0}details{border-top:1px solid #47433c;margin-top:12px}li{margin:12px 0}.status{font-size:12px;color:#ccc4b8;margin-top:22px}section{border-bottom:1px solid #47433c}a:focus-visible,summary:focus-visible,select:focus-visible{outline:3px solid #f1eadd;outline-offset:4px}footer{padding-top:36px;color:#d9d1c5}@media print{nav,select{display:none}article{break-inside:avoid}body{background:white;color:black}}</style>
<main><p class="tag">SHANIVAR / BY THE HYBRID MONK · EDITORIAL WORKSPACE</p><h1>A little more of your life, returned.</h1><p lang="hi">अपनी ज़िंदगी में, थोड़ा और लौटना</p>
<div class="intro"><p>14 September–13 October 2026. Sixteen proposed feed slots and thirty optional Stories. English and Hindi copies, one connected voice.</p><p><b>Draft calendar.</b> Final artwork and videos are not produced. No publishing or scheduling has occurred. The founder supplied the first-post URL; its creative and Insights could not be fetched.</p></div>
<nav aria-label="Campaign files"><a href="calendar.md">Calendar</a><a href="production-copy.md">Complete production copy</a><a href="calendar.csv">Calendar CSV</a><a href="publishing-drafts.csv">Publishing drafts CSV</a><a href="first-post-analysis.md">First-post intake</a><a href="operations.md">Operations</a><a href="metrics.csv">Metrics sheet</a></nav>
<nav aria-label="Calendar filters"><label>Week <select id="week"><option value="">All weeks</option><option value="1">Week 1</option><option value="2">Week 2</option><option value="3">Week 3</option><option value="4">Week 4</option><option value="5">Wrap-up</option></select></label><label>Format <select id="kind"><option value="">All days</option><option>Carousel</option><option>Reel</option><option>Story</option></select></label><label>Feed language <select id="lang"><option value="">Both / Story days</option><option value="en">English</option><option value="hi">Hindi</option></select></label></nav><p id="count" aria-live="polite"></p><div id="grid">'''
page += ''.join(cards) + '''</div><footer>Feed 19:30 IST · Stories 12:30 IST. These are test slots, not peak-time claims. Read the operations brief before release. Review performance only from actual, dated Insights.</footer></main><script>const selects=['week','kind','lang'].map(id=>document.getElementById(id));function filter(){let n=0;document.querySelectorAll('article').forEach(card=>{card.hidden=selects.some(s=>s.value&&card.dataset[s.id]!==s.value);if(!card.hidden)n++});document.getElementById('count').textContent=n+' days shown'}selects.forEach(s=>s.addEventListener('change',filter));filter();</script></html>'''
(ROOT / 'index.html').write_text(page, encoding='utf-8')
print('Validated and generated: 30 calendar days, 16 feed drafts, 12 bilingual concepts, 16 pending asset entries.')
print('Metrics intake preserved if it already existed. No network calls or publication.')
