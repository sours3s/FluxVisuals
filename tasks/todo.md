# Задачи: замена Figura на BbModelRuntime

Plan: `tasks/plan.md`

## Phase 1: Parser and renderer foundation

- [ ] Task 1: Добавить модель данных `.bbmodel`
  - Acceptance: классы `BbModel`, `BbNode`, `BbElement` хранят group tree, cube/mesh geometry, animations и texture references.
  - Verification: `./gradlew compileJava`.
  - Dependencies: none.
  - Files: `mod/src/main/java/ru/fluxvisuals/utils/cosmetics/bbmodel/BbModel.java`, `BbNode.java`, `BbElement.java`.

- [ ] Task 2: Реализовать `BbModelLoader` и проверку на всех 68 моделях
  - Acceptance: парсер читает `elements`, `outliner`, `textures`, `animations`; находит 2892 cube и 258 mesh элементов; Decode base64-текстур без исключений.
  - Verification: `./gradlew test --tests '*BbModelLoaderTest'`.
  - Dependencies: Task 1.
  - Files: `mod/src/main/java/ru/fluxvisuals/utils/cosmetics/bbmodel/BbModelLoader.java`, `mod/build.gradle`, `mod/src/test/java/ru/fluxvisuals/utils/cosmetics/bbmodel/BbModelLoaderTest.java`.

- [ ] Task 3: Добавить загрузку текстур и `BbModelRenderer`
  - Acceptance: текстуры из `.bbmodel` превращаются в `Identifier` + `NativeImageBackedTexture`; рендерер рисует cube и mesh через `VertexConsumer` с light/overlay.
  - Verification: `./gradlew compileJava`.
  - Dependencies: Task 2.
  - Files: `mod/src/main/java/ru/fluxvisuals/utils/cosmetics/bbmodel/BbTextureCache.java`, `BbModelRenderer.java`.

## Checkpoint: Foundation

- [ ] `./gradlew compileJava` проходит.
- [ ] `./gradlew test --tests '*BbModelLoaderTest'` проходит.
- [ ] Все 68 моделей парсятся, найдены 2892 cube + 258 mesh элементов.

## Phase 2: Third-person avatar path

- [ ] Task 4: Добавить `BbCosmeticManager` и флаг `fluxvisuals.bbmodel`
  - Acceptance: API `apply(CosmeticEntry)`, `clear()`, `isApplied()`, `appliedId()` повторяет `FiguraBridge`; флаг читается из system property и по умолчанию `false`.
  - Verification: `./gradlew compileJava`.
  - Dependencies: Task 3.
  - Files: `mod/src/main/java/ru/fluxvisuals/utils/cosmetics/BbCosmeticManager.java`.

- [ ] Task 5: Подключить third-person renderer hook
  - Acceptance: при активной модели и включённом флаге ванильная модель игрока скрывается, вместо неё рисуется `BbModelRenderer`; nameplate-логика не затронута.
  - Verification: `./gradlew compileJava`; ручная проверка в одиночном мире.
  - Dependencies: Task 4.
  - Files: `mod/src/main/java/ru/fluxvisuals/mixin/CosmeticPlayerRendererMixin.java`, `mod/src/main/resources/fluxvisuals.mixins.json`.

- [ ] Task 6: Переключить GUI, команду и first-person модули на новый менеджер
  - Acceptance: клик по косметике вызывает `BbCosmeticManager`; `.reset` очищает его; `FirstPersonModel` и `CameraMixin` не зависят от Figura при включённом флаге; при выключенном флаге старый путь работает.
  - Verification: `./gradlew compileJava`; ручная проверка обеих веток флага.
  - Dependencies: Task 4.
  - Files: `mod/src/main/java/ru/fluxvisuals/ui/gui/component/render/GuiRenderCosmetics.java`, `mod/src/main/java/ru/fluxvisuals/ui/fluxgui/cosmetics/CosmeticsManager.java`, `mod/src/main/java/ru/fluxvisuals/commands/ResetCommand.java`, `mod/src/main/java/ru/fluxvisuals/module/impl/visuals/FirstPersonModel.java`, `mod/src/main/java/ru/fluxvisuals/mixin/CameraMixin.java`.

## Checkpoint: Avatar

- [ ] При `-Dfluxvisuals.bbmodel=true` аватар рисуется в третьем лице.
- [ ] При выключенном флаге поведение `v1.0.65` не меняется.
- [ ] `./gradlew compileJava` проходит.

## Phase 3: First-person weapon and animation

- [ ] Task 7: Заменить Figura-item-hook на рендер кастомного меча
  - Acceptance: для `ItemTags.SWORDS` и weapon-записи в first person рисуется модель из `.bbmodel`; руки скрыты; SwingAnimation сохраняет текущую позу;third-person не дублируется.
  - Verification: `./gradlew compileJava`; ручная проверка меча и обычного предмета.
  - Dependencies: Task 5.
  - Files: `mod/src/main/java/ru/fluxvisuals/mixin/HeldItemRendererMixin.java`.

- [ ] Task 8: Добавить `BbAnimationPlayer` и стандартные позы
  - Acceptance: анимации выбираются по имени и состоянию игрока; `idle/walk/sprint/crouch/fly/elytra/blink/attack*` проигрываются с интерполяцией keyframes; несовместимые имена игнорируются без ошибок.
  - Verification: `./gradlew compileJava`; ручная проверка ходьбы, спринта, приседания и swing.
  - Dependencies: Task 3, Task 7.
  - Files: `mod/src/main/java/ru/fluxvisuals/utils/cosmetics/bbmodel/BbAnimationPlayer.java`, `BbModelRenderer.java`.

## Checkpoint: Weapon and animation

- [ ] Меч виден в 1/2/3 лице и находится в руке, не по центру экрана.
- [ ] При мече обе руки скрыты; при другом предмете руки возвращаются.
- [ ] Swing Animation работает одинаково для ванильной руки и кастомной модели.
- [ ] Стандартные анимации проигрываются без ошибок в чате.

## Phase 4: Figura removal and release

- [ ] Task 9: Удалить Java-мосты Figura и сгенерированный Lua-конвейер
  - Acceptance: удалены `FiguraBridge.java`, `FiguraCameraGuard.java`, `CPMSubmitNodeCollectorCompatMixin.java`; `CosmeticsRepository` не вызывает `CosmeticFirstPerson.refreshGenerated`; `CosmeticFirstPerson.java` удаляется; оставшиеся импорты Figura отсутствуют.
  - Verification: `./gradlew compileJava`; grep по `org.figuramc` пустой.
  - Dependencies: Task 6, Task 7, Task 8.
  - Files: 6 Java-файлов + `fluxvisuals.mixins.json`.

- [ ] Task 10: Убрать JIJ-зависимости, mixin и сгенерированные Lua-файлы
  - Acceptance: `fabric.mod.json` не содержит Figura/LuaJ jars; `build.gradle` исключает их из fileTree; три JAR удалены; 124 сгенерированных `z_fluxvisuals_first_person.lua` удалены из source/build/runtime; `cosmetics.index` обновлён; changelog/lang/release notes без Figura-упоминаний.
  - Verification: grep по `figura|luaj` в `mod/src`, `mod/build.gradle`, `fabric.mod.json` пустой; `./gradlew remapJar`.
  - Dependencies: Task 9.
  - Files: `mod/build.gradle`, `fabric.mod.json`, `mod/src/main/resources/META-INF/jars/*`, `mod/src/main/resources/fluxvisuals/cosmetics/**/z_fluxvisuals_first_person.lua`, `mod/src/main/resources/fluxvisuals/cosmetics.index`, `ChangelogPanel.java`, `en_US.json`, `release_notes.md`.

- [ ] Task 11: Обновить версию до `1.1.0`, собрать и опубликовать релиз
  - Acceptance: `mod_version=1.1.0`, все маркеры обновлены, `obfuscateJar` собран, GitHub release создан, POST `/api/mod/version` принят, JAR скопирован в `game/mods`.
  - Verification: GET `https://fluxvisuals-server.onrender.com/api/mod/version` возвращает `1.1.0`; запуск через лоадер v1.0.14 без ошибок.
  - Dependencies: Task 10.
  - Files: маркеры версии, `mod/release_notes.md`, `game/mods`, GitHub release, API marker.

## Checkpoint: Complete

- [ ] JAR не содержит Figura/LuaJ и упоминаний `figura` в `fabric.mod.json`.
- [ ] Иконки, хотбар, HUD, ClickGUI и ватермарка работают как в `v1.0.65`.
- [ ] Лоадер v1.0.14 запускает одиночный мир без ошибок.
- [ ] Пользователь подтверждает визуальную проверку.
