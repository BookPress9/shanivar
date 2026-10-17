import sys,re,subprocess,html,hashlib
url=sys.argv[1]; name=sys.argv[2]
r=subprocess.run(['curl','-sL','-m','25','-A','Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/120 Safari/537.36',url],capture_output=True)
h=r.stdout.decode('utf-8','ignore')
h=re.sub(r'(?is)<(script|style|noscript).*?</\1>','',h)
t=html.unescape(re.sub(r'<[^>]+>',' ',h)); t=re.sub(r'\s+',' ',t)
open(name+'.txt','w').write(url+'\n'+t)
print(name,len(t))
