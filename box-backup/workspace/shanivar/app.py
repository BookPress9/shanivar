import sqlite3, time
from pathlib import Path
from flask import Flask, redirect, request

ROOT = Path("/workspace/shanivar")
DB = ROOT / "notices.db"
app = Flask(__name__)

NOTICES = [
    ("scrolling", "I am scrolling"),
    ("avoiding", "I am avoiding something"),
    ("reacting", "I am reacting"),
    ("anxious", "I am anxious"),
    ("stimulation", "I am seeking stimulation"),
    ("bored", "I am bored"),
    ("escaping", "I am trying to escape this feeling"),
]
LABEL = dict(NOTICES)

def db():
    con = sqlite3.connect(DB)
    con.row_factory = sqlite3.Row
    return con

def init():
    con = db()
    con.execute("""CREATE TABLE IF NOT EXISTS notices (
        id INTEGER PRIMARY KEY,
        kind TEXT,
        note TEXT,
        created_at TEXT
    )""")
    con.commit(); con.close()

PAGE = """<!doctype html><html lang="en"><head>
<meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Shanivar</title>
<style>
:root { --ink:#1c1915; --muted:#6d675e; --line:#e4ddd2; --paper:#f6f1e8; --card:#fffdf8; }
* { box-sizing:border-box }
body { margin:0; background:var(--paper); color:var(--ink); font:18px/1.45 Georgia, "Iowan Old Style", serif; }
main { max-width:560px; margin:0 auto; padding:48px 22px 80px; }
.mark { font:12px/1 ui-sans-serif, system-ui, sans-serif; letter-spacing:.18em; text-transform:uppercase; color:var(--muted); }
h1 { font-weight:500; font-size:34px; line-height:1.15; margin:18px 0 8px; }
p { color:var(--muted); margin:0 0 22px; }
form.choices { display:flex; flex-direction:column; gap:8px; }
button, a.back { font:16px/1.3 ui-sans-serif, system-ui, sans-serif; }
button.choice { text-align:left; background:var(--card); color:var(--ink); border:1px solid var(--line); border-radius:14px; padding:14px 16px; cursor:pointer; }
button.choice:hover { border-color:#c9bfb0; }
.quiet { margin-top:28px; font:14px/1.4 ui-sans-serif, system-ui, sans-serif; color:var(--muted); }
.quiet a { color:inherit; }
.noticed { font-size:28px; margin:24px 0 12px; }
textarea { width:100%; min-height:90px; border:1px solid var(--line); border-radius:14px; padding:12px; font:16px/1.4 ui-sans-serif, system-ui, sans-serif; background:var(--card); color:var(--ink); }
button.save { margin-top:10px; background:var(--ink); color:var(--paper); border:0; border-radius:999px; padding:12px 18px; cursor:pointer; }
ul { list-style:none; padding:0; margin:18px 0 0; }
li { border-top:1px solid var(--line); padding:14px 0; }
li b { font-weight:500; display:block; }
li span { font:13px/1.4 ui-sans-serif, system-ui, sans-serif; color:var(--muted); }
</style></head><body><main>
<div class="mark">Shanivar</div>
BODY
</main></body></html>"""

def render(body):
    return PAGE.replace("BODY", body)

@app.get("/")
def home():
    buttons = "".join(
        f'<button class="choice" name="kind" value="{k}">{label}</button>' for k, label in NOTICES
    )
    return render(f"""
    <h1>What is happening right now?</h1>
    <p>Name it before the next thing you do. Nothing here scores you.</p>
    <form class="choices" method="post" action="/notice">{buttons}</form>
    <p class="quiet"><a href="/log">What you have noticed</a></p>
    """)

@app.post("/notice")
def notice():
    kind = request.form.get("kind") or ""
    if kind not in LABEL:
        return redirect("/")
    return render(f"""
    <p class="noticed">You noticed.</p>
    <p>{LABEL[kind]}.</p>
    <form method="post" action="/save">
      <input type="hidden" name="kind" value="{kind}">
      <textarea name="note" placeholder="Optional. What were you about to do?"></textarea>
      <button class="save">Leave it here</button>
    </form>
    <p class="quiet"><a href="/">Not this</a></p>
    """)

@app.post("/save")
def save():
    kind = request.form.get("kind") or ""
    if kind not in LABEL:
        return redirect("/")
    note = (request.form.get("note") or "").strip()[:500]
    con = db()
    con.execute("INSERT INTO notices (kind, note, created_at) VALUES (?,?,?)",
                (kind, note, time.strftime("%Y-%m-%d %H:%M")))
    con.commit(); con.close()
    return redirect("/log")

@app.get("/log")
def log():
    con = db()
    rows = con.execute("SELECT * FROM notices ORDER BY id DESC LIMIT 40").fetchall()
    con.close()
    if not rows:
        items = "<p>Nothing named yet.</p>"
    else:
        items = "<ul>" + "".join(
            f"<li><b>{LABEL.get(r['kind'], r['kind'])}</b>"
            + (f"<span>{(r['note'] or '').replace('&','&amp;').replace('<','&lt;')}</span>" if r['note'] else "")
            + f"<span>{r['created_at']}</span></li>"
            for r in rows
        ) + "</ul>"
    return render(f"""
    <h1>What you named</h1>
    <p>A record, not a score.</p>
    {items}
    <p class="quiet"><a href="/">Notice again</a></p>
    """)

init()
if __name__ == "__main__":
    app.run(host="127.0.0.1", port=8801, debug=False)
