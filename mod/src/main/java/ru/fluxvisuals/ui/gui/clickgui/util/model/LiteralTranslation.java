package ru.fluxvisuals.ui.gui.clickgui.util.model;
import ru.fluxvisuals.ui.gui.clickgui.util.StringLookup;
import ru.fluxvisuals.ui.gui.clickgui.util.StringUtil;

public class LiteralTranslation implements Translation {
    String firstLetterUppercase;
    final String original;

    LiteralTranslation(String str) {
        this.original = str;
    }

    @Override
    public void lookupFromDictionary(StringLookup stringLookup) {
        this.firstLetterUppercase = StringUtil.firstLetterUppercase(this.original);
    }

    @Override
    public String original() {
        return this.original;
    }

    @Override
    public String effective() {
        return this.original;
    }

    @Override
    public String firstLetterUppercase() {
        return this.firstLetterUppercase;
    }
}
