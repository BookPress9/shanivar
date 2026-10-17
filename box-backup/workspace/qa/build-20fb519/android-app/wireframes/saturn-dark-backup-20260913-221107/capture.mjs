import { chromium } from '../../../frontend/node_modules/playwright/index.mjs';
import { mkdir } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const output = join(here, 'exports-v2');
const source = pathToFileURL(join(here, 'index.html')).href;
const screens = [
  ['language', '01-language'],
  ['account', '02-account'],
  ['birth', '03-birth-context'],
  ['permissions', '04-how-it-works'],
  ['onboarding', '05-arrival'],
  ['today', '06-today'],
  ['focus', '07-focus-setup'],
  ['locked', '08-focus-held'],
  ['rhythm', '09-dincharya'],
  ['practice', '10-practice'],
  ['reflection', '11-messages'],
  ['movement', '12-movement-cue'],
  ['alarm', '13-alarm'],
  ['complete', '14-return'],
];

await mkdir(output, { recursive: true });
const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport: { width: 1600, height: 1120 }, deviceScaleFactor: 1 });

for (const language of ['en', 'hi']) {
  for (const [screen, filename] of screens) {
    await page.goto(`${source}?screen=${screen}&lang=${language}`);
    await page.locator('.phone-frame').screenshot({ path: join(output, `${language === 'hi' ? 'hi-' : ''}${filename}.png`) });
  }
}

await page.goto(`${source}?screen=today&lang=en`);
await page.screenshot({ path: join(output, 'saturn-wireframe-workspace.png'), fullPage: true });
await page.goto(`${source}?screen=today&lang=hi`);
await page.screenshot({ path: join(output, 'saturn-wireframe-workspace-hi.png'), fullPage: true });
await browser.close();

console.log(`Captured ${screens.length} English frames, ${screens.length} Hindi frames, and two workspace overviews in ${output}`);
