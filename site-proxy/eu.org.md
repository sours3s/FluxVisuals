# fluxvisuals.eu.org — пошаговая настройка

Всё бесплатно. Порядок важен: сначала получаем адреса nameserver'ов Cloudflare, потом подаём заявку в EU.org, потому что при регистрации нужно сразу указать NS.

## Текущее состояние (2026-11-27)

- Контакт `KD1269-FREE` активен, вход в панель <https://nic.eu.org/arf/en/> работает.
- Заявка на `fluxvisuals.eu.org` уже подана: `20261122174718-arf-38181`, статус **pending** (ожидает ручной проверки EU.org). Повторная отправка формы возвращает `IGNORED: you already have a pending request ...` — заново подавать не нужно.
- В заявке указаны NS Cloudflare `davina.ns.cloudflare.com` и `robert.ns.cloudflare.com`. Проверка SOA/NS прошла без ошибок.
- Раздел **Domains** в EU.org пока пуст: pending-заявки в нём не отображаются.
- Cloudflare показывает зону `Invalid nameservers` до делегирования домена — это ожидаемо. NS в Cloudflare менять не нужно.
- Согласно регламенту EU.org, решение приходит письмом после ручной проверки, которая занимает несколько дней. Рабочий адрес до одобрения: `https://fluxvisuals.fvls.workers.dev`.
- По состоянию на 27.11 ответа из EU.org нет: во входящих только письмо об активации контакта, в отправленных — запрос от 22.11 на смену NS и повторное письмо от 27.11 с просьбой уточнить статус заявки, активацию контакта и применение новых NS. Публичный DNS возвращает NXDOMAIN, то есть делегирования ещё нет.

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

Основной трафик забирает маршрут Worker `fluxvisuals.eu.org/*`. Для надёжности обе записи зоны уже переведены с Render на Northflank и остаются **Proxied**:

| Type  | Name                    | Content                                          | Proxy status |
|-------|-------------------------|--------------------------------------------------|--------------|
| CNAME | `fluxvisuals.eu.org`    | `p01--fluxvisuals-auth--7wqqdlfq28t5.code.run`   | Proxied      |
| CNAME | `www.fluxvisuals.eu.org`| `p01--fluxvisuals-auth--7wqqdlfq28t5.code.run`   | Proxied      |

**Worker:**

В репозитории уже готовы `site-proxy/worker.js`, `site-proxy/wrangler.toml`. Worker проксирует сайт и API на `fluxvisuals-auth` (Northflank), а `/news`, `/api/news/*` и `/bot-status` — на `fluxvisuals-bots` (Northflank). До одобрения домена работает короткий адрес `https://fluxvisuals.fvls.workers.dev`.

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
https://fluxvisuals.eu.org/api/mod/version  → {"version":"1.1.5",...}
https://fluxvisuals.eu.org/news             → панель публикации новостей
```

SSL-сертификат Cloudflare выпускает автоматически. В Northflank ничего дополнительно настраивать не нужно: все запросы доходят через Worker.

## Запасной вариант без Worker

Можно повесить `fluxvisuals.eu.org` как Custom Domain на сервис `fluxvisuals-auth` в Northflank, но тогда `/news`, `/api/news/*` и `/bot-status` не попадут на сервис ботов. Поэтому нужен Worker.

## Важно

- Лоадер обращается к `https://fluxvisuals.fvls.workers.dev`; после одобрения домена адрес заменяется на `https://fluxvisuals.eu.org`.
- Пока заявка не одобрена, строка `route` в `wrangler.toml` может оставаться — деплой просто не привяжется к зоне. Включённый `workers_dev = true` даёт рабочий адрес `fluxvisuals.fvls.workers.dev`.
- UptimeRobot лучше ставить на рабочий `workers.dev`-адрес, а после одобрения — на `fluxvisuals.eu.org`.
