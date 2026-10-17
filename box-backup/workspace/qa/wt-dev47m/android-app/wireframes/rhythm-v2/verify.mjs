import { chromium } from '../../../frontend/node_modules/playwright/index.mjs';
import { mkdir, writeFile } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import assert from 'node:assert/strict';

const root=dirname(fileURLToPath(import.meta.url));
const url=pathToFileURL(join(root,'index.html')).href;
const output=join(root,'exports');
await mkdir(output,{recursive:true});
const browser=await chromium.launch({headless:true});
const page=await browser.newPage({viewport:{width:1440,height:1080}});
const errors=[];
page.on('pageerror',error=>errors.push(error.message));
const checks=[];
const go=screen=>page.locator(`[data-go="${screen}"]`).first().click();
const act=name=>page.locator(`#app [data-act="${name}"]`).first().click();

await page.goto(url);
assert.match(await page.locator('#app h1').innerText(),/A day that/);
await go('rhythm');
await page.locator('#wake').fill('06:45');
await page.locator('#bed').fill('06:45');
await act('saveRhythm');
assert.match(await page.locator('.error').innerText(),/different/);
await page.locator('#bed').fill('22:45');
await act('saveRhythm');
assert.match(await page.locator('#app h1').innerText(),/A little setup/);
checks.push('Onboarding rejects identical wake/sleep times and proceeds without an account gate.');

await go('locks');
for(const checkbox of await page.locator('input[name=apps]').all())await checkbox.uncheck();
await act('saveLocks');
assert.match(await page.locator('.error').innerText(),/at least one app/);
await page.locator('input[name=apps]').first().check();
await page.locator('#start').fill('10:00');
await page.locator('#end').fill('09:00');
await act('saveLocks');
assert.match(await page.locator('.error').innerText(),/after the start/);
await page.locator('#end').fill('11:00');
await act('saveLocks');
assert.match(await page.locator('#app h1').innerText(),/device/);
await act('deny');
assert.match(await page.locator('#app').innerText(),/Needs device access/);
checks.push('Protection requires apps plus a valid same-day interval and does not claim readiness after permission denial.');

await go('ready');
assert.equal(await page.locator('[data-act=begin]').isDisabled(),true);
await page.locator('#safe').check();
assert.equal(await page.locator('[data-act=begin]').isEnabled(),true);
await act('begin');
await page.waitForTimeout(1100);
await act('pause');
assert.match(await page.locator('#app').innerText(),/PAUSED/);
await act('practiceSource');
assert.match(await page.locator('#app h1').innerText(),/Know where/);
await go('practice');
assert.match(await page.locator('#app').innerText(),/PAUSED/);
await act('end');
await page.locator('[data-feedback="Uncomfortable"]').click();
assert.match(await page.locator('#app').innerText(),/Stop here/);
checks.push('Practice needs stationary-setting confirmation; source navigation pauses; discomfort prevents celebratory escalation.');

await go('challenge');
assert.equal(await page.locator('[data-act=acceptChallenge]').isDisabled(),true);
await page.locator('#familiar').check();
await act('acceptChallenge');
assert.match(await page.locator('#app').innerText(),/No reminder has been scheduled/);
await go('independent');
await page.locator('#independent').check();
await act('saveIndependent');
await go('audit');
await act('logOwn');
assert.match(await page.locator('#app').innerText(),/Independent practice/);
checks.push('The next challenge is voluntary and independent practice is recorded separately from timed attempts.');

const screens=['welcome','rhythm','setup','dashboard','routine','alarm','locks','permissions','sadhana','chapter','ready','practice','reflection','audit','challenge','independent','protocols','protocol','source','settings','recovery'];
for(const screen of screens){
  await page.goto(`${url}?screen=${screen}`);
  await page.locator('.phone').screenshot({path:join(output,`${screen}.png`)});
  const fit=await page.locator('#app').evaluate(el=>el.scrollWidth<=el.clientWidth+1);
  assert.equal(fit,true,`${screen} has horizontal overflow`);
}
checks.push(`${screens.length} phone states captured without horizontal content overflow.`);

await page.goto(`${url}?screen=dashboard`);
await page.screenshot({path:join(output,'workspace.png'),fullPage:true});
await page.setViewportSize({width:375,height:812});
for(const screen of ['dashboard','routine','sadhana','chapter','audit','protocol']){
  await page.goto(`${url}?screen=${screen}`);
  assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true,`${screen} mobile workspace overflow`);
}
await page.screenshot({path:join(output,'mobile-dashboard.png'),fullPage:true});
checks.push('Primary mobile routes fit a 375px viewport.');
assert.deepEqual(errors,[]);
checks.push('No browser JavaScript errors occurred in exercised paths.');

const cards=screens.map(s=>`<a href="../index.html?screen=${s}"><img src="${s}.png" alt="${s} screen"><span>${s.replaceAll('-',' ')}</span></a>`).join('');
await writeFile(join(output,'index.html'),`<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>Shanivar V2 screen collection</title><style>body{margin:0;background:#0c0f0d;color:#f1f0e5;font:14px system-ui;padding:32px}header{max-width:1100px;margin:auto 0 30px}h1{font-weight:450}p{color:#b2baad}.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(210px,1fr));gap:25px}a{color:#d1dfad;text-decoration:none;text-transform:capitalize}img{width:100%;display:block;border-radius:22px;border:1px solid #47503f;margin-bottom:8px}</style></head><body><header><h1>Shanivar V2 · Routine, Dashboard, Sadhana</h1><p>${screens.length} captured states. Select a screen to open it in the interactive prototype.</p><a href="../README.md">Read the handoff</a></header><main class="grid">${cards}</main></body></html>`,`utf8`);
await writeFile(join(root,'QA.md'),`# Browser verification\n\nVerified 30 September 2026 with Chromium through Playwright. This verifies the browser wireframe only; it does not test Android alarms, app blocking, accessibility services, persistence, backend behavior or physical devices.\n\n${checks.map(x=>`- PASS: ${x}`).join('\n')}\n\n## Manual visual review\n\nThe latest exported phone states and desktop/mobile workspaces were inspected for hierarchy, text clipping, control reachability and contrast. Browser automation does not establish WCAG conformance, screen-reader quality or usability. Native implementation still requires 200% text, TalkBack, landscape, small-device and physical-device review.\n`,`utf8`);
await browser.close();
console.log(checks.join('\n'));

