package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.ArgumentParser;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.TranslatedException;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

import java.util.Collections;
import java.util.List;

public class IntegerArgumentType implements ArgumentParser<Integer> {
    public final Integer min;
    public final Integer max;

    public IntegerArgumentType() {
        this(null, null);
    }

    public IntegerArgumentType(Integer num, Integer num2) {
        this.min = num;
        this.max = num2;
    }

    @Override
    public Integer parse(String str) throws TranslatedException {
        try {
            int i = Integer.parseInt(str);
            if (this.min != null && i < this.min.intValue()) {
                throw new TranslatedException(Translation.clearText(Lang.TYPE_MIN_VALUE.effective().replace("{min}", String.valueOf(this.min))));
            }
            if (this.max == null || i <= this.max.intValue()) {
                return Integer.valueOf(i);
            }
            throw new TranslatedException(Translation.clearText(Lang.TYPE_MAX_VALUE.effective().replace("{max}", String.valueOf(this.max))));
        } catch (NumberFormatException e) {
            throw new TranslatedException(Translation.clearText(Lang.TYPE_INVALID_INTEGER.effective().replace("{input}", str)));
        }
    }

    @Override
    public List<String> getSuggestions(String str) {
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return "integer";
    }
}
