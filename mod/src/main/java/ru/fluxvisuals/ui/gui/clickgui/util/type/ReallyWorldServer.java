package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum ReallyWorldServer implements DisplayNamed {
    REALLYWORLD(Translation.clearText("ReallyWorld"));

    public final Translation displayName;

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    public Translation displayName() {
        return this.displayName;
    }

    ReallyWorldServer(Translation translation) {
        this.displayName = translation;
    }
}
