# Бесплатный адрес FluxVisuals с полным названием

Задача: короткий адрес вида `fluxvisuals.<зона>`, бесплатно, чтобы его было легко запомнить и набрать.

## Что реально получилось проверить

- Зоны `.ct` не существует — такой адрес нельзя зарегистрировать.
- Прежние «бесплатные» двухбуквенные зоны Freenom больше не бесплатны для этого имени. Живой ответ их API 22.11.2026: `fluxvisuals.tk` → `status: AVAILABLE, type: PAID, 8.22 EUR`. То же для `.cf` и `.gq`.
- `fluxvisuals.is-a.dev` — забронирован командой is-a.dev, недоступен.
- Свободны прямо сейчас: `fluxvisuals.vercel.app`, `fluxvisuals.netlify.app`.

Итого: **настоящий двухбуквенный домен бесплатно уже не получить**. Самый короткий бесплатный адрес с полным названием — `fluxvisuals.eu.org` (18 символов). Самый быстрый в реализации — `fluxvisuals.vercel.app` (22 символа).

## Вариант 1 — `fluxvisuals.vercel.app` (рекомендую, работает сразу)

Бесплатный план Hobby, HTTPS автоматический, адрес содержит полное название. Проверено: имя свободно.

1. Vercel → **Add New… → Project** → импортируйте `sours3s/FluxVisuals`.
2. Назовите проект ровно `fluxvisuals` — от этого зависит адрес.
3. **Root Directory** → `site-proxy`.
4. Framework Preset → **Other**, Build Command и Output Directory — пустыми.
5. **Deploy** → `https://fluxvisuals.vercel.app`.

`vercel.json` уже настроен и проксирует:

- `/`, `/api/*` и остальное → `fluxvisuals-server.onrender.com` (сайт и API);
- `/news`, `/api/news/*`, `/bot-status` → `fluxvisuals-news-bot.onrender.com`.

Проверка:

```text
https://fluxvisuals.vercel.app/                 → сайт FluxVisuals
https://fluxvisuals.vercel.app/api/mod/version  → JSON с версией мода
https://fluxvisuals.vercel.app/news             → панель новостей
```

## Вариант 2 — `fluxvisuals.eu.org` (короче, но нужно ожидание)

EU.org выдаёт настоящие бесплатные домены с 1996 года. Адрес `fluxvisuals.eu.org` короче версии с Vercel и не содержит названия хостинга.

1. Зарегистрируйтесь на <http://www.eu.org/> и подайте заявку на `fluxvisuals.eu.org`.
2. После одобрения в Render → сервис `fluxvisuals` → **Settings → Custom Domains → Add Custom Domain** → `fluxvisuals.eu.org`.
3. В DNS домена добавьте `CNAME` → `fluxvisuals-server.onrender.com`, удалите `AAAA`, если есть.
4. Нажмите **Verify**. Render сам выпустит SSL.

Минус: заявка одобряется вручную и может занять несколько дней. Прокси Vercel в этом случае не нужен.

## Важно

- Лоадер продолжает обращаться к `fluxvisuals-server.onrender.com` напрямую. Менять exe и API не нужно.
- Render Free засыпает через ~15 минут, поэтому первый заход может быть медленным. Держите UptimeRobot на `/ping` сервера и ботов.
- Кэш прокси в `vercel.json` отключён, чтобы кабинет и API не отдавали устаревшие данные.
- Запасной вариант — Cloudflare Workers из этой же папки (`worker.js`, `wrangler.toml`), но адрес там длиннее.
