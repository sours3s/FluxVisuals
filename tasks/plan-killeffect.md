# Implementation Plan: объединённый Kill Effect

Source spec: запрос пользователя от 26.11.2026 — «соединить всё» из найденных реализаций, при этом ванильные типы не нужны. После брака визуала `1.1.5` пользователь уточнил требование: эффекты должны полностью повторять эталонные реализации, а не собственную интерпретацию.

## Overview

Сделать один модуль `Kill Effect` с кастомными эффектами вместо пяти ванильных режимов. В модуль входят: лазер/луч, небесный луч, душа, осколки, затемнение, похищение шипами, частицы тела, scan-виньетка с зумом/тряской и тотемный триггер. Код пишется свой, под API FluxVisuals; исходники и ассеты с `All Rights Reserved` не копируются. Старый план по `.bbmodel` не трогается.

## Architecture Decisions

- **Только кастомные режимы.** `Lightning / Blood / Explosion / Heart / Smoke` удаляются из GUI. Старые сохранённые значения мигрируют в `Beam`; `Soul` и `Shatter` становятся полноценными отдельными режимами, а не маппятся на ванильные.
- **Единый trigger.** Атака запоминает цель; смерть приходит из `EntityDeathEvent`/death status `3`; есть tick-fallback. Дубли подавляются по entity id и временным окнам. Тотем `status == 35` обрабатывается отдельным прямым хуком.
- **Состояние эффектов с TTL.** Активные эффекты живут в небольшом state-хранилище с ограничениями по количеству; просроченные записи удаляются и в `tick()`, и в `render()`, чтобы не рисовать мёртвые эффекты на паузе.
- **Рендер через существующие API.** Мир — `WorldRenderer.drawLine/drawQuad/drawCube`; экран — `Renderer2D`; камера — `CameraPositionEvent`/`EventRotation` и, если понадобится, минимальная правка `GameRendererMixin`. Framebuffer-distortion переносится напрямую: эталонный `frag_effect_scan` включается через `KillEffectScanPass` и depth основного framebuffer.
- **Скрытие тела опционально.** Делается через существующий `EntityCullingMixin` по набору скрываемых entity id, только если включена настройка `Hide Body`.
- **Лицензионная чистота.** Никаких имен сторонних клиентов, пакетов, ассетов и `Figura/JIJ/LuaJ` в коде и UI.

## Task List

### Phase 1: Contract and foundation

- [x] Task 1: Реестр кастомных режимов и миграция конфига
  - Acceptance: `KillEffectModes.ALL` содержит 8 кастомных режимов; старые ванильные значения и `Soul/Shatter` мигрируют корректно; `index` восстанавливается после `Module.load`.
  - Verification: `./gradlew.bat test --no-daemon`.
  - Files: `KillEffectModes.java`, `KillEffect.java`, `KillEffectModesTest.java`, `TooltipRegistrar.java`.

- [x] Task 2: Настройки и состояние триггеров
  - Acceptance: добавлены настройки целей, цвета, длительности, частиц, радиуса, скорости, высоты/ширины луча, волны, звука, тотема, скрытия тела, зума/тряски/серого оверлея; нерелевантные настройки скрываются по режиму.
  - Verification: `./gradlew.bat compileJava --no-daemon`.
  - Files: `KillEffect.java`, `TooltipRegistrar.java`.

- [x] Task 3: Надёжный trigger и тотем
  - Acceptance: атака + death status/tick fallback работают вместе; дубли подавляются; `status == 35` запускает выбранный эффект, если `Totem` включён; при смене мира/отключении состояние очищается.
  - Verification: `./gradlew.bat test --no-daemon`; ручная проверка в одиночном мире.
  - Files: `KillEffect.java`, `KillEffectDeathMemoryTracker.java`, `ClientPlayNetworkHandlerMixin.java`.

### Checkpoint: Foundation

- [x] Тесты и компиляция проходят.
- [x] В GUI нет ванильных типов.

### Phase 2: World effects

- [x] Task 4: Луч и небесный луч
  - Acceptance: режимы `Beam` и `Sky Beam` рисуют разные эффекты, уважают `Duration`, `Beam Height`, `Beam Width`, `Radius`, `Wave` и цвет; эффекты исчезают по TTL.
  - Verification: `./gradlew.bat compileJava --no-daemon`.
  - Files: `KillEffectBeamRenderer.java`, `KillEffect.java`.

- [x] Task 5: Soul и Shatter
  - Acceptance: `Soul` рисует восходящие спирали/кольца/луч; `Shatter` — кольца, осколки, вихрь и сферы; оба работают на мобах и игроках по настройкам целей.
  - Verification: `./gradlew.bat compileJava --no-daemon`.
  - Files: `KillEffectSoulRenderer.java`, `KillEffect.java`.

- [x] Task 6: Darkness, Spike Abduction и частицы тела
  - Acceptance: `Darkness` затемняет область/экран; `Spike Abduction` поднимает цель шипами/линиями; `Body Particles` создаёт осколки с настройками количества, размера, силы и времени жизни; есть cap.
  - Verification: `./gradlew.bat compileJava --no-daemon`.
  - Files: `KillEffectDarknessRenderer.java`, `KillEffectShardsRenderer.java`, `KillEffect.java`, `EntityCullingMixin.java`.

### Checkpoint: World effects

- [x] Все world-режимы compile/test проходят.
- [x] Нет unbounded growth частиц/волн.

### Phase 3: Screen and camera

- [x] Task 7: Scan-виньетка, ударная волна и серый оверлей
  - Acceptance: `Scan` рисует шипы/виньетку, ударную волну и полупрозрачный серый оверлей; на паузе просроченные эффекты не рисуются.
  - Verification: `./gradlew.bat compileJava --no-daemon`.
  - Files: `KillEffectScanRenderer.java`, новый `KillEffectOverlayRenderer.java`, `KillEffect.java`.

- [x] Task 8: Зум и тряска камеры
  - Acceptance: при включённых настройках эффект кратковременно меняет FOV и/или позицию камеры/угол; после окончания значения возвращаются; без эффекта поведение ванильное.
  - Verification: `./gradlew.bat compileJava --no-daemon`; ручная проверка, что камера не «залипает».
  - Files: `KillEffectCameraState.java`, `CameraMixin.java`, `GameRendererMixin.java`, `KillEffect.java`.

### Checkpoint: Screen and camera

- [x] Экранные эффекты не ломают HUD/ClickGUI.
- [x] Камера возвращается в исходное состояние.

### Phase 4: Finish

- [x] Task 9: Документация и версия
  - Acceptance: обновлены tooltip, changelog, release notes; `mod_version` бампится на `1.1.5`; маркеры сервера/лоадера обновлены.
  - Verification: `git diff --check`.
  - Files: `TooltipRegistrar.java`, `ChangelogPanel.java`, `release_notes.md`, `gradle.properties`, серверные маркеры.

- [x] Task 10: Сборка и публикация
  - Acceptance: `./gradlew.bat test obfuscateJar` проходит; JAR проверен по `fabric.mod.json`; публикация выполняется только после отдельного подтверждения пользователя.
  - Verification: SHA256 артефакта; `gh release view`.
  - Files: `mod/build/release/fluxvisuals-1.1.5.jar`.

### Checkpoint: Complete

- [x] Все тесты проходят.
- [x] Обфускированный JAR собран.
- [ ] Пользователь подтвердил визуальную проверку в одиночном мире через loader `v1.0.16`.

### Phase 5: 1:1 visual port (после брака 1.1.5)

Пользователь явно отменил ограничение license-clean и потребовал переносить исходную логику и ассеты 1:1. Весь код адаптируется под пакеты/API FluxVisuals, чужие имена в code/UI не остаются.

- [x] Task 11: Точный Beam/Soul/Shatter
  - Acceptance: Beam — 6 жёстких + 8 мягких лепестков, 2 soft disc, 3 кольца; Soul — 3 DNA-спирали, 8 колец, 2 орбиты, аура и луч; Shatter — 4 кольца, 48 осколков, вихрь, 7 вращающихся колец и финальные кольца.
  - Verification: `./gradlew.bat test --no-daemon`.
  - Files: `KillEffectBeamRenderer.java`, `KillEffectSoulRenderer.java`.

- [x] Task 12: Точный Darkness/Spike/Scan и ассеты
  - Acceptance: Darkness — цилиндр/кольцо/спираль; Spike — путь и модель шипа; Scan — виньетка, скан-волна, зум/тряска, искажение; добавлены нужные текстуры/шейдеры под `assets/fluxvisuals`.
  - Verification: `./gradlew.bat test --no-daemon`.
  - Files: `KillEffectDarknessRenderer.java`, `KillEffectScanRenderer.java`, `KillEffectOverlayRenderer.java`, assets.

- [x] Task 13: Точные частицы тела и звуки
  - Acceptance: 520 осколков с палитрой/силуэтом/физикой/вращением, скрытие тела, кастомные kill-звуки.
  - Verification: `./gradlew.bat test --no-daemon`.
  - Files: `KillEffectParticleSystem.java`, `KillEffectSoundQueue.java`, `sounds.json`, sounds assets.

- [x] Task 14: Версия 1.1.6, сборка и публикация
  - Acceptance: `test obfuscateJar` проходит, changelog/release notes обновлены, релиз `v1.1.6` опубликован после прямого подтверждения пользователя.
  - Verification: `gh release view v1.1.6`; обфускированный `fluxvisuals-1.1.6.jar`.
  - Status: опубликовано.

### Phase 6: Возврат к эталонной логике вместо кубиков и линий

Пользователь подтвердил, что сборка `1.1.5` выглядит как кубики и линии.Третий этап переносит геометрию и логику частиц тех режимов, где в эталоне используются мягкие glow-квады, цилиндры и ванильные частицы. Свои «улучшенные» эффекты удалены.

- [x] Task 15: Beam и Sky Beam по эталонной геометрии
  - Acceptance: `Beam` — 40 срезов по 3 аддитивных glow-слоя с `textures/bloom.png` и 10 нижних слоёв; `Sky Beam` — цилиндры 64 блока, 24 сегмента, наземное кольцо и 28 частиц.
  - Verification: `compileJava`; отсутствие `drawCube` в рендере луча.
  - Files: `KillEffectBeamRenderer.java`, `KillEffectGlow.java`.

- [x] Task 16: Soul и Shatter с рендером сущности-призрака
  - Acceptance: для Soul создаётся `OtherClientPlayerEntity` с параметрами цели, модель рисуется с прозрачностью; Shatter сохраняет 48 линий-осколков, вихрь и кольца эталона; пользовательский силуэт из линий удалён.
  - Verification: `compileJava`; просроченные эффекты удаляются вместе с призраком.
  - Files: `KillEffectSoulRenderer.java`, `KillEffect.java`.

- [x] Task 17: Darkness, Spike и Body Particles без кубов
  - Acceptance: Darkness использует 12 → 6 ванильных частиц и исправленную альфу цилиндров; Spike использует частицу-след вместо нити; `Kind.CUBE` и `spawnVanillaParticles` удалены, Body Particles рисует только треугольники/квады.
  - Verification: `compileJava`; поиск по `CUBE` и `spawnVanillaParticles` возвращает 0 совпадений.
  - Files: `KillEffectDarknessRenderer.java`, `KillEffectParticleSystem.java`.

- [x] Task 18: Локальная сборка без бампа версии
  - Acceptance: `test obfuscateJar` зелёный; `mod_version` остаётся `1.1.5`; релиз и маркеры не обновлялись.
  - Verification: SHA256 `2AC0076205830F9A4311239DC37108E23873FF57A62FD37E52D0F1504B46A536`.
  - Files: `mod/build/release/fluxvisuals-1.1.5.jar`.

- [x] Task 19: Публикация по прямому подтверждению
  - Acceptance: пользователь прямо попросил «закинь все релизы и лоадера и мода»; ассеты `v1.1.6` и `v1.0.17-loader` заменены после порта, коммит `52a8a6c9` запушен.
  - Verification: `gh release view` — mod `sha256:327362238b3e099fcb90be2b6f50d9f0c3d2efec8dce758fbfb1948b7a8fa8f2`, loader `sha256:a41e170e2b81ab0e37de2e90bb88a4b6bf013c8e08e56bcbf486efa99f84be5b`; `/api/mod/version` отдаёт оба URL.
  - Status: опубликовано; визуальная проверка 8 режимов пользователем ещё ожидается.

### Phase 7: Версии и ссылки из админ-панели

- [x] Task 20: Loader `v1.0.17` — версия и ссылки только из админ-панели
  - Acceptance: GitHub Releases больше не переопределяет URL мода; устаревший `modDownloadUrl` в конфиге не вызывает `404`; автообновление exe работает по ссылке из панели.
  - Verification: `dotnet build`; `gh release view v1.0.17-loader`.
  - Files: `loader/Core/{AuthService,Config,Defaults,GameSession,UpdateService}.cs`, `MainWindow.xaml.cs`.

- [x] Task 21: Автосинхронизация ссылок из релизов в админ-панель
  - Acceptance: сервер раз в 15 минут подтягивает последний `.jar` и `FluxVisualsLoader.exe` из GitHub Releases в настройки; вручную ссылки задавать не нужно.
  - Verification: `dotnet build`; `/api/mod/version`; continuous deployment `cb6da6a`.
  - Files: `server-repo/AuthServer/Services/ReleaseSyncService.cs`, `Program.cs`, `appsettings.json`.

### Phase 8: Прямой порт эталонных триггеров и Scan-pass

- [x] Task 22: Beam/Soul/Spike/Scan по исходной логике
  - Acceptance: Beam — динамический цвет темы, эталонный `bloom.png`, LIGHTNING/LEQUAL, волна 1750 мс; Soul — `drawEntity` с yaw/альфой каждый кадр и удаление по `elapsed > duration`; Spike — victim-driven направление, тело жертвы, `CLOUD`-след, root-трансформенты; Scan — виньетка 600 мс, камера 650 мс, эталонные звуки и формулы.
  - Verification: `compileJava`; `test obfuscateJar`.
  - Files: `KillEffect.java`, `KillEffectBeamRenderer.java`, `KillEffectSoulRenderer.java`, `KillEffectDarknessRenderer.java`, `KillEffectOverlayRenderer.java`, `KillEffectCameraState.java`, `KillEffectSoundQueue.java`, `WorldRenderLayers.java`, `bloom.png`.

- [x] Task 23: Scan через эталонный шейдер вместо самодельной геометрии
  - Acceptance: `frag_effect_scan.vsh/fsh` подключены через `KillEffectScanPass` с UBO, depth основного framebuffer и аддитивным копированием прямо в цветовую текстуру сцены; sphere/cells/geometries удалены; лимит волн 4; глобальный `Shockwave` убран.
  - Verification: `test obfuscateJar`; SHA256 `327362238B3E099FCB90BE2B6F50D9F0C3D2EFEC8DCE758FBFB1948B7A8FA8F2`.
  - Files: `KillEffectScanPass.java`, `KillEffectScanRenderer.java`, `WorldRendererMixin.java`, `KillEffect.java`.

- [ ] Task 24: Визуальная проверка 8 режимов пользователем
  - Acceptance: пользователь подтверждает, что все режимы выглядят как эталонные реализации, настройки сохраняются, автообновление работает с `v1.0.17`.
  - Verification: ручной запуск через loader `v1.0.17`; отдельное подтверждение перед следующим бампом/публикацией.
  - Files: только отчёт пользователя; повторную публикацию не выполнять без подтверждения.

### Known deviation

- Полноценный framebuffer-pass `killdistortion` подключён в Phase 8. Java-заглушка эталона не содержала логики; использован сам шейдер `frag_effect_scan` с UBO и depth-текстурой основного framebuffer. Самодельные sphere/cells/геометрия из Scan удалены.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Слишком много режимов и настроек усложнят поддержку | High | Маленькие рендер-хелперы, общий state, hidden()-настройки, лимиты TTL |
| Правка FOV/камеры конфликтует с `Custom FOV`/`Zoom` | High | Минимальные правки существующих хуков, возврат состояния в `finally`, проверка в одиночном мире |
| Полный GPU-pass может конфликтовать с framegraph | Medium | Рендер в отдельный `RenderTarget` и копирование в основной framebuffer; при отсутствии depth-текстуры pass пропускается |
| Старые конфиги с ванильными режимами сломают UI | Medium | Нормализация после `load()` + regression-тест |
| Производительность приToMany эффектах | Medium | Cap частиц/волн, удаление просроченных записей в render/tick |
| Лицензионные ограничения исходных реализаций | High | Пользователь явно разрешил 1:1 ассеты; код/UI без чужих имен |

## Open Questions

- Настоящий shader-pass подключён; остаётся визуально подтвердить 8 режимов через loader `v1.0.17`.
- Скрытие модели цели реализуем через уже существующий `EntityCullingMixin`; если это вызовет проблемы с косметикой, оставить только осколки.

### Phase 9: Порт BetterMinecraft

- [x] Task 25: BetterMinecraft по эталонной реализации 1:1
  - Acceptance: singleton-модуль с настройками `Animation`, `Stack Messages`, `Anim Speed` (180/150/300, increment 1); точные `Animation`/`Easing`/`Easings`; обновление один раз за кадр до HUD; анимации инвентаря, таба, чата, хотбара и букв; склейка одинаковых сообщений. Пользовательские режимы `saturationDisplay`, `hideMessageIndicator`, `itemMoveAnim`, `hotbarChatLift` и самодельная геометрия удалены.
  - Verification: `compileJava`, `test obfuscateJar` — `BUILD SUCCESSFUL`; `git diff --check` чистый; релиз не публиковался.
  - Files: `BetterMinecraft.java`, `betterminecraft/{Animation,Easing,Easings}.java`, `Manager.java`, `InGameHudMixin.java`, `BetterMinecraft*Mixin.java`, `fluxvisuals.mixins.json`.
  - Known deviation: хотбар-сдвиг обёрнут через `@WrapOperation` вокруг вызова `renderHotbar` с `finally`-pop, чтобы не утекла матрица при отмене ванильного хотбара кастомным HUD; формула `30f * (1f - hotbarT)` и порог `0.001f` сохранены. `animSpeed` остаётся на верхнем уровне настроек, потому что UI-группа FluxVisuals принимает только boolean-настройки; значения 180/150/300/increment 1 совпадают с эталоном.

### Phase 10: Порт BlockOutline

- [x] Task 26: Настройки и каркас модуля
  - Acceptance: точные режимы `Обычный`/`Шейдер`, 21 шейдерный тип, `Скорость шейдера` 1.0/0.1/1.0/0.01, `Прозрачность контура` 150/0/255/1, `Скорость анимации` 2.0/1.0/3.0/0.1, `3D`, `Показывать заливку`, `Прозрачность заливки` 50/0/255/1, `Свой цвет`, `Цвет 1`, `Градиент`, `Цвет 2`, порядок и видимость по эталону.
  - Verification: `compileJava` — `BUILD SUCCESSFUL`.
  - Known deviation: эталонные `visibility`- suppliers инвертированы в target `hidden`; `ColorSetting` реализован через `HueSetting` с сохранением точного RGB и записью/чтением RGB в `Module.save()`/`load()`.

- [x] Task 27: Геометрия и анимация переключения блоков
  - Acceptance: `getOutlineShape`, `expand(0.002)`, дистанция 10000, `LINE_WIDTH = 6.5F`, smoothstep-интерполяция коробок, длительность `max(35, round(280 / speed))`, сброс при смене цели/пустом shape.
  - Verification: `compileJava` — `BUILD SUCCESSFUL`.

- [x] Task 28: Обычный рендер заливки и контура
  - Acceptance: те же 6 граней с отсечением по камере, 12 линий, `depthTest = !3D`, цвет/градиент/альфа по эталону.
  - Verification: `compileJava` — `BUILD SUCCESSFUL`.

- [x] Task 29: Шейдерный рендер и ассеты
  - Acceptance: перенос shader util, 21 режим + скрытые `Солнце`/`Комета`, UBO/pipeline, идентичные шейдеры под namespace мода, `threeD` выбирает pipeline без depth.
  - Verification: `compileJava`, `test obfuscateJar` — `BUILD SUCCESSFUL`; шейдеры совпадают с эталоном после нормализации переводов строк.
  - Known deviation: pass label переименован в `_FluxBlockShader_`; вызовы `DrawParameters` используют intermediate-имена `comp_752()`/`comp_751()`.

- [x] Task 30: Скрытие ванильного контура, регистрация, финальная сборка
  - Acceptance: mixin на `drawBlockOutline`, static guard, регистрация в `Manager`, записи в mixin-конфиге, никаких упоминаний источника в коде.
  - Verification: `test obfuscateJar` — `BUILD SUCCESSFUL` с `-PgrunteonXmx=768m` из-за занятой памяти запущенным клиентом; `git diff --check` чистый; релиз не публиковать.

### Phase 11: Порт MaceHelper

- [x] Task 31: Модуль `MaceHelper`, настройки и заряд булавы
  - Acceptance: точные три boolean-настройки `Сообщение в чат`, `Подсветка в руке`, `Подсветка в хотбаре` (по умолчанию `true`), `isMace`, `isHoldingMace`, `getChargePercentage`, `getChargeColor`, событие атаки с сообщением `Булава заряжена на N%`.
  - Verification: `compileJava` — `BUILD SUCCESSFUL`.
  - Known deviation: префикс чата берётся из target `ChatUtil` (`FluxVisuals ->`) вместо Префикса источника; категория `Visuals` по требованию порта в визуалы.

- [x] Task 32: `MaceHelperHandConsumer`
  - Acceptance: полный `VertexConsumer`-обёртыватель с теми же delegation-методами, factor `0.95F`, обрезкой `(int)`, сохранением alpha и пустыми `quad(...)`.
  - Verification: `compileJava` — `BUILD SUCCESSFUL`.

- [x] Task 33: Хуки рендера руки и хотбара
  - Acceptance: отслеживание состояния первой руки/булавы, подмена consumer для `renderBakedItemQuads`, подсветка хотбара цветом заряда с alpha `120`.
  - Verification: `compileJava` — `BUILD SUCCESSFUL`.
  - Known deviation: source-оверрайт `renderGuiItemOverlay` с посторонними модулями не портирован; подсветка хотбара перенесена через `DrawContext.drawItem`, который в 1.21.11 является активным путём.

- [x] Task 34: Регистрация, событие атаки, финальная сборка
  - Acceptance: модуль зарегистрирован, событие атаки подключено через target event API (`AttackEvent`), mixin-конфиг обновлён, нейминг под `fluxvisuals`, релиз не публиковать.
  - Verification: `compileJava` и `test obfuscateJar` — `BUILD SUCCESSFUL` с `-PgrunteonXmx=768m`; упоминаний источника в портированных файлах нет; релиз и бамп версий не делались.

### Phase 12: Порт ItemPhysics

- [x] Task 35: Модуль `ItemPhysics` и настройки
  - Acceptance: донорский модуль без настроек; имя `Item Physics`, описание `Реалистичное вращение и физика предметов на земле`, категория `Visuals`, ключ конфига сохраняется.
  - Verification: `compileJava` — `BUILD SUCCESSFUL`.
  - Known deviation: target использует `bind = -1` вместо донорского `bindKey = 0`, что эквивалентно отсутствию бинда; singleton сохранён для mixin-доступа.

- [x] Task 36: Рендер-хуки выброшенных предметов
  - Acceptance: донорская логика в полном объёме — подавление `MatrixStack.multiply(Quaternionfc)` в `ItemEntityRenderer.render` и для `ItemDisplayContext.GROUND` translate `0.0F, 0.47F, 0.0F` + rotation X `90°` в `ItemRenderer.renderItem`.
  - Verification: `compileJava` — `BUILD SUCCESSFUL`; точка инъекции подтверждена байткодом 1.21.11 (`MatrixStack.multiply:(Lorg/joml/Quaternionfc;)V`).
  - Known deviation: дескриптор метода и тип `Quaternionfc` адаптированы под 1.21.11; `require = 0` сохранён только у Redirect, как у донора.

- [x] Task 37: Регистрация, конфиг и финальная сборка
  - Acceptance: модуль уже зарегистрирован в `Manager`, оба mixin уже в конфиге, нейминг под `fluxvisuals`, подсказки приведены к донорскому описанию, релиз и бамп версий не публиковать.
  - Verification: `compileJava` и `test obfuscateJar` — `BUILD SUCCESSFUL` с `-PgrunteonXmx=768m`; `git diff --check` чистый; упоминаний источника в портированных файлах нет.
