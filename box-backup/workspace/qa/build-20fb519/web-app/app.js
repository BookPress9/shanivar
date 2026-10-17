(function () {
"use strict";
var C = SHContent, L = SHLogic, COPY = C.COPY;
var store;
try { store = new L.Store(window.localStorage); } catch (e) { store = new L.Store(null); }
var main = document.getElementById("main"), live = document.getElementById("live");
document.getElementById("notice").textContent = COPY.internalNotice;
document.getElementById("storage-warning").hidden = store.available();

var S = null; /* running session */
var timerId = null;

function el(tag, attrs, kids) {
  var n = document.createElement(tag);
  Object.keys(attrs || {}).forEach(function (k) {
    if (k === "text") n.textContent = attrs[k]; else if (k === "onclick") n.addEventListener("click", attrs[k]);
    else if (k === "oninput") n.addEventListener("input", attrs[k]); else if (attrs[k] !== false && attrs[k] != null) n.setAttribute(k, attrs[k] === true ? "" : attrs[k]);
  });
  (kids || []).forEach(function (c) { n.appendChild(typeof c === "string" ? document.createTextNode(c) : c); });
  return n;
}
function p(text, cls) { return el("p", { text: text, class: cls || false }); }
function h1(text) { return el("h1", { id: "view-title", text: text }); }
function btn(label, fn, primary, disabled) { return el("button", { type: "button", text: label, onclick: fn, class: primary ? "primary" : false, disabled: disabled ? true : false }); }
function show(nodes, announce) {
  main.replaceChildren.apply(main, nodes);
  if (announce) { live.textContent = ""; live.textContent = announce; }
}
function focusMain() { main.focus({ preventScroll: true }); }
function practiceById(id) { return C.PRACTICES.filter(function (x) { return x.id === id; })[0]; }
function minLabel(s) { return s / 60 + " min"; }

/* ---------- Home ---------- */
function home() {
  stopTimer(); S = null;
  var now = Date.now(), hist = store.history();
  var nodes = [h1("Sadhana"),
    p("Two ways in. Nothing here is counted or scored.", "muted"),
    el("section", { class: "card", "aria-labelledby": "first-h" }, [
      el("h2", { id: "first-h", text: COPY.entryTitle }), p(COPY.entryBody),
      lastLine(L.lastPractisedLabel(L.lastPractisedMs(hist, C.SITTING_ID), now)),
      btn("Start your first sitting", function () { sittingStop(); }, true)]),
    el("section", { class: "card", "aria-labelledby": "steady-h" }, [
      el("h2", { id: "steady-h", text: "Steady practices" }),
      p("Eight unpaced practices. None of them guides your breath. Choose one, then 1, 3 or 5 minutes where offered (3 is the default).", "muted"),
      el("ul", { class: "list" }, C.PRACTICES.map(function (pr) {
        var li = el("li", {}, [el("h3", { text: pr.title })]);
        if (pr.source) li.appendChild(p(pr.source, "src"));
        li.appendChild(lastLine(L.lastPractisedLabel(L.lastPractisedMs(hist, pr.id), now)));
        li.appendChild(btn("Choose " + pr.title, function () { prepare(pr); }));
        return li;
      }))]),
    historyBlock(hist)];
  show(nodes, "Sadhana home"); focusMain();
}
function lastLine(t) { return t ? p(t, "last") : document.createTextNode(""); }
function outcomeText(o) { return o === "timer" ? "Timer finished" : o === "user" ? "Ended by you" : "Interrupted"; }
function historyBlock(hist) {
  var d = el("details", { class: "card" }, [el("summary", { text: "History on this device" })]);
  if (!hist.length) { d.appendChild(p("Nothing yet.", "muted")); return d; }
  var ul = el("ul", { class: "list" });
  hist.slice().reverse().slice(0, 20).forEach(function (r) {
    var pr = practiceById(r.practiceId), title = r.practiceId === C.SITTING_ID ? COPY.entryTitle : (pr ? pr.title : r.practiceId);
    var when = new Date(r.startedAt).toLocaleString("en-GB", { day: "numeric", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" });
    var s = r.activeSeconds, len = Math.floor(s / 60) + " min " + (s % 60) + " s";
    ul.appendChild(el("li", { text: when + " · " + title + " · " + len + " · " + outcomeText(r.outcome) + (L.countsAsPractised(r) ? "" : " · saved, not counted as practised (under 90 s or interrupted)") }));
  });
  d.appendChild(ul); return d;
}

/* ---------- Steady practice: prepare ---------- */
function safeChip(onChange) {
  var cb = el("input", { type: "checkbox", id: "safe" });
  cb.addEventListener("change", function () { onChange(cb.checked); });
  return el("label", { class: "opt", for: "safe" }, [cb, COPY.safeChip]);
}
function prepare(pr) {
  var chosen = pr.defaultSeconds;
  var start = btn("Begin", function () { startSession(pr, chosen, false); }, true, true);
  var group = el("fieldset", {}, [el("legend", { text: "How long?" })]);
  pr.allowed.forEach(function (sec) {
    var rb = el("input", { type: "radio", name: "dur", id: "d" + sec, value: sec });
    if (sec === pr.defaultSeconds) rb.checked = true;
    rb.addEventListener("change", function () { chosen = sec; });
    group.appendChild(el("label", { class: "opt", for: "d" + sec }, [rb, minLabel(sec)]));
  });
  var nodes = [h1(pr.title)];
  if (pr.source) nodes.push(p(pr.source, "src"));
  nodes.push(p(COPY.globalGuidance), p(COPY.safeSetting, "muted"), group);
  if (pr.allowed.indexOf(60) >= 0) nodes.push(p("A 1 minute session is saved but is under the 90 seconds needed to count as practised.", "fine"));
  if (pr.id === "sound_listen") nodes.push(p("This web version plays no tone. Choose Notice the room, or listen to the sounds already around you.", "fine"));
  nodes.push(safeChip(function (v) { start.disabled = !v; }), el("div", { class: "row" }, [start, btn("Back", home)]));
  show(nodes, pr.title + ". Choose a length."); focusMain();
}

/* ---------- Session engine ---------- */
function startSession(pr, seconds, sitting) {
  S = { pr: pr, total: seconds, sitting: sitting, active: 0, last: performance.now(), paused: false, hostPaused: false,
    startedAt: Date.now(), shown: null, manual: 0, done: false };
  tick(true); stopTimer(); timerId = setInterval(function () { tick(false); }, 250);
}
function stopTimer() { if (timerId) { clearInterval(timerId); timerId = null; } }
function accrue() {
  var now = performance.now();
  if (!S.paused) S.active += (now - S.last) / 1000;
  S.last = now;
}
function currentKey() {
  var e = S.active;
  if (S.sitting) {
    var order = ["settle", "scan", "stay"], byClock = L.sittingStepAt(e);
    return order[Math.max(order.indexOf(byClock), S.manual)];
  }
  var ord = S.pr.sleep ? ["arrival", "instruction", "practice"] : ["arrival", "instruction", "practice", "closing"];
  var clock = L.stageAt(e, S.total, S.pr.sleep);
  return ord[Math.max(ord.indexOf(clock), S.manual)];
}
function tick(first) {
  if (!S || S.done) return;
  accrue();
  if (S.active >= S.total) { finish("timer"); return; }
  var key = currentKey();
  if (first || key !== S.shown) { S.shown = key; renderSession(true); }
  var t = document.getElementById("timer");
  if (t) t.textContent = L.formatClock(S.total - S.active);
}
function label(key) { return key === "arrival" ? "Arrive" : key === "closing" ? "Close" : "Practice"; }
function renderSession(announce) {
  var key = S.shown, nodes = [], title, lines = [];
  if (S.sitting) {
    var f = COPY[key]; title = f.title; lines = f.lines.slice();
  } else {
    title = label(key); lines = [S.pr.stages[key]];
  }
  nodes.push(h1(title));
  if (!S.sitting) nodes.push(p(S.pr.title, "src"));
  lines.forEach(function (l) { if (l) nodes.push(p(l)); });
  if (S.sitting && key === "stay") nodes.push(p(COPY.stayOptional, "muted"));
  nodes.push(el("p", { class: "timer", id: "timer", role: "timer", "aria-label": "Time left" , text: L.formatClock(S.total - S.active) }));
  if (S.hostPaused) nodes.push(p("Paused when this page went to the background. Nothing was missed.", "fine"));
  nodes.push(p(COPY.safeSetting, "fine"));
  nodes.push(el("div", { class: "row" }, [
    btn(S.paused ? "Resume" : "Pause", togglePause), btn("Skip", skip), btn("End", function () { finish("user"); })]));
  show(nodes, announce ? title + (lines[0] ? ". " + lines[0] : "") : null);
  if (announce) focusMain();
}
function togglePause() { accrue(); S.paused = !S.paused; S.hostPaused = false; renderSession(false); live.textContent = S.paused ? "Paused" : "Resumed"; }
function skip() {
  accrue();
  var maxIdx = S.sitting ? 2 : (S.pr.sleep ? 2 : 3);
  var idx = (S.sitting ? ["settle", "scan", "stay"] : (S.pr.sleep ? ["arrival", "instruction", "practice"] : ["arrival", "instruction", "practice", "closing"])).indexOf(S.shown);
  if (idx >= maxIdx) { finish("user", S.sitting); return; } /* last step: Skip ends the timer, as on Android */
  S.manual = idx + 1; S.shown = null; tick(true);
}
function finish(outcome, toAsk) {
  if (S.done) return;
  accrue(); S.done = true; stopTimer();
  var rec = { id: String(S.startedAt), practiceId: S.pr.id, startedAt: S.startedAt, activeSeconds: Math.floor(S.active),
    durationSeconds: S.total, outcome: outcome };
  store.addRecord(rec);
  if (S.sitting && (outcome === "timer" || toAsk)) sittingAsk(); else endScreen(outcome);
}
function endScreen(outcome) {
  lastEnd = function () { endScreen(outcome); };
  var title = outcome === "timer" ? COPY.closeTitle : outcome === "interrupted" ? COPY.interruptedTitle : COPY.stoppedTitle;
  var nodes = [h1(title), p(COPY.endedHelp)];
  addSankalpEcho(nodes);
  nodes.push(el("div", { class: "row" }, [btn("Done", home, true)]));
  show(nodes, title); focusMain();
}
function addSankalpEcho(nodes) {
  var line = store.sankalp();
  if (!line) return;
  nodes.push(p(COPY.readyLead, "muted"), el("p", { class: "echo", text: line }),
    el("div", { class: "row" }, [btn("Clear", function () { store.clearSankalp(); live.textContent = "Cleared"; rerenderEnd(); })]));
}
var lastEnd = null;
function rerenderEnd() { if (lastEnd) lastEnd(); }

/* Interruptions: page hidden pauses (nothing lost); page closing mid-session saves an interrupted row. */
document.addEventListener("visibilitychange", function () {
  if (!S || S.done) return;
  if (document.hidden && !S.paused) { accrue(); S.paused = true; S.hostPaused = true; }
});
document.addEventListener("visibilitychange", function () { if (S && !S.done && !document.hidden && S.hostPaused) { S.last = performance.now(); renderSession(false); } });
window.addEventListener("pagehide", function () {
  if (S && !S.done && S.active > 0) { accrue(); S.done = true; store.addRecord({ id: String(S.startedAt), practiceId: S.pr.id, startedAt: S.startedAt, activeSeconds: Math.floor(S.active), durationSeconds: S.total, outcome: "interrupted" }); }
});

/* ---------- First sitting ---------- */
function sittingStop() {
  var f = COPY.stop, begin = btn(f.primary, begin1, true, true), skipB = btn("Skip", begin1, false, true);
  function begin1() { startSession({ id: C.SITTING_ID, title: COPY.entryTitle, sleep: false }, C.SITTING_SECONDS, true); }
  var nodes = [h1(f.title)];
  f.lines.forEach(function (l) { nodes.push(p(l)); });
  nodes.push(p(COPY.safeSetting, "muted"), safeChip(function (v) { begin.disabled = !v; skipB.disabled = !v; }),
    el("div", { class: "row" }, [begin, skipB, btn(f.endLabel, home)]));
  show(nodes, f.title); focusMain();
}
function sittingAsk() {
  var input = el("input", { type: "text", id: "line", maxlength: "120", autocomplete: "off", value: store.sankalp() || "" });
  var count = el("p", { class: "fine", id: "count", "aria-live": "off" });
  var keep = btn("Keep", function () { if (store.saveSankalp(input.value)) sittingClose(); }, true);
  function refresh() {
    var v = L.limitInput(input.value); if (v !== input.value) input.value = v;
    count.textContent = L.codePoints(v).length + "/" + L.MAX_LINE; keep.disabled = !L.isAcceptable(v);
  }
  input.addEventListener("input", refresh);
  var nodes = [h1(COPY.askTitle), p(COPY.askHelp),
    el("label", { for: "line", text: COPY.lineHint, class: "fine" }), input, count, p(COPY.linePrivacy, "fine"),
    el("div", { class: "row" }, [keep,
      btn("Clear", function () { store.clearSankalp(); input.value = ""; refresh(); input.focus(); }),
      btn("Skip", sittingClose), btn("End", function () { endScreen("user"); })])];
  show(nodes, COPY.askTitle); refresh(); input.focus();
}
function sittingClose() {
  lastEnd = sittingClose;
  var nodes = [h1(COPY.closeTitle)]; addSankalpEcho(nodes);
  nodes.push(el("div", { class: "row" }, [btn("Done", home, true)]));
  show(nodes, COPY.closeTitle); focusMain();
}
/* ---------- Data ---------- */
function dataControls() {
  var box = document.getElementById("data-controls");
  function idle() {
    box.replaceChildren(btn("Delete all my data on this device", function () {
      box.replaceChildren(p("This removes your history and your Sankalp line from this browser. It cannot be undone."),
        el("div", { class: "row" }, [el("button", { type: "button", class: "danger", text: "Yes, delete everything", onclick: function () { store.deleteAll(); idle(); live.textContent = "All data deleted"; if (!S) home(); } }),
          btn("Cancel", idle)]));
    }));
  }
  idle();
}
dataControls();
home();
})();
