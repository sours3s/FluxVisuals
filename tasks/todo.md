# Задачи: переход на встроенный `.bbmodel`-рантайм

Plan: `tasks/plan.md`

## Phase 1: Parser and renderer foundation

- [x] Task 1: Добавить модель данных `.bbmodel`
  - Acceptance: классы `BbModel`, `BbNode`, `BbElement` хранят group tree, cube/mesh geometry, animations и texture references.
  - Verification: `./gradlew compileJava`.
  - Files: `mod/src/main/java/ru/fluxvisuals/utils/cosmetics/bbmodel/BbModel.java`, `BbNode.java`, `BbElement.java`.

- [x] Task 2: Реализовать `BbModelLoader` и проверку на всех 68 моделях
  - Acceptance: парсер читает `elements`, `outliner`, `textures`, `animations`; находит 2892 cube и 258 mesh элементов; decode base64-текстур без исключений.
  - Verification: `./gradlew test --tests '*BbModelLoaderTest'`.
  - Files: `mod/src/main/java/ru/fluxvisuals/utils/cosmetics/bbmodel/BbModelLoader.java`, `mod/src/test/java/ru/fluxvisuals/utils/cosmetics/bbmodel/BbModelLoaderTest.java`.

- [x] Task 3: Добавить загрузку текстур и `BbModelRenderer`
  - Acceptance: текстуры из `.bbmodel` превращаются в `Identifier` + `NativeImageBackedTexture`; рендерер рисует cube и mesh через `VertexConsumer` с light/overlay.
  - Verification: `./gradlew compileJava`.
  - Files: `mod/src/main/java/ru/fluxvisuals/utils/cosmetics/bbmodel/BbTextureCache.java`, `BbModelRenderer.java`.

## Checkpoint: Foundation

- [x] `./gradlew compileJava` проходит.
- [x] `./gradlew test --tests '*BbModelLoaderTest'` проходит.
- [x] Все 68 моделей парсятся, найдены 2892 cube + 258 mesh элементов.

## Phase 2: Third-person avatar path

- [x] Task 4: Добавить `BbCosmeticManager` как единственный путь косметики
  - Acceptance: API `apply(CosmeticEntry)`, `clear()`, `isApplied()`, `appliedId()`; загрузка модели из папки записи.
  - Verification: `./gradlew compileJava`.
  - Files: `mod/src/main/java/ru/fluxvisuals/utils/cosmetics/BbCosmeticManager.java`.

- [x] Task 5: Подключить third-person renderer hook
  - Acceptance: при активной модели ванильная модель игрока подменяется `BbModelRenderer`; nameplate-логика не затронута.
  - Verification: `./gradlew compileJava`; ручная проверка в одиночном мире.
  - Files: `mod/src/main/java/ru/fluxvisuals/mixin/CosmeticPlayerRendererMixin.java`, `mod/src/main/resources/fluxvisuals.mixins.json`.

- [x] Task 6: Переключить GUI, команду и first-person модули на новый менеджер
  - Acceptance: клик по косметике вызывает `BbCosmeticManager`; `.reset` очищает его; `FirstPersonModel` и `CameraMixin` не зависят от старого движка.
  - Verification: `./gradlew compileJava`.
  - Files: `mod/src/main/java/ru/fluxvisuals/ui/gui/component/render/GuiRenderCosmetics.java`, `mod/src/main/java/ru/fluxvisuals/ui/fluxgui/cosmetics/CosmeticsManager.java`, `mod/src/main/java/ru/fluxvisuals/commands/ResetCommand.java`, `mod/src/main/java/ru/fluxvisuals/module/impl/visuals/FirstPersonModel.java`, `mod/src/main/java/ru/fluxvisuals/mixin/CameraMixin.java`.

## Checkpoint: Avatar

- [ ] Аватар рисуется в третьем лице (ручная проверка).
- [x] Старый путь удалён, флаг `fluxvisuals.bbmodel` больше не существует.
- [x] `./gradlew compileJava` проходит.

## Phase 3: First-person weapon and animation

- [x] Task 7: Заменить item-hook на рендер кастомного меча
  - Acceptance: для `ItemTags.SWORDS` и weapon-записи в first person рисуется модель из `.bbmodel`; руки скрыты; SwingAnimation сохраняет позу; third-person не дублируется.
  - Verification: `./gradlew compileJava`; ручная проверка меча и обычного предмета.
  - Files: `mod/src/main/java/ru/fluxvisuals/mixin/HeldItemRendererMixin.java`.

- [x] Task 8: Добавить `BbAnimationPlayer` и стандартные позы
  - Acceptance: анимации выбираются по имени и состоянию игрока; `idle/walk/sprint/crouch/fly/elytra/blink/attack*` проигрываются с интерполяцией keyframes; несовместимые имена игнорируются без ошибок.
  - Verification: `./gradlew compileJava`; ручная проверка ходьбы, спринта, приседания и swing.
  - Files: `mod/src/main/java/ru/fluxvisuals/utils/cosmetics/bbmodel/BbAnimationPlayer.java`, `BbModelRenderer.java`.

## Checkpoint: Weapon and animation

- [ ] Меч виден в 1/2/3 лице и находится в руке, не по центру экрана.
- [ ] При мече обе руки скрыты; при другом предмете руки возвращаются.
- [ ] Swing Animation работает одинаково для ванильной руки и кастомной модели.
- [ ] Стандартные анимации проигрываются без ошибок в чате.

## Phase 4: Removal and release

- [x] Task 9: Удалить Java-мосты и сгенерированный Lua-конвейер
  - Acceptance: удалены bridge/guard/compat-классы; `CosmeticsRepository` не вызывает генератор скриптов; оставшиеся импорты отсутствуют.
  - Verification: `./gradlew compileJava`; grep по `org.figuramc` пустой.

- [x] Task 10: Убрать JIJ-зависимости, mixin и сгенерированные Lua-файлы
  - Acceptance: `fabric.mod.json` не содержит jars старого движка; три JAR и лицензия удалены; 124 сгенерированных `z_fluxvisuals_first_person.lua` удалены из source/build/runtime; changelog и release notes переписаны.
  - Verification: grep по `figura|luaj` в исходниках/build-файле пустой; `./gradlew obfuscateJar`.

- [x] Task 11: Обновить версию до `1.1.0`, собрать и опубликовать релиз
  - Acceptance: `mod_version=1.1.0`, маркеры сервера и лоадера обновлены, `obfuscateJar` собран, GitHub release создан, POST `/api/mod/version` принят, JAR скопирован в `game/mods`.
  - Verification: GET `/api/mod/version` возвращает `1.1.0`; запуск через лоадер v1.0.14 без ошибок.

## Checkpoint: Complete

- [x] JAR не содержит классов/JAR старого движка и LuaJ.
- [ ] Иконки, хотбар, HUD, ClickGUI и ватермарка работают как в `v1.0.65`.
- [ ] Лоадер v1.0.14 запускает одиночный мир без ошибок.
- [ ] Пользователь подтверждает визуальную проверку.
