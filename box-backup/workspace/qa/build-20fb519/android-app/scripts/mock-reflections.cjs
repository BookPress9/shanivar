// UI-only fixtures. No Gemini, chart computation, database, lock or alarm calls.
const { randomUUID } = require('node:crypto');
const sessions = new Map(), keys = new Map();
function card(index) {
  return { id: randomUUID(), index, sender: 'Shanivar',
    message: 'The reply is already typed. There is still a moment before you send it.',
    question: 'What do you want them to understand?',
    options: [{id:'fits',label:'This fits'},{id:'not_today',label:'Not today'},{id:'unsure',label:"I'm unsure"},{id:'prefer_not_to_share',label:'Prefer not to share'}],
    context_refs: [], provenance_label: 'general_fallback' };
}
module.exports = async function reflections(req, send) {
  if (!req.url.startsWith('/api/reflections/')) return false;
  let raw=''; for await (const chunk of req) {raw+=chunk; if(raw.length>8192){send(413,{error:'Too large'});return true;}}
  let body={}; try {if(raw) body=JSON.parse(raw);} catch {send(422,{error:'Invalid JSON'});return true;}
  const key=req.headers['idempotency-key'], scope=req.method+req.url+':'+key;
  if(key && keys.has(scope)){
    const previous=keys.get(scope);
    if(previous.raw!==raw) send(409,{error:'Idempotency payload conflict'});
    else send(previous.code,previous.result);
    return true;
  }
  const reply=(code,result)=>{if(key)keys.set(scope,{raw,code,result:structuredClone(result)});send(code,result);return true;};
  if(req.url==='/api/reflections/sessions' && req.method==='POST'){
    let date; try {date=new Intl.DateTimeFormat('en-CA',{timeZone:body.timezone}).format(new Date());} catch{return reply(422,{error:'Invalid timezone'});}
    const value={session_id:randomUUID(),version:0,status:'awaiting_response',mode:'general',
      context_as_of:new Date().toISOString(),local_date:date,timezone:body.timezone,answered_count:0,session_limit:5,
      current_card:card(0),answered_history:[]};
    sessions.set(value.session_id,value);return reply(200,value);
  }
  const match=req.url.match(/^\/api\/reflections\/sessions\/([^/]+)(?:\/(responses|retry|close))?$/);
  const s=match && sessions.get(match[1]);
  if(!s){send(404,{error:'Session not found'});return true;}
  if(req.method==='DELETE'){
    sessions.delete(s.session_id); for(const [k,v] of keys) if(v.result.session_id===s.session_id) keys.delete(k);
    return reply(200,{message:'Reflection session deleted'});
  }
  if(req.method==='GET') return reply(200,s);
  if(match[2]==='responses'){
    if(s.status!=='awaiting_response'||body.card_id!==s.current_card.id||body.expected_version!==s.version) return reply(409,{error:'Stale card/version'});
    const r=body.response;
    if(!r||!['text','option'].includes(r.kind)||typeof r.value!=='string'||
      (r.kind==='text' && !(r.value.trim().length>=1 && r.value.trim().length<=1000))||
      (r.kind==='option' && !s.current_card.options.some(o=>o.id===r.value))) return reply(422,{error:'Invalid response'});
    s.answered_history.push({card:s.current_card,response:{...r,created_at:new Date().toISOString()}});
    s.answered_count++;s.version++;
    s.status=s.answered_count===s.session_limit?'completed':'awaiting_response';
    s.current_card=s.status==='completed'?null:card(s.answered_count);
    return reply(200,s);
  }
  if(match[2]==='close'){s.status='closed';s.version++;s.current_card=null;return reply(200,s);}
  if(match[2]==='retry')return reply(200,s);
  return reply(404,{error:'Unknown reflection route'});
};

