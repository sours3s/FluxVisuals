# fluxvisuals.eu.org — пошаговая настройка

Всё бесплатно. Порядок важен: сначала получаем адреса nameserver'ов Cloudflare, потом подаём заявку в EU.org, потому что при регистрации нужно сразу указать NS.

## 1. Cloudflare: зона и nameserver'ы

1. Зарегистрируйтесь в Cloudflare → **Add a site** → `fluxvisuals.eu.org` → план **Free**.
2. Cloudflare попытается сканировать записи — ничего не найдёт, это нормально. Продолжайте.
3. На экране **Assign nameservers** скопируйте два значения вида `xxxx.ns.cloudflare.com`. Они понадобятся на следующем шаге.

## 2. EU.org: аккаунт и заявка

1. Откройте <https://nic.eu.org/arf/> → **Register**. Создайте handle (логин), укажите email и пароль.
2. Подтвердите email по ссылке из письма (проверьте папку Spam).
3. Войдите → **Request a new domain**:
   - Domain: `fluxvisuals.eu.org`
   - Nameservers: два значения из Cloudflare с шага 1
   - Purpose / comment (на английском):
     ```text
     Non-commercial community project: free Minecraft visual client website with download and update API. No advertising, no paid services.
     ```
4. Отправьте заявку. EU.org одобряет вручную, это обычно несколько дней. Придёт письмо на email.

> `fluxvisuals` — 11 символов, а EU.org регистрирует прямые поддомены от 4 символов, поэтому формальное ограничение соблюдено. Сайт позиционируется как бесплатный некоммерческий проект — это важно для их политики.

## 3. Cloudflare: DNS и Worker

После одобрения домена вернитесь в Cloudflare.

**DNS → Records:**

| Type  | Name | Value                              | Proxy status      |
|-------|------|------------------------------------|-------------------|
| CNAME | `@`  | `fluxvisuals-server.onrender.com`  | Proxied (оранжевый) |

**Worker:**

В репозитории уже готовы `site-proxy/worker.js`, `site-proxy/wrangler.toml`. Worker проксирует сайт на `fluxvisuals-server.onrender.com`, а `/news`, `/api/news/*` и `/bot-status` — на `fluxvisuals-news-bot.onrender.com`.

Через терминал:

```powershell
npm install -g wrangler
wrangler login
cd site-proxy
wrangler deploy
```

`wrangler.toml` уже содержит маршрут `fluxvisuals.eu.org/*`, поэтому отдельно Route в интерфейсе добавлять не нужно.

Или через веб-интерфейс: Workers & Pages → Create → Worker → вставьте код из `worker.js` → Deploy → Settings → Triggers → Routes → `fluxvisuals.eu.org/*`.

## 4. Проверка

```text
https://fluxvisuals.eu.org/                 → сайт FluxVisuals
https://fluxvisuals.eu.org/api/mod/version  → {"version":"1.0.57",...}
https://fluxvisuals.eu.org/news             → панель публикации новостей
```

SSL-сертификат Cloudflare выпускает автоматически. Render ничего настраивать не нужно: все запросы доходят через Worker.

## Запасной вариант без Worker

Можно не использовать Worker, а добавить `fluxvisuals.eu.org` в Render → Settings → Custom Domains сервиса `fluxvisuals`, а в Cloudflare поставить CNAME с **DNS only** (серое облако). Тогда сайт и API будут работать, но `/news` — нет, потому что это отдельный Render-сервис. Поэтому лучше вариант с Worker.

## Важно

- Лоадер продолжает обращаться к `fluxvisuals-server.onrender.com` напрямую — exe менять не нужно.
- Пока заявка не одобрена, строка `route` в `wrangler.toml` может оставаться — деплой просто не привяжется к зоне. Можно временно закомментировать её и пользоваться `*.workers.dev`.
- UptimeRobot лучше ставить на исходные `onrender.com`-адреса, а не на новый домен.
