package ru.fluxvisuals.ui.gui.clickgui.setting;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

import java.util.function.Supplier;

public class TextButtonSetting extends Setting {
    public String text;
    public int min;
    public int max;
    public boolean onlyDigits;
    public boolean password;
    public Runnable runnable;
    public Translation buttonName;

    public TextButtonSetting(Translation translation, Translation class254Var2) {
        super(translation, class254Var2);
        this.min = 0;
        this.max = Integer.MAX_VALUE;
        this.onlyDigits = false;
        this.password = false;
    }

    public TextButtonSetting(Translation translation) {
        this(translation, null);
    }

    public TextButtonSetting visible(Supplier<Boolean> supplier) {
        setVisible(supplier);
        return this;
    }

    public String getText() {
        return this.text;
    }

    public int getMin() {
        return this.min;
    }

    public int getMax() {
        return this.max;
    }

    public boolean isOnlyDigits() {
        return this.onlyDigits;
    }

    public boolean isPassword() {
        return this.password;
    }

    public Runnable getRunnable() {
        return this.runnable;
    }

    public Translation getButtonName() {
        return this.buttonName;
    }

    public TextButtonSetting setText(String str) {
        this.text = str;
        return this;
    }

    public TextButtonSetting setMin(int i) {
        this.min = i;
        return this;
    }

    public TextButtonSetting setMax(int i) {
        this.max = i;
        return this;
    }

    public TextButtonSetting setOnlyDigits(boolean z) {
        this.onlyDigits = z;
        return this;
    }

    public TextButtonSetting setPassword(boolean z) {
        this.password = z;
        return this;
    }

    public TextButtonSetting setRunnable(Runnable runnable) {
        this.runnable = runnable;
        return this;
    }

    public TextButtonSetting setButtonName(Translation translation) {
        this.buttonName = translation;
        return this;
    }
}
