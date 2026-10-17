import re
def esc(s):
    s=re.sub(r'(?<!\*)\*(?!\*)', r'\\*', s)
    return s
def cell(s):
    s=s.strip()
    # escape underscores inside identifiers (not _Note:_ markers)
    s=re.sub(r'(?<=[A-Za-z0-9])_(?=[A-Za-z0-9])', r'\\_', s)
    return esc(s)
def convert(md, toggle_h2=False):
    lines=md.split('\n'); out=[]; i=0
    while i<len(lines):
        l=lines[i]
        if l.startswith('|') and i+1<len(lines) and re.match(r'^\|[-| :]+\|$', lines[i+1]):
            rows=[]
            while i<len(lines) and lines[i].startswith('|'):
                if not re.match(r'^\|[-| :]+\|$', lines[i]): rows.append([c.strip() for c in lines[i].strip()[1:-1].split('|')])
                i+=1
            w=len(rows[0]); rows=[(r+['']*w)[:w] for r in rows]
            out.append('<table fit-page-width="true" header-row="true">')
            for r in rows:
                out.append('\t<tr>'+''.join(f'<td>{cell(c)}</td>' for c in r)+'</tr>')
            out.append('</table>')
            continue
        if l.startswith('# '): i+=1; continue  # title handled by page
        out.append(esc(re.sub(r'(?<=[A-Za-z0-9])_(?=[A-Za-z0-9])', r'\\_', l)) if not l.startswith('```') else l)
        i+=1
    return '\n'.join(out)
