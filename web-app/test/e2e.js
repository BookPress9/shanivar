const assert = require("node:assert/strict");
const http = require("node:http");
const fs = require("node:fs");
const path = require("node:path");
const { chromium } = require("playwright-core");

const root = path.resolve(__dirname, "..");
const externalBase = process.env.E2E_BASE_URL;
const types = { ".html": "text/html", ".js": "text/javascript", ".css": "text/css", ".svg": "image/svg+xml", ".webmanifest": "application/manifest+json" };
const server = http.createServer((req, res) => {
  const pathname = new URL(req.url, "http://localhost").pathname;
  const target = path.join(root, pathname === "/" ? "index.html" : pathname);
  if (!target.startsWith(root)) { res.writeHead(403).end(); return; }
  fs.readFile(target, (error, body) => {
    if (error) { res.writeHead(404).end("not found"); return; }
    res.writeHead(200, { "content-type": types[path.extname(target)] || "application/octet-stream" });
    res.end(body);
  });
});
let browser, page;

(async () => {
  if (!externalBase) await new Promise(resolve => server.listen(8092, "127.0.0.1", resolve));
  browser = await chromium.launch({ headless: true, executablePath: process.env.CHROME_PATH || "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe" });
  const context = await browser.newContext({ viewport: { width: 390, height: 844 } });
  page = await context.newPage();
  const errors = [];
  page.on("pageerror", error => errors.push(error.message));
  await page.goto(externalBase || "http://127.0.0.1:8092/", { waitUntil: "networkidle" });
  await page.getByRole("heading", { name: "A place to return." }).waitFor();
  assert.equal(await page.locator("nav.tabbar").isHidden(), true);
  await page.getByRole("button", { name: "Continue" }).click();
  await page.locator("#ob-wake").fill("06:45");
  await page.locator("#ob-sleep").fill("22:45");
  await page.getByRole("button", { name: "Continue" }).click();
  await page.getByRole("heading", { name: "Keep this device connected." }).waitFor();
  if (process.env.E2E_AUTH === "1") await page.getByRole("button", { name: "Continue as guest" }).click();
  else await page.getByRole("button", { name: "Begin locally" }).click();
  await page.getByRole("heading", { name: "Return to what you chose." }).waitFor();
  assert.equal(await page.locator("nav.tabbar").isVisible(), true);
  assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth), true);

  await page.getByRole("button", { name: /Routine$/ }).last().click();
  await page.getByRole("button", { name: "Add activity" }).click();
  await page.locator('[data-activity] input[name="title"]').fill("Read");
  await page.getByRole("button", { name: "Save Routine" }).click();
  await page.getByRole("heading", { name: "Return to what you chose." }).waitFor();
  assert.match(await page.locator("main").innerText(), /Read/);

  await page.getByRole("button", { name: /Sadhana$/ }).click();
  await page.getByRole("button", { name: "Choose" }).first().click();
  await page.locator('input[name="safe"]').check();
  await page.getByRole("button", { name: "Begin" }).click();
  await page.getByRole("button", { name: "Pause" }).click();
  await page.getByRole("button", { name: "End" }).click();
  await page.getByRole("heading", { name: "What happened, plainly." }).waitFor();
  assert.match(await page.locator("main").innerText(), /1\s+attempt/);
  if (process.env.E2E_AUTH === "1") {
    await page.waitForTimeout(2500);
    const syncState = await page.evaluate(() => { const state=JSON.parse(localStorage.getItem("shanivar.web.v2")); return {pending:state.outbox.map(item => item.kind),error:state.sync.error}; });
    console.log("Authenticated sync state:", syncState);
    const pending = syncState.pending;
    assert.deepEqual([...new Set(pending)], ["routine"]);
  }

  await page.reload({ waitUntil: "networkidle" });
  await page.getByRole("heading", { name: "Return to what you chose." }).waitFor();
  assert.doesNotMatch(await page.locator("main").innerText(), /A place to return/);
  await context.setOffline(true);
  await page.reload({ waitUntil: "domcontentloaded" });
  await page.getByRole("heading", { name: "Return to what you chose." }).waitFor();
  assert.equal(await page.locator("#offline-banner").isVisible(), true);
  assert.deepEqual(errors, []);
  await browser.close();
  if (!externalBase) server.close();
  console.log("E2E passed: onboarding, mobile layout, Routine, Sadhana, record, persistence, offline reopen.");
})().catch(async error => { console.error(error); try { console.error("PAGE:", await page?.locator("body").innerText()); } catch (_) {} try { await browser?.close(); } catch (_) {} if (!externalBase) server.close(); process.exitCode = 1; });
