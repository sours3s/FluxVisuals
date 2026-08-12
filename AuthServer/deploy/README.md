# FluxVisuals AuthServer — развёртывание (бесплатно)

Готовый сервер: сайт + лоадер + авторизация + админка + раздача мода — всё в одном приложении.
Сервер самодостаточный (self-contained), .NET на хосте НЕ нужен.

Что внутри `wwwroot/`:
- `/` — публичная страница (скачать клиент)
- `/admin/` — админ-панель
- `/loader/FluxVisualsLoader.exe` — сам клиент (67 МБ)
- `/mods/fluxvisuals-mod-1.21.11.jar` — мод (клиенты качают автоматически)

---

## ВАРИАНТ 1 — Oracle Cloud Always Free (лучший: всегда онлайн, бесплатно навсегда)

**Реальный VPS, работает 24/7, не засыпает. Единственное — при регистрации попросят карту** (для проверки, деньги не снимут).

1. Зайди на `oracle.com/cloud` → Start for free
2. После регистрации: **Create a VM instance** → Ubuntu 22.04/24.04, самый дешёвый (Always Free: 1/8 OCPU, 1 ГБ RAM)
3. Зайди по SSH (ключ дают при создании)
4. Залей на сервер содержимое `linux-x64/` в `/opt/fluxvisuals/` (например через scp/FileZilla)
5. Запусти установку одной командой:
   ```bash
   sudo bash /opt/fluxvisuals/setup-oracle.sh
   ```
6. Готово! IP сервера покажет скрипт. Открой `http://IP:5001/`

⚠️ В консоли Oracle открой **порт 5001** в Security List (Ingress rules).

---

## ВАРИАНТ 2 — Render.com (без карты, но сервер «засыпает»)

Бесплатно, без карты, заливается за 5 минут. Минус — после 15 минут простоя первый запрос ждёт ~30 сек (сервер просыпается).

1. Зарегистрируйся на `render.com` (через GitHub)
2. Создай **New → Web Service**
3. Подключи репозиторий с содержимым папки `linux-x64/` (или загрузи через их CLI/API)
4. Render сам найдёт `Dockerfile` (он уже готов в `deploy/`)
5. При необходимости задай переменную `PORT` (Render передаёт её сам)
6. Домен будет вида `https://название.onrender.com` — его и дай клиентам (в поле «Сервер» лоудера)

---

## ВАРИАНТ 3 — любой платный VPS

Hetzner (~€4/мес), Timeweb/Aeza (российские). Заливка — как вариант 1.

---

## Первый вход

1. Открой админку: `http://<адрес>/admin/`
2. Логин **admin**, пароль **admin123**
3. **Сразу смени пароль** и создай аккаунты клиентов.

## Клиенты

Раздай `FluxVisualsLoader.exe`. При входе поле «Сервер»:
- `http://IP:5001` — если без домена
- `https://название.onrender.com` — если Render
- `http://домен:5001` — если есть домен

URL запомнится. Мод скачается сам.

---

## Обновление мода
Пересобери `fluxvisuals-mod-1.21.11.jar` и замени файл в `wwwroot/mods/` — клиенты получат новую версию при следующем запуске. Бэкап пользователей — это файл `auth.db`.

## Сборка с нуля
```bash
# сервер (две платформы сразу)
cd D:\FluxVisuals\AuthServer
dotnet publish -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true -p:IncludeNativeLibrariesForSelfExtract=true -o deploy/win-x64
dotnet publish -c Release -r linux-x64 --self-contained true -p:PublishSingleFile=true -p:IncludeNativeLibrariesForSelfExtract=true -o deploy/linux-x64

# мод (нужна JDK 21)
cd D:\FluxVisuals\mod
JAVA_HOME="C:\Program Files\Java\jdk-21" ./gradlew.bat build -x test
# скопировать jar в wwwroot/mods/
```
