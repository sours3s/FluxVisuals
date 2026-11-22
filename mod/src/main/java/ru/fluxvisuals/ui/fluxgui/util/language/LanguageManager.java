package ru.fluxvisuals.ui.fluxgui.util.language;

import ru.fluxvisuals.ui.fluxgui.module.Category;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;

public final class LanguageManager {
    private static boolean english = false;
    private static final Map<String, String> RU_TO_EN = new HashMap<>();
    private static final Map<String, String> EFFECT_KEY_EN = new HashMap<>();
    private static final Map<String, String> HUD_ITEM_PATH_EN = new HashMap<>();

    private static void putEffectEn(String idPath, String english) {
        EFFECT_KEY_EN.put("effect.minecraft." + idPath, english);
    }

    private static void putHudItemEn(String idPath, String english) {
        HUD_ITEM_PATH_EN.put(idPath, english);
    }

    private LanguageManager() {
    }

    public static String translateModuleDescription(String description) {
        return english && description != null && !description.isEmpty() ? RU_TO_EN.getOrDefault(description, description) : description;
    }

    public static boolean isEnglish() {
        return english;
    }

    public static void toggle() {
        english = !english;
    }

    public static String translateCategoryName(Category category) {
        if (!english) {
            return category.getName();
        } else if (category == Category.CONFIGS) {
            return "Configs";
        } else if (category == Category.MARKS) {
            return "Markers";
        } else if (category == Category.PLAYER) {
            return "Player";
        } else if (category == Category.VISUALS) {
            return "Visuals";
        } else if (category == Category.MISC) {
            return "Misc";
        } else if (category == Category.INTERFACE) {
            return "Interface";
        } else if (category == Category.UTILITY) {
            return "Utility";
        } else if (category == Category.SERVER_EVENTS) {
            return "Events/Mines";
        } else if (category == Category.MINES) {
            return "Mines";
        } else if (category == Category.THEMES) {
            return "Themes";
        } else if (category == Category.EVENTS) {
            return "IRC chat";
        } else if (category == Category.MESSENGER) {
            return "Messenger";
        } else {
            return category == Category.COSMETICS ? "Cosmetics" : category.getName();
        }
    }

    public static String translateForCategory(Category category, String text) {
        if (text != null && !text.isEmpty()) {
            String source = repairIrcMojibake(text);
            if (!english) {
                return source;
            }

            if (category == Category.VISUALS && "Р Р°Р·РјРµСЂ".equals(text)) {
                return "Scale";
            }

            if (category == Category.VISUALS && "Размер".equals(source)) {
                return "Scale";
            }

            String translated = RU_TO_EN.get(source);
            if (translated != null) {
                return translated;
            }

            String trimmed = source.trim();
            String trimmedTranslated;
            if (!trimmed.equals(source) && (trimmedTranslated = RU_TO_EN.get(trimmed)) != null) {
                int leading = 0;

                while (leading < source.length() && Character.isWhitespace(source.charAt(leading))) {
                    leading++;
                }

                int trailing = 0;

                while (trailing < source.length() - leading && Character.isWhitespace(source.charAt(source.length() - 1 - trailing))) {
                    trailing++;
                }

                String prefix = leading > 0 ? source.substring(0, leading) : "";
                String suffix = trailing > 0 ? source.substring(source.length() - trailing) : "";
                return prefix + trimmedTranslated + suffix;
            } else {
                return source;
            }
        } else {
            return text;
        }
    }

    public static String translateIrcUiText(String text) {
        if (text != null && !text.isEmpty()) {
            String source = repairIrcMojibake(text);
            if (!english) {
                return source;
            }

            String translated = RU_TO_EN.getOrDefault(source, source);
            translated = translated.replace("← Назад", "← " + RU_TO_EN.getOrDefault("Назад", "Back"));
            translated = translated.replace(" (Ваш друг)", " (" + RU_TO_EN.getOrDefault("Ваш друг", "Your friend") + ")");
            translated = translated.replace("Статус: ", RU_TO_EN.getOrDefault("Статус", "Status") + ": ");
            translated = translated.replace("Друзей: ", RU_TO_EN.getOrDefault("Друзей:", "Friends:") + " ");
            translated = translated.replace("Групп: ", RU_TO_EN.getOrDefault("Групп:", "Groups:") + " ");
            translated = translated.replace("Заявок: ", RU_TO_EN.getOrDefault("Заявок:", "Requests:") + " ");
            translated = translated.replace("Блокировки (", RU_TO_EN.getOrDefault("Блокировки", "Blocked users") + " (");
            translated = translated.replace("ЛС: ", RU_TO_EN.getOrDefault("ЛС:", "DM:") + " ");
            translated = translated.replace("Мои координаты: X: ", "My coordinates: X: ");
            translated = translated.replace("Подключение...", "Connecting...");
            translated = translated.replace("Ник для приглашения...", "Nickname to invite...");
            translated = translated.replace("Только владелец может приглашать", "Only the owner can invite");
            translated = translated.replace("Удалить", "Delete");
            translated = translated.replace("Выйти", "Leave");
            translated = translated.replace("Лимит", "Limit");
            translated = translated.replace("Гифки будут доступны через ", "GIFs will be available in ");
            translated = translated.replace("Подождите ", "Wait ");
            return translated.replace("с перед отправкой", "s before sending");
        } else {
            return text;
        }
    }

    public static String repairIrcMojibake(String text) {
        String current = text;

        for (int i = 0; i < 6 && looksMojibake(current); i++) {
            try {
                String repaired = new String(current.getBytes("windows-1251"), StandardCharsets.UTF_8);
                if (repaired.equals(current) || repaired.indexOf(63) >= 0 || repaired.indexOf(65533) >= 0) {
                    break;
                }

                current = repaired;
            } catch (Exception ignored) {
                break;
            }
        }

        return current.replace(' ', ' ')
            .replace("Р’В·", "В·")
            .replace("Р’В«", "В«")
            .replace("Р’В»", "В»")
            .replace("РІвЂ С’", "в†ђ")
            .replace("РІвЂ вЂ™", "в†’")
            .replace("РІР‚В¦", "...")
            .replace("РІСљвЂў", "Г—");
    }

    private static boolean looksMojibake(String text) {
        return text.contains("Р")
            || text.contains("С")
            || text.contains("Ð")
            || text.contains("Ñ")
            || text.contains("вЂ")
            || text.contains("Р ")
            || text.contains("РЎ")
            || text.contains("СЃ")
            || text.contains("Р’В·")
            || text.contains("Р’В«")
            || text.contains("Р’В»");
    }

    private static StatusEffect effectValue(RegistryEntry<StatusEffect> entry) {
        return entry.getKey().map(key -> Registries.STATUS_EFFECT.get(key.getValue())).orElse(null);
    }

    public static String translateStatusEffectName(StatusEffectInstance effect) {
        return translateStatusEffectName(effect, english);
    }

    public static String translateStatusEffectName(StatusEffectInstance effect, boolean useEnglish) {
        if (!useEnglish) {
            return effectValue(effect.getEffectType()).getName().getString();
        }

        String key = effectValue(effect.getEffectType()).getTranslationKey();
        return EFFECT_KEY_EN.getOrDefault(key, effectValue(effect.getEffectType()).getName().getString());
    }

    public static String translateStatusEffectName(RegistryEntry<StatusEffect> effect, boolean useEnglish) {
        if (!useEnglish) {
            return effectValue(effect).getName().getString();
        }

        String key = effectValue(effect).getTranslationKey();
        return EFFECT_KEY_EN.getOrDefault(key, effectValue(effect).getName().getString());
    }

    public static String translateHudCooldownItemName(Item item) {
        return translateHudCooldownItemName(item, english);
    }

    public static String translateHudCooldownItemName(Item item, boolean useEnglish) {
        if (!useEnglish) {
            return item.getName().getString();
        }

        String path = Registries.ITEM.getId(item).getPath();
        return HUD_ITEM_PATH_EN.getOrDefault(path, item.getName().getString());
    }

    static {
        RU_TO_EN.put("Режим", "Mode");
        RU_TO_EN.put("Выбор", "Selection");
        RU_TO_EN.put("Обычный", "Normal");
        RU_TO_EN.put("Шейдер", "Shader");
        RU_TO_EN.put("Блюр", "Blur");
        RU_TO_EN.put("Скорость шейдера", "Shader speed");
        RU_TO_EN.put("Волны", "Waves");
        RU_TO_EN.put("Плазма", "Plasma");
        RU_TO_EN.put("Вода", "Water");
        RU_TO_EN.put("Тень", "Shadow");
        RU_TO_EN.put("Неон", "Neon");
        RU_TO_EN.put("Шейдер кристаллов", "Crystal shader");
        RU_TO_EN.put("Выбирает главное меню", "Selects the main menu");
        RU_TO_EN.put("Показывает таймеры трапок", "Shows trap timers");
        RU_TO_EN.put("Трапка", "Trap");
        RU_TO_EN.put("Неизбежная трапка", "Inevitable Trap");
        RU_TO_EN.put("секунд", "seconds");
        RU_TO_EN.put("Размещение разделов", "Category placement");
        RU_TO_EN.put("Блюр панелей", "Panel blur");
        RU_TO_EN.put("Блюр фона", "Background blur");
        RU_TO_EN.put("Сила блюра панелей", "Panel blur strength");
        RU_TO_EN.put("Прозрачность GUI", "GUI opacity");
        RU_TO_EN.put("Скорость анимаций", "Animation speed");
        RU_TO_EN.put("Скорость анимации", "Animation speed");
        RU_TO_EN.put("Сверху", "Top");
        RU_TO_EN.put("Слева", "Left");
        RU_TO_EN.put("Снизу", "Bottom");
        RU_TO_EN.put("Поз X", "Pos X");
        RU_TO_EN.put("Поз Y", "Pos Y");
        RU_TO_EN.put("Размер", "Scale");
        RU_TO_EN.put("Прозрачность панели", "Panel opacity");
        RU_TO_EN.put("Максимальная яркость", "Maximum brightness");
        RU_TO_EN.put("Авто точка", "Auto point");
        RU_TO_EN.put("Прописывать по таймеру", "Run by timer");
        RU_TO_EN.put("Таймер", "Timer");
        RU_TO_EN.put("Автоматический /event delay", "Automatic /event delay");
        RU_TO_EN.put("Обычный игрок", "Normal player");
        RU_TO_EN.put("Князь", "Prince");
        RU_TO_EN.put("Автоматически перевыставляет вещи на аукционе", "Automatically relists items on the auction");
        RU_TO_EN.put("Хотбар", "Hotbar");
        RU_TO_EN.put("Инвентарь", "Inventory");
        RU_TO_EN.put("Показывает кулдауны в слотах", "Shows cooldowns in slots");
        RU_TO_EN.put("Запускает команды по кнопкам", "Runs commands by keybinds");
        RU_TO_EN.put("Создать новый макрос", "Create new macro");
        RU_TO_EN.put("Количество макросов", "Macro count");
        RU_TO_EN.put("На себя", "On self");
        RU_TO_EN.put("На игроков", "On players");
        RU_TO_EN.put("Полупрозрачные крылья за спиной", "Translucent wings behind the back");
        RU_TO_EN.put("Количество", "Count");
        RU_TO_EN.put("Цвет от темы", "Theme color");
        RU_TO_EN.put("Свой цвет", "Custom color");
        RU_TO_EN.put("Цвет", "Color");
        RU_TO_EN.put("Светлячки вокруг игрока", "Fireflies around the player");
        RU_TO_EN.put("Свечение", "Glow");
        RU_TO_EN.put("Красивые метеориты в мире", "Beautiful meteors in the world");
        RU_TO_EN.put("Кастомные модели игроков", "Custom player models");
        RU_TO_EN.put("Модель", "Model");
        RU_TO_EN.put("Патрик", "Patrick");
        RU_TO_EN.put("Анимация чата", "Chat animation");
        RU_TO_EN.put("Ничего не найдено", "Nothing found");
        RU_TO_EN.put("Попробуй другой запрос", "Try another query");
        RU_TO_EN.put("Избранных функций не найдено", "No favorite functions found");
        RU_TO_EN.put("При нажатии на звездочку можно добавить функцию в избранное", "Click the star to add a function to favorites");
        RU_TO_EN.put("Показывает только избранные функции", "Shows only favorite functions");
        RU_TO_EN.put("Открывает меню с темами", "Opens the theme menu");
        RU_TO_EN.put("Открывает поиск функционала", "Opens feature search");
        RU_TO_EN.put("Переводит клиент на другой язык", "Translates the client to another language");
        RU_TO_EN.put("Тип взмаха", "Swing type");
        RU_TO_EN.put("Сила взмаха", "Swing strength");
        RU_TO_EN.put("Длительность взмаха", "Swing duration");
        RU_TO_EN.put("Только при взмахе", "Only while swinging");
        RU_TO_EN.put("Анимации удара", "Hit animations");
        RU_TO_EN.put("Желейка", "Jelly");
        RU_TO_EN.put("Красивые шляпы", "Beautiful hats");
        RU_TO_EN.put("Красивая анимация на таргет", "Beautiful target animation");
        RU_TO_EN.put("Духи", "Spirits");
        RU_TO_EN.put("Прозрачность", "Opacity");
        RU_TO_EN.put("Градиент", "Gradient");
        RU_TO_EN.put("Цвет 2", "Color 2");
        RU_TO_EN.put("Вращать", "Rotate");
        RU_TO_EN.put("Скорость", "Speed");
        RU_TO_EN.put("Переходить к другим", "Switch to others");
        RU_TO_EN.put("Цвет духов", "Spirit color");
        RU_TO_EN.put("Отображать на", "Show on");
        RU_TO_EN.put("Опции", "Options");
        RU_TO_EN.put("Черный", "Black");
        RU_TO_EN.put("РЎРІР°РїР°РµС‚ РїСЂРµРґРјРµС‚С‹", "Swaps items");
        RU_TO_EN.put("РђРІС‚РѕРјР°С‚РёС‡РµСЃРєРё РµСЃС‚", "Automatically eats");
        RU_TO_EN.put(
            "Р—Р°РїСЂРµС‰Р°РµС‚ РІС‹Р±СЂР°СЃС‹РІР°С‚СЊ РїСЂРµРґРјРµС‚С‹ РёР· РІС‹Р±СЂР°РЅРЅС‹С… СЃР»РѕС‚РѕРІ", "Prevents dropping items from selected slots"
        );
        RU_TO_EN.put(
            "РђРІС‚РѕРјР°С‚РёС‡РµСЃРєРё РєР»РёРєР°РµС‚ РјС‹С€РєРѕР№ СЃ Р·Р°РґР°РЅРЅРѕР№ Р·Р°РґРµСЂР¶РєРѕР№", "Automatically clicks the mouse with a set delay"
        );
        RU_TO_EN.put("РЈР±РёСЂР°РµС‚ Р·Р°РґРµСЂР¶РєСѓ РїСЂС‹Р¶РєР° Рё РёСЃРїРѕР»СЊР·РѕРІР°РЅРёСЏ РїСЂРµРґРјРµС‚РѕРІ", "Removes delay for jumping and item use");
        RU_TO_EN.put("РђРІС‚РѕРјР°С‚РёС‡РµСЃРєРё РЅР°Р¶РёРјР°РµС‚ Shift РїСЂРё СѓРґР°СЂРµ", "Automatically presses Shift on hit");
        RU_TO_EN.put("РџСЂРµРґРѕС‚РІСЂР°С‰Р°РµС‚ Р±СЂРѕСЃР°РЅРёРµ РІСЂРµРґРЅС‹С… Р·РµР»РёР№", "Prevents throwing harmful potions");
        RU_TO_EN.put("РђРІС‚РѕРјР°С‚РёС‡РµСЃРєРёР№ СЂРµСЃРїР°СѓРЅ РїРѕСЃР»Рµ СЃРјРµСЂС‚Рё", "Automatic respawn after death");
        RU_TO_EN.put("Р‘С‹СЃС‚СЂРѕРµ РїРµСЂРµРєР»Р°РґС‹РІР°РЅРёРµ РїСЂРµРґРјРµС‚РѕРІ", "Quickly moves items");
        RU_TO_EN.put("РџСЂРµРґРјРµС‚ 1", "Item 1");
        RU_TO_EN.put("РџСЂРµРґРјРµС‚ 2", "Item 2");
        RU_TO_EN.put("РљРЅРѕРїРєР° СЃРІР°РїР°", "Swap key");
        RU_TO_EN.put("РўРѕС‚РµРј", "Totem");
        RU_TO_EN.put("Р©РёС‚", "Shield");
        RU_TO_EN.put("РЇР±Р»РѕРєРѕ", "Apple");
        RU_TO_EN.put("РЎС„РµСЂР°", "Head");
        RU_TO_EN.put("Р РµР¶РёРјС‹", "Modes");
        RU_TO_EN.put("Р•РґР°", "Food");
        RU_TO_EN.put("Р\u0098РЅРІРёР·", "Invis");
        RU_TO_EN.put("Р“РѕР»РѕРґ", "Hunger");
        RU_TO_EN.put("РЎР»РѕС‚ 1", "Slot 1");
        RU_TO_EN.put("РЎР»РѕС‚ 2", "Slot 2");
        RU_TO_EN.put("РЎР»РѕС‚ 3", "Slot 3");
        RU_TO_EN.put("РЎР»РѕС‚ 4", "Slot 4");
        RU_TO_EN.put("РЎР»РѕС‚ 5", "Slot 5");
        RU_TO_EN.put("РЎР»РѕС‚ 6", "Slot 6");
        RU_TO_EN.put("РЎР»РѕС‚ 7", "Slot 7");
        RU_TO_EN.put("РЎР»РѕС‚ 8", "Slot 8");
        RU_TO_EN.put("РЎР»РѕС‚ 9", "Slot 9");
        RU_TO_EN.put("РљРЅРѕРїРєР°", "Button");
        RU_TO_EN.put("Р›РµРІР°СЏ", "Left");
        RU_TO_EN.put("РџСЂР°РІР°СЏ", "Right");
        RU_TO_EN.put("Р—Р°РґРµСЂР¶РєР° (СЃРµРє)", "Delay (sec)");
        RU_TO_EN.put("РџСЂС‹Р¶РѕРє", "Jump");
        RU_TO_EN.put("РџРљРњ", "RMB");
        RU_TO_EN.put("Р—Р°РґРµСЂР¶РєР° (РјСЃ)", "Delay (ms)");
        RU_TO_EN.put("РљРѕРЅС„РёРіРё", "Configs");
        RU_TO_EN.put("РњРµС‚РєРё", "Markers");
        RU_TO_EN.put("Здесь можно найти любую функцию", "Here you can find any function");
        RU_TO_EN.put("РќР°Р·РІР°РЅРёРµ", "Name");
        RU_TO_EN.put("РќР°Р·РІР°РЅРёРµ...", "Name...");
        RU_TO_EN.put("СЃРѕР·РґР°С‚СЊ", "Create");
        RU_TO_EN.put("РЎРѕР·РґР°С‚СЊ", "Create");
        RU_TO_EN.put("РђРІС‚РѕСЂ", "Author");
        RU_TO_EN.put("Р’Р°С€Рё РјРµС‚РєРё", "Your markers");
        RU_TO_EN.put("РњРµС‚РєР°", "Marker");
        RU_TO_EN.put("Р\u0098РєРѕРЅРєР°", "Icon");
        RU_TO_EN.put("Р¤РѕРЅ", "Background");
        RU_TO_EN.put("РўРµРєСЃС‚", "Text");
        RU_TO_EN.put("РџСЂРѕР·СЂР°С‡РЅРѕСЃС‚СЊ", "Opacity");
        RU_TO_EN.put("Р Р°Р·РјРµСЂ", "Size");
        RU_TO_EN.put("РљРѕРѕСЂРґРёРЅР°С‚С‹", "Coordinates");
        RU_TO_EN.put("Р¦РІРµС‚Р°", "Colors");
        RU_TO_EN.put("Р’РєР»СЋС‡РµРЅРёРµ/РІС‹РєР»СЋС‡РµРЅРёРµ РјРѕРґСѓР»РµР№", "Module toggle sounds");
        RU_TO_EN.put("Р’С‹Р±РµСЂРёС‚Рµ СЂРµР¶РёРј: ", "Select mode: ");
        RU_TO_EN.put("РљСЂРёС‚", "Crit");
        RU_TO_EN.put("Р РµР¶РёРј РєСЂРёС‚Р°", "Crit mode");
        RU_TO_EN.put("РџСЂРѕР·СЂР°С‡РЅРѕСЃС‚СЊ Р·Р°Р»РёРІРєРё", "Fill opacity");
        RU_TO_EN.put("Р Р°Р·РјРµС‰РµРЅРёРµ СЂР°Р·РґРµР»РѕРІ", "Category placement");
        RU_TO_EN.put("Р’С‹Р±РµСЂРёС‚Рµ СЂР°Р·РјРµСЂ", "Select size");
        RU_TO_EN.put("РћС‚РєСЂС‹С‚СЊ СЂРµРґР°РєС‚РѕСЂ", "Open editor");
        RU_TO_EN.put("Р”РёСЃС‚Р°РЅС†РёСЏ(РєРѕРЅРµС†)", "Distance (end)");
        RU_TO_EN.put("РќР°С‡Р°Р»Рѕ С‚СѓРјР°РЅР°", "Fog start");
        RU_TO_EN.put("Р СѓРєРё", "Hands");
        RU_TO_EN.put("Р“СЂР°РґРёРµРЅС‚", "Gradient");
        RU_TO_EN.put("Р¦РІРµС‚ РіСЂР°РґРёРµРЅС‚Р°", "Gradient color");
        RU_TO_EN.put("РћС‚РѕР±СЂР°Р·РёС‚СЊ", "Show");
        RU_TO_EN.put("РђРєС‚РёРІР°С†РёСЏ", "Activation");
        RU_TO_EN.put("РќР°Р¶Р°С‚РёРµ", "Press");
        RU_TO_EN.put("РЈРґРµСЂР¶Р°РЅРёРµ", "Hold");
        RU_TO_EN.put("РљРѕР»РёС‡РµСЃС‚РІРѕ", "Amount");
        RU_TO_EN.put("РџР»Р°РІРЅРѕСЃС‚СЊ РїРѕР»РµС‚Р°", "Flight smoothness");
        RU_TO_EN.put("РЎРєРѕСЂ. РІСЂР°С‰РµРЅРёСЏ  (СЌС‚Рѕ СЃРєРѕСЂРѕСЃС‚СЊ РІСЂР°С‰РµРЅРёСЏ)", "Rotation speed");
        RU_TO_EN.put("РЁРёСЂРёРЅР° Р»РёРЅРёРё", "Line width");
        RU_TO_EN.put("Р¦РІРµС‚ СѓРґР°СЂР°", "Hit color");
        RU_TO_EN.put("РљСЂР°СЃРёС‚СЊ Р±СЂРѕРЅСЋ", "Color armor");
        RU_TO_EN.put("РџСѓР»СЊСЃР°С†РёСЏ", "Pulse");
        RU_TO_EN.put("Р§Р°СЂРєР°", "Enchanted G.A");
        RU_TO_EN.put("Р—РѕР»РѕС‚РѕРµ СЏР±Р»РѕРєРѕ", "Golden Apple");
        RU_TO_EN.put("Р\u00adРЅРґРµСЂ-Р¶РµРјС‡СѓРі", "Ender Pearl");
        RU_TO_EN.put("РџР»РѕРґ С…РѕСЂСѓСЃР°", "Chorus Fruit");
        RU_TO_EN.put("РўСЂР°РїРєР°", "Trapka");
        RU_TO_EN.put("РџР»Р°СЃС‚", "Plast");
        RU_TO_EN.put("РЎРЅРµР¶РѕРє", "Snowball");
        RU_TO_EN.put("РџСѓР·С‹СЂРµРє РѕРїС‹С‚Р°", "Experience Bottle");
        RU_TO_EN.put("Р Р°Р·РЅРѕС†РІРµС‚РЅС‹Рµ", "Multicolored");
        RU_TO_EN.put("Р—РІС‘Р·РґРѕС‡РєРё", "Stars");
        RU_TO_EN.put("Р”РѕР»Р»Р°СЂ", "Dollar");
        RU_TO_EN.put("РЎРµСЂРґРµС‡РєРё", "Hearts");
        RU_TO_EN.put("РЎРЅРµР¶РёРЅРєР°", "Snowflake");
        RU_TO_EN.put("РљРѕСЂРѕРЅР°", "Crown");
        RU_TO_EN.put("Р Р°РЅРґРѕРј", "Random");
        RU_TO_EN.put("РљРѕРіРґР° РїСЂРёРјРµРЅСЏС‚СЊ", "When to apply");
        RU_TO_EN.put("РљРѕР»-РІРѕ РїСЂРё Р°С‚Р°РєРµ", "Amount on attack");
        RU_TO_EN.put("РљРѕР»-РІРѕ РїСЂРё РїСЂС‹Р¶РєРµ", "Amount on jump");
        RU_TO_EN.put("РљРѕР»-РІРѕ РІ РјРёСЂРµ", "Amount in world");
        RU_TO_EN.put("РљРѕР»-РІРѕ РїСЂРё С‚РѕС‚РµРјРµ", "Amount on totem");
        RU_TO_EN.put("РљРѕР»-РІРѕ РїСЂРё Р±РµРіРµ", "Amount on run");
        RU_TO_EN.put("РЈРґР°СЂ", "Hit");
        RU_TO_EN.put("Р‘РµРі", "Run");
        RU_TO_EN.put("РџРµСЂРєР°", "Ender Pearl");
        RU_TO_EN.put("Р’ РјРёСЂРµ", "In world");
        RU_TO_EN.put("Р\u00adРЅРґРµСЂ РїС‘СЂР»", "Ender Pearl");
        RU_TO_EN.put("РЎС‚СЂРµР»Р°", "Arrow");
        RU_TO_EN.put("РўСЂРµР·СѓР±РµС†", "Trident");
        RU_TO_EN.put("Р\u0098РЅС‚РµСЂРІР°Р»", "Interval");
        RU_TO_EN.put("РџРµСЂРІРѕРµ Р»РёС†Рѕ", "First person");
        RU_TO_EN.put("РҐРІРѕСЃС‚", "Tail");
        RU_TO_EN.put("Р›РёРЅРёСЏ", "Line");
        RU_TO_EN.put("РЎРєСЂС‹РІР°С‚СЊ РѕС‚ 1-РіРѕ Р»РёС†Р°", "Hide in 1st person");
        RU_TO_EN.put("РџСЂР°РІР°СЏ СЂСѓРєР°", "Right hand");
        RU_TO_EN.put("Р›РµРІР°СЏ СЂСѓРєР°", "Left hand");
        RU_TO_EN.put("Р’ Р±СЂРѕРЅРµ", "In armor");
        RU_TO_EN.put("РЎ С‚РѕС‚РµРјРѕРј", "With totem");
        RU_TO_EN.put("Р’СЂРµСЏ Р¶РёР·РЅРё: (СЃРµРє)", "Lifetime: (sec)");
        RU_TO_EN.put("Р’СЂРµРјСЏ Р¶РёР·РЅРё: (СЃРµРє)", "Lifetime: (sec)");
        RU_TO_EN.put("РќРѕРІРѕРµ РёРјСЏ", "New name");
        RU_TO_EN.put("РћРіРѕРЅСЊ", "Fire");
        RU_TO_EN.put("РўС‹РєРІР°", "Pumpkin");
        RU_TO_EN.put("РЎРІРµС‡РµРЅРёРµ", "Glow");
        RU_TO_EN.put("РўР°Р±Р»РёС†Р°", "Scoreboard");
        RU_TO_EN.put("РќРёРєРё", "NameTags");
        RU_TO_EN.put("РўСЂСЏСЃРєР°", "HurtCam");
        RU_TO_EN.put("РўРѕС‚РµРј", "Totem");
        RU_TO_EN.put("Р’Р·СЂС‹РІС‹", "Explosions");
        RU_TO_EN.put("Р¤РµР№РµСЂРІРµСЂРєРё", "Fireworks");
        RU_TO_EN.put("РїСЂРѕР·СЂР°С‡РЅРѕСЃС‚СЊ Р·Р°Р»РёРІРєРё", "Fill opacity");
        RU_TO_EN.put("СЂР°Р·РјРµС‰РµРЅРёРµ СЂР°Р·РґРµР»РѕРІ", "Category placement");
        RU_TO_EN.put("РІС‹Р±РµСЂРёС‚Рµ СЂР°Р·РјРµСЂ", "Select size");
        RU_TO_EN.put("РѕС‚РєСЂС‹С‚СЊ СЂРµРґР°РєС‚РѕСЂ", "Open editor");
        RU_TO_EN.put("РґРёСЃС‚Р°РЅС†РёСЏ(РєРѕРЅРµС†)", "Distance (end)");
        RU_TO_EN.put("РЅР°С‡Р°Р»Рѕ С‚СѓРјР°РЅР°", "Fog start");
        RU_TO_EN.put("СЂСѓРєРё", "Hands");
        RU_TO_EN.put("РіСЂР°РґРёРµРЅС‚", "Gradient");
        RU_TO_EN.put("С†РІРµС‚ РіСЂР°РґРёРµРЅС‚Р°", "Gradient color");
        RU_TO_EN.put("РѕС‚РѕР±СЂР°Р·РёС‚СЊ", "Show");
        RU_TO_EN.put("Р°РєС‚РёРІР°С†РёСЏ", "Activation");
        RU_TO_EN.put("РЅР°Р¶Р°С‚РёРµ", "Press");
        RU_TO_EN.put("СѓРґРµСЂР¶Р°РЅРёРµ", "Hold");
        RU_TO_EN.put("РєРѕР»РёС‡РµСЃС‚РІРѕ", "Amount");
        RU_TO_EN.put("РїР»Р°РІРЅРѕСЃС‚СЊ РїРѕР»РµС‚Р°", "Flight smoothness");
        RU_TO_EN.put("СЃРєРѕСЂ. РІСЂР°С‰РµРЅРёСЏ  (СЌС‚Рѕ СЃРєРѕСЂРѕСЃС‚СЊ РІСЂР°С‰РµРЅРёСЏ)", "Rotation speed");
        RU_TO_EN.put("С€РёСЂРёРЅР° Р»РёРЅРёРё", "Line width");
        RU_TO_EN.put("С†РІРµС‚ СѓРґР°СЂР°", "Hit color");
        RU_TO_EN.put("РєСЂР°СЃРёС‚СЊ Р±СЂРѕРЅСЋ", "Color armor");
        RU_TO_EN.put("РїСѓР»СЊСЃР°С†РёСЏ", "Pulse");
        RU_TO_EN.put("С‡Р°СЂРєР°", "Enchanted G.A");
        RU_TO_EN.put("Р·РѕР»РѕС‚РѕРµ СЏР±Р»РѕРєРѕ", "Golden Apple");
        RU_TO_EN.put("СЌРЅРґРµСЂ-Р¶РµРјС‡СѓРі", "Ender Pearl");
        RU_TO_EN.put("РїР»РѕРґ С…РѕСЂСѓСЃР°", "Chorus Fruit");
        RU_TO_EN.put("С‚СЂР°РїРєР°", "Trapka");
        RU_TO_EN.put("РїР»Р°СЃС‚", "Plast");
        RU_TO_EN.put("СЃРЅРµР¶РѕРє", "Snowball");
        RU_TO_EN.put("РїСѓР·С‹СЂРµРє РѕРїС‹С‚Р°", "Experience Bottle");
        RU_TO_EN.put("СЂР°Р·РЅРѕС†РІРµС‚РЅС‹Рµ", "Multicolored");
        RU_TO_EN.put("Р·РІС‘Р·РґРѕС‡РєРё", "Stars");
        RU_TO_EN.put("РґРѕР»Р»Р°СЂ", "Dollar");
        RU_TO_EN.put("СЃРµСЂРґРµС‡РєРё", "Hearts");
        RU_TO_EN.put("СЃРЅРµР¶РёРЅРєР°", "Snowflake");
        RU_TO_EN.put("РєРѕСЂРѕРЅР°", "Crown");
        RU_TO_EN.put("СЂР°РЅРґРѕРј", "Random");
        RU_TO_EN.put("РєРѕРіРґР° РїСЂРёРјРµРЅСЏС‚СЊ", "When to apply");
        RU_TO_EN.put("РєРѕР»-РІРѕ РїСЂРё Р°С‚Р°РєРµ", "Amount on attack");
        RU_TO_EN.put("РєРѕР»-РІРѕ РїСЂРё РїСЂС‹Р¶РєРµ", "Amount on jump");
        RU_TO_EN.put("РєРѕР»-РІРѕ РІ РјРёСЂРµ", "Amount in world");
        RU_TO_EN.put("РєРѕР»-РІРѕ РїСЂРё С‚РѕС‚РµРјРµ", "Amount on totem");
        RU_TO_EN.put("РєРѕР»-РІРѕ РїСЂРё Р±РµРіРµ", "Amount on run");
        RU_TO_EN.put("СѓРґР°СЂ", "Hit");
        RU_TO_EN.put("Р±РµРі", "Run");
        RU_TO_EN.put("РїРµСЂРєР°", "Ender Pearl");
        RU_TO_EN.put("РїРµСЂРєР° (ender pearl)", "Ender Pearl");
        RU_TO_EN.put("РІ РјРёСЂРµ", "In world");
        RU_TO_EN.put("СЌРЅРґРµСЂ РїС‘СЂР»", "Ender Pearl");
        RU_TO_EN.put("РІСЂРµРјСЏ Р¶РёР·РЅРё: (СЃРµРє)", "Lifetime: (sec)");
        RU_TO_EN.put("СЃС‚СЂРµР»Р°", "Arrow");
        RU_TO_EN.put("С‚СЂРµР·СѓР±РµС†", "Trident");
        RU_TO_EN.put("РёРЅС‚РµСЂРІР°Р»", "Interval");
        RU_TO_EN.put("РїРµСЂРІРѕРµ Р»РёС†Рѕ", "First person");
        RU_TO_EN.put("РґРёСЃС‚Р°РЅС†РёСЏ", "Distance");
        RU_TO_EN.put("С…РІРѕСЃС‚", "Tail");
        RU_TO_EN.put("Р»РёРЅРёСЏ", "Line");
        RU_TO_EN.put("СЃРєСЂС‹РІР°С‚СЊ РѕС‚ 1-РіРѕ Р»РёС†Р°", "Hide in 1st person");
        RU_TO_EN.put("РїСЂР°РІР°СЏ СЂСѓРєР°", "Right hand");
        RU_TO_EN.put("Р»РµРІР°СЏ СЂСѓРєР°", "Left hand");
        RU_TO_EN.put("РІ Р±СЂРѕРЅРµ", "In armor");
        RU_TO_EN.put("СЃ С‚РѕС‚РµРјРѕРј", "With totem");
        RU_TO_EN.put("РІСЂРµСЏ Р¶РёР·РЅРё: (СЃРµРє)", "Lifetime: (sec)");
        RU_TO_EN.put("РЅРѕРІРѕРµ РёРјСЏ", "New name");
        RU_TO_EN.put("Р’С‹Р±РµСЂРёС‚Рµ СЂРµР¶РёРј", "Select mode");
        RU_TO_EN.put("Р›РѕРіРѕ", "Logo");
        RU_TO_EN.put("РҐРѕС‚РєРµРё", "HotKeys");
        RU_TO_EN.put("Р—РµР»СЊСЏ", "Potions");
        RU_TO_EN.put("РљРѕРѕСЂРґРёРЅР°С‚С‹", "Coords");
        RU_TO_EN.put("Р\u00adРєРёРїРёСЂРѕРІРєР°", "Armor");
        RU_TO_EN.put("Р\u0098РЅРІРµРЅС‚Р°СЂСЊ", "Inventory");
        RU_TO_EN.put("РџСЂРµРґРјРµС‚С‹ FT", "FT Items");
        RU_TO_EN.put("РњРµРґРёР°РїР»РµРµСЂ", "Media Player");
        RU_TO_EN.put("РџРѕРґСЃРІРµС‚РєР°", "Border");
        RU_TO_EN.put("РЎРёР»Р° Р±Р»СЋСЂР°", "Blur Strength");
        RU_TO_EN.put("Р—Р°РєСЂСѓРіР»РµРЅРёРµ", "Rounding");
        RU_TO_EN.put("РќРёРє", "Name");
        RU_TO_EN.put("РџРёРЅРі", "Ping");
        RU_TO_EN.put("Р¤РџРЎ", "FPS");
        RU_TO_EN.put("РЎРєРѕСЂРѕСЃС‚СЊ", "Speed");
        RU_TO_EN.put("РЎРµСЂРІРµСЂ", "Server");
        RU_TO_EN.put("Р’СЂРµРјСЏ", "Time");
        RU_TO_EN.put("Р РµРЅРґРµСЂРёС‚ С€Р»СЏРїС‹", "Renders hats");
        RU_TO_EN.put("РћС‚РѕР±СЂР°Р¶РµРЅРёРµ РёРЅС„РѕСЂРјР°С†РёРё Рѕ С†РµР»Рё", "Displays target information");
        RU_TO_EN.put("РљСЂР°СЃРёРІС‹Р№ ESP РЅР° С‚Р°СЂРіРµС‚", "Fancy target ESP");
        RU_TO_EN.put("Р\u0098Р·РјРµРЅСЏРµС‚ Р°РЅРёРјР°С†РёРё Рё СЂР°СЃРїРѕР»РѕР¶РµРЅРёРµ СЂСѓРє", "Changes hand animations and positions");
        RU_TO_EN.put("РџРѕР»СЊР·РѕРІР°С‚РµР»СЊСЃРєРёРµ РјРµС‚РєРё", "Custom markers");
        RU_TO_EN.put("РљСЂСѓРі РїСЂРё РїСЂС‹Р¶РєРµ", "Circle on jump");
        RU_TO_EN.put("РќР°СЃС‚СЂРѕР№РєР° СЌР»РµРјРµРЅС‚РѕРІ РёРЅС‚РµСЂС„РµР№СЃР°", "Interface elements settings");
        RU_TO_EN.put("Р\u0098Р·РјРµРЅСЏРµС‚ С†РІРµС‚ СЃСѓС‰РЅРѕСЃС‚Рё РїСЂРё РїРѕР»СѓС‡РµРЅРёРё СѓСЂРѕРЅР°", "Changes entity color on damage");
        RU_TO_EN.put("РќР°СЃС‚СЂРѕР№РєРё ClickGUI", "ClickGUI settings");
        RU_TO_EN.put("РЎРєСЂС‹РІР°РµС‚ РЅРёРєРЅРµР№Рј РёРіСЂРѕРєР°", "Hides player nickname");
        RU_TO_EN.put("РџРѕРєР°Р·С‹РІР°РµС‚ СЃС‚Р°С‚СѓСЃ РІ Discord", "Shows status in Discord");
        RU_TO_EN.put("РџРѕРєР°Р·С‹РІР°РµС‚ РёРЅС„РѕСЂРјР°С†РёСЋ Рѕ РµРґРµ (Р·РѕР»РѕС‚С‹Рµ СЏР±Р»РѕРєРё)", "Shows food info (golden apples)");
        RU_TO_EN.put("РџРѕРєР°Р·С‹РІР°РµС‚ СѓРІРµРґРѕРјР»РµРЅРёСЏ РЅР° СЌРєСЂР°РЅРµ", "Shows on-screen notifications");
        RU_TO_EN.put("РћС‚РѕР±СЂР°Р¶Р°РµС‚ СЃРІРѕР№ РЅРёРєРЅРµР№Рј РїСЂРё РІРёРґРµ РѕС‚ С‚СЂРµС‚СЊРµРіРѕ Р»РёС†Р°", "Shows your nickname in third-person view");
        RU_TO_EN.put("РђРЅРёРјР°С†РёСЏ РїРѕРґРЅСЏС‚РёСЏ СЂСѓРє", "Hand raise animation");
        RU_TO_EN.put("РћСЃС‚Р°РІР»СЏРµС‚ РєСЂР°СЃРёРІС‹Р№ СЃР»РµРґ Р·Р° РёРіСЂРѕРєР°РјРё", "Leaves a nice trail behind players");
        RU_TO_EN.put("РЎРїР°РІРЅРёС‚ С„РµР№РєРѕРІРѕРіРѕ РёРіСЂРѕРєР° РґР»СЏ С‚РµСЃС‚РѕРІ", "Spawns a fake player for testing");
        RU_TO_EN.put("РЎРїР°РІРЅРёС‚ С„РµР№РєРѕРІРѕРіРѕ РёРіСЂРѕРєР°", "Spawns a fake player");
        RU_TO_EN.put("СЃРїР°РІРЅРёС‚ С„РµР№РєРѕРІРѕРіРѕ РёРіСЂРѕРєР°", "Spawns a fake player");
        RU_TO_EN.put("РЎРІРѕР±РѕРґРЅР°СЏ РєР°РјРµСЂР° (РєР°Рє РІ F5) РїСЂРё СѓРґРµСЂР¶Р°РЅРёРё РєР»Р°РІРёС€Рё", "Free camera (like F5) while holding a key");
        RU_TO_EN.put("Р РёСЃСѓРµС‚ С‚СЂР°РµРєС‚РѕСЂРёСЋ РїРѕР»С‘С‚Р° СЃРЅР°СЂСЏРґРѕРІ", "Draws projectile trajectory");
        RU_TO_EN.put("РџРѕРјРѕС‰РЅРёРє РґР»СЏ FunTime", "FunTime helper");
        RU_TO_EN.put("С‚СѓРјР°РЅ РЅРµ СЂР°Р±РѕС‚Р°РµС‚", "Fog not working");
        RU_TO_EN.put("РЎРІРµС‚Р»СЏС‡РєРё РІС‹Р»РµС‚Р°СЋС‚ РїСЂРё СѓРґР°СЂРµ", "Fireflies appear on hit");
        RU_TO_EN.put("РћСЃС‚Р°РІР»СЏРµС‚ СЃС‚РµРєР»СЏРЅРЅСѓСЋ РєРѕРїРёСЋ РёРіСЂРѕРєР°", "Leaves a glass copy of the player");
        RU_TO_EN.put("РљР°СЃС‚РѕРјРЅС‹Рµ С…РёС‚Р±РѕРєСЃС‹ СЃСѓС‰РЅРѕСЃС‚РµР№", "Custom entity hitboxes");
        RU_TO_EN.put("РџРѕРґСЃРІРµС‚РєР° Р±Р»РѕРєР°", "Block highlight");
        RU_TO_EN.put("РљР°СЃС‚РѕРјРЅС‹Рµ РїР°СЂС‚РёРєР»С‹", "Custom particles");
        RU_TO_EN.put("РЎС‚Р°РІРёС‚ РјРµС‚РєСѓ РІ С‚РѕС‡РєРµ РІР·РіР»СЏРґР°", "Places a marker at the look point");
        RU_TO_EN.put("РџРѕРјРѕС‰РЅРёРє РґР»СЏ Р°СѓРєС†РёРѕРЅР°", "Auction helper");
        RU_TO_EN.put("РљР°СЃС‚РѕРјРЅС‹Р№ СЃРєРѕСЂР±РѕСЂРґ", "Custom scoreboard");
        RU_TO_EN.put("РќР°СЃС‚СЂРѕР№РєР° РѕС‚РѕР±СЂР°Р¶РµРЅРёСЏ СЂСѓРє (Р·Р°Р»РёРІРєР°)", "Hand display settings (fill)");
        RU_TO_EN.put("РќР°СЃС‚СЂРѕР№РєР° РѕС‚РѕР±СЂР°Р¶РµРЅРёСЏ СЂСѓРє", "Hand display settings");
        RU_TO_EN.put("Р¤РёР·РёРєР° РїСЂРµРґРјРµС‚РѕРІ", "Item physics");
        RU_TO_EN.put("Р\u0098Р·РјРµРЅСЏРµС‚ РІСЂРµРјСЏ СЃСѓС‚РѕРє РЅРµ СЂР°Р±РѕС‚Р°РµС‚", "Changes time of day (not working)");
        RU_TO_EN.put("Р\u0098Р·РјРµРЅСЏРµС‚ СЃРѕРѕС‚РЅРѕС€РµРЅРёРµ СЃС‚РѕСЂРѕРЅ СЌРєСЂР°РЅР°", "Changes screen aspect ratio");
        RU_TO_EN.put("РљР°СЃС‚РѕРјРЅС‹Р№ РїСЂРёС†РµР»", "Custom crosshair");
        RU_TO_EN.put("РџСЂРµРґРѕС‚РІСЂР°С‰Р°РµС‚ РёСЃРїРѕР»СЊР·РѕРІР°РЅРёРµ CapsLock", "Prevents using CapsLock");
        RU_TO_EN.put("РќРµ РїРѕР·РІРѕР»СЏРµС‚ РїРёСЃР°С‚СЊ РєР°РїСЃРѕРј", "Prevents writing in caps");
        RU_TO_EN.put("РђРІС‚РѕРјР°С‚РёС‡РµСЃРєРё Р·Р°С…РѕРґРёС‚ РЅР° СЂРµР¶РёРјС‹", "Automatically joins modes");
        RU_TO_EN.put("РђРІС‚РѕРјР°С‚РёС‡РµСЃРєРё СЂРµРіРёСЃС‚СЂРёСЂСѓРµС‚СЃСЏ Рё Р»РѕРіРёРЅРёС‚СЃСЏ", "Automatically registers and logs in");
        RU_TO_EN.put("РџР°СЂРѕР»СЊ", "Password");
        RU_TO_EN.put("Р“СЂРёС„", "Grief");
        RU_TO_EN.put("РџРѕРєР°Р·С‹РІР°РµС‚ СѓРІРµРґРѕРјР»РµРЅРёСЏ", "Shows notifications");
        RU_TO_EN.put(
            "РђРІС‚РѕРјР°С‚РёС‡РµСЃРєРё РІС‹С…РѕРґРёС‚ СЃ СЃРµСЂРІРµСЂР° РїСЂРё РЅРёР·РєРѕРј Р·РґРѕСЂРѕРІСЊРµ", "Automatically leaves server on low health"
        );
        RU_TO_EN.put("Р”РµС‚РµРєС‚РёС‚ РёРіСЂРѕРєРѕРІ РІ С‚Р°Р±Рµ", "Detects players in tab");
        RU_TO_EN.put("Р’РёР·СѓР°Р»СЊРЅС‹Р№ СЌС„С„РµРєС‚ РїСЂРё РЅРёР·РєРѕРј Р·РґРѕСЂРѕРІСЊРµ", "Visual effect at low health");
        RU_TO_EN.put("РћС‚РєР»СЋС‡Р°РµС‚ СЂРµРЅРґРµСЂ РѕРїСЂРµРґРµР»РµРЅРЅС‹С… СЌР»РµРјРµРЅС‚РѕРІ", "Disables rendering of certain elements");
        RU_TO_EN.put("РђРІС‚РѕРјР°С‚РёС‡РµСЃРєРё РїРёС€РµС‚ GG РїРѕСЃР»Рµ РёРіСЂС‹", "Automatically sends GG after game");
        RU_TO_EN.put("РџСЂРѕРёРіСЂС‹РІР°РµС‚ Р·РІСѓРєРё РїСЂРё РЅР°Р¶Р°С‚РёРё РєР»Р°РІРёС€", "Plays sounds on key presses");
        RU_TO_EN.put("Р РµР¶РёРј", "Mode");
        RU_TO_EN.put("РЇ", "Me");
        RU_TO_EN.put("Р\u0098РіСЂРѕРєРё", "Players");
        RU_TO_EN.put("РЎРІРѕР№ С†РІРµС‚", "Custom color");
        RU_TO_EN.put("Р¦РІРµС‚", "Color");
        RU_TO_EN.put("РљРѕСЂРѕРЅР°", "Crown");
        RU_TO_EN.put("Р”СѓС…Рё", "Spirits");
        RU_TO_EN.put("Р”РёСЃС‚Р°РЅС†РёСЏ", "Distance");
        RU_TO_EN.put("Р’С‹СЃРѕС‚Р°", "Height");
        RU_TO_EN.put("РЎРєРѕСЂ. РїРµСЂРµС…РѕРґР°", "Transition speed");
        RU_TO_EN.put("РџРµСЂРµС…РѕРґРёС‚СЊ Рє РґСЂСѓРіРёРј", "Switch to others");
        RU_TO_EN.put("Р¦РІРµС‚ РґСѓС…РѕРІ", "Spirits color");
        RU_TO_EN.put("РўРѕР»СЊРєРѕ РїСЂРё РЅР°РІРѕРґРєРµ", "Only on hover");
        RU_TO_EN.put("Р‘СЂРѕРЅСЏ", "Armor");
        RU_TO_EN.put("РџСЂРµРґРјРµС‚С‹", "Items");
        RU_TO_EN.put("РЎРІРѕР№ С†РІРµС‚ HP", "Custom HP color");
        RU_TO_EN.put("Р¦РІРµС‚ HP", "HP color");
        RU_TO_EN.put("РљСЂР°СЃРёС‚СЊ РёРєРѕРЅРєСѓ РїСЂРё СѓРґР°СЂРµ", "Tint icon on hit");
        RU_TO_EN.put("РЎРІРѕР№ С†РІРµС‚ СѓРґР°СЂР°", "Custom hit color");
        RU_TO_EN.put("Р¦РІРµС‚ СѓРґР°СЂР°", "Hit color");
        RU_TO_EN.put("Р›РёС†Рѕ", "Face");
        RU_TO_EN.put("РљРІР°РґСЂР°С‚", "Square");
        RU_TO_EN.put("РљСЂСѓРі", "Circle");
        RU_TO_EN.put("РЎС‚РёР»СЊ", "Style");
        RU_TO_EN.put("РЎРѕСЂС‚РёСЂРѕРІРєР°", "Sorting");
        RU_TO_EN.put("РўР°СЂРіРµС‚", "Target");
        RU_TO_EN.put("Р”СѓРµР»Рё", "Duels");
        RU_TO_EN.put("РњРѕР±С‹", "Mobs");
        RU_TO_EN.put("Р–РёРІРѕС‚РЅС‹Рµ", "Animals");
        RU_TO_EN.put("РўРёРї", "Type");
        RU_TO_EN.put("Р РѕРјР±", "Diamond");
        RU_TO_EN.put("РџСЂРёР·СЂР°РєРё", "Ghosts");
        RU_TO_EN.put("РЎС‚РёР»СЊ РїСЂРёР·СЂР°РєРѕРІ", "Ghost style");
        RU_TO_EN.put("РЁРёСЂРёРЅР°", "Width");
        RU_TO_EN.put("Р”Р»РёРЅР°", "Length");
        RU_TO_EN.put("РљРѕР»-РІРѕ РїСЂРёР·СЂР°РєРѕРІ", "Ghost count");
        RU_TO_EN.put("РџСЂРё РЅР°РІРѕРґРєРµ", "On hover");
        RU_TO_EN.put("РњРµРЅСЏС‚СЊ РїСЂРё СѓРґР°СЂРµ", "Change on hit");
        RU_TO_EN.put("РњРµРЅСЏС‚СЊ РЅР°", "Change to");
        RU_TO_EN.put("РЎС‚Р°РЅРґР°СЂС‚", "Standard");
        RU_TO_EN.put("Р’Р·РјР°С…", "Swing");
        RU_TO_EN.put("Р’Р·РјР°С… 2", "Swing 2");
        RU_TO_EN.put("РЎРґРІРёРі", "Shift");
        RU_TO_EN.put("Р›РѕРјР°РЅРёРµ", "Break");
        RU_TO_EN.put("Р’С‹РїР°Рґ", "Thrust");
        RU_TO_EN.put("РљРѕРїСЊС‘", "Spear");
        RU_TO_EN.put("Р’РЅРёР·", "Down");
        RU_TO_EN.put("Р–РµР»Рµ", "Jelly");
        RU_TO_EN.put("РўСЂСЏСЃРєР°", "Shake");
        RU_TO_EN.put("РџРµСЂРµРјРµС‰РµРЅРёРµ", "Translation");
        RU_TO_EN.put("РўРѕР»СЊРєРѕ СЃ РєРёР»Р»РєРѕР№", "Only with KillAura");
        RU_TO_EN.put("РџРѕРІРѕСЂРѕС‚", "Rotation");
        RU_TO_EN.put("РЎРёР»Р° РЅР°РєР»РѕРЅР°", "Tilt strength");
        RU_TO_EN.put("Р—РЅР°С‡РµРЅРёРµ РЅР°РєР»РѕРЅР°", "Tilt value");
        RU_TO_EN.put("РђР№С‚РµРј 360", "Item 360");
        RU_TO_EN.put("Р’СЂРµРјСЏ Р¶РёР·РЅРё", "Lifetime");
        RU_TO_EN.put("Р”Р°Р»СЊРЅРѕСЃС‚СЊ (Pizdec)", "Range (Pizdec)");
        RU_TO_EN.put("Р—Р°Р»РёРІРєР°", "Fill");
        RU_TO_EN.put("Р‘Р»СЋСЂ", "Blur");
        RU_TO_EN.put("РЎС‚РµРєР»СЏРЅРЅС‹Р№", "Glass");
        RU_TO_EN.put("РћС‚С‚РµРЅРѕРє Р±Р»СЋСЂР°", "Blur Tint");
        RU_TO_EN.put("РЎРІРµС‡РµРЅРёРµ", "Glow");
        RU_TO_EN.put("Р—РµР»СЊСЏ", "Potions");
        RU_TO_EN.put("РЎРІРµСЂС…Сѓ", "Top");
        RU_TO_EN.put("РЎРЅРёР·Сѓ", "Bottom");
        RU_TO_EN.put("РЎР»РµРІР°", "Left");
        RU_TO_EN.put("РћС‚СЃР»РµР¶РёРІР°РµС‚ РїРѕС‚РµСЂСЋ С‚РѕС‚РµРјР° РёРіСЂРѕРєР°РјРё", "Tracks when players lose a totem");
        RU_TO_EN.put("РџСЂРµРґРїСЂРѕСЃРјРѕС‚СЂ С€Р°Р»РєРµСЂРѕРІ", "Shulker box contents preview");
        RU_TO_EN.put(
            "РђРІС‚РѕРјР°С‚РёС‡РµСЃРєРё РїРёС€РµС‚ СЃРѕРѕР±С‰РµРЅРёСЏ РїРѕСЃР»Рµ СѓР±РёР№СЃС‚РІР° Рё/РёР»Рё РїРѕ С‚Р°Р№РјРµСЂСѓ",
            "Auto chat after kills and/or on a timer"
        );
        RU_TO_EN.put("Р\u00adС„С„РµРєС‚С‹ РїСЂРё СѓР±РёР№СЃС‚РІРµ СЃСѓС‰РЅРѕСЃС‚Рё", "Effects when you kill an entity");
        RU_TO_EN.put("РџРѕРєР°Р·С‹РІР°РµС‚ СЂР°РґРёСѓСЃ СѓРґР°СЂР° РІРѕРєСЂСѓРі РёРіСЂРѕРєР°", "Shows attack reach around the player");
        RU_TO_EN.put("РЎС‡С‘С‚ РїРѕРїР°РґР°РЅРёР№ РїРѕ РёРіСЂРѕРєР°Рј РЅР°Рґ РіРѕР»РѕРІРѕР№", "Hit counter above players");
        RU_TO_EN.put("РЈР»СѓС‡С€РµРЅРёСЏ С‡Р°С‚Р°: Р°РЅС‚Рё-СЃРїР°Рј Рё РёСЃС‚РѕСЂРёСЏ", "Chat tweaks: anti-spam and history");
        RU_TO_EN.put("РђРІС‚РѕРјР°С‚РёС‡РµСЃРєРё РЅР°Р¶РёРјР°РµС‚ Shift РїСЂРё СѓРґР°СЂРµ РїРѕ РёРіСЂРѕРєСѓ", "Auto sneak when hitting a player");
        RU_TO_EN.put("Р‘С‹СЃС‚СЂРѕРµ РёСЃРїРѕР»СЊР·РѕРІР°РЅРёРµ РїСѓР·С‹СЂСЊРєРѕРІ РѕРїС‹С‚Р°", "Faster XP bottle use");
        RU_TO_EN.put("РђРІС‚РѕРјР°С‚РёС‡РµСЃРєРёР№ СЃРїСЂРёРЅС‚", "Automatic sprint");
        RU_TO_EN.put("РљРЅРѕРїРєР° СЂРµР¶РёРјР° РІ РєРѕРЅС‚РµР№РЅРµСЂР°С…", "Mode button in containers");
        RU_TO_EN.put("Р—Р°С‰РёС‚Р° РѕС‚ РІС‹С…РѕРґР° Рё РєРѕРјР°РЅРґ РІ PVP", "Blocks risky commands / disconnect in PVP");
        RU_TO_EN.put("РЎРІРѕР±РѕРґРЅР°СЏ РєР°РјРµСЂР° РїСЂРё СѓРґРµСЂР¶Р°РЅРёРё РєР»Р°РІРёС€Рё", "Free camera while holding a key");
        RU_TO_EN.put("РџСЂРёР±Р»РёР¶РµРЅРёРµ РєР°РјРµСЂС‹", "Camera zoom");
        RU_TO_EN.put("РџР»Р°РІРЅС‹Рµ Р°РЅРёРјР°С†РёРё РёРЅС‚РµСЂС„РµР№СЃР°", "Smooth UI animations");
        RU_TO_EN.put("РЈРІРµРґРѕРјР»РµРЅРёРµ Рѕ РЅРёР·РєРѕР№ РїСЂРѕС‡РЅРѕСЃС‚Рё Р±СЂРѕРЅРё", "Low armor durability alert");
        RU_TO_EN.put("РџРѕРґСЃРІРµС‚РєР° Рё СѓРІРµР»РёС‡РµРЅРёРµ РїСЂРµРґРјРµС‚РѕРІ", "Item highlight and scale");
        RU_TO_EN.put("РљР°СЃС‚РѕРјРЅС‹Р№ С†РІРµС‚ С…РёС‚Р±РѕРєСЃРѕРІ", "Custom hitbox colors");
        RU_TO_EN.put("РњР°РєСЃРёРјР°Р»СЊРЅР°СЏ СЏСЂРєРѕСЃС‚СЊ", "Maximum brightness");
        RU_TO_EN.put("РџР»Р°С‰ СЃ Р»РѕРіРѕС‚РёРїРѕРј Aura", "Cape with FluxVisuals logo");
        RU_TO_EN.put("Р\u0098Р·РјРµРЅСЏРµС‚ РІСЂРµРјСЏ СЃСѓС‚РѕРє", "Changes time of day");
        RU_TO_EN.put("РљР°СЃС‚РѕРјРЅС‹Р№ С‚СѓРјР°РЅ", "Custom fog");
        RU_TO_EN.put("РђРІС‚РѕРјР°С‚РёС‡РµСЃРєР°СЏ РїРµСЂРµРїСЂРѕРґР°Р¶Р° РЅР° Р°СѓРєС†РёРѕРЅРµ", "Auction house auto relist");
        RU_TO_EN.put("РўРµРєСЃС‚ РїРѕСЃР»Рµ СѓР±РёР№СЃС‚РІР°: ", "Message after kill: ");
        RU_TO_EN.put("67", "Sticker / emote overlay");
        RU_TO_EN.put("РћС‚РѕР±СЂР°Р¶Р°РµС‚ СЃРІРѕР№ РЅРёРєРЅРµР№Рј РѕС‚ 3 Р»РёС†Р°", "Shows your nickname in third person");
        RU_TO_EN.put("РљР°СЃС‚РѕРјРЅС‹Рµ Р·РІСѓРєРё", "Custom sounds");
        RU_TO_EN.put("РўРѕР»СЊРєРѕ Р·Р°С‡Р°СЂ. С‚РѕС‚РµРјС‹", "Enchanted totems only");
        RU_TO_EN.put("Р—Р°РґРµСЂР¶РєР°", "Delay");
        RU_TO_EN.put("РўР°Р±", "Tab");
        RU_TO_EN.put("РЎРјРµРЅР° РїРµСЂСЃРїРµРєС‚РёРІС‹", "Perspective change");
        RU_TO_EN.put("РћС‚РєСЂС‹С‚РёРµ РёРЅРІРµРЅС‚Р°СЂСЏ", "Opening inventory");
        RU_TO_EN.put("Р—СѓРј", "Zoom");
        RU_TO_EN.put("РЎРјРµРЅР° СЃР»РѕС‚РѕРІ", "Hotbar swap");
        RU_TO_EN.put("РџРѕСЂРѕРі (%)", "Threshold (%)");
        RU_TO_EN.put("РџРѕРєР°Р·С‹РІР°С‚СЊ %", "Show %");
        RU_TO_EN.put("РЎРєРѕСЂРѕСЃС‚СЊ С€РµР№РґРµСЂР°", "Shader speed");
        RU_TO_EN.put("РљРѕСЃРјРѕСЃ", "Space");
        RU_TO_EN.put("Р¦РІРµС‚ 1", "Color 1");
        RU_TO_EN.put("Р¦РІРµС‚ 2", "Color 2");
        RU_TO_EN.put("РўРѕР»СЊРєРѕ СЏ", "Only me");
        RU_TO_EN.put("Р‘Р»СЋСЂ РїР°РЅРµР»РµР№", "Panel blur");
        RU_TO_EN.put("Р‘Р»СЋСЂ С„РѕРЅР°", "Background blur");
        RU_TO_EN.put("РџСЂРѕР·СЂР°С‡РЅРѕСЃС‚СЊ GUI", "GUI opacity");
        RU_TO_EN.put("РЎРєРѕСЂРѕСЃС‚СЊ Р°РЅРёРјР°С†РёР№", "Animation speed");
        RU_TO_EN.put("РћР±РІРѕРґРєР° РјРѕРґСѓР»РµР№", "Module outline");
        RU_TO_EN.put("Р¦РІРµС‚ РѕР±РІРѕРґРєРё", "Outline color");
        RU_TO_EN.put("РћР±РІРѕРґРєР°", "Outline");
        RU_TO_EN.put("Р—РІСѓРє", "Sound");
        RU_TO_EN.put("РЎРєРѕСЂ. РІСЂР°С‰РµРЅРёСЏ", "Rotation speed");
        RU_TO_EN.put("РЎРєСЂС‹РІР°С‚СЊ РјРѕРё РїРѕРїР°РґР°РЅРёСЏ", "Hide my hits");
        RU_TO_EN.put("РџРѕРєР°Р·С‹РІР°С‚СЊ РјРѕРё РїРѕРїР°РґР°РЅРёСЏ", "Show my hits");
        RU_TO_EN.put("РЎРєСЂС‹РІР°С‚СЊ РµСЃР»Рё РЅРµС‚Сѓ РїРѕРїР°РґР°РЅРёР№", "Hide when no hits");
        RU_TO_EN.put("РЎР±СЂРѕСЃРёС‚СЊ СЃС‚Р°С‚РёСЃС‚РёРєСѓ: ", "Reset stats: ");
        RU_TO_EN.put("РћС‚РѕР±СЂР°Р¶Р°С‚СЊ РЅР° РґСЂСѓРіРёС…", "Show on others");
        RU_TO_EN.put("РћС‚РѕР±СЂР°Р¶Р°С‚СЊ РЅР°:", "Show on:");
        RU_TO_EN.put("РџР»РµРµСЂ РІРѕ РІСЃРµС… РјРµРЅСЋ", "Player in all menus");
        RU_TO_EN.put("РЎРёР»Р° РїРѕРґСЃРІРµС‚РєРё", "Glow strength");
        RU_TO_EN.put("РџР°СѓР·Р°/РџР»РµР№", "Pause/Play");
        RU_TO_EN.put("РџСЂРµРґ. С‚СЂРµРє", "Prev. track");
        RU_TO_EN.put("РЎР»РµРґ. С‚СЂРµРє", "Next track");
        RU_TO_EN.put("РџРѕРґСЃРІРµС‚РєР° РІ РёРЅРІРµРЅС‚Р°СЂРµ", "Inventory highlight");
        RU_TO_EN.put("РЈРІРµР»РёС‡РµРЅРёРµ РїСЂРµРґРјРµС‚РѕРІ", "Item scale");
        RU_TO_EN.put("РџРѕРґСЃРІРµС‚РєР° СЃР»РѕС‚РѕРІ", "Slot highlight");
        RU_TO_EN.put("Р›РѕРіРёРєР°", "Logic");
        RU_TO_EN.put("Р Р°Р·РјРµСЂ РєСЂСѓРіР°", "Circle size");
        RU_TO_EN.put("РњРѕР»РЅРёСЏ", "Lightning");
        RU_TO_EN.put("Р Р°Р·СЂСѓС€РµРЅРёРµ", "Shatter");
        RU_TO_EN.put("Р Р°Р·СЂСѓС€РµРЅРёСЏ", "Shatter");
        RU_TO_EN.put("РЈС…РѕРґ РїРѕРґ Р·РµРјР»СЋ", "Sink underground");
        RU_TO_EN.put("РџР°СЂС‚РёРєР»С‹", "Particles");
        RU_TO_EN.put("Р›РѕРіРёРєР° РїРѕР»С‘С‚Р°", "Flight logic");
        RU_TO_EN.put("РЎРІРѕР±РѕРґРЅС‹Р№ РїРѕР»С‘С‚", "Free flight");
        RU_TO_EN.put("Р’Р·Р»С‘С‚", "Takeoff");
        RU_TO_EN.put("РџР°РґРµРЅРёРµ", "Fall");
        RU_TO_EN.put("РЎРёР»Р° СЃРІРµС‡РµРЅРёСЏ", "Glow strength");
        RU_TO_EN.put("Р\u00adРЅРґРµСЂ РџС‘СЂР»", "Ender Pearl");
        RU_TO_EN.put("РљРЅРѕРїРєР° РїСЂРѕСЃРјРѕС‚СЂР°", "Peek key");
        RU_TO_EN.put("РўРёРї РІР·РјР°С…Р°", "Swing type");
        RU_TO_EN.put("РЎРёР»Р° РІР·РјР°С…Р°", "Swing strength");
        RU_TO_EN.put("Р”Р»РёС‚РµР»СЊРЅРѕСЃС‚СЊ РІР·РјР°С…Р°", "Swing duration");
        RU_TO_EN.put("РўРѕР»СЊРєРѕ РїСЂРё РІР·РјР°С…Рµ", "Only while swinging");
        RU_TO_EN.put("РљСЂРёСЃС‚Р°Р»Р»С‹", "Crystals");
        RU_TO_EN.put("РљРѕР»РёС‡РµСЃС‚РІРѕ РєСЂРёСЃС‚Р°Р»Р»РѕРІ", "Crystal count");
        RU_TO_EN.put("РљРІР°РґСЂР°С‚ 2", "Square 2");
        RU_TO_EN.put("Р§РµСЂРµРї", "Skull");
        RU_TO_EN.put("Р‘РёРЅРґ", "Bind");
        RU_TO_EN.put("РљРѕРѕСЂРґРёРЅР°С‚Р° X", "X coordinate");
        RU_TO_EN.put("РљРѕРѕСЂРґРёРЅР°С‚Р° Y", "Y coordinate");
        RU_TO_EN.put("РљРѕРѕСЂРґРёРЅР°С‚Р° Z", "Z coordinate");
        RU_TO_EN.put("РўРµРєСЃС‚", "Text");
        RU_TO_EN.put("РђРЅС‚Рё-РЎРїР°Рј", "Anti-spam");
        RU_TO_EN.put("РЎРѕС…СЂР°РЅСЏС‚СЊ РёСЃС‚РѕСЂРёСЋ", "Keep history");
        RU_TO_EN.put("Р“СЂРѕРјРєРѕСЃС‚СЊ", "Volume");
        RU_TO_EN.put("РђРІС‚Рѕ Р\u0098РІРµРЅС‚", "Auto event");
        RU_TO_EN.put("РџРµСЂРµРІС‹СЃС‚Р°РІР»РµРЅРёРµ", "Relisting");
        RU_TO_EN.put("РђРІС‚РѕР\u0098РЅРІРµСЃС‚", "Auto invest");
        RU_TO_EN.put("Р’РІРµРґРёС‚Рµ РёРјСЏ", "Enter name");
        RU_TO_EN.put("РљРЅРѕРїРєР° Р°РєС‚РёРІР°С†РёРё", "Activation key");
        RU_TO_EN.put("Р РµР¶РёРј РѕС‚РѕР±СЂР°Р¶РµРЅРёСЏ", "Display mode");
        RU_TO_EN.put("РђРЅР°СЂС…РёСЏ", "Anarchy");
        RU_TO_EN.put("Р’ РѕС‚РґРµР»СЊРЅРѕРј РѕРєРѕС€РєРµ", "In a separate window");
        RU_TO_EN.put("РћР±С‹С‡РЅС‹Р№", "Normal");
        RU_TO_EN.put("Р\u00adРЅРґРµСЂ", "Ender");
        RU_TO_EN.put("РќРµС‚", "None");
        RU_TO_EN.put("РћР±Р»Р°РєР°", "Clouds");
        RU_TO_EN.put("РљРЅСЏР·СЊ", "Knyaz");
        RU_TO_EN.put("РћР±С‹С‡РЅС‹Р№ РёРіСЂРѕРє", "Regular player");
        RU_TO_EN.put("РЎРµСЂРґРµС‡РєРѕ", "Heart");
        RU_TO_EN.put("РљСЂРµСЃС‚", "Cross");
        RU_TO_EN.put("РљСѓР±", "Cube");
        RU_TO_EN.put("РћС‚РѕР±СЂР°Р·РёС‚СЊ: ", "Show: ");
        RU_TO_EN.put("РљР»РёРєРЅРёС‚Рµ РџРљРњ РґР»СЏ РЅР°СЃС‚СЂРѕРµРє", "Right-click for settings");
        RU_TO_EN.put("РџРљРњ РґР»СЏ РЅР°СЃС‚СЂРѕРµРє", "Right-click for settings");
        RU_TO_EN.put("Р§РµСЂРЅС‹Р№", "Black");
        RU_TO_EN.put("Р§С‘СЂРЅС‹Р№", "Black");
        RU_TO_EN.put("Лава", "Lava");
        RU_TO_EN.put("Вода", "Water");
        RU_TO_EN.put("Заливка", "Fill");
        RU_TO_EN.put("Шейдер", "Shader");
        RU_TO_EN.put("Обводка блоков", "Block outline");
        RU_TO_EN.put("Прозрачность контура", "Outline opacity");
        RU_TO_EN.put("Показывать заливку", "Show fill");
        RU_TO_EN.put("Прозрачность заливки", "Fill opacity");
        RU_TO_EN.put("Свой цвет", "Custom color");
        RU_TO_EN.put("Цвет 1", "Color 1");
        RU_TO_EN.put("Цвет 2", "Color 2");
        RU_TO_EN.put("Градиент", "Gradient");
        RU_TO_EN.put("Убирает жидкости", "Removes fluids");
        RU_TO_EN.put("Показывает таймеры структур", "Shows structure timers");
        RU_TO_EN.put("Показывает таймеры трапок", "Shows trap timers");
        RU_TO_EN.put("Трапка", "Trap");
        RU_TO_EN.put("Пласт", "Plast");
        RU_TO_EN.put("Неизбежная трапка", "Inevitable trap");
        RU_TO_EN.put("Драконья ловушка", "Dragon trap");
        RU_TO_EN.put("Драконья трапка", "Dragon trap");
        RU_TO_EN.put("секунд", "seconds");
        RU_TO_EN.put("Ясно", "Clear");
        RU_TO_EN.put("Хотбар", "Hotbar");
        RU_TO_EN.put("Показывает кулдауны в слотах", "Shows cooldowns in slots");
        RU_TO_EN.put("Заряд ветра", "Wind charge");
        RU_TO_EN.put("Команда 1", "Command 1");
        RU_TO_EN.put("Кнопка 1", "Button 1");
        RU_TO_EN.put("Команда 2", "Command 2");
        RU_TO_EN.put("Кнопка 2", "Button 2");
        RU_TO_EN.put("Команда 3", "Command 3");
        RU_TO_EN.put("Кнопка 3", "Button 3");
        RU_TO_EN.put("Команда 4", "Command 4");
        RU_TO_EN.put("Кнопка 4", "Button 4");
        RU_TO_EN.put("Запускает команды по кнопкам", "Runs commands by keys");
        RU_TO_EN.put("Прозрачность панели", "Panel opacity");
        RU_TO_EN.put("Logo Scale", "Logo scale");
        RU_TO_EN.put("Р‘РµР»С‹Р№", "White");
        RU_TO_EN.put("РЎРµСЂС‹Р№", "Gray");
        RU_TO_EN.put("Р”РёСЃС‚Р°РЅС†РёСЏ (РљРѕРЅРµС†)", "Distance (end)");
        RU_TO_EN.put("РЎРєРѕСЂРѕСЃС‚СЊ РІСЂР°С‰РµРЅРёСЏ", "Rotation speed");
        RU_TO_EN.put("Р¦РІРµС‚ РїРѕРґСЃРІРµС‚РєРё", "Glow color");
        RU_TO_EN.put("РЎРЅРµР¶РёРЅРєРё", "Snowflakes");
        RU_TO_EN.put("Р\u0098РЅС‚РµСЂРІР°Р»", "Interval");
        RU_TO_EN.put("Р РµР¶РёРј:", "Mode:");
        RU_TO_EN.put("РџРѕСЃР»Рµ СѓР±РёР№СЃС‚РІР°", "After kill");
        RU_TO_EN.put("РџРѕ РІСЂРµРјРµРЅРё", "On timer");
        RU_TO_EN.put("РўРµРєСЃС‚ РїРѕСЃР»Рµ СѓР±РёР№СЃС‚РІР°: ", "Message after kill: ");
        RU_TO_EN.put("Р§Р°СЃС‚РѕС‚Р° РѕС‚РїСЂР°РІРєРё (РІ СЃРµРє): ", "Send interval (sec): ");
        RU_TO_EN.put("РўРµРєСЃС‚: ", "Text: ");
        RU_TO_EN.put("Р РёСЃРѕРІР°С‚СЊ СЂР°РґРёСѓСЃ", "Draw radius");
        RU_TO_EN.put("РљРЅРѕРїРєР° С‚СЂР°РїРєРё", "Trap key");
        RU_TO_EN.put("РљРЅРѕРїРєР° РґРµР·РѕСЂРёРµРЅС‚Р°С†РёРё", "Disorientation key");
        RU_TO_EN.put("РљРЅРѕРїРєР° СЏРІРЅРѕР№ РїС‹Р»Рё", "Blatant dust key");
        RU_TO_EN.put("РљРЅРѕРїРєР° Р±РѕР¶СЊРµР№ Р°СѓСЂС‹", "God aura key");
        RU_TO_EN.put("РљРЅРѕРїРєР° РїР»Р°СЃС‚Р°", "Plast key");
        RU_TO_EN.put("РџСѓСЃС‚Рѕ", "Empty");
        RU_TO_EN.put(" Рј", " m");
        RU_TO_EN.put("РљСѓР»РґР°СѓРЅС‹", "Cooldowns");
        RU_TO_EN.put("РЎРїРёСЃРѕРє С‚РµРј", "Theme list");
        RU_TO_EN.put("РќР°СЃС‚СЂРѕР№РєР° С‚РµРј", "Theme settings");
        RU_TO_EN.put("РќРѕРІР°СЏ С‚РµРјР°", "New theme");
        RU_TO_EN.put("Р¦РІРµС‚ РїР°РЅРµР»РµР№", "Panel color");
        RU_TO_EN.put("Р’РёР·СѓР°Р»СЊРЅС‹Рµ РјРѕРґСѓР»Рё", "Visual modules");
        RU_TO_EN.put("Р\u0098РєРѕРЅРєРё РІ РіСѓРё", "GUI icons");
        RU_TO_EN.put("Р\u0098РєРѕРЅРєРё РІ С…СѓРґРµ", "HUD icons");
        RU_TO_EN.put("РќР°Р·РІР°РЅРёСЏ РєР°С‚РµРіРѕСЂРёР№", "Category titles");
        RU_TO_EN.put("РљРЅРѕРїРєР° РІРєР». РјРѕРґСѓР»СЏ", "Module on button");
        RU_TO_EN.put("РљРЅРѕРїРєР° РІС‹РєР». РјРѕРґСѓР»СЏ", "Module off button");
        RU_TO_EN.put("Р’РєР»СЋС‡РµРЅРЅС‹Рµ РјРѕРґСѓР»Рё", "Enabled modules");
        RU_TO_EN.put("Р’С‹РєР»СЋС‡РµРЅРЅС‹Рµ РјРѕРґСѓР»Рё", "Disabled modules");
        RU_TO_EN.put("РўРµРєСЃС‚ РІ Р»РѕРіРѕ", "Logo text");
        RU_TO_EN.put("РўРµРєСЃС‚ РІ С…СѓРґРµ", "HUD list text");
        RU_TO_EN.put("Р—Р°РіРѕР»РѕРІРєРё С…СѓРґР°", "HUD headers");
        RU_TO_EN.put("РЎР»Р°Р№РґРµСЂ", "Slider");
        RU_TO_EN.put("РљСЂСѓРі СЃР»Р°Р№РґРµСЂР°", "Slider knob");
        RU_TO_EN.put("РџРѕРґСЃРєР°Р·РєРё", "Tooltips");
        RU_TO_EN.put("Р¤РѕРЅ С…СѓРґР°", "HUD background");
        RU_TO_EN.put("РўРµРјС‹", "Themes");
        RU_TO_EN.put("РџР»Р°СЃС‚", "Plast");
        RU_TO_EN.put("РўСЂР°РїРєР°", "Trap");
        RU_TO_EN.put("РђРІС‚РѕРјР°С‚РёС‡РµСЃРєРё СЂРµСЃРїР°РІРЅРёС‚ РїРѕСЃР»Рµ СЃРјРµСЂС‚Рё", "Automatically respawns after death");
        RU_TO_EN.put("РЎРѕРѕР±С‰РµРЅРёРµ РїРѕСЃР»Рµ СЂРµСЃРїР°РІРЅР°", "Respawn message");
        RU_TO_EN.put("РўРµРєСЃС‚ СЃРѕРѕР±С‰РµРЅРёСЏ", "Message text");
        RU_TO_EN.put(
            "Р’РёР·СѓР°Р»СЊРЅРѕ РјРµРЅСЏРµС‚ РјРµСЃС‚Р°РјРё Р»РµРІСѓСЋ Рё РїСЂР°РІСѓСЋ СЂСѓРєСѓ РїРѕ РѕС‚РґРµР»СЊРЅРѕРјСѓ Р±РёРЅРґСѓ",
            "Visually swaps left and right hands using a separate bind"
        );
        RU_TO_EN.put("РљРЅРѕРїРєР° СЃРјРµРЅС‹ СЂСѓРє", "Hand swap key");
        RU_TO_EN.put("РђРІС‚РѕРјР°С‚РёС‡РµСЃРєРё РїРµСЂРµРїРѕРґРєР»СЋС‡Р°РµС‚СЃСЏ РїРѕСЃР»Рµ РєРёРєР°", "Automatically reconnects after a kick");
        RU_TO_EN.put("Р—Р°РґРµСЂР¶РєР° РїРµСЂРµР·Р°С…РѕРґР°", "Reconnect delay");
        RU_TO_EN.put(
            "РџРѕРєР°Р·С‹РІР°РµС‚ РёРЅС„РѕСЂРјР°С†РёСЋ Рѕ СЂРµР°Р»СЊРЅРѕ РїРѕРґРЅСЏС‚С‹С… РїСЂРµРґРјРµС‚Р°С…", "Shows info about actually picked up items"
        );
        RU_TO_EN.put("РўРѕР»СЊРєРѕ РґРѕРЅР°С‚РµСЂСЃРєРёРµ", "Donor items only");
        RU_TO_EN.put("Р”РµР»Р°РµС‚ РјРѕРґРµР»СЊ РёРіСЂРѕРєР° Р·Р°РјРµС‚РЅРѕ РјРµРЅСЊС€Рµ", "Makes the player model noticeably smaller");
        RU_TO_EN.put("Р”СЂСѓРіРёРµ РёРіСЂРѕРєРё", "Other players");
        RU_TO_EN.put("РЈР»СѓС‡С€РµРЅРёРµ С‡Р°С‚Р°", "Chat improvements");
        RU_TO_EN.put("РђРІС‚РѕРјР°С‚РёС‡РµСЃРєРёРµ СЃРѕРѕР±С‰РµРЅРёСЏ", "Automatic messages");
        RU_TO_EN.put("РђРІС‚РѕРјР°С‚РёС‡РµСЃРєРёР№ /event delay", "Automatic /event delay");
        RU_TO_EN.put("Р\u0098РІРµРЅС‚С‹ Рё С€Р°С…С‚С‹ FunTime", "FunTime events and mines");
        putEffectEn("speed", "Speed");
        putEffectEn("slowness", "Slowness");
        putEffectEn("haste", "Haste");
        putEffectEn("mining_fatigue", "Mining Fatigue");
        putEffectEn("strength", "Strength");
        putEffectEn("instant_health", "Instant Health");
        putEffectEn("instant_damage", "Instant Damage");
        putEffectEn("jump_boost", "Jump Boost");
        putEffectEn("nausea", "Nausea");
        putEffectEn("regeneration", "Regeneration");
        putEffectEn("resistance", "Resistance");
        putEffectEn("fire_resistance", "Fire Resistance");
        putEffectEn("water_breathing", "Water Breathing");
        putEffectEn("invisibility", "Invisibility");
        putEffectEn("blindness", "Blindness");
        putEffectEn("night_vision", "Night Vision");
        putEffectEn("hunger", "Hunger");
        putEffectEn("weakness", "Weakness");
        putEffectEn("poison", "Poison");
        putEffectEn("wither", "Wither");
        putEffectEn("health_boost", "Health Boost");
        putEffectEn("absorption", "Absorption");
        putEffectEn("saturation", "Saturation");
        putEffectEn("glowing", "Glowing");
        putEffectEn("levitation", "Levitation");
        putEffectEn("luck", "Luck");
        putEffectEn("unluck", "Bad Luck");
        putEffectEn("slow_falling", "Slow Falling");
        putEffectEn("conduit_power", "Conduit Power");
        putEffectEn("dolphins_grace", "Dolphin's Grace");
        putEffectEn("bad_omen", "Bad Omen");
        putEffectEn("hero_of_the_village", "Hero of the Village");
        putEffectEn("darkness", "Darkness");
        putEffectEn("trial_omen", "Trial Omen");
        putEffectEn("wind_charged", "Wind Charged");
        putEffectEn("weaving", "Weaving");
        putEffectEn("oozing", "Oozing");
        putEffectEn("infested", "Infested");
        putHudItemEn("ender_pearl", "Ender Pearl");
        putHudItemEn("chorus_fruit", "Chorus Fruit");
        putHudItemEn("shield", "Shield");
        putHudItemEn("totem_of_undying", "Totem of Undying");
        putHudItemEn("ender_eye", "Eye of Ender");
        putHudItemEn("snowball", "Snowball");
        putHudItemEn("egg", "Egg");
        putHudItemEn("splash_potion", "Splash Potion");
        putHudItemEn("lingering_potion", "Lingering Potion");
        putHudItemEn("experience_bottle", "Bottle o' Enchanting");
        putHudItemEn("trident", "Trident");
        putHudItemEn("crossbow", "Crossbow");
        putHudItemEn("goat_horn", "Goat Horn");
        putHudItemEn("wind_charge", "Wind Charge");
        putHudItemEn("mace", "Mace");
        putHudItemEn("sugar", "Sugar");
        putHudItemEn("netherite_scrap", "Netherite Scrap");
        putHudItemEn("dried_kelp", "Dried Kelp");
        putHudItemEn("phantom_membrane", "Phantom Membrane");
        RU_TO_EN.put("Приблизить", "Zoom in");
        RU_TO_EN.put("Показывает статус игры в Discord", "Shows game status in Discord");
        RU_TO_EN.put("Иконка у игроков с FluxVisuals", "FluxVisuals icon on players");
        RU_TO_EN.put("Размер GUI", "GUI size");
        RU_TO_EN.put("Бинд ClickGUI", "ClickGUI bind");
        RU_TO_EN.put("Акцентный цвет", "Accent color");
        RU_TO_EN.put("Глобальный чат", "Global chat");
        RU_TO_EN.put("Глобальный Чат", "Global chat");
        RU_TO_EN.put("Название группы...", "Group name...");
        RU_TO_EN.put("Заявки", "Requests");
        RU_TO_EN.put("Чаты", "Chats");
        RU_TO_EN.put("Группы", "Groups");
        RU_TO_EN.put("Нет заявок", "No requests");
        RU_TO_EN.put("Нет чатов", "No chats");
        RU_TO_EN.put("Нет групп", "No groups");
        RU_TO_EN.put("Ваш друг", "Your friend");
        RU_TO_EN.put("Друзья", "Friends");
        RU_TO_EN.put("Настройки", "Settings");
        RU_TO_EN.put("Создать", "Create");
        RU_TO_EN.put("Ник друга", "Friend nickname");
        RU_TO_EN.put("Статус", "Status");
        RU_TO_EN.put("Нет сообщений", "No messages");
        RU_TO_EN.put("Сообщение...", "Message...");
        RU_TO_EN.put("Мой профиль", "My profile");
        RU_TO_EN.put("Друзей:", "Friends:");
        RU_TO_EN.put("Групп:", "Groups:");
        RU_TO_EN.put("Заявок:", "Requests:");
        RU_TO_EN.put("Назад", "Back");
        RU_TO_EN.put("Мои координаты", "My coordinates");
        RU_TO_EN.put("Гифки", "GIFs");
        RU_TO_EN.put("Животные", "Animals");
        RU_TO_EN.put("Прочее", "Other");
        RU_TO_EN.put("Заявка отправлена", "Request sent");
        RU_TO_EN.put("Пользователь не найден", "User not found");
        RU_TO_EN.put("Блокировать ЛС", "Block DMs");
        RU_TO_EN.put("Скрывать мой статус", "Hide my status");
        RU_TO_EN.put("Скрывать сервер игры", "Hide game server");
        RU_TO_EN.put("Отображать музыку", "Display music");
        RU_TO_EN.put("Уведомления", "Notifications");
        RU_TO_EN.put("Личные сообщения", "Private messages");
        RU_TO_EN.put("Звук уведомлений", "Notification sound");
        RU_TO_EN.put("Блокировки", "Blocked users");
        RU_TO_EN.put("Оффлайн", "Offline");
        RU_TO_EN.put("Онлайн", "Online");
        RU_TO_EN.put("Играет на:", "Playing on:");
        RU_TO_EN.put("Был", "Was");
        RU_TO_EN.put("участников", "members");
        RU_TO_EN.put("ЛС:", "DM:");
        RU_TO_EN.put("двойной", "Double");
        RU_TO_EN.put("тройной", "Triple");
        RU_TO_EN.put("Автоматический респавн после смерти", "Automatic respawn after death");
        RU_TO_EN.put("автоматический респавн после смерти", "Automatic respawn after death");
        RU_TO_EN.put("Автоматический shift при ударе", "Automatic Shift on hit");
        RU_TO_EN.put("автоматический shift при ударе", "Automatic Shift on hit");
        RU_TO_EN.put("Автоматический клик", "Auto click");
        RU_TO_EN.put("автоматический клик", "Auto click");
        RU_TO_EN.put("Плавные анимации", "Smooth animations");
        RU_TO_EN.put("плавные анимации", "Smooth animations");
        RU_TO_EN.put("Показывать процент", "Show percentage");
        RU_TO_EN.put("показывать процент", "Show percentage");
        RU_TO_EN.put("Прозрачность контура", "Outline opacity");
        RU_TO_EN.put("прозрачность контура", "Outline opacity");
        RU_TO_EN.put("Показывать заливку", "Show fill");
        RU_TO_EN.put("показывать заливку", "Show fill");
        RU_TO_EN.put("Делает модель игрока меньше", "Makes the player model smaller");
        RU_TO_EN.put("делает модель игрока меньше", "Makes the player model smaller");
        RU_TO_EN.put("Обводка блоков", "Block outline");
        RU_TO_EN.put("обводка блоков", "Block outline");
        RU_TO_EN.put("Плащ с логотипом", "Logo cape");
        RU_TO_EN.put("плащ с логотипом", "Logo cape");
        RU_TO_EN.put("Визуально меняет руки местами", "Visually swaps hands");
        RU_TO_EN.put("визуально меняет руки местами", "Visually swaps hands");
        RU_TO_EN.put("Кастомные руки", "Custom hands");
        RU_TO_EN.put("кастомные руки", "Custom hands");
        RU_TO_EN.put("Красивые светлячки при ударе", "Pretty fireflies on hit");
        RU_TO_EN.put("красивые светлячки при ударе", "Pretty fireflies on hit");
        RU_TO_EN.put("Красивые шляпы", "Pretty hats");
        RU_TO_EN.put("красивые шляпы", "Pretty hats");
        RU_TO_EN.put("Счёт попаданий по игрокам", "Hit counter");
        RU_TO_EN.put("счёт попаданий по игрокам", "Hit counter");
        RU_TO_EN.put("Показывает радиус удара", "Shows hit range");
        RU_TO_EN.put("показывает радиус удара", "Shows hit range");
        RU_TO_EN.put("Красивая модель игроков", "Fancy player model");
        RU_TO_EN.put("красивая модель игроков", "Fancy player model");
        RU_TO_EN.put("Оставляет красивую копию игрока", "Leaves a fancy player copy");
        RU_TO_EN.put("оставляет красивую копию игрока", "Leaves a fancy player copy");
        RU_TO_EN.put("Анимации удара", "Hit animations");
        RU_TO_EN.put("анимации удара", "Hit animations");
        RU_TO_EN.put("Красивый визуал на таргете", "Fancy target visual");
        RU_TO_EN.put("красивый визуал на таргете", "Fancy target visual");
        RU_TO_EN.put("Элемент интерфейса", "Interface element");
        RU_TO_EN.put("элемент интерфейса", "Interface element");
        RU_TO_EN.put("Информация о таргете", "Target info");
        RU_TO_EN.put("информация о таргете", "Target info");
        RU_TO_EN.put("Настройка визуала мира", "World visual settings");
        RU_TO_EN.put("настройка визуала мира", "World visual settings");
        RU_TO_EN.put("Оставляет красивый след за собой", "Leaves a pretty trail");
        RU_TO_EN.put("оставляет красивый след за собой", "Leaves a pretty trail");
        RU_TO_EN.put("Ищет анархию с наименьшим онлайном", "Finds anarchy with the lowest online");
        RU_TO_EN.put("ищет анархию с наименьшим онлайном", "Finds anarchy with the lowest online");
        RU_TO_EN.put("Помощник с аукционом", "Auction helper");
        RU_TO_EN.put("помощник с аукционом", "Auction helper");
        RU_TO_EN.put("Автоматически перевыставляет вещи на аукционе", "Automatically relists items on the auction");
        RU_TO_EN.put("автоматически перевыставляет вещи на аукционе", "Automatically relists items on the auction");
        RU_TO_EN.put("Автоматически инвест монет", "Automatic coin invest");
        RU_TO_EN.put("автоматически инвест монет", "Automatic coin invest");
        RU_TO_EN.put("Показывает статус игры в", "Shows game status in");
        RU_TO_EN.put("показывает статус игры в", "Shows game status in");
        RU_TO_EN.put("Ставит метку", "Places a marker");
        RU_TO_EN.put("ставит метку", "Places a marker");
        RU_TO_EN.put("Иконка у игроков с", "Icon on players with");
        RU_TO_EN.put("иконка у игроков с", "Icon on players with");
        RU_TO_EN.put("Показывает информацию о поднятых предметах", "Shows info about picked up items");
        RU_TO_EN.put("показывает информацию о поднятых предметах", "Shows info about picked up items");
        RU_TO_EN.put("бинд", "Bind");
        RU_TO_EN.put("размер", "Size");
        RU_TO_EN.put("кастомный цвет", "Custom color");
        RU_TO_EN.put("режим покраски", "Paint mode");
        RU_TO_EN.put("оттенок", "Hue");
        RU_TO_EN.put("полная", "Full");
        RU_TO_EN.put("рендерить себя от 1 лица", "Render self in first person");
        RU_TO_EN.put("частицы", "Particles");
        RU_TO_EN.put("контур", "Outline");
        RU_TO_EN.put("интервал (мс)", "Interval (ms)");
        RU_TO_EN.put("кастомное время", "Custom time");
        RU_TO_EN.put("кастомная погода", "Custom weather");
        RU_TO_EN.put("туман", "Fog");
        RU_TO_EN.put("небо", "Sky");
        RU_TO_EN.put("цвет неба", "Sky color");
        RU_TO_EN.put("приблизить", "Zoom in");
        RU_TO_EN.put("кнопка поиска", "Search button");
        RU_TO_EN.put("авто точка", "Auto point");
        RU_TO_EN.put("порог монет", "Coin threshold");
        RU_TO_EN.put("сумма вложения", "Investment amount");
        RU_TO_EN.put("панель ивентов", "Events panel");
        RU_TO_EN.put("включение модуля", "Module enable");
        RU_TO_EN.put("выключение", "Disable");
        RU_TO_EN.put("эффекты", "Effects");
        RU_TO_EN.put("свап", "Swap");
        RU_TO_EN.put("время до ивента", "Time until event");
        RU_TO_EN.put("Автоматический shift при ударе", "Automatic Shift on hit");
        RU_TO_EN.put("Плащ с логотипом FluxVisuals", "Cape with FluxVisuals logo");
        RU_TO_EN.put("плащ с логотипом FluxVisuals", "Cape with FluxVisuals logo");
        RU_TO_EN.put("Тип свапа", "Swap type");
        RU_TO_EN.put("тип свапа", "Swap type");
        RU_TO_EN.put("Ищет анархию с наименьшем онлайном", "Finds anarchy with the lowest online");
        RU_TO_EN.put("ищет анархию с наименьшем онлайном", "Finds anarchy with the lowest online");
        RU_TO_EN.put("Порог монет", "Coin threshold");
        RU_TO_EN.put("Сумма вложения", "Investment amount");
        RU_TO_EN.put("Кнопка поиска", "Search button");
        RU_TO_EN.put("Авто точка", "Auto point");
        RU_TO_EN.put("Панель ивентов", "Events panel");
        RU_TO_EN.put("Включение модуля", "Module enable");
        RU_TO_EN.put("Выключение", "Disable");
        RU_TO_EN.put("Эффекты", "Effects");
        RU_TO_EN.put("Свап", "Swap");
        RU_TO_EN.put("Время до ивента", "Time until event");
        RU_TO_EN.put("Приблизить", "Zoom in");
        RU_TO_EN.put("Автоматически ест", "Automatically eats");
        RU_TO_EN.put("Автоматический спринт", "Automatic sprint");
        RU_TO_EN.put("Свапает предметы", "Swaps items");
        RU_TO_EN.put("Быстрое использование пузырьков опыта", "Fast experience bottle use");
        RU_TO_EN.put("Быстрое перекладывание предметов", "Quick item moving");
        RU_TO_EN.put("Запрещает выбрасывать предметы из выбранных слотов", "Prevents dropping items from selected slots");
        RU_TO_EN.put("Автоматический Shift при ударе", "Automatic Shift on hit");
        RU_TO_EN.put("Настройки ClickGUI", "ClickGUI settings");
        RU_TO_EN.put("Уведомление о низкой прочности брони", "Low armor durability alert");
        RU_TO_EN.put("Изменяет соотношение сторон экрана", "Changes the screen aspect ratio");
        RU_TO_EN.put("Кастомный прицел", "Custom crosshair");
        RU_TO_EN.put("Кастомный цвет хитбоксов", "Custom hitbox color");
        RU_TO_EN.put("Изменяет цвет сущности при получении урона", "Changes entity color when taking damage");
        RU_TO_EN.put("Настройка элементов интерфейса", "Interface elements settings");
        RU_TO_EN.put("Подсветка и увеличение предметов", "Item highlight and scaling");
        RU_TO_EN.put("Физика предметов", "Item physics");
        RU_TO_EN.put("Круг при прыжке", "Jump circle");
        RU_TO_EN.put("Эффекты при убийстве сущности", "Effects on entity kill");
        RU_TO_EN.put("Отображает свой никнейм от 3 лица", "Shows your nickname in third person");
        RU_TO_EN.put("Отключает рендер определённых предметов", "Disables rendering of certain items");
        RU_TO_EN.put("Кастомные партиклы", "Custom particles");
        RU_TO_EN.put("Рисует траекторию полёта снаряда", "Draws projectile trajectory");
        RU_TO_EN.put("Кастомный скорборд", "Custom scoreboard");
        RU_TO_EN.put("Предпросмотр шалкеров", "Shulker preview");
        RU_TO_EN.put("Элемент интерфейса, информация о таргете", "Interface element, target info");
        RU_TO_EN.put("Настройка отображения рук", "Hand display settings");
        RU_TO_EN.put("Волны при прыжке или ударе", "Waves on jump or hit");
        RU_TO_EN.put("Добавляет в мир бегущие линии", "Adds flowing lines to the world");
        RU_TO_EN.put("Приближение камеры", "Camera zoom");
        RU_TO_EN.put("Автоматически регистрируется и логинится", "Automatically registers and logs in");
        RU_TO_EN.put("Автоматические сообщения", "Automatic messages");
        RU_TO_EN.put("Автоматически переподключается после кика", "Automatically reconnects after kick");
        RU_TO_EN.put("Улучшение чата", "Chat enhancement");
        RU_TO_EN.put("Кастомные звуки", "Custom sounds");
        RU_TO_EN.put("Ивенты и шахты FunTime", "FunTime events and mines");
        RU_TO_EN.put("Спавнит фейкового игрока", "Spawns a fake player");
        RU_TO_EN.put("Свободная камера при удержании клавиши", "Free camera while holding a key");
        RU_TO_EN.put("Помощник для FunTime", "FunTime helper");
        RU_TO_EN.put("Автоматически заходит на режимы", "Automatically joins game modes");
        RU_TO_EN.put("Не позволяет писать капсом", "Prevents typing in caps");
        RU_TO_EN.put("Показывает уведомления на экране", "Shows notifications on screen");
        RU_TO_EN.put("Защита от выхода и команд в PVP", "Protection from logout and commands in PVP");
        RU_TO_EN.put("скрывает никнейм игрока", "hides the player's nickname");
        RU_TO_EN.put("отслеживает потерю тотема игроками", "tracks players' totem pops");
        RU_TO_EN.put("Режимы", "Modes");
        RU_TO_EN.put("Еда", "Food");
        RU_TO_EN.put("Инвиз", "Invis");
        RU_TO_EN.put("Отключает рендер определенных элементов", "Disables rendering of certain elements");
        RU_TO_EN.put("Рисует траекторию полёта снарядов", "Draws projectile trajectories");
        RU_TO_EN.put("Отслеживает потерю тотема игроками", "Tracks players' totem pops");
        RU_TO_EN.put("Скрывает никнейм игрока", "Hides the player's nickname");
        RU_TO_EN.put("Голод", "Hunger");
        RU_TO_EN.put("Слот", "Slot");
        RU_TO_EN.put("Двойной", "Double");
        RU_TO_EN.put("Тройной", "Triple");
        RU_TO_EN.put("Предмет 1", "Item 1");
        RU_TO_EN.put("Предмет 2", "Item 2");
        RU_TO_EN.put("Только зачар. тотемы", "Only enchanted totems");
        RU_TO_EN.put("Кнопка свапа", "Swap key");
        RU_TO_EN.put("Руки", "Hands");
        RU_TO_EN.put("Правая", "Right");
        RU_TO_EN.put("Левая", "Left");
        RU_TO_EN.put("Сила блюра", "Blur strength");
        RU_TO_EN.put("Оттенок блюра", "Blur hue");
        RU_TO_EN.put("Порог (%)", "Threshold (%)");
        RU_TO_EN.put("Скорость вращения", "Rotation speed");
        RU_TO_EN.put("Отображать на других", "Show on others");
        RU_TO_EN.put("Таб", "Tab");
        RU_TO_EN.put("Смена перспективы", "Perspective switch");
        RU_TO_EN.put("Открытие инвентаря", "Inventory opening");
        RU_TO_EN.put("Смена слотов", "Slot switching");
        RU_TO_EN.put("Открыть редактор", "Open editor");
        RU_TO_EN.put("Кастомный цвет", "Custom color");
        RU_TO_EN.put("Кнопка смены рук", "Hand swap key");
        RU_TO_EN.put("Белый", "White");
        RU_TO_EN.put("Серый", "Gray");
        RU_TO_EN.put("Только я", "Only me");
        RU_TO_EN.put("Высота", "Height");
        RU_TO_EN.put("Скрывать мои попадания", "Hide my hits");
        RU_TO_EN.put("Рендерить себя от 1 лица", "Render self in first person");
        RU_TO_EN.put("Скрывать если нету попаданий", "Hide if there are no hits");
        RU_TO_EN.put("Режим заливки", "Fill mode");
        RU_TO_EN.put("Обычная", "Normal");
        RU_TO_EN.put("Прозрачность шейдера", "Shader opacity");
        RU_TO_EN.put("Выберите режим", "Select mode");
        RU_TO_EN.put("Скор. вращения", "Rotation speed");
        RU_TO_EN.put("Лого", "Logo");
        RU_TO_EN.put("Хоткеи", "Hotkeys");
        RU_TO_EN.put("Зелья", "Potions");
        RU_TO_EN.put("Кулдауны", "Cooldowns");
        RU_TO_EN.put("Координаты", "Coordinates");
        RU_TO_EN.put("Экипировка", "Equipment");
        RU_TO_EN.put("Предметы FT", "FT items");
        RU_TO_EN.put("Медиаплеер", "Media player");
        RU_TO_EN.put("Закругление", "Rounding");
        RU_TO_EN.put("Пред. трек", "Prev. track");
        RU_TO_EN.put("След. трек", "Next track");
        RU_TO_EN.put("Пауза", "Pause");
        RU_TO_EN.put("Режим покраски", "Paint mode");
        RU_TO_EN.put("Оттенок", "Hue");
        RU_TO_EN.put("Полная", "Full");
        RU_TO_EN.put("Красить броню", "Color armor");
        RU_TO_EN.put("Мобы", "Mobs");
        RU_TO_EN.put("Молния", "Lightning");
        RU_TO_EN.put("Разрушение", "Destruction");
        RU_TO_EN.put("Частицы", "Particles");
        RU_TO_EN.put("Огонь", "Fire");
        RU_TO_EN.put("Сердца", "Hearts");
        RU_TO_EN.put("Облако", "Cloud");
        RU_TO_EN.put("Нет", "No");
        RU_TO_EN.put("Эндер", "Ender");
        RU_TO_EN.put("Подсветка слотов", "Slot highlight");
        RU_TO_EN.put("Увеличение предметов", "Item scaling");
        RU_TO_EN.put("Пульсация", "Pulse");
        RU_TO_EN.put("Фон", "Background");
        RU_TO_EN.put("Круг", "Circle");
        RU_TO_EN.put("Квадрат", "Square");
        RU_TO_EN.put("Подсветка в инвентаре", "Inventory highlight");
        RU_TO_EN.put("Чарка", "Enchanted G.A");
        RU_TO_EN.put("Золотое яблоко", "Golden Apple");
        RU_TO_EN.put("Эндер-жемчуг", "Ender Pearl");
        RU_TO_EN.put("Плод хоруса", "Chorus Fruit");
        RU_TO_EN.put("Тотем", "Totem");
        RU_TO_EN.put("Снежок", "Snowball");
        RU_TO_EN.put("Пузырек опыта", "Experience Bottle");
        RU_TO_EN.put("Звёздочки", "Stars");
        RU_TO_EN.put("Сердечко", "Heart");
        RU_TO_EN.put("Снежинки", "Snowflakes");
        RU_TO_EN.put("Доллар", "Dollar");
        RU_TO_EN.put("Корона", "Crown");
        RU_TO_EN.put("Череп", "Skull");
        RU_TO_EN.put("Крест", "Cross");
        RU_TO_EN.put("Куб", "Cube");
        RU_TO_EN.put("Рандом", "Random");
        RU_TO_EN.put("Разноцветные", "Multicolored");
        RU_TO_EN.put("Логика полёта", "Flight logic");
        RU_TO_EN.put("Свободный полёт", "Free flight");
        RU_TO_EN.put("Взлёт", "Takeoff");
        RU_TO_EN.put("Падение", "Fall");
        RU_TO_EN.put("Когда применять", "When to apply");
        RU_TO_EN.put("Удар", "Hit");
        RU_TO_EN.put("Бег", "Run");
        RU_TO_EN.put("В мире", "In world");
        RU_TO_EN.put("Прыжок", "Jump");
        RU_TO_EN.put("Перка", "Ender Pearl");
        RU_TO_EN.put("Время жизни", "Lifetime");
        RU_TO_EN.put("Логика", "Logic");
        RU_TO_EN.put("Размер круга", "Circle size");
        RU_TO_EN.put("Кол-во при атаке", "Amount on attack");
        RU_TO_EN.put("Кол-во при прыжке", "Amount on jump");
        RU_TO_EN.put("Кол-во в мире", "Amount in world");
        RU_TO_EN.put("Кол-во при беге", "Amount on run");
        RU_TO_EN.put("Кол-во при тотеме", "Amount on totem");
        RU_TO_EN.put("Тыква", "Pumpkin");
        RU_TO_EN.put("Таблица", "Table");
        RU_TO_EN.put("Ники", "Nicknames");
        RU_TO_EN.put("Тряска", "Shake");
        RU_TO_EN.put("Интервал (мс)", "Interval (ms)");
        RU_TO_EN.put("Двойной", "Double");
        RU_TO_EN.put("Тройной", "Triple");
        RU_TO_EN.put("Слот 1", "Slot 1");
        RU_TO_EN.put("Слот 2", "Slot 2");
        RU_TO_EN.put("Слот 3", "Slot 3");
        RU_TO_EN.put("Слот 4", "Slot 4");
        RU_TO_EN.put("Слот 5", "Slot 5");
        RU_TO_EN.put("Слот 6", "Slot 6");
        RU_TO_EN.put("Слот 7", "Slot 7");
        RU_TO_EN.put("Слот 8", "Slot 8");
        RU_TO_EN.put("Слот 9", "Slot 9");
        RU_TO_EN.put("Сообщение после респавна", "Message after respawn");
        RU_TO_EN.put("Текст сообщения", "Message text");
        RU_TO_EN.put("Кнопка", "Button");
        RU_TO_EN.put("Задержка (сек)", "Delay (sec)");
        RU_TO_EN.put("Эндер пёрл", "Ender Pearl");
        RU_TO_EN.put("Стрела", "Arrow");
        RU_TO_EN.put("Трезубец", "Trident");
        RU_TO_EN.put("Ромб", "Diamond");
        RU_TO_EN.put("Квадрат 2", "Square 2");
        RU_TO_EN.put("Призраки", "Ghosts");
        RU_TO_EN.put("Кристаллы", "Crystals");
        RU_TO_EN.put("Кнопка просмотра", "View key");
        RU_TO_EN.put("Игроки", "Players");
        RU_TO_EN.put("Только при наводке", "Only on hover");
        RU_TO_EN.put("Броня", "Armor");
        RU_TO_EN.put("Предметы", "Items");
        RU_TO_EN.put("Стиль", "Style");
        RU_TO_EN.put("Красить иконку при ударе", "Color the icon on hit");
        RU_TO_EN.put("Свой цвет удара", "Custom hit color");
        RU_TO_EN.put("Цвет удара", "Hit color");
        RU_TO_EN.put("Менять при ударе", "Change on hit");
        RU_TO_EN.put("Менять на", "Change to");
        RU_TO_EN.put("При наводке", "On hover");
        RU_TO_EN.put("Сортировка", "Sorting");
        RU_TO_EN.put("Дистанция", "Distance");
        RU_TO_EN.put("Таргет", "Target");
        RU_TO_EN.put("Количество кристаллов", "Crystal count");
        RU_TO_EN.put("Отображать на:", "Show on:");
        RU_TO_EN.put("Дальность", "Range");
        RU_TO_EN.put("Линия", "Line");
        RU_TO_EN.put("Хвост", "Tail");
        RU_TO_EN.put("Длина", "Length");
        RU_TO_EN.put("Кастомное время", "Custom time");
        RU_TO_EN.put("Время", "Time");
        RU_TO_EN.put("Кастомная погода", "Custom weather");
        RU_TO_EN.put("Облака", "Clouds");
        RU_TO_EN.put("Туман", "Fog");
        RU_TO_EN.put("Небо", "Sky");
        RU_TO_EN.put("Правая рука", "Right hand");
        RU_TO_EN.put("Координата X", "Coordinate X");
        RU_TO_EN.put("Координата Y", "Coordinate Y");
        RU_TO_EN.put("Координата Z", "Coordinate Z");
        RU_TO_EN.put("Левая рука", "Left hand");
        RU_TO_EN.put("Режим:", "Mode:");
        RU_TO_EN.put("После убийства", "After kill");
        RU_TO_EN.put("По времени", "By time");
        RU_TO_EN.put("Текст после убийства:", "Text after kill:");
        RU_TO_EN.put("Частота отправки (в сек):", "Send frequency (sec):");
        RU_TO_EN.put("Текст:", "Text:");
        RU_TO_EN.put("Пароль", "Password");
        RU_TO_EN.put("Задержка перезахода", "Reconnect delay");
        RU_TO_EN.put("Анти-Спам", "Anti-spam");
        RU_TO_EN.put("Сохранять историю", "Save history");
        RU_TO_EN.put("Исправление команд", "Command correction");
        RU_TO_EN.put("Громкость", "Volume");
        RU_TO_EN.put("Включение/выключение модулей", "Module toggle sounds");
        RU_TO_EN.put("Выберите режим:", "Select mode:");
        RU_TO_EN.put("Крит", "Crit");
        RU_TO_EN.put("Режим крита", "Crit mode");
        RU_TO_EN.put("В броне", "In armor");
        RU_TO_EN.put("С тотемом", "With totem");
        RU_TO_EN.put("Введите имя", "Enter name");
        RU_TO_EN.put("Кнопка активации", "Activation key");
        RU_TO_EN.put("Время жизни: (в сек)", "Lifetime: (sec)");
        RU_TO_EN.put("Кнопка дезориентации", "Disorientation key");
        RU_TO_EN.put("Кнопка трапки", "Trap key");
        RU_TO_EN.put("Кнопка явной пыли", "Obvious dust key");
        RU_TO_EN.put("Кнопка божьей ауры", "God aura key");
        RU_TO_EN.put("Кнопка пласта", "Plast key");
        RU_TO_EN.put("Рисовать радиус", "Draw radius");
        RU_TO_EN.put("Дуэли", "Duels");
        RU_TO_EN.put("Только донатерские", "Donor only");
        RU_TO_EN.put("Новое имя", "New name");
        RU_TO_EN.put("Анархия", "Anarchy");
        RU_TO_EN.put("Название", "Name");
        RU_TO_EN.put("создать", "Create");
        RU_TO_EN.put("Автор", "Author");
        RU_TO_EN.put("Иконка", "Icon");
        RU_TO_EN.put("Текст", "Text");
        RU_TO_EN.put("Ваши метки", "Your markers");
        RU_TO_EN.put("Фон иконок", "Icon background");
        RU_TO_EN.put("Незеритовая броня", "Netherite Armor");
        RU_TO_EN.put("Алмазная броня", "Diamond Armor");
        RU_TO_EN.put("Цвет HP", "HP Color");
        RU_TO_EN.put("Цвет неба", "Sky Color");
        RU_TO_EN.put("Приблизить", "Zoom");
        RU_TO_EN.put("Приблизить:", "Zoom:");
        RU_TO_EN.put("Приблизить: ", "Zoom: ");
        RU_TO_EN.put("Двойной", "Double");
        RU_TO_EN.put("Тройной", "Triple");
        RU_TO_EN.put("Текст после убийства:", "Text After Kill:");
        RU_TO_EN.put("Текст после убийства: ", "Text After Kill: ");
        RU_TO_EN.put("Частота отправки (в сек):", "Send Frequency (sec):");
        RU_TO_EN.put("Частота отправки (в сек): ", "Send Frequency (sec): ");
        RU_TO_EN.put("Текст: ", "Text: ");
        RU_TO_EN.put("Выберите режим:", "Select Mode:");
        RU_TO_EN.put("Выберите режим: ", "Select Mode: ");
        RU_TO_EN.put("Время жизни: (сек)", "Lifetime: (sec)");
        RU_TO_EN.put("Взрывы", "Explosions");
        RU_TO_EN.put("Сбросить статистику", "Reset Stats");
        RU_TO_EN.put("Сбросить статистику:", "Reset Stats:");
        RU_TO_EN.put("Сбросить статистику: ", "Reset Stats: ");
        RU_TO_EN.put("Пауза/Плей", "Pause/Play");
        RU_TO_EN.put("Звук", "Sound");
        RU_TO_EN.put("Сила свечения", "Glow Strength");
        RU_TO_EN.put("Первое лицо", "First Person");
        RU_TO_EN.put("Скрывать от 1-го лица", "Hide in 1st Person");
        RU_TO_EN.put("Жидкое стекло", "Liquid Glass");
        RU_TO_EN.put("Сила волны", "Wave Strength");
        RU_TO_EN.put("Радиус волны", "Wave Radius");
        RU_TO_EN.put("Время волны", "Wave Duration");
        RU_TO_EN.put("Время жизни", "Lifetime");
        RU_TO_EN.put("Размер стекла", "Glass Size");
        RU_TO_EN.put("Сила искажения", "Distortion Strength");
        RU_TO_EN.put("Дальность", "Range");
        RU_TO_EN.put("Заливка", "Fill");
        RU_TO_EN.put("Прозрачность заливки", "Fill Opacity");
        RU_TO_EN.put("Шейдер", "Shader");
        RU_TO_EN.put("Режим шейдера", "Shader Mode");
        RU_TO_EN.put("Скорость шейдера", "Shader Speed");
        RU_TO_EN.put("Паровое свечение", "Vapor Glow");
        RU_TO_EN.put("No modes", "No modes");
    }
}
