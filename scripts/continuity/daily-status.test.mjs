import { test } from 'node:test';
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdtempSync, writeFileSync, readFileSync, rmSync } from 'node:fs';
import { join } from 'node:path';
import { tmpdir } from 'node:os';
import { publish, STATUS_BRANCH } from './daily-status.mjs';

function git(repo, ...args) {
  const r = spawnSync('git', ['-C', repo, ...args], { encoding: 'utf8', windowsHide: true });
  assert.equal(r.status, 0, `git ${args[0]}: ${r.stderr}`);
  return r.stdout.trim();
}

function fixture(t) {
  const dir = mkdtempSync(join(tmpdir(), 'shanivar-continuity-test-'));
  t.after(() => rmSync(dir, { recursive: true, force: true }));
  const remote = join(dir, 'remote.git');
  const repo = join(dir, 'work');
  git(dir, 'init', '--bare', remote);
  git(dir, 'init', '-b', 'main', repo);
  git(repo, 'config', 'user.name', 'Continuity test');
  git(repo, 'config', 'user.email', 'test@example.invalid');
  writeFileSync(join(repo, 'app.txt'), 'committed baseline\n');
  git(repo, 'add', 'app.txt');
  git(repo, 'commit', '-m', 'baseline');
  git(repo, 'remote', 'add', 'origin', remote);
  git(repo, 'push', '-u', 'origin', 'main');
  return { repo, remote };
}

test('publishes only metadata, preserves staged/unstaged files and main, and appends history', t => {
  const { repo, remote } = fixture(t);
  const main = git(repo, 'rev-parse', 'HEAD');
  writeFileSync(join(repo, 'app.txt'), 'staged change must survive\n');
  git(repo, 'add', 'app.txt');
  writeFileSync(join(repo, 'app.txt'), 'unstaged change must survive\n');
  writeFileSync(join(repo, 'private-untracked.txt'), 'SENTINEL_MUST_NEVER_BE_PUBLISHED\n');
  const index = git(repo, 'ls-files', '--stage');
  const before = git(repo, 'status', '--porcelain=v1');
  const sha = publish(repo, { expectedOrigin: remote, now: new Date('2026-10-17T21:30:00Z') });
  assert.equal(git(repo, 'rev-parse', 'HEAD'), main);
  assert.equal(git(repo, 'ls-files', '--stage'), index);
  assert.equal(git(repo, 'status', '--porcelain=v1'), before);
  assert.equal(readFileSync(join(repo, 'app.txt'), 'utf8'), 'unstaged change must survive\n');
  assert.equal(git(remote, 'rev-parse', 'refs/heads/main'), main);
  assert.equal(git(remote, 'ls-tree', '--name-only', sha), 'README.md');
  const page = git(remote, 'show', `${sha}:README.md`);
  assert.ok(!page.includes('SENTINEL_MUST_NEVER_BE_PUBLISHED'));
  assert.match(page, /23:30:00/); // UTC converted to Berlin daylight time.
  assert.match(page, /Dirty entries/);
  const next = publish(repo, { expectedOrigin: remote, now: new Date('2026-10-18T21:30:00Z') });
  assert.equal(git(remote, 'rev-parse', `${next}^`), sha);
  assert.equal(git(remote, 'rev-parse', `refs/heads/${STATUS_BRANCH}`), next);
  assert.equal(git(repo, 'ls-files', '--stage'), index);
});

test('rejects an unexpected fetch or push destination before writing anything', t => {
  const { repo, remote } = fixture(t);
  assert.throws(() => publish(repo), /Unexpected origin/);
  git(repo, 'remote', 'set-url', '--push', 'origin', join(remote, 'wrong'));
  assert.throws(() => publish(repo, { expectedOrigin: remote }), /Unexpected origin/);
  assert.equal(git(remote, 'for-each-ref', '--format=%(refname)', 'refs/heads/'), 'refs/heads/main');
});
