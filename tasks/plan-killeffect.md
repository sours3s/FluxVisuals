# Implementation Plan: объединённый Kill Effect

Source spec: запрос пользователя от 26.11.2026 — «соединить всё» из найденных реализаций, при этом ванильные типы не нужны. После брака визуала `1.1.5` пользователь уточнил требование: эффекты должны полностью повторять эталонные реализации, а не собственную интерпретацию.

## Overview

Сделать один модуль `Kill Effect` с кастомными эффектами вместо пяти ванильных режимов. В модуль входят: лазер/луч, небесный луч, душа, осколки, затемнение, похищение шипами, частицы тела, scan-виньетка с зумом/тряской и тотемный триггер. Код пишется свой, под API FluxVisuals; исходники и ассеты с `All Rights Reserved` не копируются. Старый план по `.bbmodel` не трогается.

## Architecture Decisions

- **Только кастомные режимы.** `Lightning / Blood / Explosion / Heart / Smoke` удаляются из GUI. Старые сохранённые значения мигрируют в `Beam`; `Soul` и `Shatter` становятся полноценными отдельными режимами, а не маппятся на ванильные.
- **Единый trigger.** Атака запоминает цель; смерть приходит из `EntityDeathEvent`/death status `3`; есть tick-fallback. Дубли подавляются по entity id и временным окнам. Тотем `status == 35` обрабатывается отдельным прямым хуком.
- **Состояние эффектов с TTL.** Активные эффекты живут в небольшом state-хранилище с ограничениями по количеству; просроченные записи удаляются и в `tick()`, и в `render()`, чтобы не рисовать мёртвые эффекты на паузе.
- **Рендер через существующие API.** Мир — `WorldRenderer.drawLine/drawQuad/drawCube`; экран — `Renderer2D`; камера — `CameraPositionEvent`/`EventRotation` и, если понадобится, минимальная правка `GameRendererMixin`. Полноценный framebuffer-distortion не переносится: в исходной реализации он заглушка, а в FluxVisuals нет безопасного готового API.
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

- [ ] Task 14: Версия 1.1.6, сборка и публикация
  - Acceptance: `test obfuscateJar` проходит, changelog/release notes обновлены, релиз опубликован только после подтверждения.
  - Verification: SHA256 артефакта; `gh release view`.
  - Status: ожидает визуальной проверки и явного подтверждения бампа/публикации.

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

- [ ] Task 19: Визуальная проверка и публикация
  - Acceptance: пользователь проверяет 8 режимов через loader `v1.0.16`; после положительного ответа — отдельное подтверждение на `1.1.6` и публикацию.
  - Status: локальный артефакт готов; публикация запрещена без подтверждения.

### Known deviation

- Полноценный framebuffer-pass `killdistortion` не подключён: в 1.21.11 безопасный GPU-pass требует отдельного `RenderPass` до HUD, а текущий хук даёт только экранный слой. Шейдеры лежат в `assets/fluxvisuals`, Java-код использует виньетку/кольцо/десатурацию. Решение по полному pass — после визуальной проверки.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Слишком много режимов и настроек усложнят поддержку | High | Маленькие рендер-хелперы, общий state, hidden()-настройки, лимиты TTL |
| Правка FOV/камеры конфликтует с `Custom FOV`/`Zoom` | High | Минимальные правки существующих хуков, возврат состояния в `finally`, проверка в одиночном мире |
| Framebuffer-distortion нет в безопасном виде | Medium | Экранный слой как fallback; полный GPU-pass отдельным шагом |
| Старые конфиги с ванильными режимами сломают UI | Medium | Нормализация после `load()` + regression-тест |
| Производительность приToMany эффектах | Medium | Cap частиц/волн, удаление просроченных записей в render/tick |
| Лицензионные ограничения исходных реализаций | High | Пользователь явно разрешил 1:1 ассеты; код/UI без чужих имен |

## Open Questions

- Реальное обесцвечивание/искажение мира через post-processing пока заменяется экранным слоем; если нужен настоящий shader-pass — решить отдельно.
- Скрытие модели цели реализуем через уже существующий `EntityCullingMixin`; если это вызовет проблемы с косметикой, оставить только осколки.
