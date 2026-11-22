# Discord-боты FluxVisuals

Один Docker-образ, два бота. Переключатель — переменная `BOT_MODE`:

- `news` — бот **FluxVisuals**, публикует новости через веб-панель `/news` или slash-команду `/news`;
- `support` — бот **FluxVisuals Support**, тот же Catal, отвечает в указанных каналах.

Токены ботов и общий секрет хранятся только в переменных окружения Render. В репозиторий и чат их писать нельзя.

---

## 1. Discord Developer Portal

### 1.1. Два приложения

1. Откройте <https://discord.com/developers/applications>.
2. Создайте приложение **FluxVisuals** — для новостей.
3. Создайте приложение **FluxVisuals Support** — для ответов.
4. В каждом: `Bot` → `Add Bot` → `Reset Token`. Token понадобится позже как `DISCORD_TOKEN` для соответствующего сервиса.
5. Скопируйте `APPLICATION ID` из раздела `General Information` — он понадобится для приглашения бота.

### 1.2. Права и приглашение

В `OAuth2` → `URL Generator` у новостного бота отметьте:

- scopes: `bot`, `applications.commands`;
- permissions: `View Channels`, `Send Messages`, `Embed Links`, `Use Slash Commands`.

У support-бота:

- scope: `bot`;
- permissions: `View Channels`, `Read Message History`, `Send Messages`, `Attach Files`.

Сгенерируйте ссылку и пригласите обоих ботов на сервер.

### 1.3. Важно для support-бота

В `Bot` → `Privileged Gateway Intents` включите **MESSAGE CONTENT INTENT**. Без этого бот не увидит текст вопросов в каналах.

### 1.4. ID, которые нужны в настройках

В Discord включите `User Settings` → `Advanced` → `Developer Mode`:

- ПКМ по своему имени → **Copy User ID** → это `DISCORD_OWNER_ID`;
- ПКМ по каналу «вопрос-ответ» → **Copy Channel ID** → это `SUPPORT_CHANNEL_IDS`;
- ПКМ по категории с тикетами → **Copy Category ID** → это `SUPPORT_CATEGORY_IDS`;
- ПКМ по роли поддержки в списке ролей сервера → **Copy Role ID** → это `SUPPORT_ROLE_ID` (роль должна быть mentionable: Role Settings → Allow anyone to @mention this role);
- ПКМ по серверу → **Copy Server ID** → это `COMMAND_GUILD_ID` (необязательно, но ускоряет появление `/news`).

Если Ticket Tool создаёт каналы с именами вида `ticket-1234`, категорию можно не указывать: бот сам находит их по шаблону `^ticket`. Шаблон меняется через `SUPPORT_CHANNEL_PATTERN`.

---

## 2. Общий секрет

Создайте одну случайную строку 32+ символов. Например, в PowerShell:

```powershell
[guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
```

Эту же строку нужно задать:

- на AuthServer: `FLUXVISUALS_BOT_SECRET`;
- у обоих ботов: `FLUXVISUALS_BOT_SECRET`.

AuthServer уже должен иметь `FLUXVISUALS_AI_KEY` — Catal и Discord-боты используют один и тот же движок и одну базу знаний.

---

## 3. Render

### 3.1. AuthServer

В уже существующем сервисе `fluxvisuals-server` добавьте переменную `FLUXVISUALS_BOT_SECRET` и redeploy. Без неё `/api/bot/ask` будет возвращать 401.

### 3.2. Два сервиса ботов

В репозитории `FluxVisuals` есть `bots/render.yaml`. Можно импортировать его через Render Dashboard → **Blueprints**, либо создать два Web Service вручную:

- Repository: `sours3s/FluxVisuals`;
- Branch: `master`;
- Runtime: `Docker`;
- Dockerfile Path: `bots/Dockerfile`;
- Plan: `Free`.

Переменные для **fluxvisuals-news-bot**:

```text
BOT_MODE=news
DISCORD_TOKEN=<token приложения FluxVisuals>
DISCORD_OWNER_ID=<ваш Discord user ID>
FLUXVISUALS_BOT_SECRET=<общий секрет>
NEWS_ADMIN_TOKEN=<пароль для веб-панели /news, необязательно>
AUTH_SERVER_URL=https://fluxvisuals-server.onrender.com
COMMAND_GUILD_ID=<ID сервера, необязательно>
```

Если `NEWS_ADMIN_TOKEN` не задан, для входа в `/news` используется `FLUXVISUALS_BOT_SECRET`.

Переменные для **fluxvisuals-support-bot**:

```text
BOT_MODE=support
DISCORD_TOKEN=<token приложения FluxVisuals Support>
FLUXVISUALS_BOT_SECRET=<тот же общий секрет>
AUTH_SERVER_URL=https://fluxvisuals-server.onrender.com
SUPPORT_CHANNEL_IDS=<ID каналов через запятую>
SUPPORT_CATEGORY_IDS=<ID категории с тикетами, необязательно>
SUPPORT_CHANNEL_PATTERN=^ticket
SUPPORT_ROLE_ID=1574052054224339078
```

`PORT` Render задаёт сам — трогать не нужно.

### 3.3. Проверка

Откройте `https://<имя-сервиса>.onrender.com/ping` у каждого бота. В ответе должно быть `ok`.

---

## 4. UptimeRobot

Render Free засыпает после ~15 минут простоя. UptimeRobot будет будить сервисы:

1. Создайте монитор типа **HTTP(s)**.
2. URL: `https://<news-bot>.onrender.com/ping`.
3. Interval: `5 minutes` (минимум бесплатного тарифа).
4. Keyword: `ok`.
5. Повторите для `https://<support-bot>.onrender.com/ping`.

Точно так же можно повесить монитор на AuthServer, если он тоже на Render Free.

---

## 5. Как пользоваться

### Новости

#### Веб-панель (рекомендуется)

Откройте `https://fluxvisuals-news-bot.onrender.com/news`, введите токен администратора (`NEWS_ADMIN_TOKEN` или `FLUXVISUALS_BOT_SECRET`) и:

- выберите канал из списка — показываются только каналы, куда бот может писать;
- наберите текст, вставьте `@everyone` / `@here` кнопками или вручную;
- перетащите до 4 файлов, нажмите на область вложений или просто вставьте скриншот по **Ctrl+V** — файл сразу появляется во вложениях и в предпросмотре;
- посмотрите предпросмотр справа и нажмите **Опубликовать**.

Лимиты: 1900 символов, до 4 файлов по 8 МБ. После отправки текст и вложения очищаются. Токен хранится только в `sessionStorage` вкладки и уходит в заголовке `X-Admin-Token`.

#### Slash-команда

В любом канале сервера:

```text
/news channel:#новости message:Долгожданное обновление FluxVisuals... file1:<вложение>
```

Кроме текста можно прикрепить до 4 файлов или скриншотов через поля `file1`–`file4`. Лимит — 8 МБ на файл. Можно отправить только текст, только файлы или и то, и другое.

Команду может выполнить только `DISCORD_OWNER_ID`. Бот сразу подтверждает команду (чтобы Discord не показал «Приложение не отвечает»), а затем отвечает «Опубликовано…» в личном (ephemeral) сообщении.

Если `/news` не появился: задайте `COMMAND_GUILD_ID` и перезапустите сервис. Без него глобальные команды Discord может поднимать до часа.

### Support

Бот читает сообщения:

- в каналах из `SUPPORT_CHANNEL_IDS`;
- во всех каналах категории `SUPPORT_CATEGORY_IDS`;
- в каналах, чьё имя подходит под `SUPPORT_CHANNEL_PATTERN` — по умолчанию `^ticket`, то есть `ticket-1234` и подобные.

Поэтому тикеты Ticket Tool подхватываются автоматически, даже если канал создан после запуска бота. Дайте боту роль с правами `View Channel` и `Send Messages` на категории тикетов — Ticket Tool создаёт каналы с наследованием прав категории. Бот отвечает прямо на сообщение пользователя, скриншоты передаёт Catal. Ответы режутся на куски до 1900 символов и не пингуют пользователей.

Catal represents один раз — при первом сообщении от пользователя. Если Catal не смог ответить, бот предлагает написать «позови саппорта». По этой фразе (или «вызови поддержку», «нужен живой человек», `call support`) бот упоминает роль из `SUPPORT_ROLE_ID` прямо в тикете. Роль должна быть Include in mentions / Allow anyone to @mention this role, иначе Discord не пришлёт уведомление.

Catal отвечает только по FluxVisuals: клиент, визуалы, лаунчер, сайт, аккаунты, запуск, ошибки, модпаки. На посторонние задачи он отвечает отказом.

---

## 6. Локальная сборка

```powershell
cd bots
dotnet build -c Release
```

Без `DISCORD_TOKEN` бот не логинится, но `/ping` работает — так удобно проверять хост.
