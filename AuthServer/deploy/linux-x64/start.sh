#!/usr/bin/env bash
# FluxVisuals AuthServer (Linux)
# Простой запуск: ./start.sh
# Для продакшена лучше systemd (см. fluxvisuals-auth.service)
cd "$(dirname "$0")"
exec ./AuthServer
