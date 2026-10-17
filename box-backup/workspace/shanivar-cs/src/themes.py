import json,re
d=json.load(open('src/reviews.json'))
themes={'price/subscription/paywall':r'subscri|price|expensive|paywall|pay |paid|premium|money|charg|refund|trial|cost',
'bugs/doesn\'t work/crash':r'bug|crash|not work|doesn.t work|stopped working|broken|glitch|freez|won.t open|error',
'easy to bypass/disable':r'bypass|loophole|uninstall|turn (it )?off|disable|workaround|cheat|easy to get around|override',
'permissions/battery/accessibility':r'permission|battery|accessibility|overlay|drain',
'alarm failed/too harsh':r'didn.t (go off|ring)|did not (go off|ring)|overslept|alarm.*(fail|not)|too loud|annoy',
'ads':r'\bads?\b|advert',
'login/account/sync':r'log ?in|account|sign ?in|sync',
'content repetitive/limited':r'repetit|same (content|thing)|boring|limited content|more content|variety',
'notifications nagging':r'notification|nag|spam'}
res={}
for k,v in d.items():
    rv=v['reviews'];n=len(rv)
    low=[r for r in rv if r['s']<=2]
    dates=sorted(r['d'][:10] for r in rv)
    t={th:sum(1 for r in low if re.search(p,r['t'],re.I)) for th,p in themes.items()}
    res[k]={'n':n,'from':dates[0],'to':dates[-1],'avg':round(sum(r['s'] for r in rv)/n,2),'low_pct':round(100*len(low)/n,1),'low_n':len(low),'themes_low':t,'meta':v['meta']}
    top=sorted(t.items(),key=lambda x:-x[1])[:4]
    print(k,n,dates[0],dates[-1],'avg',res[k]['avg'],'low%',res[k]['low_pct'],top, v['meta']['installs'], v['meta']['inAppProductPrice'])
json.dump(res,open('src/themes.json','w'),indent=1)
