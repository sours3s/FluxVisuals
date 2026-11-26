#!/bin/sh
# Один контейнер для двух Discord-ботов FluxVisuals: news и support.
# Каждый режим работает в отдельном процессе: у ботов разные токены и intents.

NEWS_PORT="${NEWS_PORT:-5001}"
SUPPORT_PORT="${SUPPORT_PORT:-5002}"

BOT_MODE=news DISCORD_TOKEN="$NEWS_DISCORD_TOKEN" PORT="$NEWS_PORT" \
  dotnet FluxVisualsBots.dll --urls "http://0.0.0.0:${NEWS_PORT}" &
news_pid=$!

BOT_MODE=support DISCORD_TOKEN="$SUPPORT_DISCORD_TOKEN" PORT="$SUPPORT_PORT" \
  dotnet FluxVisualsBots.dll --urls "http://0.0.0.0:${SUPPORT_PORT}" &
support_pid=$!

echo "FluxVisuals bots started: news=$news_pid support=$support_pid"

# Не завершаем контейнер, если один из ботов временно недоступен.
wait "$news_pid"
wait "$support_pid"
