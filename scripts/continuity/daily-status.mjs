import { spawnSync } from 'node:child_process';
import { mkdtempSync, rmSync, mkdirSync, appendFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, basename, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const ORIGIN = 'https://github.com/BookPress9/shanivar.git';
export const STATUS_BRANCH = 'codex/daily-status';

function git(repo, args, { input, env = {}, allowFailure = false } = {}) {
  const result = spawnSync('git', ['-C', repo, ...args], {
    encoding: 'utf8', input, timeout: 60_000, maxBuffer: 8 * 1024 * 1024,
    windowsHide: true,
    env: { ...process.env, GIT_TERMINAL_PROMPT: '0', GCM_INTERACTIVE: 'Never', ...env },
  });
  if (result.status !== 0 && !allowFailure) {
    // Do not echo stderr: a credential helper or a remote URL can contain secrets.
    throw new Error(`git ${args[0]} failed (exit ${result.status ?? 'timeout/startup error'})`);
  }
  return result;
}

const output = (repo, args, options) => git(repo, args, options).stdout.trim();
const cell = value => String(value).replaceAll('|', '\\|').replaceAll('`', "'").replaceAll('\n', ' ');

export function snapshot(repo, now = new Date()) {
  const base = output(repo, ['rev-parse', 'refs/remotes/origin/main']);
  const refs = output(repo, ['for-each-ref', '--format=%(refname:strip=3)\t%(objectname)', 'refs/remotes/origin/'])
    .split('\n').filter(Boolean).map(line => line.split('\t'));
  const berlin = new Intl.DateTimeFormat('en-GB', {
    timeZone: 'Europe/Berlin', dateStyle: 'full', timeStyle: 'long',
  }).format(now);
  const rows = refs.filter(([name]) => name !== 'HEAD' && name !== STATUS_BRANCH).map(([name, sha]) => {
    const related = git(repo, ['merge-base', base, sha], { allowFailure: true }).status === 0;
    const counts = related ? output(repo, ['rev-list', '--left-right', '--count', `${base}...${sha}`]).split(/\s+/) : [];
    const state = related ? (counts[1] === '0' ? 'contained in main' : 'review / integration pending') : 'separate history (archive/status)';
    return `| ${cell(name)} | ${sha} | ${counts[1] ?? 'n/a'} | ${counts[0] ?? 'n/a'} | ${state} |`;
  });
  const worktrees = output(repo, ['worktree', 'list', '--porcelain']).split('\n')
    .filter(line => line.startsWith('worktree ')).map(line => line.slice(9));
  const local = worktrees.map(path => {
    const branch = output(path, ['symbolic-ref', '--quiet', '--short', 'HEAD'], { allowFailure: true }) || '(detached)';
    const sha = output(path, ['rev-parse', 'HEAD']);
    const dirty = git(path, ['status', '--porcelain=v1', '-z']).stdout.split('\0').filter(Boolean);
    // Git emits a second pathname for a rename. Only count records with an XY status prefix.
    const changes = dirty.filter(line => line.length > 3 && line[2] === ' ');
    const published = refs.some(([name, remoteSha]) => name === branch && remoteSha === sha);
    return `| ${cell(basename(path))} | ${cell(branch)} | ${sha} | ${published ? 'yes' : 'NO: push exact branch after review'} | ${changes.length} |`;
  });
  return `# Shanivar daily GitHub checkpoint

Generated ${berlin} (Europe/Berlin); UTC ${now.toISOString()}.

Start here: [current Codex handoff](https://github.com/BookPress9/shanivar/blob/codex/continuity-2026-10-17/docs/HANDOFF_NEXT.md).
Agent-box archive: [box-backup status](https://github.com/BookPress9/shanivar/blob/box-backup/SHANIVAR_STATUS.md).

This is a metadata checkpoint, NOT a backup of unsaved files and NOT a test or release verdict.
It fetches GitHub refs, reports committed branches and this computer's worktrees, and publishes only this page.
Agent-box run history, Jira, Notion, production services and other computers are not inspected by this job.
Main, agent branches, the real index and working files are never changed.

## Remote branches

Main at inspection: ${base}. Ahead/behind counts compare each branch with this main.
Contained in main does not establish that tests passed or a feature was released.

| Branch | Commit | Ahead | Behind | Git relationship |
|---|---|---|---|---|
${rows.join('\n')}

## Worktrees on this computer

"Published" means this branch's exact HEAD is on GitHub. Dirty paths are NOT backed up here.
Other cloud computers are represented only by their pushed refs above.

| Folder | Branch | HEAD | Published | Dirty entries |
|---|---|---|---|---|
${local.join('\n')}

## Required end-of-session handoff

1. Update docs/HANDOFF_NEXT.md and a dated docs/continuity session note: owner, exact branch/SHA,
   changed paths, commands/results, remaining blockers and the next executable action.
2. Stage only task-owned paths, inspect the staged diff for secrets, commit and push that task branch.
3. Verify git ls-remote origin refs/heads/<branch> matches local HEAD; no force pushes or main merges.
4. Run node scripts/continuity/daily-status.mjs --publish to refresh this checkpoint.
5. A failed daily job leaves a nonzero Windows task result and local git continuity run log.
   It sends no message. A later successful run refreshes this page; its timestamp exposes staleness.
`;
}

export function publish(repo, { expectedOrigin = ORIGIN, now = new Date() } = {}) {
  // Fixed destination: never push to main, the archive or any worker's branch.
  const actual = output(repo, ['remote', 'get-url', 'origin']);
  const pushUrl = output(repo, ['remote', 'get-url', '--push', 'origin']);
  if (actual !== expectedOrigin || pushUrl !== expectedOrigin) throw new Error('Unexpected origin; refusing to publish');
  git(repo, ['fetch', '--prune', 'origin']);
  const body = snapshot(repo, now);
  const dir = mkdtempSync(join(tmpdir(), 'shanivar-status-'));
  try {
    const env = {
      GIT_INDEX_FILE: join(dir, 'index'),
      GIT_AUTHOR_NAME: 'Hybrid Monk', GIT_AUTHOR_EMAIL: 'admin@thehybridmonk.com',
      GIT_COMMITTER_NAME: 'Hybrid Monk', GIT_COMMITTER_EMAIL: 'admin@thehybridmonk.com',
    };
    const parent = output(repo, ['rev-parse', '--verify', `refs/remotes/origin/${STATUS_BRANCH}`], { allowFailure: true });
    git(repo, ['read-tree', '--empty'], { env });
    const blob = output(repo, ['hash-object', '-w', '--stdin'], { input: body, env });
    git(repo, ['update-index', '--add', '--cacheinfo', `100644,${blob},README.md`], { env });
    const tree = output(repo, ['write-tree'], { env });
    const sha = output(repo, ['commit-tree', tree, ...(parent ? ['-p', parent] : [])], {
      env, input: `Daily continuity checkpoint ${now.toISOString()}\n`,
    });
    git(repo, ['push', 'origin', `${sha}:refs/heads/${STATUS_BRANCH}`]);
    const remote = output(repo, ['ls-remote', '--exit-code', 'origin', `refs/heads/${STATUS_BRANCH}`]).split(/\s+/)[0];
    if (remote !== sha) throw new Error('Remote verification failed');
    return sha;
  } finally {
    rmSync(dir, { recursive: true, force: true }); // Only our freshly created OS temporary directory.
  }
}

function main() {
  const args = process.argv.slice(2);
  let repoArg = '.';
  let shouldPublish = false;
  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--publish') shouldPublish = true;
    else if (args[i] === '--repo' && args[i + 1] && !args[i + 1].startsWith('--')) repoArg = args[++i];
    else throw new Error('Usage: node daily-status.mjs [--repo <path>] [--publish]');
  }
  const repo = resolve(repoArg);
  const logDir = resolve(repo, output(repo, ['rev-parse', '--git-path', 'continuity']));
  mkdirSync(logDir, { recursive: true });
  const started = new Date().toISOString();
  try {
    if (shouldPublish) {
      const sha = publish(repo);
      appendFileSync(join(logDir, 'runs.jsonl'), JSON.stringify({ started, finished: new Date().toISOString(), ok: true, branch: STATUS_BRANCH, sha }) + '\n');
      console.log(`Verified GitHub ${STATUS_BRANCH} at ${sha}`);
    } else {
      console.log(snapshot(repo));
    }
  } catch (error) {
    appendFileSync(join(logDir, 'runs.jsonl'), JSON.stringify({ started, finished: new Date().toISOString(), ok: false, error: error.message }) + '\n');
    throw error;
  }
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try { main(); } catch (error) { console.error(error.message); process.exitCode = 1; }
}
