const p=require('puppeteer-core');(async()=>{const [file,tag]=process.argv.slice(2);const b=await p.launch({executablePath:'/usr/bin/google-chrome',args:['--no-sandbox']});
for(const [w,h,n] of [[390,844,'mobile'],[1440,900,'desktop']]){const pg=await b.newPage();await pg.setViewport({width:w,height:h});await pg.goto('file://'+file,{waitUntil:'networkidle0'}).catch(()=>{});
await pg.evaluate(()=>document.querySelectorAll('.reveal').forEach(n=>n.classList.add('in')));await new Promise(r=>setTimeout(r,800));
const r=await pg.evaluate((W)=>{const o=[];document.querySelectorAll('body *').forEach(e=>{const b=e.getBoundingClientRect();if(b.width&&(b.right>W+1||b.left<-1)){let c=e;while(c&&getComputedStyle(c).overflow!=='hidden'&&getComputedStyle(c).overflowX!=='hidden')c=c.parentElement;if(!c||c===document.body||c===document.documentElement)o.push(e.tagName+'.'+e.className+' L'+Math.round(b.left)+' R'+Math.round(b.right))}});return {sw:document.documentElement.scrollWidth,o:o.slice(0,15),
hero:(()=>{const c=document.getElementById('dust').getBoundingClientRect();return 'canvas '+c.width+'x'+c.height})()}},w);console.log(n,JSON.stringify(r));
await pg.screenshot({path:`/workspace/site-fix/${n}-${tag}.png`,fullPage:true});
await pg.screenshot({path:`/workspace/site-fix/${n}-${tag}-fold.png`});}
await b.close()})()
