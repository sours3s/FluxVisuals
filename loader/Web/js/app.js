// ============ FluxVisuals Launcher UI ============
(() => {
  const $ = (sel) => document.querySelector(sel);
  const $$ = (sel) => [...document.querySelectorAll(sel)];

  const send = (obj) => chrome.webview.postMessage(obj);
  const fmt = (n) => {
    n = Number(n) || 0;
    if (n >= 1024 * 1024 * 1024) return (n / 1024 / 1024 / 1024).toFixed(1) + ' ГБ';
    if (n >= 1024 * 1024) return (n / 1024 / 1024).toFixed(1) + ' МБ';
    if (n >= 1024) return (n / 1024).toFixed(0) + ' КБ';
    return n + ' Б';
  };

  let state = {
    config: null,
    isAdmin: false,
    role: 'user',
    hasAccess: false,
    accessExpiresAt: null,
    mods: [],
    modpacks: [],
    modpackIds: new Set(),
    fileFilter: '',
    shop: {
      source: 'modrinth', type: 'mod', category: '', loader: 'fabric', sort: 'downloads',
      items: [], offset: 0, total: 0, seq: 0, busy: false, catsLoaded: false,
      versions: {},           // кэш списка версий по id проекта
      versionsLoading: new Set(),
    },
    downloads: new Map(),     // id -> { id, title, file, done, total, percent, status }
  };
  let launching = false;
  let authenticated = false;

  // ---------- Оформление: акцент из настроек ----------
  function applyAccent(cfg) {
    const hex = (v, def) => '#' + (((v ?? def) & 0xffffff) >>> 0).toString(16).padStart(6, '0');
    const root = document.documentElement.style;
    root.setProperty('--accent', hex(cfg.accent, 0x7c5cff));
    root.setProperty('--accent3', hex(cfg.accentSecond, 0xec4899));
    const c1 = hex(cfg.accent, 0x7c5cff).slice(1).match(/../g).map(x => parseInt(x, 16));
    const c3 = hex(cfg.accentSecond, 0xec4899).slice(1).match(/../g).map(x => parseInt(x, 16));
    root.setProperty('--accent-rgb', c1.join(', '));
    root.setProperty('--accent3-rgb', c3.join(', '));
  }

  // ---------- Авторизация ----------
  function showLogin() {
    $('#loginOverlay').classList.add('show');
    if (state.config && state.config.authServerUrl) $('#loginUrl').value = state.config.authServerUrl;
    setTimeout(() => $('#loginUser').focus(), 50);
  }
  function hideLogin() {
    $('#loginOverlay').classList.remove('show');
    $('#loginError').textContent = '';
  }
  function setAuthState(auth) {
    authenticated = auth;
    $('#btnLaunch').disabled = !auth || launching;
    if (auth) {
      const u = $('#loginUser').value || state.config?.username || 'Player';
      $('#homeUser').textContent = u;
      $('#sfUserName').textContent = u;
    }
  }

  $('#btnLogin').addEventListener('click', doLogin);
  $('#loginPass').addEventListener('keydown', (e) => { if (e.key === 'Enter') doLogin(); });
  $('#loginUser').addEventListener('keydown', (e) => { if (e.key === 'Enter') doLogin(); });

  function doLogin() {
    const username = $('#loginUser').value.trim();
    const password = $('#loginPass').value;
    const authUrl = $('#loginUrl').value.trim() || state.config?.authServerUrl || '';
    if (!username || !password) {
      $('#loginError').textContent = 'Введите логин и пароль';
      return;
    }
    $('#btnLogin').disabled = true;
    $('#loginError').textContent = '';
    send({ cmd: 'login', username, password, authUrl });
  }

  // ---------- Навигация ----------
  $$('.nav-item').forEach((item) => {
    item.addEventListener('click', () => {
      $$('.nav-item').forEach((i) => i.classList.remove('active'));
      item.classList.add('active');
      $$('.page').forEach((p) => p.classList.remove('active'));
      $('#page-' + item.dataset.page).classList.add('active');
      if (item.dataset.page === 'mods') send({ cmd: 'listMods' });
      if (item.dataset.page === 'shop' && !state.shop.catsLoaded) loadCategories();
      if (item.dataset.page === 'support') send({ cmd: 'aiStatus' });
    });
  });

  // ---------- Окно ----------
  $('#btnMin').addEventListener('click', () => send({ cmd: 'minimize' }));
  $('#btnMax').addEventListener('click', () => send({ cmd: 'toggleMaximize' }));
  $('#btnClose').addEventListener('click', () => send({ cmd: 'close' }));
  $('.tb-drag').addEventListener('mousedown', (e) => {
    if (e.button === 0 && e.target === e.currentTarget) send({ cmd: 'drag' });
  });

  // ---------- Монитор загрузок ----------
  $('#btnDownloads').addEventListener('click', () => $('#downloadPanel').classList.toggle('show'));
  $('#dpClose').addEventListener('click', () => $('#downloadPanel').classList.remove('show'));

  function renderDownloads() {
    const list = $('#downloadList');
    const badge = $('#dlBadge');
    const btn = $('#btnDownloads');
    const all = [...state.downloads.values()];
    const active = all.filter((d) => d.status === 'loading');
    const failed = all.filter((d) => d.status === 'error');

    badge.textContent = String(active.length || failed.length || '');
    badge.classList.toggle('show', active.length > 0 || failed.length > 0);
    btn.classList.toggle('busy', active.length > 0);

    if (!all.length) {
      list.innerHTML = '<div class="dp-empty">Загрузок пока нет.<br>Установите что-нибудь из мастерской.</div>';
      return;
    }
    list.innerHTML = '';
    all.slice(-12).forEach((d) => {
      const row = document.createElement('div');
      row.className = 'dp-item' + (d.status === 'done' ? ' done' : d.status === 'error' ? ' error' : '');
      const pct = d.percent != null ? Math.round(d.percent) : 0;
      row.innerHTML = `
        <div class="dp-item-top"><span>${esc(d.title)}</span><span>${d.status === 'error' ? 'ошибка' : d.status === 'done' ? 'готово' : pct + '%'}</span></div>
        <div class="dp-file">${esc(d.file || '')}${d.total > 0 ? ' · ' + fmt(d.done) + ' / ' + fmt(d.total) : ''}</div>
        <div class="dp-bar"><div class="dp-fill" style="width:${d.status === 'error' ? 100 : pct}%"></div></div>`;
      list.appendChild(row);
    });
  }

  // ---------- Приём событий от C# ----------
  chrome.webview.addEventListener('message', (e) => {
    const msg = typeof e.data === 'string' ? JSON.parse(e.data) : e.data;
    handleEvent(msg);
  });

  function handleEvent(msg) {
    switch (msg.ev) {
      case 'state': {
        state.config = msg.config;
        state.isAdmin = !!msg.config?.isAdmin;
        state.role = msg.config?.role || 'user';
        state.hasAccess = !!msg.config?.hasAccess;
        state.accessExpiresAt = msg.config?.accessExpiresAt || null;
        applyAccent(msg.config || {});
        if (msg.config?.authServerUrl) $('#loginUrl').value = msg.config.authServerUrl;
        renderHome(msg.config);
        renderSettings(msg.config);
        break;
      }
      case 'authChecked': {
        if (msg.authenticated) {
          authenticated = true;
          state.isAdmin = !!msg.isAdmin;
          state.role = msg.role || state.config?.role || 'user';
          state.hasAccess = !!msg.hasAccess;
          state.accessExpiresAt = msg.accessExpiresAt || null;
          if (state.config) {
            state.config.username = msg.username || state.config.username;
            state.config.role = state.role;
            state.config.hasAccess = state.hasAccess;
          }
          setAuthState(true);
          renderSettings(state.config);
          renderHome(state.config);
          $('#loginUser').value = msg.username || state.config?.username || '';
        } else {
          showLogin();
        }
        break;
      }
      case 'authSuccess': {
        $('#btnLogin').disabled = false;
        hideLogin();
        authenticated = true;
        state.isAdmin = !!msg.isAdmin;
        state.role = msg.role || 'user';
        state.hasAccess = !!msg.hasAccess;
        state.accessExpiresAt = msg.accessExpiresAt || null;
        if (state.config) {
          state.config.isAdmin = state.isAdmin;
          state.config.role = state.role;
          state.config.hasAccess = state.hasAccess;
          state.config.username = msg.username || state.config.username;
          state.config.uid = msg.uid ?? state.config.uid;
        }
        setAuthState(true);
        renderSettings(state.config);
        renderHome(state.config);
        $('#loginUser').value = msg.username || '';
        toast(`Добро пожаловать, ${msg.username}!`);
        break;
      }
      case 'authError': {
        $('#btnLogin').disabled = false;
        $('#loginError').textContent = msg.message || 'Ошибка входа';
        break;
      }
      case 'authLogout': {
        authenticated = false;
        state.hasAccess = false;
        setAuthState(false);
        showLogin();
        break;
      }
      case 'mods':
      case 'files':
        state.mods = msg.files || msg.mods || [];
        if (msg.installed) state.shop.installed = new Set(msg.installed);
        renderFiles(state.mods);
        if (state.shop.items && state.shop.items.length) renderShop(state.shop.items);
        break;
      case 'modpacks':
        state.modpacks = msg.modpacks || [];
        state.modpackIds = new Set((msg.installedIds || state.modpacks.map((p) => p.id)).map(String));
        renderFiles(state.mods);
        if (state.shop.items && state.shop.items.length) renderShop(state.shop.items);
        break;
      case 'folderPicked':
        if (msg.key === 'gameDir') {
          if (state.config) state.config.gameDir = msg.path;
          $('#setGameDirPath').textContent = msg.path || '—';
        } else if (msg.key === 'java') {
          if (state.config) state.config.javaPath = msg.path;
          $('#setJavaPath').textContent = msg.path || '—';
        }
        break;
      case 'searchResult':
        if (msg.seq !== state.shop.seq) return;
        state.shop.busy = false;
        state.shop.source = msg.source;
        const items = msg.items || [];
        state.shop.items = msg.offset === 0 ? items : state.shop.items.concat(items);
        state.shop.offset = msg.offset + items.length;
        state.shop.total = msg.total;
        renderShop(state.shop.items);
        break;
      case 'installResult':
        toast(msg.ok ? `Установлено: ${msg.file}` : `Ошибка: ${msg.message}`, !msg.ok);
        break;
      case 'catalogMeta':
        if (msg.source === state.shop.source) renderCategories(msg.categories || []);
        break;
      case 'versions':
        renderVersions(msg.id, msg.versions || []);
        break;
      case 'downloadProgress': {
        const d = state.downloads.get(msg.id) || {
          id: msg.id, title: msg.title || 'Загрузка', file: '', done: 0, total: 0, percent: 0, status: 'loading',
        };
        d.title = msg.title || d.title;
        d.file = msg.file || d.file;
        d.done = msg.done || 0;
        d.total = msg.total || 0;
        d.percent = msg.percent || 0;
        d.status = 'loading';
        state.downloads.set(msg.id, d);
        renderDownloads();
        break;
      }
      case 'downloadDone': {
        const d = state.downloads.get(msg.id);
        if (d) {
          d.status = msg.ok ? 'done' : 'error';
          if (!msg.ok) d.file = msg.message || d.file;
          if (msg.ok) d.percent = 100;
          state.downloads.set(msg.id, d);
          renderDownloads();
        }
        break;
      }
      case 'progress':
        showProgress(msg.step, msg.percent, msg.message);
        break;
      case 'launchState':
        if (msg.state === 'launched') {
          launching = false;
          $('#btnLaunch').disabled = false;
          toast('Minecraft запущен!');
          setTimeout(hideProgress, 400);
          setTimeout(() => send({ cmd: 'close' }), 700);
        } else if (msg.state === 'error') {
          launching = false;
          $('#btnLaunch').disabled = false;
          hideProgress();
          toast('Ошибка запуска: ' + msg.message, true);
        }
        break;
      case 'aiStatus':
        setAiStatus(msg.available, msg.reason);
        break;
      case 'aiThinking':
        setAiBusy(!!msg.busy);
        break;
      case 'aiAnswer':
        setAiBusy(false);
        appendAiMessage('model', msg.answer);
        aiHistory.push({ role: 'model', text: msg.answer || '' });
        trimAiHistory();
        break;
      case 'aiError':
        setAiBusy(false);
        appendAiMessage('error', msg.message || 'Catal не ответил. Попробуйте ещё раз.');
        break;
      case 'error':
        toast(msg.message, true);
        break;
      case 'configSaved':
        toast('Настройки сохранены');
        break;
    }
  }

  // ---------- Главная ----------
  function renderHome(cfg) {
    if (!cfg) return;
    $('#homeUser').textContent = cfg.mcNickname || cfg.username || 'Player';
    $('#homeUid').textContent = cfg.username || '—';
    $('#homeVersion').textContent = cfg.version || '1.21.11';
    $('#homeRam').textContent = (cfg.ramMb || 4096) + ' МБ';
    $('#sfUserName').textContent = cfg.username || 'Player';
    const roleName = state.role === 'admin' ? 'Администратор'
      : state.role === 'client' ? 'FluxVisuals' : 'Бесплатный доступ';
    $('#sfUserRole').textContent = roleName;
    $('#ramLabel').textContent = Math.round((cfg.ramMb || 4096) / 1024) + ' ГБ';
    $('#ramFill').style.width = Math.min(100, ((cfg.ramMb || 4096) / 16384) * 100) + '%';

    const acc = $('#homeAccess');
    if (state.hasAccess) {
      acc.textContent = 'Бесплатный';
      acc.style.color = 'var(--green)';
    } else {
      acc.textContent = authenticated ? 'Нужен вход' : 'Войдите в аккаунт';
      acc.style.color = 'var(--yellow)';
    }
  }

  $('#btnLaunch').addEventListener('click', () => {
    if (launching) return;
    if (!authenticated) { showLogin(); return; }
    launching = true;
    $('#btnLaunch').disabled = true;
    showProgress('start', 0, 'Начинаем запуск…');
    send({ cmd: 'launch', config: state.config });
  });

  // ---------- Мои файлы ----------
  const FILE_TYPE_NAMES = { mod: 'Мод', shader: 'Шейдер', resourcepack: 'Ресурспак', modpack: 'Модпак' };
  const ADD_LABELS = { mod: 'Добавить мод', shader: 'Добавить шейдер', resourcepack: 'Добавить ресурспак', modpack: 'Добавить модпак' };
  const SITE_ICON = '<svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.6"/><path d="M3 12h18M12 3c2.5 2.6 3.8 5.6 3.8 9S14.5 18.4 12 21c-2.5-2.6-3.8-5.6-3.8-9S9.5 5.6 12 3z" stroke="currentColor" stroke-width="1.5"/></svg>';
  const DELETE_ICON = '<svg viewBox="0 0 24 24" fill="none"><path d="M4 7h16M9 7V5a1 1 0 011-1h4a1 1 0 011 1v2M6 7l1 13a1 1 0 001 1h8a1 1 0 001-1l1-13" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>';
  const FILE_ICONS = {
    mod: '<svg viewBox="0 0 24 24" fill="none"><path d="M12 3l2.2 2.2h3.3a1 1 0 011 1v3.3L21 12l-2.5 2.5v3.3a1 1 0 01-1 1h-3.3L12 21l-2.2-2.2H6.5a1 1 0 01-1-1v-3.3L3 12l2.5-2.5V6.2a1 1 0 011-1h3.3z" stroke="currentColor" stroke-width="1.6"/></svg>',
    shader: '<svg viewBox="0 0 24 24" fill="none"><path d="M12 2l2 6 6 2-6 2-2 6-2-6-6-2 6-2z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/><path d="M19 16l.8 2.2L22 19l-2.2.8L19 22l-.8-2.2L16 19l2.2-.8z" fill="currentColor"/></svg>',
    resourcepack: '<svg viewBox="0 0 24 24" fill="none"><path d="M12 2.8l8 4.6v9.2l-8 4.6-8-4.6V7.4z" stroke="currentColor" stroke-width="1.5"/><path d="M4 7.4l8 4.6 8-4.6M12 12v9.2" stroke="currentColor" stroke-width="1.5"/></svg>',
    modpack: '<svg viewBox="0 0 24 24" fill="none"><rect x="3" y="4" width="18" height="16" rx="3" stroke="currentColor" stroke-width="1.6"/><path d="M3 9h18M8 4v5M8 14h8M8 17h5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>',
  };

  const FILE_SECTIONS = { mod: 'mods', shader: 'shaders', resourcepack: 'resourcepacks', modpack: 'modpacks' };

  function cleanFileName(name) {
    return (name || '')
      .replace(/\.(jar|zip|mcpack)(\.disabled)?$/i, '')
      .replace(/[-_ ]?[rv]?(\d+\.)+\d+.*$/i, '')
      .replace(/\+.*$/, '')
      .replace(/[-_ +]+(fabric|forge|neoforge|quilt|mc|minecraft)\b.*$/i, '')
      .replace(/(fabric|forge|neoforge|quilt)$/i, '')
      .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
      .replace(/[-_]+/g, ' ')
      .trim();
  }

  function fileSiteUrl(m) {
    if (m.projectUrl) return m.projectUrl;
    const name = cleanFileName(m.title || m.name);
    const section = FILE_SECTIONS[m.type] || 'mods';
    return name
      ? 'https://modrinth.com/' + section + '?q=' + encodeURIComponent(name)
      : 'https://modrinth.com/' + section;
  }

  function renderFiles(files) {
    const grid = $('#modsGrid');
    const filter = state.fileFilter || '';
    const list = filter === 'modpack'
      ? state.modpacks
      : (filter ? (files || []).filter((f) => f.type === filter) : (files || []));

    if (!list || !list.length) {
      grid.innerHTML = `<div class="shop-empty">${filter === 'modpack'
        ? 'Модпаков пока нет — установите их в мастерской.'
        : filter ? 'В этой категории пусто.' : 'Файлов пока нет — добавьте или установите из мастерской.'}</div>`;
      return;
    }

    grid.innerHTML = '';
    list.forEach((m) => {
      const card = document.createElement('div');
      card.className = 'mod-card glass' + (m.enabled === false ? ' mod-off' : '');
      const type = m.type || filter || 'mod';
      const typeName = FILE_TYPE_NAMES[type] || 'Файл';
      const letter = esc((m.title || m.name || '?').charAt(0).toUpperCase());
      const icon = m.iconUrl || '';
      const url = fileSiteUrl(m);
      const isPack = filter === 'modpack';
      const source = m.source === 'curseforge' ? 'CurseForge'
        : m.source === 'modrinth' ? 'Modrinth' : '';
      const glyph = FILE_ICONS[type] || `<span>${letter}</span>`;
      const desc = m.description || (isPack
        ? `${typeName} · ${m.fileCount || 0} файлов · ${fmtDl(m.downloads || 0)} скачиваний`
        : '');

      card.innerHTML = `
        <div class="mod-icon type-${esc(type)}">${icon
          ? `<img class="file-img" src="${esc(icon)}" alt="" loading="lazy"><span class="file-glyph" style="display:none">${glyph}</span>`
          : `<span class="file-glyph">${glyph}</span>`}</div>
        <div class="mod-info">
          <div class="mod-name">${esc(m.title || m.name)}</div>
          <div class="mod-size">${typeName}${source ? ` · ${source}` : ''}${isPack
            ? ` · ${esc(m.versionNumber || '')} · ${fmtDl(m.downloads || 0)} скач.`
            : ` · ${fmt(m.size)}`}</div>
          ${desc ? `<div class="mod-desc">${esc(desc)}</div>` : ''}
        </div>
        <div class="mod-actions">
          <button class="shop-info-btn file-site" title="Открыть страницу проекта">${SITE_ICON}</button>
          ${isPack
            ? `<button class="del-btn" title="Удалить модпак">${DELETE_ICON}</button>`
            : `<label class="switch"><input type="checkbox" ${m.enabled ? 'checked' : ''}><span class="slider"></span></label>
               <button class="del-btn" title="Удалить">${DELETE_ICON}</button>`}
        </div>`;

      const img = card.querySelector('.file-img');
      if (img) img.addEventListener('error', () => {
        img.remove();
        const glyphEl = card.querySelector('.file-glyph');
        if (glyphEl) glyphEl.style.display = '';
      });
      const site = card.querySelector('.file-site');
      if (site) site.addEventListener('click', () => send({ cmd: 'openUrl', url }));
      const del = card.querySelector('.del-btn');
      if (del) del.addEventListener('click', () => {
        const name = m.title || m.name;
        if (isPack) {
          if (confirm('Удалить модпак «' + name + '» и его файлы?')) send({ cmd: 'removeModpack', id: m.id });
        } else if (confirm('Удалить ' + typeName.toLowerCase() + ' ' + name + '?')) {
          send({ cmd: 'removeMod', file: m.path });
        }
      });
      const sw = card.querySelector('.switch input');
      if (sw) sw.addEventListener('change', (e) => {
        send({ cmd: 'toggleMod', file: m.path, enabled: e.target.checked });
      });
      grid.appendChild(card);
    });
  }

  function bindFileCats() {
    const cats = $$('#fileCats .cat-chip');
    cats.forEach((chip) => {
      chip.addEventListener('click', () => {
        cats.forEach((c) => c.classList.remove('active'));
        chip.classList.add('active');
        state.fileFilter = chip.dataset.type || '';
        $('#btnAddMod').textContent = ADD_LABELS[state.fileFilter] || 'Добавить файл';
        renderFiles(state.mods);
      });
    });
  }
  bindFileCats();
  $('#btnAddMod').textContent = ADD_LABELS[state.fileFilter || 'mod'] || 'Добавить файл';

  // Диалог «архив или папка» рисуется внутри лаунчера, отдельным WPF-окном больше не пользуемся.
  let addType = 'mod';
  function openAddDialog(type) {
    addType = type;
    const label = ADD_LABELS[type] || 'Добавить файл';
    const noun = type === 'shader' ? 'шейдеров' : 'ресурспаков';
    $('#addModeTitle').textContent = label;
    $('#addModeText').textContent = `Добавьте архив ${noun} или распакованную папку — файл появится в этом разделе.`;
    $$('.choice-desc').forEach((el) => {
      el.textContent = el.closest('.choice').id === 'addModeArchive'
        ? (type === 'shader' ? '.zip — архив шейдеров' : '.zip — архив ресурспаков')
        : 'Распакованная папка';
    });
    $('#addModeOverlay').classList.add('show');
  }
  function closeAddDialog() { $('#addModeOverlay').classList.remove('show'); }
  function chooseAddMode(mode) {
    closeAddDialog();
    send({ cmd: 'addMod', type: addType, mode });
  }

  $('#btnAddMod').addEventListener('click', () => {
    const type = state.fileFilter || 'mod';
    if (type === 'shader' || type === 'resourcepack') openAddDialog(type);
    else send({ cmd: 'addMod', type });
  });
  $('#addModeArchive').addEventListener('click', () => chooseAddMode('file'));
  $('#addModeFolder').addEventListener('click', () => chooseAddMode('folder'));
  $('#addModeCancel').addEventListener('click', closeAddDialog);
  $('#addModeOverlay').addEventListener('click', (e) => {
    if (e.target.id === 'addModeOverlay') closeAddDialog();
  });
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && $('#addModeOverlay').classList.contains('show')) closeAddDialog();
  });

  // Вкладка «Поддержка»: ссылка открывается системным браузером через лаунчер.
  $$('[data-open-url]').forEach((b) => b.addEventListener('click', () => send({ cmd: 'openUrl', url: b.dataset.openUrl })));
  $('#btnOpenMods').addEventListener('click', () => {
    const type = state.fileFilter || '';
    const key = type === 'shader' ? 'shaderpacks'
      : type === 'resourcepack' ? 'resourcepacks'
      : type === 'mod' ? 'mods' : 'gameDir';
    send({ cmd: 'openPath', key });
  });

  // ---------- Catal: ИИ-помощник ----------
  let aiHistory = [];
  let aiBusy = false;
  let aiImage = null;
  let aiTyping = null;

  function setAiStatus(available, reason) {
    const el = $('#aiStatus');
    if (!el) return;
    el.textContent = available ? 'На связи · может видеть скриншоты' : (reason || 'Сейчас недоступен');
    el.style.color = available ? 'var(--green)' : 'var(--yellow)';
  }

  function trimAiHistory() {
    if (aiHistory.length > 10) aiHistory = aiHistory.slice(-10);
  }

  function renderAiText(text) {
    const holder = document.createElement('div');
    holder.textContent = text || '';
    let html = holder.innerHTML; // текст уже экранирован
    html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    html = html.replace(/`([^`]+)`/g, '<code>$1</code>');
    html = html.replace(/(https?:\/\/[^\s<]+)/g, '<a href="$1" target="_blank" rel="noopener">$1</a>');
    return html;
  }

  function scrollAi() {
    const box = $('#aiMessages');
    if (box) box.scrollTop = box.scrollHeight;
  }

  function appendAiMessage(role, text, image) {
    const box = $('#aiMessages');
    if (!box) return;
    const empty = $('#aiEmpty');
    if (empty) empty.remove();
    const el = document.createElement('div');
    el.className = 'ai-msg ' + (role === 'user' ? 'user' : role === 'error' ? 'error' : 'model');
    el.innerHTML = renderAiText(text);
    if (image) {
      const img = document.createElement('img');
      img.className = 'ai-msg-image';
      img.src = image;
      img.alt = 'Прикреплённый скриншот';
      el.appendChild(img);
    }
    box.appendChild(el);
    scrollAi();
  }

  function setAiSendState() {
    const btn = $('#aiSend');
    const input = $('#aiInput');
    if (btn) btn.disabled = aiBusy || !((input?.value || '').trim() || aiImage);
  }

  function setAiBusy(busy) {
    aiBusy = busy;
    setAiSendState();
    const box = $('#aiMessages');
    if (busy && box) {
      if (!aiTyping) {
        aiTyping = document.createElement('div');
        aiTyping.className = 'ai-typing';
        aiTyping.innerHTML = '<i></i><i></i><i></i>';
        box.appendChild(aiTyping);
      }
      scrollAi();
    } else if (aiTyping) {
      aiTyping.remove();
      aiTyping = null;
    }
  }

  function prepareAiImage(file) {
    return new Promise((resolve, reject) => {
      if (!file || !file.type.startsWith('image/')) { reject(new Error('Нужен файл изображения.')); return; }
      if (file.size > 20 * 1024 * 1024) { reject(new Error('Изображение слишком большое — максимум 20 МБ.')); return; }
      const reader = new FileReader();
      reader.onerror = () => reject(new Error('Не удалось прочитать изображение.'));
      reader.onload = () => {
        const img = new Image();
        img.onerror = () => reject(new Error('Не удалось открыть изображение.'));
        img.onload = () => {
          const max = 1600;
          let w = img.naturalWidth || img.width;
          let h = img.naturalHeight || img.height;
          const scale = Math.min(1, max / Math.max(w, h));
          w = Math.max(1, Math.round(w * scale));
          h = Math.max(1, Math.round(h * scale));
          const canvas = document.createElement('canvas');
          canvas.width = w; canvas.height = h;
          const ctx = canvas.getContext('2d');
          if (!ctx) { reject(new Error('Браузер не смог обработать изображение.')); return; }
          ctx.drawImage(img, 0, 0, w, h);
          resolve(canvas.toDataURL('image/jpeg', 0.82));
        };
        img.src = reader.result;
      };
      reader.readAsDataURL(file);
    });
  }

  function updateAiAttachment() {
    const chip = $('#aiAttachment');
    if (!chip) return;
    chip.hidden = !aiImage;
    const thumb = $('#aiAttachmentThumb');
    if (thumb) {
      thumb.hidden = !aiImage;
      thumb.src = aiImage || '';
    }
    $('#aiAttachmentName').textContent = aiImage ? 'Скриншот прикреплён' : '';
    setAiSendState();
  }

  async function attachAiFile(file) {
    if (!file) return;
    try {
      const prepared = await prepareAiImage(file);
      aiImage = prepared;
      updateAiAttachment();
    } catch (err) {
      appendAiMessage('error', err.message || 'Не удалось прикрепить изображение.');
    }
  }

  function sendAi() {
    if (aiBusy) return;
    const input = $('#aiInput');
    const text = (input.value || '').trim();
    if (!text && !aiImage) return;
    const image = aiImage;
    const shown = text || '[скриншот]';
    const history = aiHistory.slice();
    aiHistory.push({ role: 'user', text: shown });
    trimAiHistory();
    appendAiMessage('user', shown, image);
    input.value = '';
    input.style.height = '';
    aiImage = null;
    updateAiAttachment();
    setAiBusy(true);
    send({ cmd: 'aiAsk', message: text || 'Посмотри на скриншот и помоги разобраться.', history, image });
  }

  $('#aiSend')?.addEventListener('click', sendAi);
  $('#aiInput')?.addEventListener('input', (e) => {
    e.target.style.height = 'auto';
    e.target.style.height = Math.min(e.target.scrollHeight, 130) + 'px';
    setAiSendState();
  });
  $('#aiInput')?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendAi(); }
  });

  // Ctrl+V со скриншотом работает в любом месте вкладки «Поддержка», а не только в поле ввода.
  document.addEventListener('paste', (e) => {
    const items = e.clipboardData && e.clipboardData.items;
    if (!items) return;
    for (const item of items) {
      if (item.kind !== 'file' || !(item.type || '').startsWith('image/')) continue;
      const blob = item.getAsFile();
      if (!blob) continue;
      e.preventDefault();
      const file = (blob.type || '').startsWith('image/')
        ? blob
        : new File([blob], 'screenshot.png', { type: item.type || 'image/png' });
      attachAiFile(file);
      return;
    }
  }, true);

  $('#aiAttach')?.addEventListener('click', () => $('#aiImageInput').click());
  $('#aiImageInput')?.addEventListener('change', (e) => {
    const file = e.target.files && e.target.files[0];
    e.target.value = '';
    attachAiFile(file);
  });
  $('#aiAttachmentRemove')?.addEventListener('click', () => {
    aiImage = null;
    const input = $('#aiImageInput');
    if (input) input.value = '';
    updateAiAttachment();
  });
  setAiSendState();

  // ---------- Мастерская ----------
  function setFiltersOpen(open) {
    $('#shopLayout').classList.toggle('filters-closed', !open);
    const toggle = $('#filterToggle');
    toggle.setAttribute('aria-expanded', String(open));
    toggle.title = open ? 'Скрыть фильтры' : 'Показать фильтры';
    $('#filterToggleLabel').textContent = open ? 'Фильтры' : 'Показать';
  }
  $('#filterToggle').addEventListener('click', () => setFiltersOpen($('#shopLayout').classList.contains('filters-closed')));
  $('#filterClose').addEventListener('click', () => setFiltersOpen(false));

  $('#shopReset').addEventListener('click', () => {
    $$('.shop-tab').forEach((t) => t.classList.toggle('active', t.dataset.src === 'modrinth'));
    $$('.type-btn').forEach((b) => b.classList.toggle('active', b.dataset.type === 'mod'));
    state.shop.source = 'modrinth';
    state.shop.type = 'mod';
    state.shop.category = '';
    state.shop.loader = 'fabric';
    state.shop.sort = 'downloads';
    state.shop.offset = 0;
    $('#shopLoader').value = 'fabric';
    $('#shopSort').value = 'downloads';
    $('#shopQuery').value = '';
    loadCategories();
    search();
  });

  $$('.shop-tab').forEach((tab) => {
    tab.addEventListener('click', () => {
      $$('.shop-tab').forEach((t) => t.classList.remove('active'));
      tab.classList.add('active');
      state.shop.source = tab.dataset.src;
      state.shop.category = '';
      state.shop.offset = 0;
      loadCategories();
      search();
    });
  });

  $$('.type-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      $$('.type-btn').forEach((b) => b.classList.remove('active'));
      btn.classList.add('active');
      state.shop.type = btn.dataset.type;
      state.shop.category = '';
      state.shop.offset = 0;
      loadCategories();
      search();
    });
  });

  $('#shopLoader').addEventListener('change', () => {
    state.shop.loader = $('#shopLoader').value;
    state.shop.offset = 0;
    search();
  });
  $('#shopSort').addEventListener('change', () => {
    state.shop.sort = $('#shopSort').value;
    state.shop.offset = 0;
    search();
  });

  $('#shopCats').addEventListener('click', (e) => {
    const chip = e.target.closest('.cat-chip');
    if (!chip) return;
    const active = chip.classList.contains('active');
    $$('#shopCats .cat-chip').forEach((c) => c.classList.remove('active'));
    if (!active) {
      chip.classList.add('active');
      state.shop.category = chip.dataset.id;
    } else {
      state.shop.category = '';
      const all = $('#shopCats .cat-chip');
      if (all) all.classList.add('active');
    }
    state.shop.offset = 0;
    search();
  });

  let searchTimer = null;
  $('#shopQuery').addEventListener('input', () => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(search, 350);
  });

  function search() {
    state.shop.seq++;
    state.shop.busy = true;
    state.shop.items = [];
    state.shop.offset = 0;
    state.shop.total = 0;
    $('#shopGrid').innerHTML = '<div class="shop-empty">Загрузка…</div>';
    $('#shopListHint').textContent = 'Ищем в каталоге…';
    send({
      cmd: 'search',
      source: state.shop.source, type: state.shop.type,
      category: state.shop.category, loader: state.shop.loader, sort: state.shop.sort,
      query: $('#shopQuery').value.trim(), offset: 0, seq: state.shop.seq,
    });
  }

  function loadCategories() {
    state.shop.catsLoaded = true;
    send({ cmd: 'catalogMeta', source: state.shop.source, type: state.shop.type });
  }

  function loadMore() {
    if (state.shop.busy) return;
    if (state.shop.items.length >= state.shop.total) return;
    state.shop.busy = true;
    send({
      cmd: 'search',
      source: state.shop.source, type: state.shop.type,
      category: state.shop.category, loader: state.shop.loader, sort: state.shop.sort,
      query: $('#shopQuery').value.trim(), offset: state.shop.offset, seq: state.shop.seq,
    });
  }

  $('.content').addEventListener('scroll', () => {
    const el = $('.content');
    if (el.scrollTop + el.clientHeight >= el.scrollHeight - 500) loadMore();
  });

  function normKey(s) {
    return (s || '').toLowerCase().replace(/\.(jar|zip|mrpack)$/i, '').replace(/[^\w]+/g, '');
  }
  function isInstalled(item) {
    if (state.shop.type === 'modpack') {
      const id = String(item.id || '');
      if (!id) return false;
      return state.modpackIds.has(id);
    }
    if (!state.shop.installed) return false;
    const name = normKey(item.title);
    if (!name) return false;
    for (const f of state.shop.installed) {
      const fn = normKey(f);
      if (fn && (fn.includes(name) || name.includes(fn))) return true;
    }
    return false;
  }

  function renderShop(items) {
    const grid = $('#shopGrid');
    const hint = $('#shopListHint');
    hint.textContent = state.shop.total
      ? `Найдено ${state.shop.total.toLocaleString('ru')} · показано ${items.length.toLocaleString('ru')}`
      : 'Ничего не найдено — попробуйте другие фильтры';
    grid.innerHTML = '';
    if (!items.length) {
      grid.innerHTML = '<div class="shop-empty">Ничего не найдено</div>';
      return;
    }
    const isPack = state.shop.type === 'modpack';
    items.forEach((item) => {
      const card = document.createElement('div');
      card.className = 'shop-card glass';
      card.dataset.id = item.id;
      const dl = fmtDl(item.downloads || 0);
      const src = state.shop.source;
      card.innerHTML = `
        <div class="shop-top">
          <div class="shop-logo">${item.iconUrl ? `<img src="${esc(item.iconUrl)}" alt="" loading="lazy" onerror="this.style.display='none'">` : ''}</div>
          <div class="shop-title">${esc(item.title)}</div>
          <span class="src-badge ${src === 'curseforge' ? 'cf' : 'mr'}">${src === 'curseforge' ? 'CF' : 'MR'}</span>
          ${item.projectUrl ? `<button class="shop-info-btn" title="Открыть страницу проекта">${SITE_ICON}</button>` : ''}
        </div>
        <div class="shop-desc">${esc(item.description)}</div>
        <div class="shop-meta">
          <span class="author">${esc(item.author || '')}</span>
          <span class="downloads">⬇ ${dl}</span>
        </div>
        <div class="shop-actions">
          <select class="shop-version-select" data-id="${esc(item.id)}">
            <option value="">Версия…</option>
            <option value="latest" selected>Последний релиз</option>
          </select>
        </div>
        <button class="shop-install${isInstalled(item) ? ' installed' : ''}" data-id="${esc(item.id)}">${isInstalled(item) ? 'Установлено' : 'Установить'}</button>`;

      const infoBtn = card.querySelector('.shop-info-btn');
      if (infoBtn) infoBtn.addEventListener('click', () => send({ cmd: 'openUrl', url: item.projectUrl }));

      const sel = card.querySelector('.shop-version-select');
      if (sel) {
        sel.addEventListener('change', () => {
          item.selectedVersion = selectedVersion(item, sel.value);
        });
        sel.addEventListener('focus', () => loadVersions(item.id));
      }

      card.querySelector('.shop-install').addEventListener('click', (e) => {
        if (isInstalled(item)) return;
        const btn = e.currentTarget;
        item.selectedVersion = selectedVersion(item, sel ? sel.value : 'latest');
        btn.disabled = true;
        btn.textContent = 'Установка…';
        if (isPack) send({ cmd: 'installModpack', source: src, item });
        else send({ cmd: 'install', source: src, type: state.shop.type, item });
      });
      grid.appendChild(card);
    });

    const foot = document.createElement('div');
    foot.className = 'shop-empty';
    foot.textContent = state.shop.items.length >= state.shop.total
      ? `Каталог: показаны все ${state.shop.total.toLocaleString('ru')}`
      : `Загружено ${state.shop.items.length.toLocaleString('ru')} из ${state.shop.total.toLocaleString('ru')} — листайте вниз`;
    grid.appendChild(foot);
  }

  function selectedVersion(item, value) {
    if (!value || value === 'latest') return null;
    return (state.shop.versions[item.id] || []).find((v) => v.versionId === value) || null;
  }

  function loadVersions(projectId) {
    if (!projectId) return;
    if (state.shop.versions[projectId]) return;
    if (state.shop.versionsLoading.has(projectId)) return;
    state.shop.versionsLoading.add(projectId);
    send({ cmd: 'versions', source: state.shop.source, type: state.shop.type, id: projectId });
  }

  function renderVersions(itemId, versions) {
    state.shop.versions[itemId] = versions || [];
    state.shop.versionsLoading.delete(itemId);
    const sel = document.querySelector(`.shop-version-select[data-id="${CSS.escape(itemId)}"]`);
    if (!sel) return;
    const prev = sel.value;
    sel.innerHTML = '<option value="latest">Последний релиз</option>';
    (versions || []).forEach((v) => {
      const opt = document.createElement('option');
      opt.value = v.versionId;
      opt.textContent = v.versionNumber || v.fileName || v.versionId;
      sel.appendChild(opt);
    });
    sel.value = prev === 'latest' || !prev ? 'latest'
      : ([...sel.options].some((o) => o.value === prev) ? prev : 'latest');
  }

  function fmtDl(n) {
    n = Number(n) || 0;
    if (n >= 1_000_000) return (n / 1_000_000).toFixed(1).replace('.0', '') + ' млн';
    if (n >= 1_000) return (n / 1_000).toFixed(1).replace('.0', '') + ' тыс';
    return String(n);
  }

  function renderCategories(cats) {
    const box = $('#shopCats');
    box.innerHTML = '';
    $('#shopCatGroup').hidden = !(cats && cats.length);
    const all = document.createElement('div');
    all.className = 'cat-chip' + (state.shop.category ? '' : ' active');
    all.dataset.id = '';
    all.textContent = 'Все';
    box.appendChild(all);
    (cats || []).forEach((c) => {
      const chip = document.createElement('div');
      chip.className = 'cat-chip' + (state.shop.category === c.id ? ' active' : '');
      chip.dataset.id = c.id;
      chip.textContent = c.name;
      box.appendChild(chip);
    });
  }

  // ---------- Настройки ----------
  function renderSettings(cfg) {
    if (!cfg) return;
    $('#setUsername').value = cfg.mcNickname || cfg.username || 'Player';
    const clientVal = $('#setClientNameVal');
    if (clientVal) clientVal.textContent = cfg.username || '—';
    $('#setVersion').textContent = cfg.version ?? '1.21.11';
    $('#setAccent').value = '#' + ((cfg.accent ?? 0x7c5cff) & 0xffffff).toString(16).padStart(6, '0');
    const ram = cfg.ramMb ?? 4096;
    $('#setRam').value = ram;
    $('#setRamVal').textContent = ram + ' МБ';
    $('#setGameDirPath').textContent = cfg.gameDir ?? '';
    $('#setJavaPath').textContent = cfg.javaPath || 'Автоопределение';
  }

  function collectConfig() {
    const cfg = Object.assign({}, state.config || {});
    cfg.mcNickname = $('#setUsername').value || 'Player';
    cfg.isAdmin = state.isAdmin;
    cfg.accent = (parseInt($('#setAccent').value.replace('#', ''), 16) | 0xff000000) >>> 0;
    cfg.ramMb = parseInt($('#setRam').value) || 4096;
    cfg.version = state.config?.version || '1.21.11';
    cfg.modules = cfg.modules || {};
    return cfg;
  }

  $('#setUsername').addEventListener('change', saveSettings);
  $('#setAccent').addEventListener('change', saveSettings);
  $('#setRam').addEventListener('input', () => {
    $('#setRamVal').textContent = $('#setRam').value + ' МБ';
  });
  $('#setRam').addEventListener('change', saveSettings);

  function saveSettings() {
    state.config = collectConfig();
    applyAccent(state.config);
    renderHome(state.config);
    send({ cmd: 'saveConfig', config: state.config });
  }

  $('#btnGameDir').addEventListener('click', () => send({ cmd: 'pickFolder', key: 'gameDir' }));
  $('#btnOpenGameDir').addEventListener('click', () => send({ cmd: 'openPath', key: 'gameDir' }));
  $('#btnJava').addEventListener('click', () => send({ cmd: 'pickJava' }));
  $('#btnOpenJava').addEventListener('click', () => send({ cmd: 'openPath', key: 'java' }));

  // ---------- Прогресс запуска ----------
  function showProgress(step, percent, message) {
    $('#overlay').classList.add('show');
    $('#ovFill').style.width = Math.max(0, Math.min(100, percent)) + '%';
    $('#ovPercent').textContent = Math.round(percent) + '%';
    $('#ovMessage').textContent = message || '';
  }
  function hideProgress() { $('#overlay').classList.remove('show'); }

  // ---------- Toast ----------
  let toastBox = null;
  function toast(text, isError) {
    if (!toastBox) {
      toastBox = document.createElement('div');
      toastBox.id = 'toast';
      document.body.appendChild(toastBox);
    }
    toastBox.textContent = text;
    toastBox.className = isError ? 'show error' : 'show';
    setTimeout(() => (toastBox.className = ''), 3400);
  }

  // ---------- Хелперы ----------
  function esc(s) {
    return String(s ?? '').replace(/[&<>"']/g, (c) =>
      ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  }

  // ---------- Старт ----------
  renderDownloads();
  send({ cmd: 'getState' });
  send({ cmd: 'checkAuth' });
  setTimeout(() => { search(); }, 600);
})();
