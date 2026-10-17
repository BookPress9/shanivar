#!/usr/bin/env bash
# Bring the LOCAL Supabase stack back after a box restart (dockerd is not a system service on the box).
# Usage: scripts/dev/start-local-supabase.sh [repo-or-worktree-dir] [--reset] [--test]
#   --reset  supabase db reset (re-applies supabase/migrations of that dir; wipes local data)
#   --test   supabase test db (pgTAP in supabase/tests)
# Local only: never links, pushes or talks to a cloud project. Prints URLs only, never keys.
set -euo pipefail

DIR=""; RESET=0; TEST=0
for a in "$@"; do
  case "$a" in
    --reset) RESET=1 ;;
    --test) TEST=1 ;;
    -h|--help) sed -n '2,7p' "$0"; exit 0 ;;
    *) DIR="$a" ;;
  esac
done
if [ -z "$DIR" ]; then DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"; fi
[ -f "$DIR/supabase/config.toml" ] || { echo "no supabase/config.toml in $DIR" >&2; exit 1; }

need() { command -v "$1" >/dev/null 2>&1 || { echo "missing: $1 ($2)" >&2; exit 1; }; }
need docker "sudo apt-get install -y docker.io"
need supabase "download https://github.com/supabase/cli/releases (v2.120.0 used for DEV-42/45) to /usr/local/bin"

# 1. dockerd (no systemd on the box)
if ! docker info >/dev/null 2>&1; then
  if ! pgrep -x dockerd >/dev/null 2>&1; then
    echo "starting dockerd (log: /tmp/dockerd.log)"
    sudo sh -c 'nohup dockerd > /tmp/dockerd.log 2>&1 &'
  fi
  for _ in $(seq 1 30); do sudo docker info >/dev/null 2>&1 && break; sleep 1; done
  sudo docker info >/dev/null 2>&1 || { echo "dockerd did not come up; see /tmp/dockerd.log" >&2; exit 1; }
  # let the box user talk to the daemon without sudo (box-local socket)
  sudo chmod 666 /var/run/docker.sock
fi

# 2. container-to-container traffic (legacy iptables FORWARD policy is DROP on the box; docs/SUPABASE_E2E.md)
sudo iptables -P FORWARD ACCEPT 2>/dev/null || true
sudo iptables-legacy -P FORWARD ACCEPT 2>/dev/null || true

# 3. the stack (one stack per project_id; all worktrees share project_id "shanivar-v3", so one at a time)
cd "$DIR"
if supabase status >/dev/null 2>&1; then
  echo "local Supabase already running"
else
  supabase start >/tmp/supabase-start.log 2>&1 || { echo "supabase start failed; see /tmp/supabase-start.log (contains local dev keys)" >&2; exit 1; }
fi
[ "$RESET" = 1 ] && { echo "db reset from $DIR/supabase/migrations"; supabase db reset >/dev/null; }
[ "$TEST" = 1 ] && supabase test db

# URLs only. Keys: `supabase status -o env` (local demo keys; do not paste them anywhere).
supabase status -o env 2>/dev/null | grep -E '^(API_URL|DB_URL|STUDIO_URL|MAILPIT_URL)=' || true
echo "ok: local Supabase up for $DIR"
