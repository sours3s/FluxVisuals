# Implementation Plan: встроенный `.bbmodel`-рантайм

Source spec: запрос пользователя «убрать старый движок и сделать что-то другое», решение от 24.11.2026.

## Overview

Собственный рантайм читает существующие `.bbmodel` напрямую и рендерит их в third person и first person. Модели, папки, превью и weapon-записи остаются на местах; конвертация в другие форматы не требуется. Сохраняем внешний вид и стандартные позы (`idle`, `walk`, `sprint`, `crouch`, `elytra`, `fly`, `blink`, `attack*`). `action_wheel`, `pings` и произвольный Lua не переносим.

Переход завершён: рантайм — единственный путь косметики, флаг `fluxvisuals.bbmodel` удалён, релиз — `1.1.0`.

## Architecture Decisions

- **Свой рантайм вместо внешнего мода.** CPM/BPM не грузят `.bbmodel` как есть, не поддерживают mesh-модели и weapon-аватары; встроенный бандл имел несовместимую лицензию. Свой рантайм даёт контроль над камерой, руками и first-person.
- **Никакой конвертации ассетов.** 68 `.bbmodel`, 124 `avatar.json`, 124 `avatar.png` и 186 оригинальных `.lua` остаются без изменений. Сгенерированные `z_fluxvisuals_first_person.lua` удалены.
- **Единый рендер для body/head/weapon.** Один `BbModelRenderer` используется и для аватара игрока, и для модели оружия в руке.
- **Версия релиза `1.1.0`** как мажерный переход рендер-стека.
- **Иконки не зависят от движка.** Превью читается напрямую из `avatar.png`, без внешнего рендера аватаров.

## Task List

### Phase 1: Parser and renderer foundation

- [x] Task 1: Добавить модель данных `.bbmodel`
- [x] Task 2: Реализовать `BbModelLoader` и проверку на всех 68 моделях
- [x] Task 3: Добавить загрузку текстур и `BbModelRenderer`

### Checkpoint: Foundation
- [x] `./gradlew compileJava` проходит
- [x] `./gradlew test --tests '*BbModelLoaderTest'` проходит
- [x] Все 68 моделей парсятся, 2892 cube + 258 mesh элементов найдены

### Phase 2: Third-person avatar path

- [x] Task 4: Добавить `BbCosmeticManager` как единственный путь косметики
- [x] Task 5: Подключить third-person renderer hook
- [x] Task 6: Переключить GUI, команду и first-person модули на новый менеджер

### Checkpoint: Avatar
- [ ] Аватар рисуется в третьем лице (ручная проверка)
- [x] Флаг удалён, старый путь более не используется
- [x] `./gradlew compileJava` проходит

### Phase 3: First-person weapon and animation

- [x] Task 7: Заменить item-hook на рендер кастомного меча
- [x] Task 8: Добавить `BbAnimationPlayer` и стандартные позы

### Checkpoint: Weapon and animation
- [ ] Меч виден в 1/2/3 лице и находится в руке, не по центру экрана
- [ ] При мече обе руки скрыты; при другом предмете руки возвращаются
- [ ] Swing Animation работает одинаково для ванильной руки и кастомной модели
- [ ] Стандартные анимации проигрываются без ошибок в чате

### Phase 4: Removal and release

- [x] Task 9: Удалить Java-мосты и сгенерированный Lua-конвейер
- [x] Task 10: Убрать JIJ-зависимости, mixin и сгенерированные Lua-файлы
- [x] Task 11: Обновить версию до `1.1.0`, собрать и опубликовать релиз

### Checkpoint: Complete
- [x] JAR не содержит классов/JAR старого движка и LuaJ
- [ ] Иконки, хотбар, HUD, ClickGUI и ватермарка работают как в `v1.0.65`
- [ ] Лоадер v1.0.14 запускает одиночный мир без ошибок
- [ ] Пользователь подтверждает визуальную проверку

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Mesh-части (258 элементов в 29 моделях) рендерятся с артефактами | High | Парсить `vertices`/`faces` напрямую, триангулировать quads, проверять `Cool scythe` и weapon-модели в первую очередь |
| Трансформ оружия в first person не совпадёт с ванильным | High | Переиспользовать точку `@Redirect` в `HeldItemRendererMixin` и матрицу руки; SwingAnimation без изменений |
| Third-person аватар conflict с NameTags/камерой | Medium | Mixin только для подмены модели; nameplate-логика не тронута |
| Сборка Grunteon требует JDK 25 | Low | Использовать `-PgrunteonJava=D:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe` |
| Нечаянное изменение защищённых файлов | High | Не трогать `server-repo/AuthServer/auth.db`, `mod/logs/`, backup-папку и `stash@{0}` |

## Open Questions

- Нужны ли нестандартные анимации (`attackr`, `minel`, `blockl`, `sit`, `trident`) помимо стандартных поз? По умолчанию добавляем их в приоритетную таблицу, если модель их содержит.
- Какой точный вид first-person weapon-трансформа нужен для `Cool scythe` — уточнить после визуального теста.
