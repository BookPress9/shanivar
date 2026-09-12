import { chromium } from '../../../frontend/node_modules/playwright/index.mjs';
import { mkdir } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const output = join(here, 'exports');
const source = pathToFileURL(join(here, 'index.html')).href;
const screens = [
  ['onboarding', '01-arrival'],
  ['today', '02-today'],
  ['focus', '03-focus-setup'],
  ['locked', '04-focus-held'],
  ['rhythm', '05-rhythm'],
  ['practice', '06-practice'],
  ['reflection', '07-messages'],
  ['alarm', '08-alarm'],
  ['complete', '09-return'],
];

await mkdir(output, { recursive: true });
const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport: { width: 1600, height: 1120 }, deviceScaleFactor: 1 });

for (const [screen, filename] of screens) {
  await page.goto(`${source}?screen=${screen}`);
  await page.locator('.phone-frame').screenshot({ path: join(output, `${filename}.png`) });
}

await page.goto(`${source}?screen=today`);
await page.screenshot({ path: join(output, 'saturn-wireframe-workspace.png'), fullPage: true });
await browser.close();

console.log(`Captured ${screens.length} mobile frames and one workspace overview in ${output}`);
