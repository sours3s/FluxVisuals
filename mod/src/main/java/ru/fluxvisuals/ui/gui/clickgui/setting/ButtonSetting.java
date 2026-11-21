package ru.fluxvisuals.ui.gui.clickgui.setting;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

import java.util.function.Supplier;

public class ButtonSetting extends Setting {
    public Runnable runnable;
    public Translation buttonName;

    public ButtonSetting(Translation translation, Translation class254Var2) {
        super(translation, class254Var2);
    }

    public ButtonSetting(Translation translation) {
        this(translation, null);
    }

    public ButtonSetting visible(Supplier<Boolean> supplier) {
        setVisible(supplier);
        return this;
    }

    public Runnable getRunnable() {
        return this.runnable;
    }

    public Translation getButtonName() {
        return this.buttonName;
    }

    public ButtonSetting setRunnable(Runnable runnable) {
        this.runnable = runnable;
        return this;
    }

    public ButtonSetting setButtonName(Translation translation) {
        this.buttonName = translation;
        return this;
    }
}
