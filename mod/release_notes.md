## FluxVisuals v1.0.33 — исправление краша после v1.0.32

- Исправлен crashes при запуске: `Missing 'atlas' section in MSDF font: assets/fluxvisuals/fonts/icons.json`.
- В v1.0.32 три файла шрифтов (`fonts/icons.json`, `fonts/custom.json`, `fonts/globals_icon.json`) были заменены донорскими версиями со ссылками на несуществующий namespace `auravisuals`. Возвращены рабочие MSDF/ссылки `fluxvisuals` из v1.0.31.
- HUD из v1.0.32 сохранён без изменений: ватермарка, TargetHUD, хотбар, инвентарь, броня, медиаплеер, хоткеи, зелья, кулдауны и уведомления.
