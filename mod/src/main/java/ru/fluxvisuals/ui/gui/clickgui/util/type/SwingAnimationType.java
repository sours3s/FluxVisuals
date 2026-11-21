package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum SwingAnimationType implements DisplayNamed {
    TYPE_1(Translation.clearText("Type 1")),
    TYPE_2(Translation.clearText("Type 2")),
    TYPE_3(Translation.clearText("Type 3")),
    TYPE_4(Translation.clearText("Type 4")),
    TYPE_5(Translation.clearText("Type 5"));

    final Translation displayName;

    SwingAnimationType(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
