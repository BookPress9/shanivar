import json,sys
from google_play_scraper import reviews, Sort, app
apps={'opal':'com.withopal.opal','alarmy':'droom.sleepIfUCan','onesec':'wtf.riedel.onesec','screenzen':'com.screenzen','freedom':'to.freedom.android2','forest':'cc.forestapp','fabulous':'co.thefabulous.app','sleepandroid':'com.urbandroid.sleep','habitica':'com.habitrpg.android.habitica','headspace':'com.getsomeheadspace.android','calm':'com.calm.android','insight':'com.spotlightsix.zentimerlite2','wakingup':'org.wakingup.android','medito':'meditofoundation.medito','balance':'com.elevatelabs.geonosis'}
out={}
for k,p in apps.items():
    try:
        r,_=reviews(p,lang='en',country='us',sort=Sort.NEWEST,count=500)
        a=app(p,lang='en',country='us')
        out[k]={'meta':{x:a.get(x) for x in ['title','installs','realInstalls','score','ratings','reviews','histogram','free','offersIAP','inAppProductPrice','updated','developer']},
                'reviews':[{'s':x['score'],'t':x['content'],'d':str(x['at']),'up':x['thumbsUpCount']} for x in r]}
        print(k,len(r),a.get('realInstalls'),a.get('ratings'),flush=True)
    except Exception as e: print(k,'ERR',e)
json.dump(out,open('src/reviews.json','w'))
