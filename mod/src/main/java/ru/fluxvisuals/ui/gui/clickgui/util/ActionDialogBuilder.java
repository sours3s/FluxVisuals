package ru.fluxvisuals.ui.gui.clickgui.util;
import ru.fluxvisuals.ui.gui.clickgui.util.model.ActionDialogData;
import ru.fluxvisuals.ui.gui.clickgui.FrameElementColumn;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class ActionDialogBuilder {
    public Translation titleText;
    public Translation descriptionText;
    public Translation confirmLabelText;
    public GlTexture iconTexture;
    public boolean changePlaceholderColor;
    public int placeholderColorValue;
    public Translation placeholderTextValue;
    public Consumer<FrameElementColumn> contentConfigurator;
    public BooleanSupplier enableConfirmSupplier;
    public float dialogWidth = 320.0f;

    public Runnable onConfirmAction;

    public ActionDialogBuilder title(Translation translation) {
        this.titleText = translation;
        return this;
    }

    public ActionDialogBuilder title(String str) {
        this.titleText = Translation.clearText(str);
        return this;
    }

    public ActionDialogBuilder description(Translation translation) {
        this.descriptionText = translation;
        return this;
    }

    public ActionDialogBuilder description(String str) {
        this.descriptionText = Translation.clearText(str);
        return this;
    }

    public ActionDialogBuilder confirmLabel(Translation translation) {
        this.confirmLabelText = translation;
        return this;
    }

    public ActionDialogBuilder confirmLabel(String str) {
        this.confirmLabelText = Translation.clearText(str);
        return this;
    }

    public ActionDialogBuilder icon(GlTexture glTexture) {
        this.iconTexture = glTexture;
        return this;
    }

    public ActionDialogBuilder placeholderColor(int i) {
        this.changePlaceholderColor = true;
        this.placeholderColorValue = i;
        return this;
    }

    public ActionDialogBuilder placeholderText(Translation translation) {
        this.placeholderTextValue = translation;
        return this;
    }

    public ActionDialogBuilder placeholderText(String str) {
        this.placeholderTextValue = Translation.clearText(str);
        return this;
    }

    public ActionDialogBuilder content(Consumer<FrameElementColumn> consumer) {
        this.contentConfigurator = consumer;
        return this;
    }

    public ActionDialogBuilder enableConfirmButton(BooleanSupplier booleanSupplier) {
        this.enableConfirmSupplier = booleanSupplier;
        return this;
    }

    public ActionDialogBuilder width(float f) {
        this.dialogWidth = f;
        return this;
    }

    public ActionDialogBuilder onConfirm(Runnable runnable) {
        this.onConfirmAction = runnable;
        return this;
    }

    public ActionDialogData build() {
        return new ActionDialogData(this.titleText, this.descriptionText, this.confirmLabelText, this.iconTexture, this.changePlaceholderColor, this.placeholderColorValue, this.placeholderTextValue, this.contentConfigurator, this.enableConfirmSupplier, this.dialogWidth, this.onConfirmAction);
    }
}
