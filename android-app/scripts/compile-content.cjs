// Compile the Content team's original adaptations; never substitute generated technique text.
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '../..');
const source = path.join(root, 'shanivar-vbt/content');
const output = path.join(root, 'android-app/app/src/main/assets/techniques.json');
const section = (text, name) => {
  const marker = '## ' + name;
  const start = text.indexOf(marker);
  if (start < 0) throw new Error('Missing section: ' + name);
  return text.slice(start + marker.length).split(/\r?\n## /)[0].trim();
};
const techniques = fs.readdirSync(source).filter(name => /^technique-\d+.*\.md$/.test(name)).sort().map(name => {
  const text = fs.readFileSync(path.join(source, name), 'utf8');
  const primary = section(text, 'Primary Source');
  const verse = primary.split(/\r?\n/).find(line => line.includes('**Verse'));
  const edition = primary.split(/\r?\n/).find(line => line.includes('**Translation cited'));
  if (!verse || !edition) throw new Error('Missing citation: ' + name);
  return {
    id: name.replace('.md', ''),
    title: text.split(/\r?\n/)[0].replace(/^# Technique \d+: /, ''),
    essence: section(text, 'Essence'),
    duration: section(text, 'Suggested Duration'),
    script: section(text, 'Guided Session Script').replace(/^\*\(Adaptation[\s\S]*?\)\*\s*/, ''),
    source: [verse, edition].join('\n\n').replace(/^- /gm, '').replace(/\*\*/g, '') + '\n\nGuided text is a modern adaptation, not a literal translation.'
  };
});
if (techniques.length !== 12) throw new Error('Expected all 12 reviewed technique files.');
fs.mkdirSync(path.dirname(output), { recursive: true });
fs.writeFileSync(output, JSON.stringify(techniques, null, 2) + '\n');
console.log('Compiled ' + techniques.length + ' techniques with source citations.');
