# Адрес сайта FluxVisuals — `fluxvisuals.eu.org`

Выбранный вариант: полное название `fluxvisuals` + бесплатная зона `eu.org`. Итого 18 символов, настоящий домен без названия хостинга.

**Полная пошаговая инструкция: [eu.org.md](eu.org.md).** Там есть регистрация в EU.org, nameserver'ы Cloudflare, заявка на домен, Worker и проверка.

Кратко:

1. Cloudflare → Add a site `fluxvisuals.eu.org` → Free → получить два NS.
2. <https://nic.eu.org/arf/> → зарегистрироваться → Request a new domain → `fluxvisuals.eu.org` + эти NS → ждать письма об одобрении.
3. `npx wrangler deploy` из этой папки — Worker с маршрутом `fluxvisuals.eu.org/*` и включённым `workers.dev`.
4. Пока NS не делегированы, пользоваться коротким адресом `https://fluxvisuals.fvls.workers.dev`.

`worker.js` проксирует:

- `/`, `/api/*`, `/css/*`, `/js/*` → `fluxvisuals-auth` на Northflank (сайт, кабинет, Catal, API);
- `/news`, `/api/news/*`, `/bot-status` → `fluxvisuals-bots` на Northflank (панель новостей).

Проверка после деплоя:

```text
https://fluxvisuals.fvls.workers.dev/                 → сайт FluxVisuals
https://fluxvisuals.fvls.workers.dev/api/mod/version  → JSON с версией
https://fluxvisuals.fvls.workers.dev/news             → панель новостей
```

## Временный адрес

Рабочий адрес на время ожидания EU.org: `https://fluxvisuals.fvls.workers.dev`. После одобрения домена и делегирования NS на Cloudflare (`davina`/`robert`) тот же Worker начнёт отвечать на `fluxvisuals.eu.org`; менять NS вручную до одобрения не нужно.

## Почему не двухбуквенная зона

Зоны `.ct` не существует. Живая проверка API Freenom от 22.11.2026: `fluxvisuals.tk`, `.cf`, `.gq` отдаются как `PAID` по €8.22/год. `fluxvisuals.is-a.dev` забронирован. Поэтому бесплатный короткий домен с полным названием — только `eu.org`.

## Важно

- Лоадер ходит на `https://fluxvisuals.fvls.workers.dev` — exe и API менять не нужно.
- Все сервисы (сайт/API, БД, оба бота) находятся в одном проекте Northflank; `fluxvisuals.eu.org` остаётся конечным доменом.
- EU.org одобряет заявки вручную, это может занять несколько дней. Пока в аккаунте `nic.eu.org` нет ни одного домена — зона в Cloudflare остаётся pending.
