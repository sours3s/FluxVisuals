// ============ FluxVisuals Launcher UI ============
(() => {
  const $ = (sel) => document.querySelector(sel);
  const $$ = (sel) => [...document.querySelectorAll(sel)];

  const send = (obj) => chrome.webview.postMessage(obj);
  const fmt = (n) => (n / 1024 / 1024).toFixed(1) + ' МБ';

  let state = {
    config: null,
    isAdmin: false,
    role: 'user',
    hasAccess: false,
    accessExpiresAt: null,
    mods: [],
    fileFilter: '',
    shop: {
      source: 'modrinth', type: 'mod', category: '', loader: 'fabric', sort: 'downloads',
      items: [], offset: 0, total: 0, seq: 0, busy: false, catsLoaded: false,
      versions: {},   // кэш списка версий по id проекта
      versionsLoading: new Set(),
    },
  };
  let launching = false;
  let authenticated = false;

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
    });
  });

  // ---------- Окно ----------
  $('#btnMin').addEventListener('click', () => send({ cmd: 'minimize' }));
  $('#btnMax').addEventListener('click', () => send({ cmd: 'toggleMaximize' }));
  $('#btnClose').addEventListener('click', () => send({ cmd: 'close' }));
  document.querySelector('.tb-drag').addEventListener('mousedown', (e) => {
    if (e.button === 0 && e.target === e.currentTarget) send({ cmd: 'drag' });
  });

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
        if (state.config) state.config.isAdmin = state.isAdmin;
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
        setAuthState(false);
        showLogin();
        break;
      }
      case 'mods':
      case 'files':
        state.mods = msg.files || msg.mods || [];
        if (msg.installed) state.shop.installed = new Set(msg.installed);
        renderFiles(state.mods);
        // Обновляем карточки магазина, чтобы кнопка «Установлено» появилась сразу после установки
        if (state.shop.items && state.shop.items.length) renderShop(state.shop.items);
        break;
      case 'folderPicked':
        // Обязательно синхронизируем state.config — иначе сохранение настроек/запуск
        // перезапишут выбранный путь старым (баг «выбрал диск D, а качает на C»).
        if (msg.key === 'gameDir') {
          if (state.config) state.config.gameDir = msg.path;
          $('#setGameDirPath').textContent = msg.path || '—';
        } else if (msg.key === 'java') {
          if (state.config) state.config.javaPath = msg.path;
          $('#setJavaPath').textContent = msg.path || '—';
        }
        break;
      case 'searchResult':
        if (msg.seq !== state.shop.seq) return; // устаревший ответ (пользователь уже переключился)
        state.shop.busy = false;
        state.shop.source = msg.source;
        const items = msg.items || [];
        state.shop.items = msg.offset === 0 ? items : state.shop.items.concat(items);
        state.shop.offset = msg.offset + items.length;
        state.shop.total = msg.total;
        renderShop(state.shop.items);
        break;
      case 'installResult':
        toast(msg.ok ? `Установлено: ${msg.file}` : `Ошибка: ${msg.message}`);
        break;
      case 'catalogMeta':
        if (msg.source === state.shop.source) renderCategories(msg.categories || []);
        break;
      case 'versions':
        renderVersions(msg.id, msg.versions || []);
        break;
      case 'progress':
        showProgress(msg.step, msg.percent, msg.message);
        break;
      case 'launchState':
        if (msg.state === 'launched') {
          launching = false;
          $('#btnLaunch').disabled = false;
          toast('Minecraft запущен!');
          setTimeout(hideProgress, 400);
          // Игра пошла — закрываем лоадер, чтобы остался только процесс Minecraft.
          setTimeout(() => send({ cmd: 'close' }), 700);
        } else if (msg.state === 'error') {
          launching = false;
          $('#btnLaunch').disabled = false;
          hideProgress();
          toast('Ошибка запуска: ' + msg.message, true);
        }
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
    // «Игрок» на главной = Minecraft-ник (с ним заходишь на сервера)
    $('#homeUser').textContent = cfg.mcNickname || cfg.username || 'Player';
    $('#homeUid').textContent = cfg.uid ? 'UID ' + cfg.uid : '—';
    $('#homeVersion').textContent = cfg.version;
    $('#homeRam').textContent = cfg.ramMb + ' МБ';
    $('#sfUserName').textContent = cfg.username;
    // avatar uses FluxVisuals logo image
    $('#ramLabel').textContent = Math.round(cfg.ramMb / 1024) + ' ГБ';
    $('#ramFill').style.width = Math.min(100, (cfg.ramMb / 16384) * 100) + '%';

    // Статус доступа
    const acc = $('#homeAccess');
    if (state.hasAccess) {
      acc.textContent = 'Активен ✓';
      acc.style.color = '#4ade80';
    } else {
      acc.textContent = state.role === 'user' ? 'Нет (купи)' : 'Истёк';
      acc.style.color = '#f85149';
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

  // ---------- Мои файлы (моды + шейдеры + ресурспаки) ----------
  const FILE_TYPE_NAMES = { mod: 'Мод', shader: 'Шейдер', resourcepack: 'Ресурспак' };
  function renderFiles(files) {
    const grid = $('#modsGrid');
    if (!files || !files.length) {
      grid.innerHTML = '<div class="shop-empty">Файлов пока нет — добавьте или установите из магазина.</div>';
      return;
    }
    const filter = state.fileFilter || '';
    const list = filter ? files.filter((f) => f.type === filter) : files;
    if (!list.length) {
      grid.innerHTML = '<div class="shop-empty">В этой категории пусто.</div>';
      return;
    }
    grid.innerHTML = '';
    list.forEach((m) => {
      const card = document.createElement('div');
      card.className = 'mod-card' + (m.enabled ? '' : ' mod-off');
      const typeName = FILE_TYPE_NAMES[m.type] || 'Файл';
      const letter = esc((m.name || '?').charAt(0).toUpperCase());
      card.innerHTML = `
        <div class="mod-icon">${m.iconUrl
          ? `<img src="${esc(m.iconUrl)}" alt="" loading="lazy" onload="this.nextElementSibling.style.display='none'" onerror="this.style.display='none'">`
          : ''}<span>${letter}</span></div>
        <div class="mod-info">
          <div class="mod-name">${esc(m.name)}</div>
          <div class="mod-size">${typeName} · ${fmt(m.size)}</div>
        </div>
        <div class="mod-actions">
          <label class="switch"><input type="checkbox" ${m.enabled ? 'checked' : ''}><span class="slider"></span></label>
          <button class="del-btn" title="Удалить"><svg viewBox="0 0 24 24" fill="none"><path d="M4 7h16M9 7V5a1 1 0 011-1h4a1 1 0 011 1v2M6 7l1 13a1 1 0 001 1h8a1 1 0 001-1l1-13" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg></button>
        </div>`;
      card.querySelector('.switch input').addEventListener('change', (e) => {
        send({ cmd: 'toggleMod', file: m.path, enabled: e.target.checked });
      });
      card.querySelector('.del-btn').addEventListener('click', () => {
        if (confirm('Удалить ' + typeName.toLowerCase() + ' ' + m.name + '?')) send({ cmd: 'removeMod', file: m.path });
      });
      grid.appendChild(card);
    });
  }

  // Фильтр по типу
  function bindFileCats() {
    const cats = document.querySelectorAll('#fileCats .cat-chip');
    cats.forEach((chip) => {
      chip.addEventListener('click', () => {
        cats.forEach((c) => c.classList.remove('active'));
        chip.classList.add('active');
        state.fileFilter = chip.dataset.type || '';
        renderFiles(state.mods);
      });
    });
  }
  bindFileCats();

  $('#btnAddMod').addEventListener('click', () => send({ cmd: 'addMod' }));
  $('#btnOpenMods').addEventListener('click', () => {
    const type = state.fileFilter || '';
    const key = type === 'shader' ? 'shaderpacks' : type === 'resourcepack' ? 'resourcepacks' : type === 'mod' ? 'mods' : 'gameDir';
    send({ cmd: 'openPath', key });
  });

  // ---------- Мастерская ----------
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

  // фильтры: загрузчик и сортировка
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

  // категории — кликабельные чипсы
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
    }
    state.shop.offset = 0;
    search();
  });

  let searchTimer = null;
  $('#shopQuery').addEventListener('input', () => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(search, 350);
  });

  // пустой запрос → весь каталог выбранного типа
  function search() {
    state.shop.seq++;
    state.shop.busy = true;
    state.shop.items = [];
    state.shop.offset = 0;
    state.shop.total = 0;
    $('#shopGrid').innerHTML = '<div class="shop-empty">Загрузка…</div>';
    send({
      cmd: 'search',
      source: state.shop.source, type: state.shop.type,
      category: state.shop.category, loader: state.shop.loader, sort: state.shop.sort,
      query: $('#shopQuery').value.trim(), offset: 0, seq: state.shop.seq,
    });
  }

  // загрузка категорий для текущего источника и типа (один раз на смену)
  function loadCategories() {
    state.shop.catsLoaded = true;
    send({ cmd: 'catalogMeta', source: state.shop.source, type: state.shop.type });
  }

  // бесконечная прокрутка: докрутили до низа → следующая страница
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

  document.querySelector('.content').addEventListener('scroll', () => {
    const el = document.querySelector('.content');
    if (el.scrollTop + el.clientHeight >= el.scrollHeight - 500) loadMore();
  });

  function normKey(s) {
    return (s || '').toLowerCase().replace(/\.(jar|zip)$/i, '').replace(/[^\w]+/g, '');
  }
  function isInstalled(item) {
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
    grid.innerHTML = '';
    if (!items.length) {
      grid.innerHTML = '<div class="shop-empty">Ничего не найдено</div>';
      return;
    }
    items.forEach((item) => {
      const card = document.createElement('div');
      card.className = 'shop-card';
      card.dataset.id = item.id;
      const dl = fmtDl(item.downloads || 0);
      card.innerHTML = `
        <div class="shop-top">
          <div class="shop-logo">${item.iconUrl ? `<img src="${esc(item.iconUrl)}" alt="">` : ''}</div>
          <div class="shop-title">${esc(item.title)}</div>
          ${item.projectUrl ? `<button class="shop-info-btn" title="Открыть страницу проекта"><svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.6"/><path d="M12 11v5M12 8v.01" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg></button>` : ''}
        </div>
        <div class="shop-desc">${esc(item.description)}</div>
        <div class="shop-meta">
          <span class="author">${esc(item.author || '')}</span>
          <span class="downloads">⬇ ${dl}</span>
        </div>
        <div class="shop-actions">
          ${state.shop.type === 'mod' ? `
          <select class="shop-version-select" data-id="${esc(item.id)}">
            <option value="">Версия…</option>
            <option value="latest" selected>Последний релиз</option>
          </select>` : ''}
        </div>
        <button class="shop-install${isInstalled(item) ? ' installed' : ''}" data-id="${esc(item.id)}">${isInstalled(item) ? 'Установлено' : 'Установить'}</button>`;

      // Info — открыть страницу проекта в браузере
      const infoBtn = card.querySelector('.shop-info-btn');
      if (infoBtn) infoBtn.addEventListener('click', () => {
        send({ cmd: 'openUrl', url: item.projectUrl });
      });

      // выбор версии → грузим список версий проекта (только для модов; у шейдеров/РП селектора нет)
      const sel = card.querySelector('.shop-version-select');
      if (sel) {
        sel.addEventListener('change', () => {
          if (sel.value && sel.value !== 'latest') {
            const v = (state.shop.versions[item.id] || []).find(x => x.versionId === sel.value);
            if (v) item.selectedVersion = v;
          } else {
            item.selectedVersion = null;
          }
        });
        sel.addEventListener('focus', () => loadVersions(item.id));
      }

      card.querySelector('.shop-install').addEventListener('click', (e) => {
        if (isInstalled(item)) return;
        const btn = e.currentTarget;
        const sel = card.querySelector('.shop-version-select');
        if (sel && sel.value && sel.value !== 'latest') {
          const v = (state.shop.versions[item.id] || []).find(x => x.versionId === sel.value);
          if (v) item.selectedVersion = v;
        } else {
          item.selectedVersion = null;
        }
        btn.disabled = true; btn.textContent = 'Установка…';
        send({ cmd: 'install', source: state.shop.source, type: state.shop.type, item });
      });
      grid.appendChild(card);
    });

    // футер: сколько загружено / всё показано
    const foot = document.createElement('div');
    foot.className = 'shop-empty';
    foot.textContent = state.shop.items.length >= state.shop.total
      ? `Каталог: показаны все ${state.shop.total.toLocaleString('ru')}`
      : `Загружено ${state.shop.items.length.toLocaleString('ru')} из ${state.shop.total.toLocaleString('ru')} — листайте вниз`;
    grid.appendChild(foot);
  }

  // загрузка списка версий проекта (мод/шейдер/РП) для выпадающего списка
  function loadVersions(projectId) {
    if (!projectId) return;
    if (state.shop.versions[projectId]) return;          // уже загружено
    if (state.shop.versionsLoading.has(projectId)) return; // уже грузится
    state.shop.versionsLoading.add(projectId);
    send({ cmd: 'versions', source: state.shop.source, type: state.shop.type, id: projectId });
  }

  function renderVersions(itemId, versions) {
    state.shop.versions[itemId] = versions || [];
    state.shop.versionsLoading.delete(itemId);
    const sel = document.querySelector(`.shop-version-select[data-id="${CSS.escape(itemId)}"]`);
    if (!sel) return;
    // сохранить текущий выбор
    const prev = sel.value;
    sel.innerHTML = '<option value="latest">Последний релиз</option>';
    (versions || []).forEach((v) => {
      const opt = document.createElement('option');
      opt.value = v.versionId;
      opt.textContent = v.versionNumber || v.fileName || v.versionId;
      sel.appendChild(opt);
    });
    sel.value = prev === 'latest' || !prev ? 'latest' : (prev && [...sel.options].some(o => o.value === prev) ? prev : 'latest');
  }

  function fmtDl(n) {
    if (n >= 1_000_000) return (n / 1_000_000).toFixed(1).replace('.0', '') + ' млн';
    if (n >= 1_000) return (n / 1_000).toFixed(1).replace('.0', '') + ' тыс';
    return String(n);
  }

  // категории в виде чипсов
  function renderCategories(cats) {
    const box = $('#shopCats');
    box.innerHTML = '';
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
    // Никнейм = Minecraft-ник (с ним заходишь на сервера), отдельно от ника аккаунта.
    $('#setUsername').value = cfg.mcNickname || cfg.username || 'Player';
    // Клиент (watermark) = ник аккаунта с сайта; не редактируется.
    const clientVal = $('#setClientNameVal');
    if (clientVal) clientVal.textContent = cfg.username || '—';
    $('#setVersion').textContent = cfg.version ?? '1.21.11';
    $('#setAccent').value = '#' + ((cfg.accent ?? 0xA855F7) & 0xFFFFFF).toString(16).padStart(6, '0');
    const ram = cfg.ramMb ?? 4096;
    $('#setRam').value = ram;
    $('#setRamVal').textContent = ram + ' МБ';
    $('#setGameDirPath').textContent = cfg.gameDir ?? '';
    $('#setJavaPath').textContent = cfg.javaPath || 'Автоопределение';

    const box = $('#moduleToggles');
    box.innerHTML = '';
    const modules = Object.keys(cfg.modules || {});
    if (!modules.length) {
      box.innerHTML = '<div class="set-hint">Модули появятся после первого запуска</div>';
      return;
    }
    modules.forEach((name) => {
      const div = document.createElement('div');
      div.className = 'mt-item';
      div.innerHTML = `<span>${esc(name)}</span>
        <label class="switch"><input type="checkbox" ${cfg.modules[name] ? 'checked' : ''}><span class="slider"></span></label>`;
      div.querySelector('input').addEventListener('change', (e) => {
        state.config.modules[name] = e.target.checked;
        send({ cmd: 'saveConfig', config: state.config });
      });
      box.appendChild(div);
    });
  }

  function collectConfig() {
    const cfg = Object.assign({}, state.config || {});
    cfg.mcNickname = $('#setUsername').value || 'Player';
    cfg.isAdmin = state.isAdmin;
    cfg.accent = parseInt($('#setAccent').value.replace('#', ''), 16) | 0xFF000000;
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
    renderHome(state.config);
    send({ cmd: 'saveConfig', config: state.config });
  }

  $('#btnGameDir').addEventListener('click', () => send({ cmd: 'pickFolder', key: 'gameDir' }));
  $('#btnOpenGameDir').addEventListener('click', () => send({ cmd: 'openPath', key: 'gameDir' }));
  $('#btnJava').addEventListener('click', () => send({ cmd: 'pickJava' }));
  $('#btnOpenJava').addEventListener('click', () => send({ cmd: 'openPath', key: 'java' }));

  // ---------- Прогресс ----------
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
    setTimeout(() => (toastBox.className = ''), 3200);
  }

  // ---------- Хелперы ----------
  function esc(s) {
    return String(s ?? '').replace(/[&<>"']/g, (c) =>
      ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  }

  // ---------- Старт ----------
  send({ cmd: 'getState' });
  send({ cmd: 'checkAuth' });

  // предзагрузка магазина (популярные моды)
  setTimeout(() => { search(); }, 600);
})();
