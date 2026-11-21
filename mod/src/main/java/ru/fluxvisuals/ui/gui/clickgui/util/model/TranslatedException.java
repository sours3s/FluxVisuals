package ru.fluxvisuals.ui.gui.clickgui.util.model;

public class TranslatedException extends RuntimeException {
    public final Translation text;

    public TranslatedException(Translation translation) {
        super(translation.effective());
        this.text = translation;
    }

    public TranslatedException(Translation translation, Throwable th) {
        super(translation.effective(), th);
        this.text = translation;
    }

    public Translation getText() {
        return this.text;
    }

    @Override
    public String getMessage() {
        return this.text.effective();
    }
}
