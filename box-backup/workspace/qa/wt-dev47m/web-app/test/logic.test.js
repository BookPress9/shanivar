const test = require("node:test"), assert = require("node:assert");
const L = require("../logic.js"), C = require("../content.js");
const at = (y, m, d, h = 12) => new Date(y, m - 1, d, h).getTime();

test("90 s rule", () => {
  assert.equal(L.MIN_PRACTISED_SECONDS, 90);
  assert.equal(L.countsAsPractised({ outcome: "timer", activeSeconds: 90 }), true);
  assert.equal(L.countsAsPractised({ outcome: "user", activeSeconds: 120 }), true);
  assert.equal(L.countsAsPractised({ outcome: "user", activeSeconds: 89 }), false);
  assert.equal(L.countsAsPractised({ outcome: "timer", activeSeconds: 60 }), false);
  assert.equal(L.countsAsPractised({ outcome: "interrupted", activeSeconds: 170 }), false);
});
test("last practised label", () => {
  const now = at(2026, 10, 10);
  assert.equal(L.lastPractisedLabel(at(2026, 10, 10, 1), now), "Last practised today");
  assert.equal(L.lastPractisedLabel(at(2026, 10, 9, 23), now), "Last practised yesterday");
  assert.equal(L.lastPractisedLabel(at(2026, 10, 3), now), "Last practised 3 Oct");
  assert.equal(L.lastPractisedLabel(at(2025, 12, 31), now), "Last practised 31 Dec 2025");
  assert.equal(L.lastPractisedLabel(null, now), null);
});
test("last practised ignores uncounted records", () => {
  const recs = [{ practiceId: "a", startedAt: 5, outcome: "timer", activeSeconds: 100 },
    { practiceId: "a", startedAt: 9, outcome: "user", activeSeconds: 30 },
    { practiceId: "a", startedAt: 11, outcome: "interrupted", activeSeconds: 200 }];
  assert.equal(L.lastPractisedMs(recs, "a"), 5);
  assert.equal(L.lastPractisedMs(recs, "b"), null);
});
test("60-char limit", () => {
  assert.equal(L.limitInput("x".repeat(80)).length, 60);
  assert.equal(L.limitInput("a\nb"), "a b");
  assert.equal(L.codePoints(L.limitInput("😀".repeat(70))).length, 60);
  assert.ok(L.isAcceptable("x".repeat(60)));
  assert.ok(!L.isAcceptable("x".repeat(61)));
  assert.ok(!L.isAcceptable("   "));
});
function stub() { const m = {}; return { getItem: k => (k in m ? m[k] : null), setItem: (k, v) => { m[k] = v; }, removeItem: k => { delete m[k]; }, m }; }
test("store round trip, trim, delete all", () => {
  const st = stub(), s = new L.Store(st);
  s.addRecord({ practiceId: "a", startedAt: 1, outcome: "timer", activeSeconds: 100 });
  assert.equal(s.history().length, 1);
  for (let i = 0; i < 150; i++) s.addRecord({ practiceId: "a", startedAt: i, outcome: "user", activeSeconds: 1 });
  assert.equal(s.history().length, 100);
  assert.ok(s.saveSankalp("my line")); assert.equal(s.sankalp(), "my line");
  assert.ok(!s.saveSankalp("x".repeat(61)));
  s.clearSankalp(); assert.equal(s.sankalp(), null);
  s.saveSankalp("again"); s.deleteAll();
  assert.deepEqual(Object.keys(st.m), []);
});
test("store survives broken or throwing storage", () => {
  const bad = { getItem() { throw new Error("no"); }, setItem() { throw new Error("no"); }, removeItem() { throw new Error("no"); } };
  const s = new L.Store(bad);
  assert.doesNotThrow(() => { s.addRecord({ practiceId: "a" }); s.saveSankalp("x"); s.deleteAll(); });
  const st = stub(); st.m[L.KEYS.history] = "{not json"; assert.deepEqual(new L.Store(st).history(), []);
});
test("stages and clock", () => {
  assert.equal(L.sittingStepAt(0), "settle"); assert.equal(L.sittingStepAt(30), "scan"); assert.equal(L.sittingStepAt(120), "stay");
  assert.equal(L.stageAt(5, 180), "arrival"); assert.equal(L.stageAt(15, 180), "instruction");
  assert.equal(L.stageAt(100, 180), "practice"); assert.equal(L.stageAt(175, 180), "closing");
  assert.equal(L.stageAt(175, 180, true), "practice");
  assert.equal(L.formatClock(179.2), "3:00"); assert.equal(L.formatClock(65), "1:05");
});
test("content: 8 practices, allowed durations, banned phrases", () => {
  assert.equal(C.PRACTICES.length, 8);
  C.PRACTICES.forEach(p => assert.ok(p.allowed.includes(p.defaultSeconds)));
  const text = JSON.stringify([C.PRACTICES.map(p => p.stages), C.COPY]).toLowerCase();
  ["deep breath", "breathe into", "heal", "blood flow", "nervous system", "streak", "badge", "saturn"].forEach(w => assert.ok(!text.includes(w), w));
});
