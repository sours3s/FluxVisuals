# Адрес сайта FluxVisuals — `fluxvisuals.eu.org`

Выбранный вариант: полное название `fluxvisuals` + бесплатная зона `eu.org`. Итого 18 символов, настоящий домен без названия хостинга.

**Полная пошаговая инструкция: [eu.org.md](eu.org.md).** Там есть регистрация в EU.org, nameserver'ы Cloudflare, заявка на домен, Worker и проверка.

Кратко:

1. Cloudflare → Add a site `fluxvisuals.eu.org` → Free → получить два NS.
2. <https://nic.eu.org/arf/> → зарегистрироваться → Request a new domain → `fluxvisuals.eu.org` + эти NS → ждать письма об одобрении.
3. Cloudflare → DNS: CNAME `@` → `fluxvisuals-server.onrender.com`, **Proxied**.
4. `npx wrangler deploy` из этой папки — Worker с маршрутом `fluxvisuals.eu.org/*`.

`worker.js` проксирует:

- `/`, `/api/*`, `/css/*`, `/js/*` → `fluxvisuals-server.onrender.com` (сайт, кабинет, Catal, API);
- `/news`, `/api/news/*`, `/bot-status` → `fluxvisuals-news-bot.onrender.com` (панель новостей).

Проверка после деплоя:

```text
https://fluxvisuals.eu.org/                 → сайт FluxVisuals
https://fluxvisuals.eu.org/api/mod/version  → JSON с версией
https://fluxvisuals.eu.org/news             → панель новостей
```

## Если нужен адрес прямо сейчас

Пока EU.org рассматривает заявку, можно задеплоить тот же прокси на `fluxvisuals.vercel.app`: Vercel → импорт репозитория, Root Directory = `site-proxy`, Framework = Other, название проекта ровно `fluxvisuals`. В `vercel.json` уже прописаны те же маршруты. Имя свободно.

## Почему не двухбуквенная зона

Зоны `.ct` не существует. Живая проверка API Freenom от 22.11.2026: `fluxvisuals.tk`, `.cf`, `.gq` отдаются как `PAID` по €8.22/год. `fluxvisuals.is-a.dev` забронирован. Поэтому бесплатный короткий домен с полным названием — только `eu.org`.

## Важно

- Лоадер ходит на `fluxvisuals-server.onrender.com` напрямую — exe и API менять не нужно.
- Render Free засыпает через ~15 минут; UptimeRobot держите на исходных `onrender.com`-адресах.
- EU.org одобряет заявки вручную, это может занять несколько дней.
