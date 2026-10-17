import re,glob,os
ESC = set('$^{}~[]<>|')
def esc_inline(s):
    parts=s.split('`')
    return '`'.join(p if k%2 else _esc(p) for k,p in enumerate(parts))
def _esc(s):
    # keep ** and * and ` markdown; escape other specials; leave URLs alone except no brackets present
    out=[];i=0
    for ch in s:
        if ch in ESC: out.append('\\'+ch)
        else: out.append(ch)
    return ''.join(out)
def esc_line(line):
    m=re.match(r'^(\s*(?:#{1,6} |> |- |\d+\. )?)(.*)$',line)
    pre,rest=m.group(1),m.group(2)
    return pre+esc_inline(rest)
def conv(text):
    lines=text.split('\n'); out=[]; i=0
    while i<len(lines):
        l=lines[i]
        if l.startswith('|') and i+1<len(lines) and re.match(r'^\|[\s\-:|]+\|$',lines[i+1]):
            rows=[]
            while i<len(lines) and lines[i].startswith('|'):
                if not re.match(r'^\|[\s\-:|]+\|$',lines[i]):
                    cells=[c.strip() for c in re.split(r'(?<!\\)\|',lines[i].strip())[1:-1]]
                    rows.append(cells)
                i+=1
            out.append('<table header-row="true">')
            for r in rows:
                out.append('<tr>'+''.join('<td>'+esc_inline(c)+'</td>' for c in r)+'</tr>')
            out.append('</table>')
            continue
        if l.startswith('# '):  # page title -> drop
            i+=1; continue
        out.append(esc_line(l)); i+=1
    return '\n'.join(out).strip()+'\n'
for f in sorted(glob.glob('md/*.md')):
    t=open(f).read()
    title=t.split('\n',1)[0].lstrip('# ').strip()
    open('notion/'+os.path.basename(f),'w').write(conv(t))
    print(os.path.basename(f),'|',title)
