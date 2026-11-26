# Implementation Plan: объединённый Kill Effect

Source spec: запрос пользователя от 26.11.2026 — «соединить всё» из найденных реализаций, при этом ванильные типы не нужны.

## Overview

Сделать один модуль `Kill Effect` с кастомными эффектами вместо пяти ванильных режимов. В модуль входят: лазер/луч, небесный луч, душа, осколки, затемнение, похищение шипами, частицы тела, scan-виньетка с зумом/тряской и тотемный триггер. Код пишется свой, под API FluxVisuals; исходники и ассеты с `All Rights Reserved` не копируются. Старый план по `.bbmodel` не трогается.

## Architecture Decisions

- **Только кастомные режимы.** `Lightning / Blood / Explosion / Heart / Smoke` удаляются из GUI. Старые сохранённые значения мигрируют в `Beam`; `Soul` и `Shatter` становятся полноценными отдельными режимами, а не маппятся на ванильные.
- **Единый trigger.** Атака запоминает цель; смерть приходит из `EntityDeathEvent`/death status `3`; есть tick-fallback. Дубли подавляются по entity id и временным окнам. Тотем `status == 35` обрабатывается отдельным прямым хуком.
- **Состояние эффектов с TTL.** Активные эффекты живут в небольшом state-хранилище с ограничениями по количеству; просроченные записи удаляются и в `tick()`, и в `render()`, чтобы не рисовать мёртвые эффекты на паузе.
- **Рендер через существующие API.** Мир — `WorldRenderer.drawLine/drawQuad/drawCube`; экран — `Renderer2D`; камера — `CameraPositionEvent`/`EventRotation` и, если понадобится, минимальная правка `GameRendererMixin`. Полноценный framebuffer-distortion не переносится: в доноре он заглушка, а в FluxVisuals нет безопасного готового API.
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

- [ ] Task 5: Soul и Shatter
  - Acceptance: `Soul` рисует восходящие спирали/кольца/луч; `Shatter` — кольца, осколки, вихрь и сферы; оба работают на мобах и игроках по настройкам целей.
  - Verification: `./gradlew.bat compileJava --no-daemon`.
  - Files: `KillEffectSoulRenderer.java`, `KillEffect.java`.

- [ ] Task 6: Darkness, Spike Abduction и частицы тела
  - Acceptance: `Darkness` затемняет область/экран; `Spike Abduction` поднимает цель шипами/линиями; `Body Particles` создаёт осколки с настройками количества, размера, силы и времени жизни; есть cap.
  - Verification: `./gradlew.bat compileJava --no-daemon`.
  - Files: `KillEffectDarknessRenderer.java`, `KillEffectShardsRenderer.java`, `KillEffect.java`, `EntityCullingMixin.java`.

### Checkpoint: World effects

- [ ] Все world-режимы compile/test проходят.
- [ ] Нет unbounded growth частиц/волн.

### Phase 3: Screen and camera

- [ ] Task 7: Scan-виньетка, ударная волна и серый оверлей
  - Acceptance: `Scan` рисует шипы/виньетку, ударную волну и полупрозрачный серый оверлей; на паузе просроченные эффекты не рисуются.
  - Verification: `./gradlew.bat compileJava --no-daemon`.
  - Files: `KillEffectScanRenderer.java`, новый `KillEffectOverlayRenderer.java`, `KillEffect.java`.

- [ ] Task 8: Зум и тряска камеры
  - Acceptance: при включённых настройках эффект кратковременно меняет FOV и/или позицию камеры/угол; после окончания значения возвращаются; без эффекта поведение ванильное.
  - Verification: `./gradlew.bat compileJava --no-daemon`; ручная проверка, что камера не «залипает».
  - Files: `KillEffectCameraState.java`, `CameraMixin.java`, `GameRendererMixin.java`, `KillEffect.java`.

### Checkpoint: Screen and camera

- [ ] Экраниые эффекты не ломают HUD/ClickGUI.
- [ ] Камера возвращается в исходное состояние.

### Phase 4: Finish

- [ ] Task 9: Документация и версия
  - Acceptance: обновлены tooltip, changelog, release notes; `mod_version` бампится на `1.1.5`; маркеры сервера/лоадера обновлены.
  - Verification: `git diff --check`.
  - Files: `TooltipRegistrar.java`, `ChangelogPanel.java`, `release_notes.md`, `gradle.properties`, серверные маркеры.

- [ ] Task 10: Сборка и публикация
  - Acceptance: `./gradlew.bat test obfuscateJar` проходит; JAR проверен по `fabric.mod.json`; публикация выполняется только после отдельного подтверждения пользователя.
  - Verification: SHA256 артефакта; `gh release view`.
  - Files: `mod/build/release/fluxvisuals-1.1.5.jar`.

### Checkpoint: Complete

- [ ] Все тесты проходят.
- [ ] Обфускированный JAR собран.
- [ ] Пользователь подтвердил визуальную проверку в одиночном мире через loader `v1.0.14`.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Слишком много режимов и настроек усложнят поддержку | High | Маленькие рендер-хелперы, общий state, hidden()-настройки, лимиты TTL |
| Правка FOV/камеры конфликтует с `Custom FOV`/`Zoom` | High | Минимальные правки существующих хуков, возврат состояния в `finally`, проверка в одиночном мире |
| Framebuffer-distortion нет в безопасном виде | Medium | Не переносить; вместо него волна/серый оверлей |
| Старые конфиги с ванильными режимами сломают UI | Medium | Нормализация после `load()` + regression-тест |
| Производительность приToMany эффектах | Medium | Cap частиц/волн, удаление просроченных записей в render/tick |
| Лицензионные ограничения доноров | High | Только поведение, свой код, без чужих имен/ассетов |

## Open Questions

- Реальное обесцвечивание мира через post-processing пока заменяется полупрозрачным серым оверлеем; если нужен настоящий shader-pass — решить отдельно.
- Скрытие модели цели реализуем через уже существующий `EntityCullingMixin`; если это вызовет проблемы с косметикой, оставить только осколки.
