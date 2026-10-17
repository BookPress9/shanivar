const $=s=>document.querySelector(s);
const esc=s=>String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));

const DEFAULT_APPS=[
  'Instagram','Facebook','X / Twitter','Threads','Snapchat','LinkedIn',
  'YouTube','TikTok','Pinterest','Reddit','Discord','Telegram',
  'Netflix','Prime Video','Hotstar','Twitch','Chrome','Play Games'
];

const APP_GROUPS={
  'Social':['Instagram','Facebook','X / Twitter','Threads','Snapchat','LinkedIn'],
  'Feeds & video':['YouTube','TikTok','Pinterest'],
  'Communities':['Reddit','Discord','Telegram'],
  'Entertainment':['Netflix','Prime Video','Hotstar','Twitch'],
  'Easy escape routes':['Chrome','Play Games']
};

const newWindow=(start='09:00',end='10:30',repeat='Weekdays')=>({
  id:Date.now()+Math.random(),start,end,repeat,customDays:[1,2,3,4,5]
});

const newActivity=(title='Personal activity',time='18:00',duration=30,repeat='Every day')=>({
  id:Date.now()+Math.random(),title,time,duration,repeat,customDays:[1]
});

const initial=()=>({
  screen:'welcome',wake:'07:00',bed:'23:00',alarm:false,locks:false,permission:false,
  apps:['Instagram','YouTube'],windows:[newWindow()],activities:[newActivity('Sadhana','07:30',1)],chapter:0,safe:false,paused:false,
  remaining:60,session:false,records:[],feedback:'',independent:false,reminders:false,
  protocol:0,error:'',back:'sadhana'
});

let state=initial(),ticker=null;

const chapters=[
  ['Arrive','Stop running on automatic.','Foundation'],
  ['Return','Notice the drift. Come back on purpose.','Repetition'],
  ['Understand','See the pattern before you obey it.','Clarity'],
  ['Make it yours','Use the practice without needing the app.','Independence'],
  ['Deepen','Study the source. Choose depth without theatre.','Study']
];

const protocols=[
  {title:'Daylight & a steady wake time',tag:'Daily rhythm',why:'Give the day a repeatable physical starting point.',cue:'When daylight and your setting allow, spend a little time outdoors. Never look directly at the sun. Adapt to access, weather and your schedule.',measure:'Optional self-report: outdoors / adapted / skipped.',source:'Huberman Lab sleep toolkit; NHLBI sleep guidance. The time follows the user’s chosen routine, not a claimed biological optimum.',url:'https://www.hubermanlab.com/newsletter/toolkit-for-sleep'},
  {title:'Steady · natural breathing',tag:'Practice',why:'Train one clean repetition: attention moves, you return.',cue:'Do not pace, deepen or manipulate the breath. Notice the breathing already happening. When attention moves, return to the next natural breath.',measure:'Attempt and elapsed time only. No breath-compliance score and no claim about mental state.',source:'Shanivar adaptation using natural breathing as a low-intensity attention anchor; retention and subtle-body instructions are omitted.',url:'RESEARCH.md#sadhana-and-practice-boundary'},
  {title:'A quieter end to the day',tag:'Rest',why:'Close the day without stealing from sleep.',cue:'Choose a quieter activity before your intended sleep time and lower unnecessary stimulation when practical.',measure:'Optional routine check-in. Sleep quality is not inferred from phone behavior.',source:'NHLBI healthy sleep habits.',url:'https://www.nhlbi.nih.gov/health/sleep-deprivation/healthy-sleep-habits'},
  {title:'One action that matters',tag:'Reflection',why:'Turn insight into one concrete action.',cue:'Name one thing that matters today. Choose one action small enough to actually do.',measure:'User-reported action attempted. No required journal text.',source:'Original Shanivar wording informed by agency/value-oriented reflection patterns. Not a diagnostic tool.',url:'RESEARCH.md#reflection-boundary'},
  {title:'Movement & connection',tag:'Daily life',why:'Push the practice back into real life.',cue:'Choose a comfortable movement break or a deliberate moment with someone you want to be present with.',measure:'Optional self-report. No calorie or performance target.',source:'General daily-life practice direction; not a clinical exercise prescription.',url:'RESEARCH.md#product-foundation'}
];

const screens={
  welcome:['01 · Welcome','Routine and Sadhana only. The opening is a challenge to act, not a wellness dashboard.'],
  rhythm:['02 · Your rhythm','Only wake and intended sleep anchors. No schedule persona or body-clock claim.'],
  setup:['03 · Choose setup','Wake alarm and app protection are optional tools configured once and edited later.'],
  routine:['Routine','Personal growth is scheduled and trackable instead of left to memory and mood.'],
  routineEdit:['Edit Routine','Edit the day as one followable personal calendar: anchors, activities, times and recurrence.'],
  alarm:['Alarm setup','Save an explicit wake alarm while preserving an accessible non-physical dismissal path.'],
  locks:['App protection','Choose distracting apps once and create multiple repeated protection windows across the day.'],
  permissions:['Device access','Ask for system capability only after the user chooses app protection.'],
  recovery:['Essential access','Protection must always have an immediate local exit.'],
  sadhana:['Sadhana','A training path toward self-initiated practice; the progression is intentionally light until soft gamification is designed.'],
  chapter:['Chapter detail','Direct concept → real-life application → one practice.'],
  ready:['Practice setup','Shanivar chooses natural breathing as the foundation anchor and explains why.'],
  practice:['Steady player','Natural breathing only. Saturn motion marks the practice without pacing the breath.'],
  reflection:['After practice','Record only what is useful. Completion is not proof of a mental state.'],
  audit:['Practice record','Separate app-observed attempts from self-report.'],
  independent:['Independent practice','Needing fewer prompts is a valid outcome.'],
  protocols:['Practice library','Sources and adaptations remain visible.'],
  protocol:['Protocol detail','Purpose, action, measurement and boundary.'],
  source:['Source & adaptation','Keep tradition, modern synthesis and product adaptation distinct.'],
  settings:['Preferences','Control reminders and local demo history.']
};

const groups={
  routine:['routine','routineEdit','rhythm','setup','alarm','locks','permissions','recovery','settings'],
  sadhana:['sadhana','chapter','ready','practice','reflection','audit','independent','protocols','protocol','source']
};

const btn=(text,act,cls='primary',attrs='')=>`<button class="${cls}" data-act="${act}" ${attrs}>${text}</button>`;
const go=(text,screen,cls='secondary')=>`<button class="${cls}" data-go="${screen}">${text}</button>`;
const head=(eye,title,sub='')=>`<header><p class="eyebrow">${eye}</p><h1 tabindex="-1">${title}</h1>${sub?`<p class="lede">${sub}</p>`:''}</header>`;
const back=(screen='routine')=>`<div class="topline">${go('← Back',screen,'text')}<span class="tag">SHANIVAR</span></div>`;
const steps=n=>`<div class="steps" aria-label="Step ${n} of 3">${[1,2,3].map(i=>`<i class="${i<=n?'on':''}"></i>`).join('')}</div>`;
const field=(label,id,value,type='time')=>`<label>${label}<input id="${id}" type="${type}" value="${esc(value)}"></label>`;
const icon=n=>`<svg viewBox="0 0 24 24" aria-hidden="true">${n==='routine'?'<rect x="4" y="5" width="16" height="16" rx="3"/><path d="M8 3v5m8-5v5M4 11h16m-11 5h6"/>':'<circle cx="12" cy="12" r="7"/><ellipse cx="12" cy="12" rx="11" ry="4" transform="rotate(-30 12 12)"/>'}</svg>`;

function navigate(screen){
  if(!screens[screen])screen='routine';
  if(state.screen==='practice'&&screen!=='practice')pauseTimer();
  state.screen=screen;state.error='';render();
  $('#app').scrollTop=0;$('#app h1')?.focus({preventScroll:true});
}
function pauseTimer(){clearInterval(ticker);ticker=null;state.paused=true;}
function currentCue(){
  const elapsed=60-state.remaining;
  if(elapsed<15)return 'Do not improve the breath. Notice the one already happening.';
  if(elapsed<30)return 'Attention moved. Return to the next breath. No argument.';
  if(elapsed<45)return 'Feel it arrive. Feel it leave. Do not control it.';
  return 'One more return. This is the work.';
}
function updatePractice(){
  const timer=$('#timer'),cue=$('#practice-cue');
  if(timer)timer.textContent=formatTimer(state.remaining);
  if(cue)cue.textContent=currentCue();
}
function formatTimer(seconds){
  const value=Math.max(0,seconds);
  return `${Math.floor(value/60)}:${String(value%60).padStart(2,'0')}`;
}
function tick(){
  clearInterval(ticker);state.paused=false;
  ticker=setInterval(()=>{
    if(state.remaining>0)state.remaining--;
    updatePractice();
    if(state.remaining===0)endPractice('Completed Steady');
  },1000);
}
function endPractice(reason){
  pauseTimer();
  if(state.session){
    state.records.push({id:Date.now(),kind:reason,seconds:60-state.remaining,origin:'App timer',report:'No answer'});
    state.session=false;
  }
  state.feedback='';navigate('reflection');
}

function repeatOptions(value){
  return ['Every day','Weekdays','Weekends','Custom week'].map(v=>`<option ${value===v?'selected':''}>${v}</option>`).join('');
}
function customWeek(win,index){
  if(win.repeat!=='Custom week')return '';
  return `<div class="week-picker" aria-label="Choose days">${['Mon','Tue','Wed','Thu','Fri','Sat','Sun'].map((d,i)=>`<button type="button" class="day-chip ${win.customDays.includes(i+1)?'selected':''}" data-window-day="${index}" data-day="${i+1}" aria-pressed="${win.customDays.includes(i+1)}">${d}</button>`).join('')}</div>`;
}
function protectionWindow(win,index){
  return `<div class="window-card ${index%2?'light':'dark'}">
    <div class="row"><div><p class="micro">PROTECTION WINDOW ${index+1}</p><h3>${index===0?'First boundary':'Another boundary'}</h3></div>${index===0?'':`<button class="text danger-text" data-remove-window="${index}">Remove</button>`}</div>
    <div class="metrics compact">${field('From',`start-${index}`,win.start)}${field('Until',`end-${index}`,win.end)}</div>
    <label>Repeat<select id="repeat-${index}" data-repeat="${index}">${repeatOptions(win.repeat)}</select></label>
    ${customWeek(win,index)}
  </div>`;
}

function activityRepeatOptions(value){
  return ['Every day','Weekly','Custom'].map(v=>`<option ${value===v?'selected':''}>${v}</option>`).join('');
}
function activityDays(activity,index){
  if(activity.repeat==='Every day')return '';
  if(activity.repeat==='Weekly')return `<label>Day<select id="activity-day-${index}">${['Mon','Tue','Wed','Thu','Fri','Sat','Sun'].map((d,i)=>`<option value="${i+1}" ${activity.customDays[0]===i+1?'selected':''}>${d}</option>`).join('')}</select></label>`;
  return `<div class="week-picker" aria-label="Choose activity days">${['Mon','Tue','Wed','Thu','Fri','Sat','Sun'].map((d,i)=>`<button type="button" class="day-chip ${activity.customDays.includes(i+1)?'selected':''}" data-activity-day="${index}" data-day="${i+1}" aria-pressed="${activity.customDays.includes(i+1)}">${d}</button>`).join('')}</div>`;
}
function activityEditor(activity,index){
  return `<div class="window-card ${index%2?'dark':'light'} activity-card">
    <div class="row"><p class="micro">PERSONAL ACTIVITY ${index+1}</p><button class="text danger-text" data-remove-activity="${index}">Remove</button></div>
    <label>Activity<input id="activity-title-${index}" value="${esc(activity.title)}" maxlength="50"></label>
    <div class="metrics compact">${field('Start',`activity-time-${index}`,activity.time)}<label>Duration<select id="activity-duration-${index}">${[1,5,10,15,20,30,45,60,90].map(v=>`<option value="${v}" ${activity.duration===v?'selected':''}>${v} min</option>`).join('')}</select></label></div>
    <label>Occurrence<select id="activity-repeat-${index}" data-activity-repeat="${index}">${activityRepeatOptions(activity.repeat)}</select></label>
    ${activityDays(activity,index)}
  </div>`;
}
function occurrenceLabel(activity){
  if(activity.repeat==='Every day')return 'EVERY DAY';
  const names=['','MON','TUE','WED','THU','FRI','SAT','SUN'];
  if(activity.repeat==='Weekly')return `WEEKLY · ${names[activity.customDays[0]]}`;
  return activity.customDays.map(d=>names[d]).join(' · ');
}
function clockOrder(time,wake){
  const [h,m]=time.split(':').map(Number),[wh,wm]=wake.split(':').map(Number);
  return (h*60+m-(wh*60+wm)+1440)%1440;
}
function routineTimedEvents(s){
  const events=s.windows.map((w,i)=>({time:w.start,html:`<div class="event"><div class="event-line"><time>${w.start}–${w.end} · ${w.repeat.toUpperCase()}</time><span class="state-chip ${s.locks?(s.permission?'ready':'attention'):'off'}">${s.locks?(s.permission?'READY':'NEEDS ACCESS'):'NOT SET'}</span></div><h3>Protect the work.</h3><p>${s.apps.length} selected apps · window ${i+1}</p>${go('Edit protection','locks','text')}</div>`}));
  s.activities.forEach((a,i)=>events.push({time:a.time,html:`<div class="event"><div class="event-line"><time>${a.time} · ${occurrenceLabel(a)}</time><span class="state-chip ready">${a.duration} MIN</span></div><h3>${esc(a.title)}</h3><p>Personal activity · editable in Routine</p>${go('Edit activity','routineEdit','text')}</div>`}));
  return events.sort((a,b)=>clockOrder(a.time,s.wake)-clockOrder(b.time,s.wake)).map(e=>e.html).join('');
}

function body(){
  const s=state,chapter=chapters[s.chapter];
  const attempts=s.records.filter(r=>r.origin==='App timer').length;
  const own=s.records.filter(r=>r.origin==='Self-report').length;
  switch(s.screen){
    case 'welcome':return steps(1)+head('DISCIPLINE FOR REAL LIFE','Stop leaving your life<br>to chance.','Work gets calendars. Deadlines get reminders. Other people get your structure. Shanivar starts giving some of it back to you.')+
      `<div class="stack"><div class="card light"><p class="micro">ROUTINE</p><h2>Build the day you intend to live.</h2><p>Wake. Focus. Protect time. Put your personal work on the calendar.</p></div><div class="card black"><p class="micro">SADHANA</p><h2>Do the work. Need fewer prompts.</h2><p>Train attention until the practice can leave the phone and enter your life.</p></div></div>`+
      go('Build my rhythm','rhythm','primary')+`<p class="manifesto">Shanivar is not here to keep you busy. It is here to make what matters harder to ignore.</p>`;

    case 'rhythm':return back('welcome')+steps(2)+head('YOUR DAY','Give the day<br>two fixed points.','Not an ideal morning. Your actual life.')+
      `<div class="card light stack">${field('I usually wake around','wake',s.wake)}${field('I make room for sleep around','bed',s.bed)}</div>`+
      `<p class="manifesto">We use your chosen times to place daily performance anchors and keep enough room for sleep. The rest of the day can be built around reality.</p>`+
      `${s.error?`<p class="error" role="alert">${s.error}</p>`:''}`+btn('Use these times','saveRhythm')+go('Set this later','setup','text');

    case 'setup':return back('rhythm')+steps(3)+head('SET THE GUARDRAILS','Set it once.<br>Then get on with life.','Two tools. Both editable. Neither should need your attention every day.')+
      `<div class="card light"><div class="row"><div><p class="micro">WAKE ALARM</p><h3>Start when you said you would.</h3><p>${s.alarm?`Alarm set for ${s.wake}.`:`Use your ${s.wake} anchor if you want Shanivar to wake you.`}</p></div>${go(s.alarm?'Edit':'Set up','alarm','text dark-link')}</div></div>`+
      `<div class="card black"><div class="row"><div><p class="micro">APP PROTECTION</p><h3>Choose what loses access to you.</h3><p>${s.locks?`${s.windows.length} protection window${s.windows.length===1?'':'s'} saved.`:'Choose distracting apps once. Create the windows that fit your day.'}</p></div>${go(s.locks?'Edit':'Set up','locks','text')}</div></div>`+
      `<p class="manifesto">This is not discipline theatre. Remove the decision when you already know what the right decision is.</p>`+
      go('Go to Routine','routine','primary');

    case 'routine':return `<div class="topline"><span class="eyebrow">ROUTINE</span>${go('Preferences','settings','text')}</div>`+
      head('YOUR PERSONAL CALENDAR','Your personal life<br>deserves structure too.')+
      `<div class="routine-tools"><span class="tag">WAKE ${s.wake} · SLEEP ${s.bed}</span>${go('Edit Routine','routineEdit','secondary compact-button')}</div>`+
      `<div class="card light statement"><p>You follow calendars for meetings, deadlines and other people. Then you expect your own growth to survive on memory and mood.</p><h2>That is backwards.</h2><p>Your personal life carries the bigger work. Put it on the calendar. Make it trackable. Start today.</p></div>`+
      `<div class="timeline dark-panel"><div class="event"><div class="event-line"><time>${s.wake} · WAKE</time><span class="state-chip ${s.alarm?'ready':'off'}">${s.alarm?'SAVED':'OPTIONAL'}</span></div><h3>Get up. Meet the day.</h3><p>Daylight when practical.</p>${go('Alarm settings','alarm','text')}</div>`+
      routineTimedEvents(s)+
      `<div class="event"><div class="event-line"><time>${s.bed} · SLEEP</time><span class="state-chip off">ANCHOR</span></div><h3>Close the day properly.</h3><p>Keep enough room for sleep. Tomorrow needs you functional.</p></div></div>`+
      go('Manage setup','setup','secondary')+go('Sources behind these anchors','source','text');

    case 'routineEdit':return back('routine')+head('EDIT ROUTINE','Build a calendar<br>you can follow.','Change the anchors. Add the personal work. Decide when it happens.')+
      `<div class="card light stack"><p class="micro">DAY ANCHORS</p>${field('I usually wake around','routine-wake',s.wake)}${field('I make room for sleep around','routine-bed',s.bed)}<p class="hint dark-hint">These organize the calendar. Your wake alarm remains a separate choice.</p></div>`+
      `<div class="section-title"><h3>Personal activities</h3><p>Gym. Read a book. Extend Sadhana. Add what deserves a real place in the day.</p></div>`+
      `<div class="quick-add" aria-label="Quick add activities">${['Gym','Read a book','Extend Sadhana'].map(title=>`<button class="choice" data-add-activity="${title}">+ ${title}</button>`).join('')}</div>`+
      `<div id="activities" class="stack">${s.activities.map(activityEditor).join('')}</div>`+
      `<div class="add-row"><button class="plus" data-act="addActivity" aria-label="Add a personal activity">+</button></div>`+
      `${s.error?`<p class="error" role="alert">${s.error}</p>`:''}`+btn('Save my Routine','saveRoutine')+go('Cancel','routine','text');

    case 'alarm':return back('setup')+head('ROUTINE / ALARM','Start when<br>you said you would.')+`<div class="card light">${field('Wake alarm','alarmTime',s.wake)}<label class="check dark-check"><input id="alarmEnabled" type="checkbox" ${s.alarm?'checked':''}>Enable this alarm in the demo</label></div><div class="card black"><h3>Dismissal must stay humane.</h3><p>A native build can ask you to stand or move, but an accessible non-physical exit remains available. This browser preview makes no sound.</p></div>`+btn('Save alarm','saveAlarm')+go('Leave alarm off','setup','text');

    case 'locks':return back('routine')+head('ROUTINE / APP PROTECTION','Choose what<br>gets cut off.','You already know which apps take more than they give. Select them once. Shanivar handles the windows.')+
      Object.entries(APP_GROUPS).map(([group,apps],groupIndex)=>`<section class="app-group ${groupIndex%2?'light-surface':'dark-surface'}"><p class="micro">${group.toUpperCase()}</p><div class="app-grid">${apps.map(a=>`<label class="app-check"><input type="checkbox" name="apps" value="${esc(a)}" ${s.apps.includes(a)?'checked':''}>${esc(a)}</label>`).join('')}</div></section>`).join('')+
      `<div class="section-title"><h3>Protection windows</h3><p>Morning. Work. Night. Add the boundaries your 24-hour cycle needs.</p></div>`+
      `<div id="windows" class="stack">${s.windows.map(protectionWindow).join('')}</div>`+
      `<div class="add-row"><button class="plus" data-act="addWindow" aria-label="Add another protection window">+</button></div>`+
      `<p class="hint">Essential access must remain available. A native build should show real device status rather than pretending a saved rule is active.</p>`+
      `${s.error?`<p class="error" role="alert">${s.error}</p>`:''}`+btn('Save & check device access','saveLocks')+go('Essential access & recovery','recovery','text')+btn('Turn protection off','offLocks','text');

    case 'permissions':return back('locks')+head('DEVICE ACCESS','One system<br>check remains.')+`<div class="card light"><h3>Usage access & overlay</h3><p>Needed on supported Android builds to detect selected apps and show the protection screen. The native app must recheck the real system status when you return.</p><span class="tag dark-tag">${s.permission?'Granted in simulation':'Not granted'}</span></div>`+btn('Simulate granting access','grant')+btn('Continue without access','deny','secondary')+`<p class="hint">Your rules stay saved if you decline. Shanivar should say “Needs device access,” not pretend you are protected.</p>`;

    case 'recovery':return back('routine')+head('ESSENTIAL ACCESS','You can always<br>step out.')+`<div class="card light"><h3>Stop protection now.</h3><p>No waiting. No payment. No forced practice. No server dependency.</p></div>`+btn('Stop protection','recover','danger')+go('Return to Routine','routine','secondary');

    case 'sadhana':return `<div class="topline"><span class="eyebrow">SADHANA</span>${go('Practice record','audit','text')}</div>`+head('TRAIN THE RETURN','Practice until<br>the prompt is unnecessary.','The app is training wheels. The work is what survives when the screen is gone.')+
      `<div class="card light hero"><p class="micro">FOUNDATION PRACTICE</p><h2>Steady · Natural breath</h2><p>One minute. One clean repetition: attention moves, you return.</p>${go('Begin Steady','ready','primary dark-primary')}</div>`+
      `<div class="path-note"><strong>Current Sadhana path</strong><p>Kept intentionally light. We will design the soft gamification, milestones and progression next instead of inheriting the old system by accident.</p></div>`+
      `<div>${chapters.map((c,i)=>`<button class="chapter ${i===s.chapter?'current':''}" data-chapter="${i}"><span class="num">0${i+1}</span><div><h3>${c[0]}</h3><p>${c[2]} · ${c[1]}</p></div><span class="chev">↗</span></button>`).join('')}</div>`+go('Practice library & sources','protocols')+go('Practice without the app','independent','text');

    case 'chapter':return back('sadhana')+head(`CHAPTER 0${s.chapter+1} / ${chapter[2].toUpperCase()}`,chapter[0],chapter[1])+`<div class="card ${s.chapter%2?'black':'light'}"><p class="micro">THE POINT</p><h2>${['Stop long enough to notice what is already happening.','The return matters more than pretending you never drift.','A pattern becomes useful when you can see it before acting.','The practice is working when you can carry it without the player.','Depth starts with source, context and better questions.'][s.chapter]}</h2></div><div class="card ${s.chapter%2?'light':'black'}"><p class="micro">USE IT TODAY</p><p>${['Before the next task, take one minute with natural breathing.','Catch one automatic reach for a feed and return to the thing you chose.','Before one familiar reaction, ask what happened immediately before it.','Use Steady once without opening Shanivar, then continue your day.','Open one source and separate what it says from what Shanivar adapts.'][s.chapter]}</p></div>`+(s.chapter<4?go(s.chapter===3?'Practice independently':'Do the practice',s.chapter===3?'independent':'ready','primary'):go('Open the source library','protocols','primary'))+go('Back to Sadhana','sadhana','text');

    case 'ready':return back('sadhana')+head('STEADY / FOUNDATION','Use the breath<br>you already have.','You are not here to become good at breathing. Your breath already knows its job. For one minute, use it as the place you return to.')+`<div class="card light"><p class="micro">THE METHOD</p><h2>Natural breath. Nothing else.</h2><p>Do not make it deeper. Do not slow it down. Do not count it. Attention will move. When it does, return to the next breath.</p></div><label class="check readiness"><input id="safe" type="checkbox" ${s.safe?'checked':''}>I am stationary in a suitable place. I am not driving or operating equipment.</label>`+btn('I’m in place. Begin.','begin','primary',s.safe?'':'disabled')+go('Not now','sadhana','text');

    case 'practice':return `<div class="topline"><span class="eyebrow">STEADY / ${s.paused?'PAUSED':'PRACTISING'}</span>${btn('End','end','text')}</div><div class="practice"><h1 tabindex="-1">Return. Again.</h1><div class="saturn" aria-hidden="true"><div class="saturn-orbit ${s.paused?'paused':''}"><i></i></div><div class="saturn-body"></div><div class="saturn-ring"></div></div><p class="practice-cue" id="practice-cue">${currentCue()}</p><p class="timer" id="timer">${formatTimer(s.remaining)}</p><p class="hint">The planet moves. Your breath does not follow it. No pacing. No target state.</p></div>`+btn(s.paused?'Resume':'Pause',s.paused?'resume':'pause','secondary')+btn('Source & adaptation','practiceSource','text');

    case 'reflection':return head('PRACTICE DONE','What actually<br>happened?','No gold star. No required feeling. Record what is useful and move on.')+`<div class="card light"><p class="micro">OPTIONAL CHECK-IN</p><div class="stack">${['Clearer','No change','Uncomfortable'].map(f=>`<button class="choice light-choice ${s.feedback===f?'selected':''}" data-feedback="${f}" aria-pressed="${s.feedback===f}">${f}</button>`).join('')}</div></div>${s.feedback==='Uncomfortable'?'<p class="banner">Stop here. Do not force another round. Use ordinary surroundings or leave the practice; persistent or severe distress deserves appropriate support.</p>':'<p class="manifesto">The next action should probably happen outside Shanivar.</p>'}`+go('Return to Routine','routine','primary')+go('View practice record','audit','text');

    case 'audit':return back('sadhana')+head('PRACTICE RECORD','Evidence.<br>Not mythology.')+`<div class="metrics"><div class="metric light-metric"><strong>${attempts}</strong><span>timed attempts</span></div><div class="metric dark-metric"><strong>${own}</strong><span>independent reports</span></div></div><p class="hint">Minutes do not measure awareness. Self-report stays self-report.</p><div>${s.records.length?s.records.map(r=>`<div class="audit-row"><div class="row"><h3>${esc(r.kind)}</h3><button class="text" data-delete="${r.id}" aria-label="Delete ${esc(r.kind)} record">Delete</button></div><p>${esc(r.origin)}${r.seconds!==null?' · '+r.seconds+' sec':''} · ${esc(r.report)}</p></div>`).join(''):'<div class="empty card light"><h3>No record yet.</h3><p>Do a practice or log one you did without the app.</p></div>'}</div>`+btn('Log an independent practice','logOwn')+`<div class="audit-actions">${btn('Download record','export','text')}${btn('Clear history','clear','text')}</div>`;

    case 'independent':return back('sadhana')+head('MAKE IT YOURS','Put the phone down<br>and do the work.')+`<div class="card black"><h3>One cue you already know.</h3><p>At a transition, notice the natural breath. When attention moves, return once. Then choose your next action.</p></div><label class="check readiness"><input id="independent" type="checkbox" ${s.independent?'checked':''}>Use independent mode and turn practice reminders off.</label>`+btn('Save preference','saveIndependent')+btn('I practised without the app','logOwn','secondary')+go('Guidance is still here','ready','text');

    case 'protocols':return back('sadhana')+head('PRACTICE LIBRARY','Know what you are doing.<br>Know where it came from.','No protocol becomes sacred because it is inside an app.')+`<div>${protocols.map((p,i)=>`<button class="chapter" data-protocol="${i}"><span class="num">${i+1}</span><div><h3>${p.title}</h3><p>${p.tag}</p></div><span class="chev">↗</span></button>`).join('')}</div><p class="hint">Traditional context, modern educational material and Shanivar adaptation stay separate.</p>`;

    case 'protocol':{const p=protocols[s.protocol];return back('protocols')+head(p.tag.toUpperCase(),p.title,p.why)+`<div class="card ${s.protocol%2?'black':'light'}"><p class="micro">DO</p><p>${p.cue}</p></div><div class="card ${s.protocol%2?'light':'black'}"><p class="micro">WHAT WE RECORD</p><p>${p.measure}</p></div><details open><summary>Source & boundary</summary><p>${p.source}</p><a href="${p.url}" target="_blank" rel="noopener">Read source ↗</a></details>${s.protocol===1?go('Do Steady','ready','primary'):btn('I tried this · self-report','logProtocol')}<p class="hint">No score for intensity, compliance or a particular feeling.</p>`;}

    case 'source':return back(s.back)+head('PROVENANCE','Source first.<br>Adaptation second.')+`<details open><summary>Steady & natural breathing</summary><p>The current foundation practice uses natural breathing as an attention anchor. Shanivar does not pace the breath, require retention or claim a particular altered state.</p></details><details><summary>Daily rhythm</summary><p>The routine uses the user’s chosen wake and intended-sleep times. It does not calculate dosha, cortisol, circadian phase or a biologically perfect schedule.</p></details><details><summary>App protection</summary><p>Protection is a behavior-design tool. A saved rule is not the same thing as device access or active enforcement. Native status must stay truthful.</p></details><a href="RESEARCH.md" target="_blank">Read the research & adaptation register ↗</a>`+(s.session?go('Return to practice','practice','primary'):go('Back to Sadhana','sadhana','primary'));

    case 'settings':return back('routine')+head('PREFERENCES','Control the support.<br>Keep the agency.')+`<div class="card light"><h3>Practice reminders</h3><label class="check dark-check"><input id="reminders" type="checkbox" ${s.reminders?'checked':''}>One reminder at my chosen time</label>${field('Reminder time','reminderTime',s.reminderTime||'18:00')}<p class="hint dark-hint">Off by default. Independent mode turns these off.</p></div>`+btn('Save preferences','saveSettings')+go('Practice history','audit')+go('Essential access','recovery','text');

    default:return '';
  }
}

function render(){
  const [title,note]=screens[state.screen];
  $('#app').innerHTML=body();
  $('#note-title').textContent=title;
  $('#note-body').textContent=note;
  $('#routes').innerHTML=Object.entries(screens).map(([id,v])=>`<button data-go="${id}" class="${state.screen===id?'active':''}">${v[0]}</button>`).join('');
  const onboard=['welcome','rhythm','setup'];
  const immersive=['practice','ready','reflection'];
  $('#tabs').innerHTML=onboard.includes(state.screen)||immersive.includes(state.screen)?'':['routine','sadhana'].map(t=>`<button data-go="${t}" class="${groups[t].includes(state.screen)?'active':''}" ${groups[t].includes(state.screen)?'aria-current="page"':''}>${icon(t)}${t[0].toUpperCase()+t.slice(1)}</button>`).join('');
}

function readWindowsFromDOM(){
  return state.windows.map((w,i)=>({
    ...w,
    start:$(`#start-${i}`)?.value||w.start,
    end:$(`#end-${i}`)?.value||w.end,
    repeat:$(`#repeat-${i}`)?.value||w.repeat
  }));
}

function readActivitiesFromDOM(){
  return state.activities.map((activity,index)=>({
    ...activity,
    title:$(`#activity-title-${index}`)?.value.trim()||'',
    time:$(`#activity-time-${index}`)?.value||activity.time,
    duration:Number($(`#activity-duration-${index}`)?.value||activity.duration),
    repeat:$(`#activity-repeat-${index}`)?.value||activity.repeat,
    customDays:($(`#activity-repeat-${index}`)?.value||activity.repeat)==='Weekly'
      ? [Number($(`#activity-day-${index}`)?.value||activity.customDays[0]||1)]
      : activity.customDays
  }));
}

function preserveRoutineEditor(){
  state.wake=$('#routine-wake')?.value||state.wake;
  state.bed=$('#routine-bed')?.value||state.bed;
  state.activities=readActivitiesFromDOM();
}

function validateWindows(windows){
  for(const w of windows){
    if(!w.start||!w.end||w.start===w.end)return 'Each protection window needs different start and end times.';
    if(w.repeat==='Custom week'&&!w.customDays.length)return 'Choose at least one day for a custom week.';
  }
  return '';
}

document.addEventListener('change',e=>{
  if(e.target.id==='safe'){
    state.safe=e.target.checked;
    const begin=$('#app [data-act="begin"]');if(begin)begin.disabled=!state.safe;
  }
  if(e.target.dataset.repeat!==undefined){
    const i=Number(e.target.dataset.repeat);
    state.windows=readWindowsFromDOM();
    state.windows[i].repeat=e.target.value;
    render();
  }
  if(e.target.dataset.activityRepeat!==undefined){
    const index=Number(e.target.dataset.activityRepeat);
    preserveRoutineEditor();
    state.activities[index].repeat=e.target.value;
    if(e.target.value==='Every day')state.activities[index].customDays=[1,2,3,4,5,6,7];
    if(e.target.value==='Weekly')state.activities[index].customDays=[state.activities[index].customDays[0]||1];
    render();
  }
});

document.addEventListener('click',e=>{
  const b=e.target.closest('button');if(!b)return;
  if(b.dataset.go){state.back=state.screen==='practice'?'practice':state.screen;return navigate(b.dataset.go);}
  if(b.dataset.chapter!==undefined){state.chapter=Number(b.dataset.chapter);return navigate('chapter');}
  if(b.dataset.protocol!==undefined){state.protocol=Number(b.dataset.protocol);return navigate('protocol');}
  if(b.dataset.addActivity!==undefined){
    preserveRoutineEditor();
    const defaults={'Gym':['18:00',60],'Read a book':['20:00',30],'Extend Sadhana':['07:30',15]};
    const [time,duration]=defaults[b.dataset.addActivity]||['18:00',30];
    state.activities.push(newActivity(b.dataset.addActivity,time,duration,'Weekly'));
    return render();
  }
  if(b.dataset.feedback){state.feedback=b.dataset.feedback;const r=state.records.at(-1);if(r&&r.origin==='App timer')r.report=state.feedback;return render();}
  if(b.dataset.delete){state.records=state.records.filter(r=>String(r.id)!==b.dataset.delete);return render();}
  if(b.dataset.removeWindow!==undefined){
    state.windows=readWindowsFromDOM();
    if(state.windows.length>1)state.windows.splice(Number(b.dataset.removeWindow),1);
    return render();
  }
  if(b.dataset.removeActivity!==undefined){
    preserveRoutineEditor();
    state.activities.splice(Number(b.dataset.removeActivity),1);
    return render();
  }
  if(b.dataset.windowDay!==undefined){
    state.windows=readWindowsFromDOM();
    const i=Number(b.dataset.windowDay),day=Number(b.dataset.day),days=state.windows[i].customDays;
    state.windows[i].customDays=days.includes(day)?days.filter(d=>d!==day):[...days,day].sort((a,c)=>a-c);
    return render();
  }
  if(b.dataset.activityDay!==undefined){
    preserveRoutineEditor();
    const index=Number(b.dataset.activityDay),day=Number(b.dataset.day),days=state.activities[index].customDays;
    state.activities[index].customDays=days.includes(day)?days.filter(d=>d!==day):[...days,day].sort((a,c)=>a-c);
    return render();
  }
  switch(b.dataset.act){
    case 'saveRhythm':{
      const w=$('#wake').value,v=$('#bed').value;
      if(!w||!v||w===v){state.error='Choose different wake and sleep times.';return render();}
      state.wake=w;state.bed=v;return navigate('setup');
    }
    case 'saveAlarm':
      if(!$('#alarmTime').value)return;
      state.wake=$('#alarmTime').value;state.alarm=$('#alarmEnabled').checked;return navigate('setup');
    case 'addWindow':
      state.windows=readWindowsFromDOM();
      if(state.windows.length<8)state.windows.push(newWindow('18:00','19:00','Every day'));
      return render();
    case 'addActivity':
      preserveRoutineEditor();
      if(state.activities.length<12)state.activities.push(newActivity());
      return render();
    case 'saveRoutine':{
      preserveRoutineEditor();
      const problem=!state.wake||!state.bed||state.wake===state.bed?'Choose different wake and sleep times.':
        state.activities.some(a=>!a.title||!a.time)?'Every activity needs a name and time.':
        state.activities.some(a=>a.repeat==='Custom'&&!a.customDays.length)?'Choose at least one day for each custom activity.':'';
      if(problem){state.error=problem;return render();}
      state.error='';return navigate('routine');
    }
    case 'saveLocks':{
      const apps=[...document.querySelectorAll('input[name=apps]:checked')].map(x=>x.value);
      const windows=readWindowsFromDOM();
      const error=!apps.length?'Choose at least one app to protect your attention from.':validateWindows(windows);
      if(error){state.error=error;return render();}
      Object.assign(state,{apps,windows,locks:true,error:''});return navigate('permissions');
    }
    case 'grant':state.permission=true;return navigate('routine');
    case 'deny':state.permission=false;return navigate('routine');
    case 'offLocks':case 'recover':state.locks=false;return navigate('routine');
    case 'begin':
      if(!state.safe)return;
      state.remaining=60;state.session=true;state.paused=false;navigate('practice');return tick();
    case 'pause':pauseTimer();return render();
    case 'resume':if(!state.session){state.session=true;state.remaining=60;}tick();return render();
    case 'end':return endPractice('Ended early');
    case 'practiceSource':state.back='practice';return navigate('source');
    case 'logOwn':state.records.push({id:Date.now(),kind:'Independent practice',seconds:null,origin:'Self-report',report:'Practised without the app'});return navigate('audit');
    case 'logProtocol':state.records.push({id:Date.now(),kind:protocols[state.protocol].title,seconds:null,origin:'Self-report',report:'Tried this activity'});return navigate('audit');
    case 'saveIndependent':state.independent=$('#independent').checked;if(state.independent)state.reminders=false;return navigate('sadhana');
    case 'saveSettings':state.reminders=$('#reminders').checked;if(state.reminders)state.independent=false;state.reminderTime=$('#reminderTime').value;return navigate('routine');
    case 'clear':state.records=[];return render();
    case 'export':{
      const blob=new Blob([JSON.stringify({scope:'Wireframe visit only',records:state.records},null,2)],{type:'application/json'});
      const url=URL.createObjectURL(blob),a=document.createElement('a');a.href=url;a.download='shanivar-practice-demo.json';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);return;
    }
    case 'reset':pauseTimer();state=initial();return render();
  }
});

document.addEventListener('visibilitychange',()=>{if(document.hidden&&state.session){pauseTimer();render();}});
const requested=new URLSearchParams(location.search).get('screen');
if(screens[requested])state.screen=requested;
if(state.screen==='practice')state.paused=true;
render();
