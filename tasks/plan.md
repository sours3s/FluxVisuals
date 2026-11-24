# Implementation Plan: Замена Figura на встроенный BbModelRuntime

Source spec: запрос пользователя «убрать Figura и добавить что-то получше», решение от 24.11.2026.

## Overview

Заменяем bundled Figura + LuaJ на собственный рантайм, который читает уже существующие `.bbmodel` напрямую и рендерит их в third person и first person. Модели, папки,превью и weapon-аватары остаются на местах; конвертация в другие форматы не требуется. Сохраняем внешний вид и стандартные позы (`idle`, `walk`, `sprint`, `crouch`, `elytra`, `fly`, `blink`, `attack*`). `action_wheel`, `pings` и произвольный Lua не переносим.

Переход делается через флаг `fluxvisuals.bbmodel` (по умолчанию `false` до финального чекпоинта), чтобы каждая стадия оставалась откатываемой и не ломала рабочий `v1.0.65`.

## Architecture Decisions

- **Свой `BbModelRuntime` вместо внешнего мода.** CPM/BPM не грузят `.bbmodel` как есть, не поддерживают 29 mesh-моделей и weapon-аватары; Figura-бандл имеет лицензию Polyform-Noncommercial. Свой рантайм даёт контроль над камерой, руками и first-person без Lua-магии.
- **Никакой конвертации ассетов.** 68 `.bbmodel`, 124 `avatar.json`, 124 `avatar.png` и 186 оригинальных `.lua` остаются без изменений. Сгенерированные `z_fluxvisuals_first_person.lua` удаляются только на финальной уборке.
- **Единый рендер для body/head/weapon.** Один `BbModelRenderer` используется и для аватара игрока, и для модели оружия в руке.
- **Совместимость с текущим кодом до финального шага.** Пока флаг выключен, поведение полностью соответствует `v1.0.65`.
- **Версия релиза `1.1.0`** как мажерный переход рендер-стека.

## Task List

### Phase 1: Parser and renderer foundation

- [ ] Task 1: Добавить модель данных `.bbmodel`
- [ ] Task 2: Реализовать `BbModelLoader` и проверку на всех 68 моделях
- [ ] Task 3: Добавить загрузку текстур и `BbModelRenderer`

### Checkpoint: Foundation
- [ ] `./gradlew compileJava` проходит
- [ ] `./gradlew test --tests '*BbModelLoaderTest'` проходит
- [ ] Все 68 моделей парсятся, 2892 cube + 258 mesh элементов найдены

### Phase 2: Third-person avatar path

- [ ] Task 4: Добавить `BbCosmeticManager` и флаг `fluxvisuals.bbmodel`
- [ ] Task 5: Подключить third-person renderer hook
- [ ] Task 6: Переключить GUI, команду и first-person модули на новый менеджер

### Checkpoint: Avatar
- [ ] При `-Dfluxvisuals.bbmodel=true` аватар рисуется в третьем лице
- [ ] При выключенном флаге поведение `v1.0.65` не меняется
- [ ] `./gradlew compileJava` проходит

### Phase 3: First-person weapon and animation

- [ ] Task 7: Заменить Figura-item-hook на рендер кастомного меча
- [ ] Task 8: Добавить `BbAnimationPlayer` и стандартные позы

### Checkpoint: Weapon and animation
- [ ] Меч виден в 1/2/3 лице и находится в руке, не по центру экрана
- [ ] При мече обе руки скрыты; при другом предмете руки возвращаются
- [ ] Swing Animation работает одинаково для ванильной руки и кастомной модели
- [ ] Стандартные анимации проигрываются без ошибок в чате

### Phase 4: Figura removal and release

- [ ] Task 9: Удалить Java-мосты Figura и сгенерированный Lua-конвейер
- [ ] Task 10: Убрать JIJ-зависимости, mixin и сгенерированные Lua-файлы
- [ ] Task 11: Обновить версию до `1.1.0`, собрать и опубликовать релиз

### Checkpoint: Complete
- [ ] JAR не содержит Figura/LuaJ и упоминаний `figura` в `fabric.mod.json`
- [ ] Иконки, хотбар, HUD, ClickGUI и ватермарка работают как в `v1.0.65`
- [ ] Лоадер v1.0.14 запускает одиночный мир без ошибок
- [ ] Пользователь подтверждает визуальную проверку

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Mesh-части (258 элементов в 29 моделях) рендерятся с артефактами | High | Парсить `vertices`/`faces` напрямую, триангулировать quads, проверять на `Cool scythe` и weapon-моделях в первую очередь |
| Трансформ оружия в first person не совпадёт с ванильным | High | Переиспользовать текущую точку `@Redirect` в `HeldItemRendererMixin` и матрицу руки; оставить SwingAnimation без изменений |
| Third-person аватар conflict с NameTags/камерой | Medium | Новый mixin только для подмены модели; не трогать `PlayerEntityRendererMixin` nameplate-логику |
| Потеря сгенерированных Lua-скриптов сломает старый путь | Medium | Удалять их только после переключения флага в `true` и успешного чекпоинта |
| Сборка Grunteon требует JDK 25 | Low | Использовать `-PgrunteonJava=D:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe` |
| Нечаянное изменение защищённых файлов | High | Не трогать `server-repo/AuthServer/auth.db`, `mod/logs/`, backup-папку и `stash@{0}`; `server-repo` уже имеет unrelated modification |

## Open Questions

- Нужны ли нестандартные анимации (`attackr`, `minel`, `blockl`, `sit`, `trident`) помимо стандартных поз? По умолчанию добавляем их в приоритетную таблицу, если модель их содержит.
- Какой точный вид first-person weapon-трансформа нужен для `Cool scythe` — уточнить после первого визуального теста.
