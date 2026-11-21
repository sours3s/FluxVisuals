package ru.fluxvisuals.ui.fluxgui.util.language;

import ru.fluxvisuals.ui.fluxgui.module.Category;

/** Минимальная языковая поддержка ClickGUI FluxVisuals. */
public final class LanguageManager {
    private static boolean english = false;

    private LanguageManager() {
    }

    public static boolean isEnglish() {
        return english;
    }

    public static void toggle() {
        english = !english;
    }

    public static String translateModuleDescription(String description) {
        return description;
    }

    public static String translateCategoryName(Category category) {
        return category.getName();
    }

    public static String translateForCategory(Category category, String text) {
        return repairIrcMojibake(text);
    }

    public static String translateIrc(String text) {
        return repairIrcMojibake(text);
    }

    /** Восстанавливает русский текст, если он был прочитан как CP1251 → UTF-8. */
    public static String repairIrcMojibake(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        boolean looksMojibake = text.indexOf('Р') >= 0 || text.indexOf('С') >= 0 || text.indexOf('â') >= 0;
        if (!looksMojibake) {
            return text;
        }
        try {
            byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
            String fixed = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            int good = 0;
            for (int i = 0; i < fixed.length(); i++) {
                char c = fixed.charAt(i);
                if (c == 'А' || c == 'а' || c == 'Б' || c == 'б' || c == 'В' || c == 'в' || c == ' ' || c == '-') {
                    good++;
                }
            }
            return good > 0 ? fixed : text;
        } catch (Exception ignored) {
            return text;
        }
    }
}
