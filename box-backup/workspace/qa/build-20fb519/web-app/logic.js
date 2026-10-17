/* Pure logic, no DOM. Mirrors BreathingSessionState.kt (90 s rule), SankalpStore.kt, lastPractisedLabel. */
(function (root) {
"use strict";
var MIN_PRACTISED_SECONDS = 90, MAX_LINE = 60, MAX_RECORDS = 100;
var KEYS = { history: "shanivar.history.v1", sankalp: "shanivar.sankalp.v1" };
var MONTHS = ["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"];

function countsAsPractised(rec) {
  return !!rec && (rec.outcome === "timer" || rec.outcome === "user") && rec.activeSeconds >= MIN_PRACTISED_SECONDS;
}
function codePoints(s) { return Array.from(String(s)); }
function limitInput(t) {
  var single = String(t).replace(/[\r\n]/g, " ");
  var cp = codePoints(single);
  return cp.length <= MAX_LINE ? single : cp.slice(0, MAX_LINE).join("");
}
function isAcceptable(t) { return String(t).trim() !== "" && codePoints(t).length <= MAX_LINE; }

function dayNumber(ms) { var d = new Date(ms); return Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()) / 86400000; }
/* "Last practised today / yesterday / 3 Oct" (with year when not this year); null when nothing. */
function lastPractisedLabel(ms, nowMs) {
  if (ms == null) return null;
  var diff = dayNumber(nowMs) - dayNumber(ms);
  if (diff === 0) return "Last practised today";
  if (diff === 1) return "Last practised yesterday";
  var d = new Date(ms), n = new Date(nowMs);
  var s = d.getDate() + " " + MONTHS[d.getMonth()];
  if (d.getFullYear() !== n.getFullYear()) s += " " + d.getFullYear();
  return "Last practised " + s;
}
function lastPractisedMs(records, practiceId) {
  var best = null;
  records.forEach(function (r) {
    if (r.practiceId === practiceId && countsAsPractised(r) && typeof r.startedAt === "number" && (best === null || r.startedAt > best)) best = r.startedAt;
  });
  return best;
}
function trimHistory(list) { return list.length <= MAX_RECORDS ? list : list.slice(list.length - MAX_RECORDS); }

/* Which steady-practice stage the clock points at. Stage ids as in the catalogue. */
function stageAt(elapsed, total, sleep) {
  var closing = sleep ? 0 : 10, practiceEnd = total - closing;
  if (elapsed < 10) return "arrival";
  if (elapsed < 30) return "instruction";
  if (elapsed < practiceEnd || closing === 0) return "practice";
  return "closing";
}
function sittingStepAt(elapsed) { return elapsed >= 120 ? "stay" : elapsed >= 30 ? "scan" : "settle"; }
function formatClock(sec) { sec = Math.max(0, Math.ceil(sec)); return Math.floor(sec / 60) + ":" + ("0" + (sec % 60)).slice(-2); }

/* localStorage wrapper. Never throws; falls back to memory when storage is unavailable. */
function Store(storage) {
  var mem = {}, ok = !!storage;
  function safe(fn, fallback) { try { return fn(); } catch (e) { ok = false; return fallback; } }
  this.available = function () { return ok; };
  this.getJSON = function (key, dflt) {
    var raw = ok ? safe(function () { return storage.getItem(key); }, null) : (key in mem ? mem[key] : null);
    if (raw == null) return dflt;
    try { return JSON.parse(raw); } catch (e) { return dflt; }
  };
  this.setJSON = function (key, val) {
    var raw = JSON.stringify(val);
    if (ok) { var done = safe(function () { storage.setItem(key, raw); return true; }, false); if (done) return true; }
    mem[key] = raw; return false;
  };
  this.remove = function (key) { delete mem[key]; if (ok) safe(function () { storage.removeItem(key); }, null); };
  this.history = function () { var h = this.getJSON(KEYS.history, []); return Array.isArray(h) ? h : []; };
  this.addRecord = function (rec) { var h = this.history(); h.push(rec); this.setJSON(KEYS.history, trimHistory(h)); };
  this.sankalp = function () { var s = this.getJSON(KEYS.sankalp, null); return typeof s === "string" && s.trim() ? s : null; };
  this.saveSankalp = function (t) { if (!isAcceptable(t)) return false; this.setJSON(KEYS.sankalp, t); return true; };
  this.clearSankalp = function () { this.remove(KEYS.sankalp); };
  this.deleteAll = function () { this.remove(KEYS.history); this.remove(KEYS.sankalp); };
}

var api = { MIN_PRACTISED_SECONDS: MIN_PRACTISED_SECONDS, MAX_LINE: MAX_LINE, KEYS: KEYS, countsAsPractised: countsAsPractised,
  limitInput: limitInput, isAcceptable: isAcceptable, lastPractisedLabel: lastPractisedLabel, lastPractisedMs: lastPractisedMs,
  trimHistory: trimHistory, stageAt: stageAt, sittingStepAt: sittingStepAt, formatClock: formatClock, Store: Store, codePoints: codePoints };
if (typeof module !== "undefined" && module.exports) module.exports = api; else root.SHLogic = api;
})(typeof self !== "undefined" ? self : this);
