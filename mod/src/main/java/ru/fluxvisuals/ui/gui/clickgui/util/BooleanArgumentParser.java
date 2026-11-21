package ru.fluxvisuals.ui.gui.clickgui.util;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.TranslatedException;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BooleanArgumentParser implements ArgumentParser<Boolean> {
    public static final List<String> trueValues = Arrays.asList("true", "yes", "on", "1");
    public static final List<String> falseValues = Arrays.asList("false", "no", "off", "0");

    @Override
    public Boolean parse(String str) throws TranslatedException {
        String lowerCase = str.toLowerCase();
        if (trueValues.contains(lowerCase)) {
            return true;
        }
        if (falseValues.contains(lowerCase)) {
            return false;
        }
        throw new TranslatedException(Translation.clearText(Lang.TYPE_INVALID_BOOLEAN.effective().replace("{input}", str)));
    }

    @Override
    public List<String> getSuggestions(String str) {
        String lowerCase = str.toLowerCase();
        return (List) Stream.of(new String[]{"true", "false"}).filter(str2 -> {
            return str2.startsWith(lowerCase);
        }).collect(Collectors.toList());
    }

    @Override
    public String getName() {
        return "boolean";
    }
}
