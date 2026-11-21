package ru.fluxvisuals.ui.gui.clickgui.externall.lang;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class Lang {

    public enum Language { RUSSIAN, ENGLISH }

    private static final Path LANG_FILE = Path.of("C:/Exclusive/client1_21_11/language.txt");

    private static final Map<String, String> DICT = Translations.build();

    private static Language current = readSavedLanguage();

    private Lang() {}

    public static void init() {
        System.out.println("[Lang] переводов в словаре: " + DICT.size() + ", язык: " + tag());
    }

    public static Language getLanguage() {
        return current;
    }

    public static boolean isEnglish() {
        return current == Language.ENGLISH;
    }

    public static String tag() {
        return current == Language.ENGLISH ? "EN" : "RU";
    }

    public static void setLanguage(Language language) {
        if (language == null || language == current) return;
        current = language;
        saveLanguage();
    }

    public static void toggle() {
        setLanguage(current == Language.ENGLISH ? Language.RUSSIAN : Language.ENGLISH);
    }

    public static void reloadDict() {  }

    public static String tr(String ru) {
        if (ru == null || current != Language.ENGLISH) return ru;
        String en = DICT.get(ru);
        return en != null ? en : ru;
    }

    public static String pick(String ru, String en) {
        return current == Language.ENGLISH ? en : ru;
    }

    private static Language readSavedLanguage() {
        try {
            if (Files.exists(LANG_FILE)) {
                String s = Files.readString(LANG_FILE, StandardCharsets.UTF_8).trim();
                if (s.equalsIgnoreCase("EN") || s.equalsIgnoreCase("ENGLISH")) {
                    return Language.ENGLISH;
                }
            }
        } catch (Exception ignored) {}
        return Language.RUSSIAN;
    }

    private static void saveLanguage() {
        try {
            Files.createDirectories(LANG_FILE.getParent());
            Files.writeString(LANG_FILE, current == Language.ENGLISH ? "EN" : "RU", StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
    }
}

