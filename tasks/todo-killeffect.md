# Задачи: объединённый Kill Effect

Plan: `tasks/plan-killeffect.md`

## Phase 1: Contract and foundation

- [x] Task 1: Реестр кастомных режимов и миграция конфига
  - Acceptance: 8 кастомных режимов; ванильные значения и `Soul/Shatter` мигрируют; `index` восстанавливается после `load`.
  - Verification: `./gradlew.bat test --no-daemon`.
  - Files: `KillEffectModes.java`, `KillEffect.java`, `KillEffectModesTest.java`, `TooltipRegistrar.java`.

- [x] Task 2: Настройки и состояние триггеров
  - Acceptance: все настройки добавлены и скрываются по режиму.
  - Verification: `./gradlew.bat compileJava --no-daemon`.
  - Files: `KillEffect.java`, `TooltipRegistrar.java`.

- [x] Task 3: Надёжный trigger и тотем
  - Acceptance: attack + death/tick fallback + `status == 35`; дубли подавляются; состояние очищается.
  - Verification: `./gradlew.bat test --no-daemon`; ручная проверка.
  - Files: `KillEffect.java`, `KillEffectDeathMemoryTracker.java`, `ClientPlayNetworkHandlerMixin.java`.

### Checkpoint: Foundation

- [x] Тесты и компиляция проходят.
- [x] В GUI нет ванильных типов.

## Phase 2: World effects

- [x] Task 4: Луч и небесный луч
  - Acceptance: `Beam` и `Sky Beam` разные, уважают настройки и TTL.
  - Verification: `./gradlew.bat compileJava --no-daemon`.
  - Files: `KillEffectBeamRenderer.java`, `KillEffect.java`.

- [x] Task 5: Soul и Shatter
  - Acceptance: спирали/кольца/луч и осколки/вихрь/сферы.
  - Verification: `./gradlew.bat compileJava --no-daemon`.
  - Files: `KillEffectSoulRenderer.java`, `KillEffect.java`.

- [x] Task 6: Darkness, Spike Abduction и частицы тела
  - Acceptance: затемнение, похищение шипами, осколки с cap.
  - Verification: `./gradlew.bat compileJava --no-daemon`.
  - Files: `KillEffectDarknessRenderer.java`, `KillEffectShardsRenderer.java`, `KillEffect.java`, `EntityCullingMixin.java`.

### Checkpoint: World effects

- [x] Compile/test проходят.
- [x] Нет unbounded growth частиц/волн.

## Phase 3: Screen and camera

- [x] Task 7: Scan-виньетка, ударная волна и серый оверлей
  - Acceptance: шипы/виньетка/волна/оверлей; на паузе просроченные эффекты не рисуются.
  - Verification: `./gradlew.bat compileJava --no-daemon`.
  - Files: `KillEffectScanRenderer.java`, `KillEffectOverlayRenderer.java`, `KillEffect.java`.

- [x] Task 8: Зум и тряска камеры
  - Acceptance: временный FOV/сдвиг/угол; возврат состояния; без эффекта ванильное поведение.
  - Verification: `./gradlew.bat compileJava --no-daemon`; ручная проверка.
  - Files: `KillEffectCameraState.java`, `CameraMixin.java`, `GameRendererMixin.java`, `KillEffect.java`.

### Checkpoint: Screen and camera

- [x] HUD/ClickGUI не сломаны.
- [x] Камера возвращается в исходное состояние.

## Phase 4: Finish

- [x] Task 9: Документация и версия
  - Acceptance: tooltip/changelog/release notes обновлены; `mod_version=1.1.5`; маркеры обновлены.
  - Verification: `git diff --check`.
  - Files: `TooltipRegistrar.java`, `ChangelogPanel.java`, `release_notes.md`, `gradle.properties`, серверные маркеры.

- [x] Task 10: Сборка и публикация
  - Acceptance: `test obfuscateJar` проходит; артефакт проверен; публикация только после подтверждения.
  - Verification: SHA256; `gh release view`.
  - Files: `mod/build/release/fluxvisuals-1.1.5.jar`.

### Checkpoint: Complete

- [x] Все тесты проходят.
- [x] Обфускированный JAR собран.
- [ ] Пользователь подтвердил проверку в одиночном мире через loader `v1.0.16`.

## Phase 5: 1:1 visual port

- [x] Task 11: Точный Beam/Soul/Shatter
- [x] Task 12: Точный Darkness/Spike/Scan и ассеты
- [x] Task 13: Точные частицы тела и звуки
- [x] Task 14: Версия 1.1.6, сборка и публикация — REL-1.1.6 опубликован после прямого подтверждения
- [ ] Отступление: полный framebuffer-pass `killdistortion` — после визуальной проверки

## Phase 6: Возврат к эталонной логике вместо кубиков и линий

- [x] Task 15: Beam и Sky Beam по эталонной геометрии
  - Acceptance: 40 срезов × 3 glow-слоя, 10 нижних слоёв, цилиндры 64 блока, наземное кольцо, 28 частиц.
  - Verification: `compileJava`; нет `drawCube` в рендере луча.
  - Files: `KillEffectBeamRenderer.java`, `KillEffectGlow.java`.

- [x] Task 16: Soul и Shatter с рендером сущности-призрака
  - Acceptance: `OtherClientPlayerEntity` с параметрами цели и прозрачностью; пользовательский силуэт удалён; призрак удаляется вместе с эффектом.
  - Verification: `compileJava`.
  - Files: `KillEffectSoulRenderer.java`, `KillEffect.java`.

- [x] Task 17: Darkness, Spike и Body Particles без кубов
  - Acceptance: 12 → 6 ванильных частиц, исправленная альфа цилиндров, частица-след шипа, `Kind.CUBE` и `spawnVanillaParticles` удалены.
  - Verification: поиск по `CUBE` и `spawnVanillaParticles` — 0 совпадений.
  - Files: `KillEffectDarknessRenderer.java`, `KillEffectParticleSystem.java`.

- [x] Task 18: Локальная сборка без бампа версии
  - Acceptance: `test obfuscateJar` зелёный; `mod_version=1.1.5`; релиз не публиковался.
  - Verification: SHA256 `2AC0076205830F9A4311239DC37108E23873FF57A62FD37E52D0F1504B46A536`.
  - Files: `mod/build/release/fluxvisuals-1.1.5.jar`.

- [x] Task 19: Публикация по прямому подтверждению
  - Acceptance: пользователь попросил «залей релиз новой версии мода»; `v1.1.6` и `v1.0.17-loader` опубликованы.
  - Status: опубликовано; визуальная проверка 8 режимов ожидается.

## Phase 7: Версии и ссылки из админ-панели

- [x] Task 20: Loader `v1.0.17` берёт версию и обе ссылки только из `/api/mod/version`; 404 устранён, автообновление работает по ссылке из панели.
- [x] Task 21: Сервер автосинхронизирует ссылки на мод и лоадер из GitHub Releases (интервал 15 минут); вручную задавать не нужно.
