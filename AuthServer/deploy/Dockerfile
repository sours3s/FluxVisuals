# FluxVisuals AuthServer — контейнер для бесплатных Docker-хостингов
# (Render, Koyeb, Fly.io и т.п.). Бинарь self-contained, .NET в образе не нужен.
# Собери локально:  docker build -t fluxvisuals-auth -f Dockerfile .
# Залить на хостинг можно прямо из папки linux-x64 через интерфейс хостинга.

FROM debian:bookworm-slim

WORKDIR /app

# Биань + конфиг + статика (сайт, админка, лоадер, мод)
COPY linux-x64/AuthServer ./AuthServer
COPY linux-x64/appsettings.json ./appsettings.json
COPY linux-x64/wwwroot ./wwwroot

# Порт из appsettings.json (по умолчанию 5001).
# Для Render: они сами задают PORT — shell-подстановка подхватит его.
EXPOSE 5001

CMD ["sh", "-c", "exec ./AuthServer --urls http://0.0.0.0:${PORT:-5001}"]
