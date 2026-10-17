#!/usr/bin/env bash
# snapshot-to-github.sh — back up all Shanivar work on this box to GitHub (BookPress9/shanivar).
# Idempotent. Safe to run while agents work: never touches any worktree's files, index, branch or HEAD.
#  1) push every local non-main branch as-is (never force; non-fast-forward -> backup/<branch>-YYYYMMDD) + all tags
#  2) snapshot uncommitted/untracked changes of every worktree via a temporary index -> wip/<branch> (force only on wip/*)
#  3) copy non-repo Shanivar work into an orphan branch `box-backup` under box-backup/<original path>
#  4) generate SHANIVAR_STATUS.md at the root of box-backup
# Usage: /workspace/bin/snapshot-to-github.sh [--dry-run]
set -uo pipefail
export TZ=Europe/Berlin
REPO=BookPress9/shanivar
URL=https://github.com/$REPO.git
MAIN=/workspace/shanivar-v3
EXTRA_REPOS=(/workspace/shanivar-app)          # stand-alone clones of the same repo (snapshot only, never push main)
BK=/home/box/.shanivar-box-backup              # private clone used only for the box-backup branch
GH=/usr/local/bin/gh
MAXB=$((50*1024*1024))
DAY=$(date +%Y%m%d)
NOW=$(date '+%Y-%m-%d %H:%M %Z')
DRY=0; [ "${1:-}" = "--dry-run" ] && DRY=1
LOG=/workspace/logs/snapshot-to-github.log
STATE=$(mktemp -d)
mkdir -p /workspace/logs
exec 9>/tmp/snapshot-to-github.lock; flock -n 9 || { echo "another snapshot is running"; exit 1; }
exec > >(tee -a "$LOG") 2>&1
echo "=== snapshot-to-github $NOW ==="

gitp(){ git -c credential.helper= -c 'credential.helper=!/usr/local/bin/gh auth git-credential' "$@"; }
push(){ if [ $DRY = 1 ]; then echo "DRY: git $*"; return 0; fi; gitp "$@"; }
note(){ echo "$*" >> "$STATE/$1.tsv.tmp"; }   # unused helper kept simple
HARD='eyJhb[G]ci|sb_secre[t]_|gh[p]_[A-Za-z0-9]{20,}|github_pat_|-----BEGIN [A-Z ]*PRIVATE KEY'
SOFT='(service_role|api_key|apikey|password|secret|token)["'"'"']?[[:space:]]*[=:][[:space:]]*["'"'"']?[A-Za-z0-9_./+-]{16,}'
EXCL_RE='(^|/)(\.env(\..*)?|.*\.keystore|.*\.jks|local\.properties|\.netrc|id_rsa.*|.*\.pem|.*\.p12)$|(^|/)(build|\.gradle|\.kotlin|node_modules|venv|\.venv|__pycache__|\.cache|\.idea|captures|\.cxx)/|(^|/)supabase/\.temp/'
is_env_example(){ [[ "$1" =~ \.env\.(example|template)$ ]]; }

# ---------- 0) visibility ----------
VIS=$($GH repo view $REPO --json visibility --jq .visibility 2>/dev/null || echo UNKNOWN)
echo "visibility: $VIS"
: > "$STATE/branches.tsv"; : > "$STATE/wip.tsv"; : > "$STATE/excluded.tsv"; : > "$STATE/failures.tsv"

# ---------- 1) branches + tags ----------
cd "$MAIN"
gitp fetch -q origin --prune '+refs/heads/*:refs/remotes/origin/*' || echo "fetch failed" >> "$STATE/failures.tsv"
for b in $(git for-each-ref --format='%(refname:short)' refs/heads/); do
  [ "$b" = main ] && continue
  l=$(git rev-parse "refs/heads/$b"); r=$(git rev-parse -q --verify "refs/remotes/origin/$b" || true)
  if [ "$l" = "$r" ]; then res="up-to-date"
  elif [ -z "$r" ] || git merge-base --is-ancestor "$r" "$l"; then
    if push push -q origin "refs/heads/$b:refs/heads/$b"; then res="pushed"; else res="FAILED"; fi
  else res="non-ff"; fi
  if [ "$res" = non-ff ] || [ "$res" = FAILED ]; then
    if push push -q origin "refs/heads/$b:refs/heads/backup/$b-$DAY"; then res="$res -> backup/$b-$DAY"; else res="$res; backup push FAILED"; echo -e "$b\tpush failed" >> "$STATE/failures.tsv"; fi
  fi
  echo -e "$b\t$res" >> "$STATE/branches.tsv"; echo "branch $b: $res"
done
# tags (never forced; an existing different remote tag is reported, not overwritten)
for t in $(git tag -l); do
  rt=$(gitp ls-remote origin "refs/tags/$t" | cut -f1); lt=$(git rev-parse "refs/tags/$t")
  if [ "$rt" = "$lt" ]; then echo "tag $t: up-to-date"
  elif [ -n "$rt" ]; then echo "tag $t: differs on remote, left alone"; echo -e "tag $t\tdiffers on remote" >> "$STATE/failures.tsv"
  else push push -q origin "refs/tags/$t:refs/tags/$t" && echo "tag $t: pushed" || echo -e "tag $t\tpush failed" >> "$STATE/failures.tsv"; fi
done

# ---------- 2) WIP snapshots ----------
snapshot(){ # $1 = worktree path
  local w=$1 br name tmp tree head commit rtree rpar f sz
  br=$(git -C "$w" symbolic-ref -q --short HEAD || echo "")
  if [ -z "$br" ]; then name="detached-$(basename "$w")"; else name="$br"; fi
  [ "$w" != "$MAIN" ] && [[ " ${EXTRA_REPOS[*]} " == *" $w "* ]] && name="$(basename "$w")-$br"
  [ "$name" = main ] && name="main-$(basename "$w")"
  if [ -z "$(git -C "$w" status --porcelain 2>/dev/null)" ]; then echo -e "$name\t$w\tclean\t-" >> "$STATE/wip.tsv"; return; fi
  tmp=$(mktemp); rm -f "$tmp"
  head=$(git -C "$w" rev-parse HEAD)
  ( export GIT_INDEX_FILE=$tmp
    cd "$w"
    git read-tree HEAD
    git add -A -- . ':(exclude,glob)**/build/**' ':(exclude,glob)**/.gradle/**' ':(exclude,glob)**/.kotlin/**' \
      ':(exclude,glob)**/node_modules/**' ':(exclude,glob)**/.venv/**' ':(exclude,glob)**/venv/**' ':(exclude,glob)**/__pycache__/**' \
      ':(exclude,glob)**/supabase/.temp/**' ':(exclude,glob)**/.env' ':(exclude,glob)**/.env.*' ':(exclude,glob)**/*.keystore' \
      ':(exclude,glob)**/*.jks' ':(exclude,glob)**/local.properties' ':(exclude,glob)**/*.apk' ':(exclude,glob)**/*.aab' 2>/dev/null
    for f in $(git diff --cached --name-only --diff-filter=AM HEAD); do
      is_env_example "$f" && continue
      sz=$(stat -c %s "$f" 2>/dev/null || echo 0)
      if [ "$sz" -gt $MAXB ]; then git reset -q HEAD -- "$f"; echo -e "wip/$name:$f\tover 50 MB" >> "$STATE/excluded.tsv"; continue; fi
      if git diff --cached HEAD -- "$f" | grep '^+' | grep -Eq "$HARD|$SOFT"; then
        git reset -q HEAD -- "$f"
        echo -e "wip/$name:$f\tsecret-like pattern in changed lines (file left out of snapshot)" >> "$STATE/excluded.tsv"
      fi
      if [[ "$f" == *gradle.properties ]] && grep -Eiq 'key|secret|token|password' "$f"; then git reset -q HEAD -- "$f"; echo -e "wip/$name:$f\tgradle.properties with key-like entries" >> "$STATE/excluded.tsv"; fi
    done
    git write-tree > "$tmp.tree" )
  tree=$(cat "$tmp.tree"); rm -f "$tmp" "$tmp.tree"
  rtree=$(git -C "$w" rev-parse -q --verify "refs/remotes/origin/wip/$name^{tree}" 2>/dev/null || true)
  rpar=$(git -C "$w" rev-parse -q --verify "refs/remotes/origin/wip/$name^" 2>/dev/null || true)
  if [ "$tree" = "$(git -C "$w" rev-parse "HEAD^{tree}")" ]; then echo -e "$name\t$w\tonly ignored/excluded changes\t-" >> "$STATE/wip.tsv"; return; fi
  if [ "$tree" = "$rtree" ] && [ "$rpar" = "$head" ]; then
    echo -e "$name\t$w\tunchanged since last snapshot\t$(git -C "$w" rev-parse --short "refs/remotes/origin/wip/$name")" >> "$STATE/wip.tsv"; echo "wip/$name: unchanged"; return; fi
  commit=$(git -C "$w" -c user.name="Shanivar box backup" -c user.email="box-backup@shanivar.invalid" commit-tree "$tree" -p HEAD -m "WIP snapshot ${br:-detached@$(git -C "$w" rev-parse --short HEAD)} $NOW

Worktree: $w
Base: $head
Uncommitted + untracked (non-ignored) changes captured with a temporary index; the worktree was not touched.
Restore: git checkout <branch> && git cherry-pick --no-commit origin/wip/$name  (or: git diff $head origin/wip/$name | git apply)")
  if push -C "$w" push -q origin "+$commit:refs/heads/wip/$name"; then
    [ $DRY = 0 ] && git -C "$w" update-ref "refs/remotes/origin/wip/$name" "$commit"
    echo -e "$name\t$w\tsnapshot pushed\t$(git -C "$w" rev-parse --short "$commit")" >> "$STATE/wip.tsv"; echo "wip/$name: pushed $commit"
  else echo -e "wip/$name\tpush failed" >> "$STATE/failures.tsv"; echo -e "$name\t$w\tPUSH FAILED\t-" >> "$STATE/wip.tsv"; fi
}
gitp -C "$MAIN" fetch -q origin '+refs/heads/wip/*:refs/remotes/origin/wip/*' 2>/dev/null || true
for w in $(git -C "$MAIN" worktree list --porcelain | sed -n 's/^worktree //p'); do snapshot "$w"; done
for r in "${EXTRA_REPOS[@]}"; do [ -d "$r/.git" ] || continue
  gitp -C "$r" fetch -q origin '+refs/heads/wip/*:refs/remotes/origin/wip/*' 2>/dev/null || true; snapshot "$r"; done

# ---------- 3) box-backup ----------
SOURCES=(
  /workspace/pm /workspace/handoffs /workspace/shanivar-cs /workspace/bin /workspace/logs
  /workspace/campaign /workspace/instagram /workspace/ig-post-2 /workspace/ig-post-3
  /workspace/initiation-shots /workspace/sadhana-shots /workspace/wireframes /workspace/jira-edits
  /workspace/qa /workspace/grok-001 /workspace/shanivar /workspace/site-fix /workspace/site-founder
  /workspace/apk /workspace/shanivar-v3.broken
  /workspace/content-calendar-summary.md /workspace/content-calendar.xlsx /workspace/build_cal.py
  /workspace/shanivar-website.zip /workspace/web-app.zip /workspace/pages.json
  /workspace/qa_build.log /workspace/qa_shots.log /workspace/qa_test.log
  /workspace/uploads/sadhana-v1.zip /workspace/uploads/sadhana-v1b.zip
)
# Shanivar agents' user attachments/assets (agent store.db files are NOT copied: internal state, unscannable)
for a in /home/box/sand-data/agents/*/; do
  n=$(python3 -c "import json;print(json.load(open('$a/profile.json')).get('name',''))" 2>/dev/null)
  case "$n" in *[Ss]hanivar*|Sana) for s in attachments assets; do [ -d "$a$s" ] && SOURCES+=("${a%/}/$s"); done;; esac
done
for d in /cursor/stores/*/; do [ -r "$d" ] && [ -n "$(ls -A "$d" 2>/dev/null)" ] && rg -qil 'shanivar' "$d" 2>/dev/null && SOURCES+=("${d%/}"); done

if [ "$VIS" != PRIVATE ]; then
  echo "Repo is $VIS (not PRIVATE): skipping box-backup (non-repo/marketing content)."; echo -e "box-backup\tskipped, repo $VIS" >> "$STATE/failures.tsv"
else
  mkdir -p "$BK"; cd "$BK"
  [ -d .git ] || { git init -q; git remote add origin "$URL"; }
  if gitp ls-remote --exit-code -q origin refs/heads/box-backup >/dev/null 2>&1; then
    gitp fetch -q origin box-backup && git symbolic-ref HEAD refs/heads/box-backup && git reset -q --mixed FETCH_HEAD
  else git symbolic-ref HEAD refs/heads/box-backup; fi
  rm -rf "$BK/box-backup"; mkdir -p "$BK/box-backup"
  python3 - "$BK/box-backup" "$STATE/excluded.tsv" "$EXCL_RE" $MAXB "${SOURCES[@]}" <<'PY'
import os, re, shutil, sys, zipfile
dst, excl, exre, maxb, srcs = sys.argv[1], sys.argv[2], re.compile(sys.argv[3]), int(sys.argv[4]), sys.argv[5:]
hard = re.compile(rb'eyJhb[G]ci|sb_secre[t]_|gh[p]_[A-Za-z0-9]{20,}|github_pat_|-----BEGIN [A-Z ]*PRIVATE KEY')
soft = re.compile(rb'(service_role|api_key|apikey|password|secret|token)["\']?\s*[=:]\s*["\']?(?!mock|test|example|your|xxx|changeme)[A-Za-z0-9_./+-]{16,}', re.I)
SKIP_NAMES = {'store.db','store.db-shm','store.db-wal','.git'}
out = open(excl, 'a')
def scan(p):
    try:
        if p.endswith('.zip'):
            z = zipfile.ZipFile(p)
            for n in z.namelist():
                if exre.search(n) and not re.search(r'\.env\.(example|template)$', n): return 'archive contains sensitive file ' + n
                d = z.read(n)
                if hard.search(d) or soft.search(d): return 'secret-like pattern inside archive member ' + n
            return None
        with open(p, 'rb') as f: d = f.read()
        if hard.search(d): return 'secret-like pattern (' + hard.search(d).group(0)[:10].decode(errors="replace") + '…)'
        if soft.search(d): return 'secret-like assignment (' + soft.search(d).group(1).decode() + '=…)'
        if p.endswith('gradle.properties') and re.search(rb'(?i)key|secret|token|password', d): return 'gradle.properties with key-like entries'
    except Exception as e: return 'unreadable: %s' % e
    return None
def consider(src, rel):
    if os.path.islink(src): return
    if exre.search(rel) and not re.search(r'\.env\.(example|template)$', rel): out.write(f'{src}\tsecret/build/cache exclusion rule\n'); return
    if re.search(r'emulator', os.path.basename(src), re.I): out.write(f'{src}\temulator file (emulator setup cancelled)\n'); return
    if src.endswith(('.apk', '.aab')): out.write(f'{src}\tAndroid build artifact (reproducible from commit in filename)\n'); return
    sz = os.path.getsize(src)
    if sz > maxb: out.write(f'{src}\tover 50 MB ({sz/1048576:.0f} MB) — raw/large media not uploaded\n'); return
    if '/workspace/logs/' in src + '/' and sz > 1048576: out.write(f'{src}\tlog over 1 MB\n'); return
    why = scan(src)
    if why: out.write(f'{src}\t{why}\n'); return
    t = os.path.join(dst, rel.lstrip('/'))
    os.makedirs(os.path.dirname(t), exist_ok=True); shutil.copy2(src, t)
for s in srcs:
    if not os.path.exists(s): continue
    if os.path.isfile(s): consider(s, s); continue
    for root, dirs, files in os.walk(s):
        keep = []
        for d in dirs:
            full = os.path.join(root, d)
            if os.path.isfile(os.path.join(full, '.git')):   # nested git worktree: covered by the wip/ snapshots
                out.write(f'{full}/\tgit worktree, backed up via wip/ ref instead\n'); continue
            if re.search(r'emulator|\.android|avd', d, re.I):
                out.write(f'{full}/\temulator files (emulator setup cancelled)\n'); continue
            if d in SKIP_NAMES or exre.search(full + '/'):
                if d != '.git': out.write(f'{os.path.join(root, d)}/\tdirectory excluded (build/cache/secret rule)\n')
            else: keep.append(d)
        dirs[:] = keep
        for fn in files:
            if fn in SKIP_NAMES: continue
            p = os.path.join(root, fn); consider(p, p)
PY
fi

# ---------- 4) status doc ----------
gen_status(){
  local o=$1
  cd "$MAIN"
  gitp fetch -q origin --prune '+refs/heads/*:refs/remotes/origin/*' 2>/dev/null || true
  {
  echo "# SHANIVAR_STATUS"
  echo; echo "Generated: **$NOW** (Europe/Berlin) by \`/workspace/bin/snapshot-to-github.sh\` on the agent box. Repo visibility: **$VIS**."
  echo; echo "## Branches"
  echo; echo "| Branch | Ticket | Last commit (Berlin time) | Pushed | wip/ snapshot | Worktree |"; echo "|---|---|---|---|---|---|"
  for b in $(git for-each-ref --format='%(refname:short)' refs/heads/); do
    t=$(echo "$b" | grep -oiE 'dev-?[0-9]+' | head -1 | tr a-z A-Z | sed 's/DEV-\?/DEV-/'); [ -z "$t" ] && t="-"
    lc=$(git log -1 --date=format-local:'%Y-%m-%d %H:%M' --format='`%h` %cd %s' "refs/heads/$b" | cut -c1-110 | sed 's/|/\\|/g')
    r=$(git rev-parse -q --verify "refs/remotes/origin/$b" || true)
    if [ "$r" = "$(git rev-parse refs/heads/$b)" ]; then p=yes; elif [ -n "$(git for-each-ref "refs/remotes/origin/backup/$b-*")" ]; then p="as backup/$b-*"; elif [ -n "$r" ]; then p="no (remote differs)"; else p=no; fi
    wp=$(awk -F'\t' -v n="$b" '$1==n{print $3" "$4}' "$STATE/wip.tsv"); [ -z "$wp" ] && wp="-"
    rw=$(git rev-parse -q --verify "refs/remotes/origin/wip/$b" >/dev/null && echo "wip/$b exists" || echo "none")
    wt=$(git worktree list --porcelain | awk -v b="refs/heads/$b" '/^worktree/{w=$2} $0=="branch "b{print w}')
    echo "| \`$b\` | $t | $lc | $p | $rw ($wp) | ${wt:--} |"
  done
  echo; echo "Other snapshots / remote refs:"; echo
  awk -F'\t' '$1 ~ /^detached-|^shanivar-app/ {print "- `wip/"$1"` from `"$2"`: "$3" "$4}' "$STATE/wip.tsv"
  git for-each-ref --format='- remote `%(refname:short)` `%(objectname:short)` %(subject)' refs/remotes/origin/ | grep -v 'origin/HEAD' | cut -c1-140
  echo "- tags: $(git tag -l | sed 's/.*/`&`/' | paste -sd, -)"
  echo; echo "## Agents and current assignments"; echo
  if [ -f /workspace/pm/agent-status.md ]; then sed '1{/^# /d}' /workspace/pm/agent-status.md; else echo "(no /workspace/pm/agent-status.md)"; fi
  echo; echo "## Uncommitted work captured this run"; echo
  echo "| wip ref | Worktree | State | Commit |"; echo "|---|---|---|---|"
  awk -F'\t' '{print "| `wip/"$1"` | `"$2"` | "$3" | "$4" |"}' "$STATE/wip.tsv"
  echo; echo "## Key pointers"; echo
  echo "- GitHub: https://github.com/$REPO (default branch \`main\`). PR #1: https://github.com/$REPO/pull/1 (DEV-39, open; do not merge without the founder)"
  echo "- Jira: project **DEV** on https://shanivar.atlassian.net"
  echo "- Notion thesis page: https://app.notion.com/p/3fc10ae18c9f8158abd7e59528eee0a6"
  echo "- Notion launch campaign (Sana): https://app.notion.com/p/3fc10ae18c9f81a09007dbbd63dd8c99"
  echo "- Supabase project ref: \`katleeaentaugfszpmus\` (keys are NOT in git; get them from the Supabase dashboard)"
  echo "- Local helpers: \`box-backup/workspace/bin/\` (start-local-supabase.sh, snapshot-to-github.sh)"
  echo; echo "## Restore after a box reset"; echo
  cat <<'EOF'
```bash
cd /workspace
git clone https://github.com/BookPress9/shanivar.git shanivar-v3 && cd shanivar-v3
git fetch origin '+refs/heads/*:refs/remotes/origin/*' --tags
# 1. recreate the worktrees (one per ticket branch)
for b in $(git branch -r --format='%(refname:lstrip=3)' | grep -E '^dev-'); do
  n=$(echo "$b" | grep -oE '^dev-[0-9]+' | tr -d -); git worktree add "/workspace/shanivar-v3-$n" "$b" 2>/dev/null || true
done            # (main checkout itself was on dev-38-outbox-gaps: git checkout dev-38-outbox-gaps)
# 2. re-apply uncommitted work: in each worktree whose branch has a wip/<branch> ref
cd /workspace/shanivar-v3-dev45 && git cherry-pick --no-commit origin/wip/dev-45-sync-engine && git reset -q   # leaves changes uncommitted, like before
#    (same for dev-47, dev-89; detached scratch: git worktree add --detach /workspace/scratch-dev47x45 origin/wip/detached-scratch-dev47x45~1 && cd there && git cherry-pick --no-commit origin/wip/detached-scratch-dev47x45)
# 3. copy the non-repo work back
git -C /workspace/shanivar-v3 worktree add /tmp/box-backup origin/box-backup
cp -a /tmp/box-backup/box-backup/workspace/. /workspace/
cp -a /tmp/box-backup/box-backup/home/box/. /home/box/     # agent attachments (optional)
# 4. secrets: recreate .env / local.properties / keystores by hand (never in git); gh auth login; gh auth setup-git
```
EOF
  echo; echo "## Excluded from the backup (secrets, size, build output)"; echo
  if [ -s "$STATE/excluded.tsv" ]; then sort -u "$STATE/excluded.tsv" | grep -vE '/(build|\.gradle|\.kotlin|node_modules|__pycache__|\.venv)/' | awk -F'\t' '{print "- `"$1"`: "$2}'
    echo "- build/cache dirs skipped: $(sort -u "$STATE/excluded.tsv" | grep -E '/(build|\.gradle|\.kotlin|node_modules|__pycache__|\.venv)/' | cut -f1 | sed 's/.*/`&`/' | paste -sd, - | cut -c1-600)"
  else echo "- nothing"; fi
  echo "- Not copied by design: agent internal \`store.db\` files under /home/box/sand-data/agents, unrelated projects on the box (Mahi, Hybrid Monk, BookPress, paathshala, ShopApotheke), classical-text scrapes (/workspace/src, /workspace/sweep1017, root *.txt/*.html), the Android AVD."
  echo; echo "## Failures this run"; echo
  if [ -s "$STATE/failures.tsv" ]; then awk -F'\t' '{print "- "$1": "$2}' "$STATE/failures.tsv"; else echo "- none"; fi
  echo; echo "## Check at end"; echo
  echo "See the hand-maintained list above, plus:"
  awk -F'\t' '$3 ~ /pushed|unchanged/ {print "- [ ] `"$2"` has uncommitted work (snapshot `wip/"$1"`): commit it on its branch"}' "$STATE/wip.tsv"
  for b in $(git for-each-ref --format='%(refname:short)' refs/heads/); do [ "$b" = main ] && continue
    git merge-base --is-ancestor "refs/heads/$b" refs/remotes/origin/main 2>/dev/null || echo "- [ ] \`$b\` not merged into main yet"; done
  } > "$o"
}

if [ "$VIS" = PRIVATE ]; then
  gen_status "$BK/SHANIVAR_STATUS.md"
  cp /workspace/bin/snapshot-to-github.sh "$BK/snapshot-to-github.sh"
  cat > "$BK/README.md" <<EOF
# box-backup (orphan branch)
Snapshot of Shanivar work that lives on the agent box outside git, mirrored under \`box-backup/<original absolute path>\`.
Start with **SHANIVAR_STATUS.md**. Regenerate with \`/workspace/bin/snapshot-to-github.sh\` (copy kept here as \`snapshot-to-github.sh\`).
EOF
  cd "$BK"; git add -A
  if git diff --cached --quiet && git rev-parse -q --verify HEAD >/dev/null; then echo "box-backup: no content changes"
  else git -c user.name="Shanivar box backup" -c user.email="box-backup@shanivar.invalid" commit -q -m "box-backup snapshot $NOW"; fi
  if [ "$(gitp ls-remote origin refs/heads/box-backup | cut -f1)" != "$(git rev-parse HEAD)" ]; then
    if push push -q origin HEAD:refs/heads/box-backup || push push -q origin +HEAD:refs/heads/box-backup; then
      echo "box-backup: pushed $(git rev-parse --short HEAD) ($(du -sh box-backup | cut -f1))"
    else echo -e "box-backup\tpush failed" >> "$STATE/failures.tsv"; fi
  else echo "box-backup: remote already at $(git rev-parse --short HEAD)"; fi
fi
cp "$STATE"/*.tsv /workspace/logs/ 2>/dev/null; rm -rf "$STATE"
echo "=== done ==="
