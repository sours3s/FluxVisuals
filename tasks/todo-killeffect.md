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
  - Acceptance: пользователь попросил «закинь все релизы и лоадера и мода»; ассеты `v1.1.6` и `v1.0.17-loader` заменены после порта, коммит `52a8a6c9` запушен.
  - Verification: mod `sha256:327362238b3e099fcb90be2b6f50d9f0c3d2efec8dce758fbfb1948b7a8fa8f2`, loader `sha256:a41e170e2b81ab0e37de2e90bb88a4b6bf013c8e08e56bcbf486efa99f84be5b`; `/api/mod/version` отдаёт оба URL.
  - Status: опубликовано; визуальная проверка 8 режимов ожидается.

## Phase 7: Версии и ссылки из админ-панели

- [x] Task 20: Loader `v1.0.17` берёт версию и обе ссылки только из `/api/mod/version`; 404 устранён, автообновление работает по ссылке из панели.
- [x] Task 21: Сервер автосинхронизирует ссылки на мод и лоадер из GitHub Releases (интервал 15 минут); вручную задавать не нужно.

## Phase 8: Прямой порт эталонных триггеров и Scan-pass

- [x] Task 22: Beam/Soul/Spike/Scan по исходной логике
  - Acceptance: Beam — динамический цвет темы, эталонный `bloom.png`, LIGHTNING/LEQUAL, волна 1750 мс; Soul — `drawEntity` с yaw/альфой каждый кадр и удаление по `elapsed > duration`; Spike — victim-driven направление, тело жертвы, `CLOUD`-след; Scan — виньетка 600 мс, камера 650 мс, эталонные звуки и формулы.
  - Verification: `compileJava`; `test obfuscateJar`.
  - Files: `KillEffect.java`, `KillEffectBeamRenderer.java`, `KillEffectSoulRenderer.java`, `KillEffectDarknessRenderer.java`, `KillEffectOverlayRenderer.java`, `KillEffectCameraState.java`, `KillEffectSoundQueue.java`, `WorldRenderLayers.java`, `bloom.png`.

- [x] Task 23: Scan через эталонный шейдер вместо самодельной геометрии
  - Acceptance: `frag_effect_scan.vsh/fsh` подключены через `KillEffectScanPass` с UBO, depth основного framebuffer и аддитивным копированием прямо в цветовую текстуру сцены; sphere/cells/geometries удалены; лимит волн 4; глобальный `Shockwave` убран.
  - Verification: `test obfuscateJar`; SHA256 `327362238B3E099FCB90BE2B6F50D9F0C3D2EFEC8DCE758FBFB1948B7A8FA8F2`.
  - Files: `KillEffectScanPass.java`, `KillEffectScanRenderer.java`, `WorldRendererMixin.java`, `KillEffect.java`.

- [ ] Task 24: Визуальная проверка 8 режимов пользователем
  - Acceptance: пользователь подтверждает 8 эталонных эффектов, сохранение настроек и работу автообновления с `v1.0.17`.
  - Verification: ручной запуск через loader `v1.0.17`; перед следующим бампом/публикацией требуется отдельное подтверждение.
  - Status: ожидает пользователя; повторную публикацию не выполнять без подтверждения.

## Phase 9: Порт BetterMinecraft

- [x] Task 25: BetterMinecraft по эталонной реализации 1:1
  - Acceptance: singleton-модуль, точные `Animation`/`Easing`/`Easings`, настройки `Animation`, `Stack Messages`, `Anim Speed` (180/150/300, increment 1), анимации инвентаря, таба, чата, хотбара и букв, склейка сообщений. Самодельные режимы удалены.
  - Verification: `compileJava`, `test obfuscateJar` — `BUILD SUCCESSFUL`; `git diff --check` чистый.
  - Status: готово локально; хотбар обёрнут через `@WrapOperation` с безопасным pop матрицы; релиз не публиковался, ждём следующий модуль.

## Phase 10: Порт BlockOutline

- [x] Task 26: Настройки и каркас модуля
  - Acceptance: точные 12 настроек, порядок, значения и видимость по эталону; hidden-условия адаптированы под target API.
  - Files: `BlockOutline.java`, `ColorSetting.java`, `Module.java`.
  - Status: `compileJava` зелёный.

- [x] Task 27: Геометрия и анимация переключения блоков
  - Acceptance: `getOutlineShape`, `expand(0.002)`, дистанция 10000, smoothstep, длительность `max(35, round(280 / speed))`, объявленные `LINE_WIDTH`/`BASE_SWITCH_ANIMATION_MS` сохранены.
  - Files: `BlockOutline.java`.
  - Status: `compileJava` зелёный.

- [x] Task 28: Обычный рендер заливки и контура
  - Acceptance: 6 граней с отсечением по камере, 12 линий, `depthTest = !3D`, градиент и альфа по эталону.
  - Files: `BlockOutline.java`, `ColorUtil.java`.
  - Status: `compileJava` зелёный.

- [x] Task 29: Шейдерный рендер и ассеты
  - Acceptance: `BlockOutlineShaderUtil` с 21 режимом и скрытыми `Солнце`/`Комета`, UBO/pipeline под `fluxvisuals`, нейтральный pass label; шейдеры совпадают с эталоном.
  - Files: `BlockOutlineShaderUtil.java`, `assets/fluxvisuals/shaders/core/block_shader.vsh/.fsh`.
  - Status: `compileJava` и `test obfuscateJar` зелёные.

- [x] Task 30: Скрытие ванильного контура, регистрация, финальная сборка
  - Acceptance: `BlockOutlineMixin` с static guard, регистрация и mixin-конфиг без изменений, упоминаний источника нет.
  - Files: `BlockOutlineMixin.java`.
  - Status: готово локально; `git diff --check` чистый; релиз и бамп версий не делались.
