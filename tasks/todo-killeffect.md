# Задачи: объединённый Kill Effect

Plan: `tasks/plan-killeffect.md`

## Phase 1: Contract and foundation

- [x] Task 1: Реестр кастомных режимов и миграция конфига
  - Acceptance: 8 кастомных режимов; ванильные значения и `Soul/Shatter` мигрируют; `index` восстанавливается после `load`.
  - Verification: `./gradlew.bat test --no-daemon`.
  - Files: `KillEffectModes.java`, `KillEffect.java`, `KillEffectModesTest.java`, `TooltipRegistrar.java`.

- [ ] Task 2: Настройки и состояние триггеров
  - Acceptance: все настройки добавлены и скрываются по режиму.
  - Verification: `./gradlew.bat compileJava --no-daemon`.
  - Files: `KillEffect.java`, `TooltipRegistrar.java`.

- [ ] Task 3: Надёжный trigger и тотем
  - Acceptance: attack + death/tick fallback + `status == 35`; дубли подавляются; состояние очищается.
  - Verification: `./gradlew.bat test --no-daemon`; ручная проверка.
  - Files: `KillEffect.java`, `KillEffectDeathMemoryTracker.java`, `ClientPlayNetworkHandlerMixin.java`.

### Checkpoint: Foundation

- [ ] Тесты и компиляция проходят.
- [ ] В GUI нет ванильных типов.

## Phase 2: World effects

- [ ] Task 4: Луч и небесный луч
  - Acceptance: `Beam` и `Sky Beam` разные, уважают настройки и TTL.
  - Verification: `./gradlew.bat compileJava --no-daemon`.
  - Files: `KillEffectBeamRenderer.java`, `KillEffect.java`.

- [ ] Task 5: Soul и Shatter
  - Acceptance: спирали/кольца/луч и осколки/вихрь/сферы.
  - Verification: `./gradlew.bat compileJava --no-daemon`.
  - Files: `KillEffectSoulRenderer.java`, `KillEffect.java`.

- [ ] Task 6: Darkness, Spike Abduction и частицы тела
  - Acceptance: затемнение, похищение шипами, осколки с cap.
  - Verification: `./gradlew.bat compileJava --no-daemon`.
  - Files: `KillEffectDarknessRenderer.java`, `KillEffectShardsRenderer.java`, `KillEffect.java`, `EntityCullingMixin.java`.

### Checkpoint: World effects

- [ ] Compile/test проходят.
- [ ] Нет unbounded growth частиц/волн.

## Phase 3: Screen and camera

- [ ] Task 7: Scan-виньетка, ударная волна и серый оверлей
  - Acceptance: шипы/виньетка/волна/оверлей; на паузе просроченные эффекты не рисуются.
  - Verification: `./gradlew.bat compileJava --no-daemon`.
  - Files: `KillEffectScanRenderer.java`, `KillEffectOverlayRenderer.java`, `KillEffect.java`.

- [ ] Task 8: Зум и тряска камеры
  - Acceptance: временный FOV/сдвиг/угол; возврат состояния; без эффекта ванильное поведение.
  - Verification: `./gradlew.bat compileJava --no-daemon`; ручная проверка.
  - Files: `KillEffectCameraState.java`, `CameraMixin.java`, `GameRendererMixin.java`, `KillEffect.java`.

### Checkpoint: Screen and camera

- [ ] HUD/ClickGUI не сломаны.
- [ ] Камера возвращается в исходное состояние.

## Phase 4: Finish

- [ ] Task 9: Документация и версия
  - Acceptance: tooltip/changelog/release notes обновлены; `mod_version=1.1.5`; маркеры обновлены.
  - Verification: `git diff --check`.
  - Files: `TooltipRegistrar.java`, `ChangelogPanel.java`, `release_notes.md`, `gradle.properties`, серверные маркеры.

- [ ] Task 10: Сборка и публикация
  - Acceptance: `test obfuscateJar` проходит; артефакт проверен; публикация только после подтверждения.
  - Verification: SHA256; `gh release view`.
  - Files: `mod/build/release/fluxvisuals-1.1.5.jar`.

### Checkpoint: Complete

- [ ] Все тесты проходят.
- [ ] Обфускированный JAR собран.
- [ ] Пользователь подтвердил проверку в одиночном мире через loader `v1.0.14`.
