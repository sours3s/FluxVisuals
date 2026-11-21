package ru.fluxvisuals.ui.gui.clickgui.util;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TranslationRegistry {
    public static final List<Translation> translations = new ArrayList();

    public TranslationRegistry() {
    }

    public static void register(Translation translation) {
        translations.add(translation);
    }

    public static List<Translation> getAll() {
        return Collections.unmodifiableList(translations);
    }
}
