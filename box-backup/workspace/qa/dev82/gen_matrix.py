import json, collections, sys
sys.path.insert(0,'.')
from matrix_data import D
meta=json.load(open('/workspace/agent-tools/meta.json'))
EP={'DEV-56':'A — Strategy & measurement','DEV-57':'B — Protection','DEV-58':'C — Routine','DEV-59':'D — Sadhana','DEV-60':'E — Security, privacy & reliability','DEV-61':'F — QA & release','DEV-62':'G — CS & growth',None:'No epic'}
LAB={'AP':'automated-pass','MO':'manual-only','RD':'real-device-only (NOT RUN)','EMU':'emulator-pending','GAP':'no test (gap)','PEND':'pending (in QA)'}
missing=[k for k in meta if k not in D and k not in EP]
by=collections.defaultdict(list)
for k,v in meta.items():
    e=k if k in EP else v[0]
    if k=='DEV-89': e='DEV-59'
    by[e].append(k)
tot=collections.Counter(); per_epic={}
out=[]
out.append("# DEV-82 — AC-to-test matrix (all DEV tickets)\n")
out.append("Generated 2026-10-17 (Europe/Berlin) by Shanivar Tester from Jira JQL `project = DEV ORDER BY key ASC` (89 issues) and the repo worktrees. No source code changed; no tests written for unconfirmed tickets.\n")
out.append("**Builds referenced:** `main@20fb519` = /workspace/shanivar-v3 (dev-38-outbox-gaps, 518 unit tests, 0 fail, 1 skipped = SupabaseSyncTest.e2eGuestPushReadBack, run 21:55); `dev39@8e122a8` (512/0/1, 21:59); `dev42@ba38de6` (507/0/0 Android + pgTAP 01–05 on local stack); `dev45@2fce42d` (in QA now); `dev47@1e69cba` (548/0/1 developer run 22:36, not QA-reviewed); `dev54@7a56cf6` + uncommitted work. Android test paths are under `android-app/app/src/test/java/com/shanivar/nativeapp/`; pgTAP under `supabase/tests/`.\n")
out.append("**Status key:** automated-pass · manual-only (needs a human check; result noted) · real-device-only (needs a physical phone; NOT RUN) · emulator-pending (DEV-71/75 cases planned for the emulator; run when PM confirms it is booted — emulator results never replace the real-device rows) · no test (gap) · pending (in QA now, DEV-45 only). `(derived)` = Jira has no AC section; AC inferred from the description and **returned to the owner to confirm** (DEV-82 edge case). `[DoD]` = row added by QA under the new definition of done (real-device proof for alarms/protection).\n")
for e in ['DEV-56','DEV-57','DEV-58','DEV-59','DEV-60','DEV-61','DEV-62',None]:
    keys=sorted(by.get(e,[]),key=lambda k:int(k.split('-')[1]))
    if not keys: continue
    c=collections.Counter(); out.append(f"\n## Epic {EP[e]}" + (f" ([{e}](https://shanivar.atlassian.net/browse/{e}))" if e else "")+"\n")
    out.append("| Ticket | Jira status | AC | Test (file · class.method / pgTAP / manual) | Status |\n|---|---|---|---|---|")
    for k in keys:
        st=meta[k][1]; title=meta[k][3]
        if k in EP:
            out.append(f"| [{k}](https://shanivar.atlassian.net/browse/{k}) epic container | {st} | — no ACs (container) | — | — |"); continue
        rows,note=D[k]
        for i,(ac,t,s) in enumerate(rows):
            c[s]+=1
            kc=f"[{k}](https://shanivar.atlassian.net/browse/{k}) {title[:60]}" if i==0 else "″"
            out.append(f"| {kc} | {st if i==0 else ''} | {ac} | {t.replace('|','/')} | {LAB[s]} |")
        if note: out.append(f"| ″ | | _Note:_ {note} | | |")
    per_epic[EP[e]]=c; tot+=c
    out.append(f"\n_Epic subtotal:_ {sum(c.values())} ACs — " + ", ".join(f"{LAB[s]} {c[s]}" for s in LAB if c[s]))
hdr=["\n## Summary counts\n","| Epic | ACs | automated-pass | manual-only | real-device-only (NOT RUN) | emulator-pending | no test (gap) | pending |","|---|---|---|---|---|---|---|---|"]
for ep,c in per_epic.items(): hdr.append(f"| {ep} | {sum(c.values())} | {c['AP']} | {c['MO']} | {c['RD']} | {c['EMU']} | {c['GAP']} | {c['PEND']} |")
hdr.append(f"| **Total** | **{sum(tot.values())}** | **{tot['AP']}** | **{tot['MO']}** | **{tot['RD']}** | **{tot['EMU']}** | **{tot['GAP']}** | **{tot['PEND']}** |")
derived=sum(1 for k,(r,n) in D.items() if r and r[0][0].startswith('(derived)'))
hdr.append(f"\nRows: {sum(tot.values())} = 315 Jira/derived ACs + 28 planned DEV-71/DEV-75 device cases (17 + 15 rows, minus the 4 umbrella ACs already counted).")
hdr.append(f"\nTickets: {len(meta)} (82 work tickets + 7 epic containers). Tickets whose ACs were derived because Jira has no AC section: {derived}.")
open('ac_test_matrix.md','w').write("\n".join(out[:4]+hdr+out[4:])+"\n")
json.dump({'tot':tot,'per_epic':per_epic,'derived':derived,'missing':missing},open('counts.json','w'),indent=1)
print(missing, dict(tot), sum(tot.values()), derived)
