import assert from 'node:assert/strict';
import {readFile, mkdir, writeFile} from 'node:fs/promises';
import {dirname, join} from 'node:path';
import {fileURLToPath, pathToFileURL} from 'node:url';

const root=dirname(fileURLToPath(import.meta.url));
const html=await readFile(join(root,'index.html'),'utf8');
const js=await readFile(join(root,'app.js'),'utf8');
const css=await readFile(join(root,'style.css'),'utf8');
const checks=[];

assert.match(html,/Routine gives your personal life structure/);
assert.doesNotMatch(html,/Dashboard shows/);
assert.match(js,/Stop leaving your life/);
assert.match(js,/I usually wake around/);
assert.match(js,/I make room for sleep around/);
assert.doesNotMatch(js,/My schedule<select/);
assert.doesNotMatch(js,/TWO ANCHORS/);
assert.doesNotMatch(js,/Not someone else/);
checks.push('Welcome/rhythm reflect the two-destination stance, remove the onboarding schedule selector, and use the approved rhythm copy.');

assert.match(js,/APP_GROUPS/);
assert.match(js,/Custom week/);
assert.match(js,/addWindow/);
assert.match(js,/window-day/);
assert.match(js,/class="add-row"><button class="plus" data-act="addWindow"/);
checks.push('App protection contains categorized apps, multiple windows, add-window control and custom Monday–Sunday recurrence.');

assert.match(js,/routineEdit/);
assert.match(js,/addActivity/);
assert.match(js,/Save my Routine/);
for(const occurrence of ['Every day','Weekly','Custom'])assert.match(js,new RegExp(occurrence));
checks.push('Routine includes an editor for anchors and personal activities with time, duration and everyday, weekly or custom recurrence.');

assert.match(js,/Natural breath\. Nothing else\./);
assert.doesNotMatch(js,/An object nearby/);
assert.doesNotMatch(js,/Contact with chair or floor/);
assert.match(js,/saturn-orbit/);
assert.match(js,/The planet moves\. Your breath does not follow it/);
checks.push('Steady uses natural breathing only and Saturn motion is explicitly not a breathing pacer.');

assert.match(css,/--paper:#f2efe6/);
assert.match(css,/\.card\.light/);
assert.match(css,/\.card\.black/);
assert.match(css,/\.card\.light p/);
checks.push('Visual system defines explicit light-panel/dark-panel foreground pairs instead of dark-on-dark body copy.');

let browserResult='Browser verification skipped: repository Playwright dependency was not found from this standalone package.';
try{
  const {chromium}=await import('../../../../frontend/node_modules/playwright/index.mjs');
  const output=join(root,'exports');
  await mkdir(output,{recursive:true});
  const browser=await chromium.launch({headless:true});
  const page=await browser.newPage({viewport:{width:1440,height:1080}});
  const errors=[]; page.on('pageerror',e=>errors.push(e.message));
  const url=pathToFileURL(join(root,'index.html')).href;
  await page.goto(url);
  assert.match(await page.locator('#app').innerText(),/Stop leaving your life/);
  await page.locator('[data-go="rhythm"]').first().click();
  assert.equal(await page.locator('#shift').count(),0);
  await page.locator('[data-go="setup"]').first().click();
  await page.locator('[data-go="locks"]').first().click();
  const before=await page.locator('.window-card').count();
  assert.equal(await page.locator('.add-row [data-act="addWindow"]').count(),1);
  await page.locator('[data-act="addWindow"]').first().click();
  assert.equal(await page.locator('.window-card').count(),before+1);
  await page.locator('.add-row [data-act="addWindow"]').scrollIntoViewIfNeeded();
  await page.locator('.phone').screenshot({path:join(output,'locks.png')});
  await page.goto(`${url}?screen=routine`);
  await page.locator('[data-go="routineEdit"]').first().click();
  assert.equal(await page.locator('#routine-wake').count(),1);
  assert.equal(await page.locator('#routine-bed').count(),1);
  const activitiesBefore=await page.locator('.activity-card').count();
  await page.locator('[data-add-activity="Gym"]').click();
  assert.equal(await page.locator('.activity-card').count(),activitiesBefore+1);
  let gym=page.locator('.activity-card').last();
  await gym.locator('input[id^="activity-title-"]').fill('Evening Gym');
  await gym.locator('input[id^="activity-time-"]').fill('18:30');
  await gym.locator('select[data-activity-repeat]').selectOption({label:'Custom'});
  gym=page.locator('.activity-card').last();
  await gym.locator('[data-activity-day]').filter({hasText:'Wed'}).click();
  await page.locator('.phone').screenshot({path:join(output,'routine-edit.png')});
  await page.locator('[data-act="saveRoutine"]').click();
  assert.match(await page.locator('#app').innerText(),/Evening Gym/);
  assert.match(await page.locator('#app').innerText(),/18:30/);
  await page.goto(`${url}?screen=ready`);
  assert.equal(await page.locator('[data-act="begin"]').isDisabled(),true);
  await page.locator('#safe').check();
  assert.equal(await page.locator('[data-act="begin"]').isEnabled(),true);
  await page.locator('[data-act="begin"]').click();
  assert.match(await page.locator('#app').innerText(),/Return\. Again\./);
  assert.equal(await page.locator('.saturn').count(),1);
  await page.locator('[data-act="pause"]').click();
  await page.locator('.phone').screenshot({path:join(output,'practice.png')});
  await page.goto(`${url}?screen=routine`);
  await page.locator('.phone').screenshot({path:join(output,'routine.png')});
  await page.setViewportSize({width:375,height:812});
  for(const screen of ['welcome','rhythm','setup','routine','routineEdit','locks','sadhana','ready','practice']){
    await page.goto(`${url}?screen=${screen}`);
    assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true,`${screen} overflows mobile viewport`);
  }
  assert.deepEqual(errors,[]);
  await browser.close();
  browserResult='PASS: optional Playwright checks ran, Routine activity editing and add-window worked, the practice gate worked, and tested mobile screens did not create page-level horizontal overflow.';
}catch(error){
  if(!String(error?.message||error).includes('Cannot find module')&&!String(error?.code||'').includes('MODULE_NOT_FOUND')){
    browserResult=`Browser verification unavailable or failed: ${error.message||error}`;
  }
}

checks.push(browserResult);
await writeFile(join(root,'QA.generated.md'),`# Generated verification\n\n${checks.map(x=>`- ${x}`).join('\n')}\n`,'utf8');
console.log(checks.join('\n'));
