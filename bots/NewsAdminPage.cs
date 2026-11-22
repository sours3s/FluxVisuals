namespace FluxVisualsBots;

/// <summary>Встроенная HTML-страница панели новостей (без внешних файлов).</summary>
public static class NewsAdminPage
{
    public const string Html = """
<!DOCTYPE html>
<html lang="ru">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>FluxVisuals — панель новостей</title>
<style>
:root{--bg:#07080d;--card:rgba(255,255,255,.045);--card2:rgba(255,255,255,.028);--line:rgba(255,255,255,.09);--text:#f3f5f9;--muted:#98a1b2;--violet:#8b6cff;--cyan:#3ad7ff;--ok:#4ade9b;--err:#ff6b86}
*{box-sizing:border-box}
html,body{min-height:100%}
body{margin:0;background:radial-gradient(900px 520px at 8% -12%,rgba(139,108,255,.20),transparent 60%),radial-gradient(760px 480px at 100% 0%,rgba(58,215,255,.10),transparent 55%),var(--bg);color:var(--text);font:15px/1.55 Inter,"Segoe UI",system-ui,-apple-system,sans-serif;-webkit-font-smoothing:antialiased}
button,input,textarea,select{font:inherit;color:inherit}
button{cursor:pointer}
a{color:var(--cyan);text-decoration:none}
.glass{background:var(--card);border:1px solid var(--line);border-radius:20px;backdrop-filter:blur(22px);-webkit-backdrop-filter:blur(22px);box-shadow:0 18px 50px rgba(0,0,0,.28)}
.wrap{max-width:1240px;margin:0 auto;padding:26px 22px 60px}

/* login */
.login-wrap{min-height:100vh;display:grid;place-items:center;padding:24px}
.login{width:min(430px,100%);padding:34px}
.brand{display:flex;align-items:center;gap:12px;margin-bottom:22px}
.logo{width:42px;height:42px;border-radius:13px;background:linear-gradient(135deg,var(--violet),var(--cyan));display:grid;place-items:center;font-weight:800;font-size:16px;letter-spacing:-.5px;box-shadow:0 8px 24px rgba(139,108,255,.35)}
.brand b{display:block;font-size:19px;letter-spacing:-.3px}
.brand span{display:block;color:var(--muted);font-size:12.5px;margin-top:1px}
h1{font-size:24px;letter-spacing:-.6px;margin:0 0 8px}
p.sub{color:var(--muted);margin:0 0 22px;font-size:14px}
label{display:block;font-size:12.5px;color:var(--muted);margin:0 0 8px;letter-spacing:.2px}
input[type=password],input[type=text],textarea,select{width:100%;background:rgba(0,0,0,.26);border:1px solid var(--line);border-radius:12px;padding:12px 14px;outline:none;transition:border .18s,box-shadow .18s}
input:focus,textarea:focus,select:focus{border-color:rgba(139,108,255,.7);box-shadow:0 0 0 3px rgba(139,108,255,.14)}
.btn{border:1px solid transparent;border-radius:12px;padding:12px 18px;font-weight:650;background:linear-gradient(135deg,var(--violet),#6d4fe0);color:#fff;box-shadow:0 8px 22px rgba(139,108,255,.26);transition:transform .15s,filter .15s}
.btn:hover{filter:brightness(1.1)}
.btn:active{transform:translateY(1px)}
.btn:disabled{opacity:.45;cursor:not-allowed;filter:none}
.btn.full{width:100%;margin-top:16px}
.err{color:var(--err);font-size:13px;margin-top:12px;min-height:18px}
.hint{color:var(--muted);font-size:12px;margin-top:16px;line-height:1.6}

/* app */
.top{display:flex;align-items:center;gap:14px;margin-bottom:20px;flex-wrap:wrap}
.top .brand{margin:0;flex:1}
.status{font-size:12.5px;padding:7px 12px;border-radius:999px;border:1px solid var(--line);background:rgba(255,255,255,.04);white-space:nowrap}
.status.on{color:var(--ok);border-color:rgba(74,222,155,.28);background:rgba(74,222,155,.08)}
.status.off{color:var(--err);border-color:rgba(255,107,134,.28);background:rgba(255,107,134,.08)}
.icon-btn{background:rgba(255,255,255,.05);border:1px solid var(--line);border-radius:11px;padding:9px 13px;font-size:13px}
.icon-btn:hover{background:rgba(255,255,255,.09)}
.grid{display:grid;grid-template-columns:minmax(0,1.35fr) minmax(310px,.85fr);gap:18px;align-items:start}
.panel{padding:22px}
.panel h2{font-size:15px;margin:0 0 16px;letter-spacing:-.2px;display:flex;align-items:center;gap:9px}
.panel h2 i{width:7px;height:7px;border-radius:50%;background:var(--violet);box-shadow:0 0 12px var(--violet);display:inline-block}
.field{margin-bottom:18px}
.row{display:flex;gap:10px;align-items:center}
.row>*{min-width:0}
textarea{min-height:190px;resize:vertical;line-height:1.6}
select{appearance:none;background-image:linear-gradient(45deg,transparent 50%,var(--muted) 50%),linear-gradient(135deg,var(--muted) 50%,transparent 50%);background-position:calc(100% - 18px) 50%,calc(100% - 13px) 50%;background-size:5px 5px,5px 5px;background-repeat:no-repeat;padding-right:34px}
select option{background:#151722;color:var(--text)}
.meta{display:flex;justify-content:space-between;align-items:center;margin-top:7px;font-size:11.5px;color:var(--muted)}
.chips{display:flex;gap:7px;flex-wrap:wrap;margin-top:9px}
.chip{background:rgba(255,255,255,.05);border:1px solid var(--line);border-radius:9px;padding:6px 11px;font-size:12px;color:var(--muted)}
.chip:hover{background:rgba(139,108,255,.14);border-color:rgba(139,108,255,.4);color:var(--text)}

/* dropzone */
.drop{border:1.5px dashed rgba(255,255,255,.18);border-radius:14px;padding:20px;text-align:center;color:var(--muted);font-size:13px;transition:.18s;background:rgba(0,0,0,.12)}
.drop.hover{border-color:var(--cyan);background:rgba(58,215,255,.07);color:var(--text)}
.drop b{color:var(--text);display:block;margin-bottom:3px;font-size:13.5px}
.files{display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:9px;margin-top:12px}
.file{position:relative;display:flex;align-items:center;gap:9px;background:rgba(0,0,0,.24);border:1px solid var(--line);border-radius:11px;padding:8px;min-width:0}
.file img{width:38px;height:38px;object-fit:cover;border-radius:7px;flex:none}
.ficon{width:38px;height:38px;border-radius:7px;background:rgba(139,108,255,.16);display:grid;place-items:center;flex:none;font-size:17px}
.fname{font-size:11.5px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;flex:1}
.fname small{display:block;color:var(--muted);font-size:10.5px}
.file button{position:absolute;top:-6px;right:-6px;width:20px;height:20px;border-radius:50%;border:1px solid var(--line);background:#20222e;color:var(--text);line-height:1;font-size:14px;padding:0}

/* preview */
.preview-card{background:#171922;border:1px solid rgba(255,255,255,.07);border-radius:14px;padding:15px;box-shadow:0 10px 30px rgba(0,0,0,.22)}
.pv-head{display:flex;align-items:center;gap:10px;margin-bottom:10px}
.pv-avatar{width:34px;height:34px;border-radius:50%;background:linear-gradient(135deg,var(--violet),var(--cyan));display:grid;place-items:center;font-size:11px;font-weight:800;flex:none}
.pv-name{font-size:13.5px;font-weight:700}
.pv-name span{font-size:10px;color:var(--cyan);background:rgba(58,215,255,.12);padding:1px 5px;border-radius:4px;margin-left:6px;font-weight:600}
.pv-time{font-size:10.5px;color:#727a8b}
.pv-text{font-size:13.5px;white-space:pre-wrap;word-break:break-word;min-height:22px}
.mention{background:rgba(139,108,255,.22);color:#c4b5ff;border-radius:4px;padding:0 3px;font-weight:600}
.pv-files{display:flex;gap:7px;flex-wrap:wrap;margin-top:11px}
.pv-files img{max-width:100%;max-height:190px;border-radius:9px;border:1px solid var(--line)}
.pv-file{display:flex;align-items:center;gap:7px;background:#20222d;border:1px solid var(--line);border-radius:8px;padding:7px 10px;font-size:12px}
.empty{color:#6f7787;font-size:13px;font-style:italic}
.notes{margin-top:18px;padding-top:16px;border-top:1px solid var(--line);font-size:12.5px;color:var(--muted)}
.notes div{display:flex;gap:8px;margin-bottom:8px}
.notes b{color:var(--text);font-weight:600}
.publish{margin-top:18px;width:100%;font-size:15px;padding:14px}

.toast{position:fixed;left:50%;bottom:28px;transform:translateX(-50%);background:rgba(18,20,28,.94);border:1px solid var(--line);border-radius:13px;padding:13px 20px;font-size:13.5px;box-shadow:0 18px 40px rgba(0,0,0,.45);backdrop-filter:blur(16px);z-index:50;max-width:min(520px,92vw);text-align:center}
.toast.ok{border-color:rgba(74,222,155,.4);color:var(--ok)}
.toast.err{border-color:rgba(255,107,134,.45);color:var(--err)}
[hidden]{display:none!important}
@media(max-width:900px){.grid{grid-template-columns:1fr}.wrap{padding:18px 14px 40px}}
</style>
</head>
<body>

<div class="login-wrap" id="login">
  <form class="glass login" id="loginForm">
    <div class="brand">
      <div class="logo">FV</div>
      <div><b>FluxVisuals</b><span>Панель публикации новостей</span></div>
    </div>
    <h1>Вход в панель</h1>
    <p class="sub">Управлять новостями может только владелец бота.</p>
    <label for="tokenInput">Токен администратора</label>
    <input id="tokenInput" type="password" autocomplete="off" placeholder="NEWS_ADMIN_TOKEN">
    <div class="err" id="loginError"></div>
    <button class="btn full" type="submit">Войти</button>
    <div class="hint">Токен задаётся переменной <b>NEWS_ADMIN_TOKEN</b> в Render. Если она не задана — используется <b>FLUXVISUALS_BOT_SECRET</b>. Токен хранится только в этой вкладке.</div>
  </form>
</div>

<div class="wrap" id="app" hidden>
  <div class="top">
    <div class="brand">
      <div class="logo">FV</div>
      <div><b>Новости FluxVisuals</b><span>Публикация сообщений и файлов на сервер</span></div>
    </div>
    <div class="status off" id="status">● Проверка…</div>
    <button class="icon-btn" id="refresh" type="button">Обновить</button>
    <button class="icon-btn" id="logout" type="button">Выйти</button>
  </div>

  <div class="grid">
    <section class="glass panel">
      <h2><i></i>Новая публикация</h2>

      <div class="field">
        <label for="channel">Канал</label>
        <select id="channel"><option value="">Загрузка…</option></select>
      </div>

      <div class="field">
        <label for="message">Сообщение</label>
        <textarea id="message" maxlength="1900" placeholder="Долгожданное обновление FluxVisuals…"></textarea>
        <div class="meta"><span id="limits">—</span><span id="counter">0 / 1900</span></div>
        <div class="chips">
          <button class="chip" type="button" data-insert="@everyone">@everyone</button>
          <button class="chip" type="button" data-insert="@here">@here</button>
          <button class="chip" type="button" data-insert="**жирный**">жирный</button>
          <button class="chip" type="button" data-insert="`код`">код</button>
        </div>
      </div>

      <div class="field">
        <label>Вложения</label>
        <div class="drop" id="drop"><b>Перетащите файлы сюда или вставьте по Ctrl+V</b>нажмите, чтобы выбрать скриншоты и файлы из проводника</div>
        <input id="fileInput" type="file" multiple hidden>
        <div class="files" id="files"></div>
      </div>

      <button class="btn publish" id="publish" type="button" disabled>Опубликовать</button>
    </section>

    <aside>
      <section class="glass panel">
        <h2><i style="background:var(--cyan);box-shadow:0 0 12px var(--cyan)"></i>Предпросмотр</h2>
        <div class="preview-card">
          <div class="pv-head">
            <div class="pv-avatar">FV</div>
            <div><div class="pv-name">FluxVisuals <span>БОТ</span></div><div class="pv-time" id="pvChannel">канал не выбран</div></div>
          </div>
          <div class="pv-text" id="pvText"><span class="empty">Введите текст сообщения…</span></div>
          <div class="pv-files" id="pvFiles"></div>
        </div>
        <div class="notes">
          <div><b>●</b><span><b>@everyone</b> и <b>@here</b> пингуют участников. Упоминания ролей и пользователей из текста отключены.</span></div>
          <div><b>●</b><span>Скриншот можно вставить по <b>Ctrl+V</b> — он появится во вложениях сразу.</span></div>
          <div><b>●</b><span>Можно отправить только текст, только файлы или всё вместе.</span></div>
          <div><b>●</b><span>После отправки поле и вложения очищаются.</span></div>
        </div>
      </section>
    </aside>
  </div>
</div>

<div class="toast" id="toast" hidden></div>

<script>
const $ = s => document.querySelector(s);
const KEY = 'fv_news_token';
let token = sessionStorage.getItem(KEY) || '';
let channels = [];
let files = [];
let toastTimer = 0;

function esc(s){const d=document.createElement('div');d.textContent=s??'';return d.innerHTML.replace(/"/g,'&quot;')}
function fmt(n){return n<1024?n+' Б':n<1048576?(n/1024).toFixed(0)+' КБ':(n/1048576).toFixed(1)+' МБ'}
function toast(text, bad){
  const t=$('#toast');t.textContent=text;t.className='toast '+(bad?'err':'ok');t.hidden=false;
  clearTimeout(toastTimer);toastTimer=setTimeout(()=>t.hidden=true,bad?6000:3200);
}

async function api(path, opts){
  const r = await fetch(path, {...(opts||{}), headers:{'X-Admin-Token':token, ...((opts&&opts.headers)||{})}});
  let data={};try{data=await r.json()}catch(e){}
  if(r.status===401){logout();throw new Error(data.error||'Неверный токен.')}
  if(!r.ok)throw new Error(data.error||'Ошибка запроса ('+r.status+')');
  return data;
}

async function login(){
  token=$('#tokenInput').value.trim();
  if(!token){$('#loginError').textContent='Введите токен администратора.';return}
  $('#loginError').textContent='';
  try{
    const s=await api('/api/news/status');
    sessionStorage.setItem(KEY,token);
    $('#login').hidden=true;$('#app').hidden=false;
    $('#limits').textContent='до '+s.maxFiles+' файлов · '+Math.round(s.maxFileBytes/1e6)+' МБ каждый';
    $('#message').maxLength=s.maxChars;
    renderStatus(s);
    await loadChannels();
  }catch(e){$('#loginError').textContent=e.message}
}

function logout(){
  token='';sessionStorage.removeItem(KEY);
  $('#app').hidden=true;$('#login').hidden=false;$('#loginError').textContent='';
}

function renderStatus(s){
  const el=$('#status');
  el.className='status '+(s.connected?'on':'off');
  el.textContent=s.connected?('● '+s.user+' онлайн · '+s.guilds+' сервер'):'● Бот не в сети';
  $('#refresh').disabled=false;
}

async function loadChannels(){
  const sel=$('#channel');sel.innerHTML='<option value="">Загрузка…</option>';
  try{
    const d=await api('/api/news/channels');
    channels=d.channels||[];
    sel.innerHTML='<option value="">Выберите канал…</option>'+channels.map(c=>
      '<option value="'+esc(c.id)+'">#'+esc(c.name)+(c.guild?' — '+esc(c.guild):'')+'</option>').join('');
    if(!channels.length)toast('Бот не видит доступных каналов. Проверьте права.',true);
  }catch(e){sel.innerHTML='<option value="">Не удалось загрузить</option>';toast(e.message,true)}
  update();
}

function update(){
  const text=$('#message').value;
  $('#counter').textContent=text.length+' / '+$('#message').maxLength;
  const ch=channels.find(c=>c.id===$('#channel').value);
  $('#pvChannel').textContent=ch?('#'+ch.name+(ch.guild?' · '+ch.guild:'')):'канал не выбран';
  const safe=esc(text).replace(/@everyone|@here/g,'<span class="mention">$&</span>')
    .replace(/\*\*(.+?)\*\*/g,'<b>$1</b>');
  $('#pvText').innerHTML=text.trim()?safe:'<span class="empty">Введите текст сообщения…</span>';
  $('#pvFiles').innerHTML=files.map(x=>/\.(png|jpe?g|gif|webp|bmp)$/i.test(x.file.name)
    ?'<img src="'+x.url+'" alt="">'
    :'<div class="pv-file">📎 '+esc(x.file.name)+'</div>').join('');
  $('#publish').disabled=!(ch&&(text.trim()||files.length));
}

function stamp(){const d=new Date(),p=n=>String(n).padStart(2,'0');return d.getFullYear()+p(d.getMonth()+1)+p(d.getDate())+'-'+p(d.getHours())+p(d.getMinutes())+p(d.getSeconds())}
function addFiles(list, pasted){
  let added=0;
  for(const f of list){
    if(files.length>=4){toast('Максимум 4 файла.',true);break}
    if(f.size>8000000){toast('«'+f.name+'» больше 8 МБ.',true);continue}
    // Буфер обмена отдаёт файлы с безликими именами вроде image.png — даём имя со временем.
    const isImage=/^image\//.test(f.type||'');
    const name=isImage&&/^(image|screenshot|clipboard|paste)/i.test(f.name)
      ?'screenshot-'+stamp()+'.'+((f.type||'image/png').split('/')[1]||'png')
      :f.name;
    files.push({file:isImage?new File([f],name,{type:f.type}):f,url:URL.createObjectURL(f)});
    added++;
  }
  $('#fileInput').value='';
  renderFiles();update();
  if(pasted&&added)toast('Вставлено во вложения: '+added);
}
function renderFiles(){
  $('#files').innerHTML=files.map((x,i)=>
    '<div class="file">'+(/\.(png|jpe?g|gif|webp|bmp)$/i.test(x.file.name)
      ?'<img src="'+x.url+'" alt="">':'<div class="ficon">📎</div>')+
    '<div class="fname">'+esc(x.file.name)+'<small>'+fmt(x.file.size)+'</small></div>'+
    '<button type="button" data-remove="'+i+'">×</button></div>').join('');
}

async function publish(){
  const btn=$('#publish');btn.disabled=true;const old=btn.textContent;btn.textContent='Публикация…';
  const fd=new FormData();
  fd.append('channel',$('#channel').value);
  fd.append('message',$('#message').value);
  files.forEach(x=>fd.append('files',x.file,x.file.name));
  try{
    const d=await api('/api/news/publish',{method:'POST',body:fd});
    toast('Опубликовано в #'+d.channel+(d.guild?' · '+d.guild:''));
    $('#message').value='';
    files.forEach(x=>URL.revokeObjectURL(x.url));files=[];
    renderFiles();update();
  }catch(e){toast(e.message,true)}
  finally{btn.textContent=old;update()}
}

$('#loginForm').addEventListener('submit',e=>{e.preventDefault();login()});
$('#logout').addEventListener('click',logout);
$('#refresh').addEventListener('click',async()=>{try{renderStatus(await api('/api/news/status'));await loadChannels()}catch(e){toast(e.message,true)}});
$('#message').addEventListener('input',update);
$('#channel').addEventListener('change',update);
document.querySelectorAll('[data-insert]').forEach(b=>b.addEventListener('click',()=>{
  const m=$('#message'),pos=m.selectionStart,txt=b.dataset.insert;
  m.value=m.value.slice(0,pos)+txt+m.value.slice(m.selectionEnd);
  m.selectionStart=m.selectionEnd=pos+txt.length;m.focus();update();
}));
$('#fileInput').addEventListener('change',e=>addFiles(e.target.files));
document.addEventListener('paste',e=>{
  const blobs=[...(e.clipboardData&&e.clipboardData.items||[])]
    .filter(i=>i.kind==='file')
    .map(i=>i.getAsFile())
    .filter(Boolean);
  if(!blobs.length)return; // текстовый Ctrl+V оставляем текстовому полю
  e.preventDefault();
  addFiles(blobs,true);
});
$('#drop').addEventListener('click',()=>$('#fileInput').click());
['dragenter','dragover'].forEach(ev=>$('#drop').addEventListener(ev,e=>{e.preventDefault();$('#drop').classList.add('hover')}));
['dragleave','drop'].forEach(ev=>$('#drop').addEventListener(ev,e=>{e.preventDefault();$('#drop').classList.remove('hover')}));
$('#drop').addEventListener('drop',e=>addFiles(e.dataTransfer.files));
$('#files').addEventListener('click',e=>{
  const b=e.target.closest('[data-remove]');if(!b)return;
  const i=+b.dataset.remove;URL.revokeObjectURL(files[i].url);files.splice(i,1);renderFiles();update();
});
$('#publish').addEventListener('click',publish);

if(token){$('#tokenInput').value=token;login()}
</script>
</body>
</html>
""";
}
