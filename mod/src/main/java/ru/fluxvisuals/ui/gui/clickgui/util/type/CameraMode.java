package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum CameraMode implements DisplayNamed {
    ZOOM(Translation.clearText("Zoom")),
    PERSPECTIVE(Translation.clearText("Perspective"));

    final Translation displayName;

    CameraMode(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
