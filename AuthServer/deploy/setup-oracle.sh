#!/usr/bin/env bash
# ============================================================
#  FluxVisuals AuthServer — установка за 1 команду
#  Для Oracle Cloud Always Free (Ubuntu 22.04/24.04)
#
#  Залить на VPS содержимое папки linux-x64/ в /opt/fluxvisuals,
#  потом запустить:  sudo bash /opt/fluxvisuals/setup-oracle.sh
# ============================================================
set -e

APP_DIR="/opt/fluxvisuals"
SERVICE="fluxvisuals-auth"

echo "==> Проверяю файлы..."
test -f "$APP_DIR/AuthServer" || { echo "Ошибка: AuthServer не найден в $APP_DIR"; exit 1; }
chmod +x "$APP_DIR/AuthServer"

echo "==> Ставлю сервис (автозапуск + перезапуск при падении)..."
cat > /etc/systemd/system/$SERVICE.service <<EOF
[Unit]
Description=FluxVisuals AuthServer
After=network.target

[Service]
Type=simple
WorkingDirectory=$APP_DIR
ExecStart=$APP_DIR/AuthServer
Restart=always
RestartSec=3
Environment=ASPNETCORE_URLS=http://0.0.0.0:5001

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable --now $SERVICE

echo "==> Открываю порт 5001 в фаерволе (если ufw включён)..."
if command -v ufw >/dev/null 2>&1; then
  ufw allow 5001/tcp || true
fi

sleep 3
echo ""
echo "==> Проверка:"
curl -s http://127.0.0.1:5001/api/mod/version && echo "" || echo "  ! Сервер не ответил"
systemctl --no-pager status $SERVICE --lines=3 || true

echo ""
echo "============================================================"
echo "  ГОТОВО!"
echo "  Внешний IP сервера: $(curl -s ifconfig.me 2>/dev/null || echo 'см. в панели Oracle')"
echo "  Сайт:              http://$(curl -s ifconfig.me 2>/dev/null || echo '<IP>'):5001/"
echo "  Админка:           http://$(curl -s ifconfig.me 2>/dev/null || echo '<IP>'):5001/admin/"
echo "  Логин: admin / пароль: admin123 (СМЕНИ! через админку)"
echo "============================================================"
